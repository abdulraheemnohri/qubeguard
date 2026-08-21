package com.qubeguard.app.vpn

import com.qubeguard.app.data.blocklist.DeterministicBlocker
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/** Lightweight DNS proxy used by the VPN layer. */
class DnsProxy @Inject constructor(
    private val deterministicBlocker: DeterministicBlocker
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
                        val blocked = runBlocking { deterministicBlocker.isBlocked(request.domain) }
                        if (blocked) {
                            val response = DnsResponse.createNxDomainResponse(request, buffer, packet.length)
                            socket?.send(DatagramPacket(response, response.size, packet.address, packet.port))
                        } else {
                            forward(packet)
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

    private fun forward(packet: DatagramPacket) {
        val upstream = DatagramSocket()
        try {
            upstream.soTimeout = 5000
            val address = InetAddress.getByName("1.1.1.1")
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
}
