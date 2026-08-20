package com.qubeguard.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.qubeguard.app.R
import com.qubeguard.app.data.blocklist.DeterministicBlocker
import com.qubeguard.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import javax.inject.Inject

/**
 * Layer 2: VPN Service Implementation.
 * Intercepts DNS traffic (UDP port 53) and blocks requests based on the deterministic blocker.
 */
@AndroidEntryPoint
class VpnServiceImplementation : VpnService() {

    @Inject
    lateinit var deterministicBlocker: DeterministicBlocker

    private lateinit var vpnThread: Thread
    private var isRunning = false
    private lateinit var datagramChannel: DatagramChannel
    private val scope = CoroutineScope(Dispatchers.IO)

    // DNS proxy settings
    private val dnsProxyPort = 5353
    private val upstreamDnsServer = "1.1.1.1" // Cloudflare DNS
    private val upstreamDnsPort = 53

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning) {
            return START_STICKY
        }

        // Start the VPN service
        startVpn()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }

    /**
     * Starts the VPN service and sets up the DNS proxy.
     */
    private fun startVpn() {
        if (isRunning) return

        // Create a notification for the foreground service
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Configure the VPN
        val builder = Builder()
            .setSession("QubeGuard VPN")
            .addAddress("10.0.0.2", 24) // Virtual IP for the VPN
            .addDnsServer("10.0.0.2") // Route DNS through the proxy
            .addRoute("0.0.0.0", 0) // Route all traffic
            .setMtu(1500)

        // Establish the VPN
        val vpnInterface = builder.establish()

        // Start the DNS proxy thread
        vpnThread = Thread {
            try {
                datagramChannel = DatagramChannel.open()
                datagramChannel.socket().bind(InetSocketAddress(dnsProxyPort))
                datagramChannel.configureBlocking(false)

                isRunning = true
                processDnsRequests()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRunning = false
            }
        }
        vpnThread.start()
    }

    /**
     * Stops the VPN service.
     */
    private fun stopVpn() {
        isRunning = false
        try {
            datagramChannel.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            vpnThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * Processes incoming DNS requests and responds based on the blocklist.
     */
    private fun processDnsRequests() {
        val buffer = ByteArray(512)
        val byteBuffer = ByteBuffer.wrap(buffer)

        while (isRunning) {
            try {
                byteBuffer.clear()
                val clientAddress = datagramChannel.receive(byteBuffer)
                if (clientAddress == null) continue

                byteBuffer.flip()
                val request = DnsRequest.parse(byteBuffer)

                // Check if the domain is blocked
                scope.launch {
                    val isBlocked = deterministicBlocker.isBlocked(request.domain)
                    if (isBlocked) {
                        // Respond with NXDOMAIN (domain does not exist)
                        val response = DnsResponse.createNxDomainResponse(request, byteBuffer)
                        datagramChannel.send(ByteBuffer.wrap(response), clientAddress)
                    } else {
                        // Forward the request to the upstream DNS server
                        val upstreamAddress = InetSocketAddress(upstreamDnsServer, upstreamDnsPort)
                        val upstreamChannel = DatagramChannel.open()
                        upstreamChannel.connect(upstreamAddress)
                        upstreamChannel.send(byteBuffer, upstreamAddress)

                        // Wait for the response
                        byteBuffer.clear()
                        upstreamChannel.receive(byteBuffer)
                        byteBuffer.flip()

                        // Forward the response back to the client
                        datagramChannel.send(byteBuffer, clientAddress)
                        upstreamChannel.close()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Creates a notification for the foreground service.
     */
    private fun createNotification(): Notification {
        val channelId = "QubeGuard VPN Channel"
        val channelName = "QubeGuard VPN"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, channelId)
            .setContentTitle("QubeGuard VPN")
            .setContentText("Protecting your privacy")
            .setSmallIcon(R.drawable.ic_vpn)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Represents a DNS request.
     */
    private data class DnsRequest(
        val id: Int,
        val domain: String
    ) {
        companion object {
            fun parse(buffer: ByteBuffer): DnsRequest {
                val id = buffer.short.toInt() and 0xFFFF
                buffer.position(12) // Skip flags, QDCOUNT, etc.

                // Read the domain name (compressed format)
                val domainBuilder = StringBuilder()
                var length = buffer.get().toInt() and 0xFF
                while (length > 0) {
                    if (domainBuilder.isNotEmpty()) {
                        domainBuilder.append('.')
                    }
                    val bytes = ByteArray(length)
                    buffer.get(bytes)
                    domainBuilder.append(String(bytes, Charsets.UTF_8))
                    length = buffer.get().toInt() and 0xFF
                }

                return DnsRequest(id, domainBuilder.toString())
            }
        }
    }

    /**
     * Represents a DNS response.
     */
    private data class DnsResponse(
        val id: Int,
        val domain: String,
        val isBlocked: Boolean
    ) {
        companion object {
            fun createNxDomainResponse(request: DnsRequest, buffer: ByteBuffer): ByteArray {
                val responseBuffer = ByteBuffer.allocate(512)

                // Copy the request ID
                responseBuffer.putShort(request.id.toShort())

                // Set response flags (QR = 1, Opcode = 0, AA = 0, TC = 0, RD = 0, RA = 0, Z = 0, RCODE = 3 (NXDOMAIN))
                responseBuffer.putShort(0x8003.toShort())

                // QDCOUNT = 1
                responseBuffer.putShort(1)
                // ANCOUNT = 0
                responseBuffer.putShort(0)
                // NSCOUNT = 0
                responseBuffer.putShort(0)
                // ARCOUNT = 0
                responseBuffer.putShort(0)

                // Copy the question section from the request
                buffer.position(2) // Skip the ID
                val questionBytes = ByteArray(buffer.remaining())
                buffer.get(questionBytes)
                responseBuffer.put(questionBytes)

                responseBuffer.flip()
                val response = ByteArray(responseBuffer.remaining())
                responseBuffer.get(response)
                return response
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1
    }
}