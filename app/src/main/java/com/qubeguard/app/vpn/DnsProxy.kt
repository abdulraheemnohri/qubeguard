package com.qubeguard.app.vpn

import android.content.Context
import com.qubeguard.app.data.blocklist.BlocklistDao
import com.qubeguard.app.data.blocklist.DeterministicBlocker
import com.qubeguard.app.data.blocklist.DnsLogEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/** Lightweight Pi-hole style DNS proxy used by the VPN layer. */
class DnsProxy @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deterministicBlocker: DeterministicBlocker,
    private val blocklistDao: BlocklistDao
) {
    private var socket: DatagramSocket? = null
    @Volatile private var isRunning = false
    private val port = 5353

    fun start() {
        if (isRunning) return
        try {
            socket = DatagramSocket(port)
            isRunning = true
            Thread {
                while (isRunning) {
                    try {
                        val buffer = ByteArray(4096)
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket?.receive(packet)
                        val request = DnsRequest.parse(packet.data, packet.length)

                        // 1. Local DNS Custom Records
                        val localRecord = runBlocking { blocklistDao.getLocalDnsRecordForDomain(request.domain) }
                        if (localRecord != null) {
                            runBlocking {
                                blocklistDao.insertDnsLog(
                                    DnsLogEntity(
                                        id = UUID.randomUUID().toString(),
                                        domain = request.domain,
                                        isBlocked = false,
                                        reason = "Local DNS (${localRecord.ipAddress})",
                                        timestamp = System.currentTimeMillis().toString()
                                    )
                                )
                            }
                            val response = DnsResponse.createIpResponse(request, localRecord.ipAddress)
                            socket?.send(DatagramPacket(response, response.size, packet.address, packet.port))
                            continue
                        }

                        // 2. Deterministic Blocklist Check
                        val blocked = runBlocking { deterministicBlocker.isBlocked(request.domain) }

                        runBlocking {
                            blocklistDao.insertDnsLog(
                                DnsLogEntity(
                                    id = UUID.randomUUID().toString(),
                                    domain = request.domain,
                                    isBlocked = blocked,
                                    reason = if (blocked) "Gravity Blocklist" else "Allowed",
                                    timestamp = System.currentTimeMillis().toString()
                                )
                            )
                        }

                        if (blocked) {
                            val mode = getSinkholeMode()
                            val response = when (mode) {
                                "NULL_IP", "0.0.0.0" -> DnsResponse.createIpResponse(request, "0.0.0.0")
                                "NODATA" -> DnsResponse.createNoDataResponse(request, buffer, packet.length)
                                "REFUSED" -> DnsResponse.createRefusedResponse(request, buffer, packet.length)
                                else -> DnsResponse.createNxDomainResponse(request, buffer, packet.length)
                            }
                            socket?.send(DatagramPacket(response, response.size, packet.address, packet.port))
                        } else {
                            // 3. Conditional Forwarding Check
                            val condServer = getConditionalTargetServer(request.domain)
                            forward(packet, condServer ?: getUpstreamDnsServer())
                        }
                    } catch (_: Exception) {
                        if (isRunning) continue
                    }
                }
            }.start()
        } catch (_: Exception) {
            isRunning = false
        }
    }

    private fun getSinkholeMode(): String {
        val prefs = context.getSharedPreferences("qubeguard_settings", Context.MODE_PRIVATE)
        return prefs.getString("pihole_sinkhole_mode", "NXDOMAIN") ?: "NXDOMAIN"
    }

    private fun getUpstreamDnsServer(): String {
        val prefs = context.getSharedPreferences("qubeguard_settings", Context.MODE_PRIVATE)
        return prefs.getString("upstream_dns_ip", "1.1.1.1") ?: "1.1.1.1"
    }

    private fun getConditionalTargetServer(domain: String): String? {
        val prefs = context.getSharedPreferences("qubeguard_settings", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("pihole_conditional_enabled", false)
        if (!enabled) return null
        val condDomain = prefs.getString("pihole_conditional_domain", "home.arpa") ?: "home.arpa"
        if (domain.endsWith(condDomain) || domain == condDomain) {
            return prefs.getString("pihole_conditional_ip", "192.168.1.1")
        }
        return null
    }

    private fun forward(packet: DatagramPacket, dnsServerIp: String) {
        val upstream = DatagramSocket()
        try {
            upstream.soTimeout = 5000
            val address = InetAddress.getByName(dnsServerIp)
            upstream.send(DatagramPacket(packet.data, packet.length, address, 53))
            val responseBuffer = ByteArray(4096)
            val response = DatagramPacket(responseBuffer, responseBuffer.size)
            upstream.receive(response)
            socket?.send(DatagramPacket(response.data, response.length, packet.address, packet.port))
        } catch (_: Exception) {
            // Drop packet on timeout or error
        } finally {
            upstream.close()
        }
    }

    fun stop() {
        isRunning = false
        socket?.close()
        socket = null
    }

    data class DnsRequest(val id: Int, val domain: String) {
        companion object {
            fun parse(data: ByteArray, length: Int): DnsRequest {
                require(length >= 12) { "Packet length too short for DNS header" }
                val id = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                var position = 12
                val builder = StringBuilder()
                while (position < length) {
                    val labelLength = data[position++].toInt() and 0xFF
                    if (labelLength == 0) break
                    require(labelLength <= 63 && position + labelLength <= length) { "Invalid DNS label length" }
                    if (builder.isNotEmpty()) builder.append('.')
                    builder.append(String(data, position, labelLength, Charsets.UTF_8))
                    position += labelLength
                }
                return DnsRequest(id, builder.toString().trimEnd('.').lowercase())
            }
        }
    }

    object DnsResponse {
        fun createNxDomainResponse(request: DnsRequest, query: ByteArray, queryLength: Int): ByteArray {
            val response = query.copyOf(queryLength)
            response[0] = (request.id ushr 8).toByte()
            response[1] = request.id.toByte()
            response[2] = 0x85.toByte()
            response[3] = 0x83.toByte()
            return response
        }

        fun createNoDataResponse(request: DnsRequest, query: ByteArray, queryLength: Int): ByteArray {
            val response = query.copyOf(queryLength)
            response[0] = (request.id ushr 8).toByte()
            response[1] = request.id.toByte()
            response[2] = 0x85.toByte()
            response[3] = 0x80.toByte() // NO ERROR, 0 answers
            return response
        }

        fun createRefusedResponse(request: DnsRequest, query: ByteArray, queryLength: Int): ByteArray {
            val response = query.copyOf(queryLength)
            response[0] = (request.id ushr 8).toByte()
            response[1] = request.id.toByte()
            response[2] = 0x85.toByte()
            response[3] = 0x85.toByte() // REFUSED
            return response
        }

        fun createIpResponse(request: DnsRequest, ipAddress: String): ByteArray {
            val response = ByteArray(12 + 16)
            response[0] = (request.id ushr 8).toByte()
            response[1] = request.id.toByte()
            response[2] = 0x81.toByte() // Standard query response, No error
            response[3] = 0x80.toByte()
            response[4] = 0x00; response[5] = 0x00 // QDCOUNT
            response[6] = 0x00; response[7] = 0x01 // ANCOUNT = 1
            response[8] = 0x00; response[9] = 0x00 // NSCOUNT
            response[10] = 0x00; response[11] = 0x00 // ARCOUNT

            // Answer section
            val ipBytes = InetAddress.getByName(ipAddress).address
            var p = 12
            response[p++] = 0xC0.toByte(); response[p++] = 0x0C.toByte() // Pointer to qname
            response[p++] = 0x00; response[p++] = 0x01 // TYPE A
            response[p++] = 0x00; response[p++] = 0x01 // CLASS IN
            response[p++] = 0x00; response[p++] = 0x00; response[p++] = 0x01; response[p++] = 0x2C.toByte() // TTL 300
            response[p++] = 0x00; response[p++] = 0x04 // RDLENGTH 4
            System.arraycopy(ipBytes, 0, response, p, 4)
            return response
        }
    }
}
