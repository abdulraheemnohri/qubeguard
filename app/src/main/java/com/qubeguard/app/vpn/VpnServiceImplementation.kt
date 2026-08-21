package com.qubeguard.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.qubeguard.app.data.blocklist.DeterministicBlocker
import com.qubeguard.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import javax.inject.Inject

@AndroidEntryPoint
class VpnServiceImplementation : VpnService() {
    @Inject lateinit var deterministicBlocker: DeterministicBlocker
    private lateinit var vpnThread: Thread
    @Volatile private var isRunning = false
    private var vpnInterface: ParcelFileDescriptor? = null
    private lateinit var datagramChannel: DatagramChannel
    private val scope = CoroutineScope(Dispatchers.IO)
    private val dnsProxyPort = 5353
    private val upstreamDnsServer = "1.1.1.1"
    private val upstreamDnsPort = 53

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) startVpn()
        return START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun startVpn() {
        if (isRunning) return
        startForeground(NOTIFICATION_ID, createNotification())
        vpnInterface = Builder()
            .setSession("QubeGuard VPN")
            .addAddress("10.0.0.2", 24)
            .addDnsServer("10.0.0.2")
            .addRoute("0.0.0.0", 0)
            .setMtu(1500)
            .establish()

        vpnThread = Thread {
            try {
                datagramChannel = DatagramChannel.open()
                datagramChannel.bind(InetSocketAddress(dnsProxyPort))
                datagramChannel.configureBlocking(false)
                isRunning = true
                processDnsRequests()
            } catch (_: Exception) {
                isRunning = false
            }
        }.also { it.start() }
    }

    private fun stopVpn() {
        isRunning = false
        if (::datagramChannel.isInitialized) runCatching { datagramChannel.close() }
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        if (::vpnThread.isInitialized) runCatching { vpnThread.join(1000) }
    }

    private fun processDnsRequests() {
        while (isRunning) {
            try {
                val buffer = ByteBuffer.allocate(4096)
                val clientAddress = datagramChannel.receive(buffer) ?: continue
                buffer.flip()
                val queryBytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                val request = DnsRequest.parse(ByteBuffer.wrap(queryBytes))
                scope.launch {
                    val response = if (deterministicBlocker.isBlocked(request.domain)) {
                        DnsResponse.createNxDomainResponse(request, queryBytes)
                    } else {
                        forwardQuery(queryBytes)
                    }
                    if (response.isNotEmpty()) {
                        datagramChannel.send(ByteBuffer.wrap(response), clientAddress)
                    }
                }
            } catch (_: Exception) {
                if (!isRunning) break
            }
        }
    }

    private fun forwardQuery(query: ByteArray): ByteArray {
        return try {
            DatagramChannel.open().use { upstream ->
                val address = InetSocketAddress(upstreamDnsServer, upstreamDnsPort)
                upstream.configureBlocking(true)
                upstream.socket().soTimeout = 3000
                upstream.send(ByteBuffer.wrap(query), address)
                val response = ByteBuffer.allocate(4096)
                upstream.receive(response)
                response.flip()
                ByteArray(response.remaining()).also { response.get(it) }
            }
        } catch (_: Exception) {
            ByteArray(0)
        }
    }

    private fun createNotification(): Notification {
        val channelId = "QubeGuard.VPN"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channelId, "QubeGuard VPN", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, channelId)
            .setContentTitle("QubeGuard VPN")
            .setContentText("Protecting your privacy")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .build()
    }

    private data class DnsRequest(val id: Int, val domain: String) {
        companion object {
            fun parse(buffer: ByteBuffer): DnsRequest {
                require(buffer.remaining() >= 12) { "DNS header too short" }
                val id = buffer.short.toInt() and 0xFFFF
                buffer.position(12)
                val labels = mutableListOf<String>()
                while (buffer.hasRemaining()) {
                    val length = buffer.get().toInt() and 0xFF
                    if (length == 0) break
                    require(length <= 63 && length <= buffer.remaining()) { "Invalid label length" }
                    val bytes = ByteArray(length)
                    buffer.get(bytes)
                    labels += String(bytes, Charsets.UTF_8)
                }
                return DnsRequest(id, labels.joinToString(".").lowercase())
            }
        }
    }

    private object DnsResponse {
        fun createNxDomainResponse(request: DnsRequest, query: ByteArray): ByteArray {
            val response = query.copyOf()
            // Transaction ID
            response[0] = (request.id ushr 8).toByte()
            response[1] = request.id.toByte()
            // Flags: QR=1 (response), Opcode=0, AA=1, TC=0, RD=1 -> 0x85
            response[2] = 0x85.toByte()
            // Flags: RA=1, Z=0, RCODE=3 (NXDomain) -> 0x83
            response[3] = 0x83.toByte()
            return response
        }
    }

    companion object { private const val NOTIFICATION_ID = 1 }
}
