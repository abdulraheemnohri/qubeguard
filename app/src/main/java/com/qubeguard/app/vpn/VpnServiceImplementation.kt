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
import com.qubeguard.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject

/**
 * DNS-only local VPN.
 *
 * QubeGuard does not claim to be a general IP router. Only DNS packets sent to
 * the VPN DNS address are consumed from the TUN interface; all normal traffic
 * remains on Android's underlying network.
 */
@AndroidEntryPoint
class VpnServiceImplementation : VpnService() {
    @Inject lateinit var dnsProxy: DnsProxy

    @Volatile private var isRunning = false
    private var vpnInterface: ParcelFileDescriptor? = null
    private var worker: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) startVpn()
        return START_STICKY
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun startVpn() {
        if (isRunning) return
        startForeground(NOTIFICATION_ID, createNotification())

        val builder = Builder()
            .setSession("QubeGuard DNS Firewall")
            .addAddress(TUN_ADDRESS, 24)
            .addDnsServer(DNS_ADDRESS)
            .addRoute(DNS_ADDRESS, 32)
            .setMtu(1500)
            .setBlocking(true)

        val prefs = getSharedPreferences("qubeguard_settings", Context.MODE_PRIVATE)
        val bypassSet = prefs.getStringSet("bypass_packages", emptySet()) ?: emptySet()
        bypassSet.forEach { packageName ->
            runCatching { builder.addDisallowedApplication(packageName) }
        }

        val established = runCatching { builder.establish() }.getOrNull() ?: run {
            stopSelf()
            return
        }
        vpnInterface = established
        isRunning = true

        // The proxy is bound locally and the TUN loop is the only component
        // touching the VPN packet stream. This removes the previous duplicate
        // UDP:5353 bind and fixes the incorrect full-tunnel architecture.
        dnsProxy.setSocketProtector { socket -> protect(socket) }
        dnsProxy.start()

        worker = Thread(::runTunLoop, "QubeGuard-DNS-TUN").also { it.start() }
    }

    private fun runTunLoop() {
        val descriptor = vpnInterface ?: return
        try {
            FileInputStream(descriptor.fileDescriptor).use { input ->
                FileOutputStream(descriptor.fileDescriptor).use { output ->
                    val packet = ByteArray(32767)
                    while (isRunning) {
                        val count = input.read(packet)
                        if (count <= 0) continue
                        val requestPacket = packet.copyOf(count)
                        val dnsRequest = DnsTunPacketCodec.extractDnsQuery(requestPacket) ?: continue
                        val response = queryLocalProxy(dnsRequest.payload) ?: continue
                        val responsePacket = DnsTunPacketCodec.buildDnsResponse(dnsRequest, response)
                        output.write(responsePacket)
                        output.flush()
                    }
                }
            }
        } catch (_: Exception) {
            if (isRunning) stopSelf()
        }
    }

    private fun queryLocalProxy(payload: ByteArray): ByteArray? {
        return runCatching {
            DatagramSocket().use { socket ->
                socket.soTimeout = DNS_PROXY_TIMEOUT_MS
                protect(socket)
                val proxy = InetAddress.getByName(LOOPBACK)
                socket.send(DatagramPacket(payload, payload.size, proxy, DNS_PROXY_PORT))
                val responseBuffer = ByteArray(8192)
                val response = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.receive(response)
                response.data.copyOf(response.length)
            }
        }.getOrNull()
    }

    private fun stopVpn() {
        if (!isRunning && vpnInterface == null) return
        isRunning = false
        dnsProxy.stop()
        worker?.interrupt()
        worker = null
        runCatching { vpnInterface?.close() }
        vpnInterface = null
    }

    private fun createNotification(): Notification {
        val channelId = "QubeGuard.VPN"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channelId, "QubeGuard VPN", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("QubeGuard DNS Firewall")
            .setContentText("DNS protection is active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val TUN_ADDRESS = "10.0.0.2"
        private const val DNS_ADDRESS = "10.0.0.1"
        private const val LOOPBACK = "127.0.0.1"
        private const val DNS_PROXY_PORT = 5353
        private const val DNS_PROXY_TIMEOUT_MS = 3000
    }
}
