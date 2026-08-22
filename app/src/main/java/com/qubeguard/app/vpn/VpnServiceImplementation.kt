package com.qubeguard.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.qubeguard.app.data.blocklist.BlocklistDao
import com.qubeguard.app.data.blocklist.DeterministicBlocker
import com.qubeguard.app.data.blocklist.DnsLogEntity
import com.qubeguard.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class VpnServiceImplementation : VpnService() {
    @Inject lateinit var deterministicBlocker: DeterministicBlocker
    @Inject lateinit var blocklistDao: BlocklistDao
    @Inject lateinit var dnsProxy: DnsProxy
    private lateinit var vpnThread: Thread
    @Volatile private var isRunning = false
    private var vpnInterface: ParcelFileDescriptor? = null
    private var datagramChannel: DatagramChannel? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val dnsProxyPort = 5353
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

        dnsProxy.start()

        val builder = Builder()
            .setSession("QubeGuard VPN")
            .addAddress("10.0.0.2", 24)
            .addDnsServer("127.0.0.1")
            .addRoute("0.0.0.0", 0)
            .setMtu(1500)

        // Per-App Split Tunneling Bypass
        val prefs = getSharedPreferences("qubeguard_settings", Context.MODE_PRIVATE)
        val bypassSet = prefs.getStringSet("bypass_packages", emptySet()) ?: emptySet()
        bypassSet.forEach { pkg ->
            runCatching {
                builder.addDisallowedApplication(pkg)
            }
        }

        vpnInterface = builder.establish()

        try {
            datagramChannel = DatagramChannel.open().apply {
                bind(InetSocketAddress(dnsProxyPort))
                configureBlocking(false)
            }
        } catch (_: Exception) {
            // Channel open/bind fallback
        }

        isRunning = true
        vpnThread = Thread {
            processDnsRequests()
        }.also { it.start() }
    }

    private fun stopVpn() {
        isRunning = false
        dnsProxy.stop()
        runCatching { datagramChannel?.close() }
        datagramChannel = null
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        if (::vpnThread.isInitialized) runCatching { vpnThread.join(1000) }
    }

    private fun getUpstreamDnsServer(): String {
        val prefs = getSharedPreferences("qubeguard_settings", Context.MODE_PRIVATE)
        return prefs.getString("upstream_dns_ip", "1.1.1.1") ?: "1.1.1.1"
    }

    private fun processDnsRequests() {
        while (isRunning) {
            try {
                val channel = datagramChannel ?: break
                val buffer = ByteBuffer.allocate(4096)
                val clientAddress = channel.receive(buffer)
                if (clientAddress == null) {
                    Thread.sleep(10)
                    continue
                }
                buffer.flip()
                val queryBytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                val request = DnsRequest.parse(ByteBuffer.wrap(queryBytes))
                scope.launch {
                    val isBlocked = deterministicBlocker.isBlocked(request.domain)
                    blocklistDao.insertDnsLog(
                        DnsLogEntity(
                            id = UUID.randomUUID().toString(),
                            domain = request.domain,
                            isBlocked = isBlocked,
                            reason = if (isBlocked) "Layer 1 Deterministic" else "Layer 2 DNS Allowed",
                            timestamp = System.currentTimeMillis().toString()
                        )
                    )

                    val response = if (isBlocked) {
                        DnsResponse.createNxDomainResponse(request, queryBytes)
                    } else {
                        forwardQuery(queryBytes)
                    }
                    if (response.isNotEmpty()) {
                        channel.send(ByteBuffer.wrap(response), clientAddress)
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
                val address = InetSocketAddress(getUpstreamDnsServer(), upstreamDnsPort)
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
        return NotificationCompat.Builder(this, channelId)
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
            response[0] = (request.id ushr 8).toByte()
            response[1] = request.id.toByte()
            response[2] = 0x85.toByte()
            response[3] = 0x83.toByte()
            return response
        }
    }

    companion object { private const val NOTIFICATION_ID = 1 }
}
