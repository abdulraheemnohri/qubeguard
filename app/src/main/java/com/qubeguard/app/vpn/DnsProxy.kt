package com.qubeguard.app.vpn

import com.qubeguard.app.data.blocklist.DeterministicBlocker
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject

/**
 * A lightweight DNS proxy for intercepting and forwarding DNS requests.
 * Used by the VPN service to block or allow DNS queries based on the deterministic blocker.
 */
class DnsProxy @Inject constructor(
    private val deterministicBlocker: DeterministicBlocker
) {
    private var socket: DatagramSocket? = null
    private var isRunning = false
    private val port = 5353 // Local DNS proxy port

    /**
     * Starts the DNS proxy on the specified port.
     */
    fun start() {
        if (isRunning) return

        socket = DatagramSocket(port)
        isRunning = true

        Thread {
            while (isRunning) {
                try {
                    val buffer = ByteArray(512)
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)

                    // Parse the DNS request
                    val request = DnsRequest.parse(packet.data)

                    // Check if the domain is blocked
                    val isBlocked = deterministicBlocker.isBlocked(request.domain)

                    if (isBlocked) {
                        // Respond with NXDOMAIN (domain does not exist)
                        val response = DnsResponse.createNxDomainResponse(request)
                        val responsePacket = DatagramPacket(
                            response,
                            response.size,
                            packet.address,
                            packet.port
                        )
                        socket?.send(responsePacket)
                    } else {
                        // Forward the request to the upstream DNS server
                        val upstreamAddress = InetAddress.getByName("1.1.1.1") // Cloudflare DNS
                        val upstreamPacket = DatagramPacket(
                            packet.data,
                            packet.length,
                            upstreamAddress,
                            53
                        )
                        val upstreamSocket = DatagramSocket()
                        upstreamSocket.send(upstreamPacket)

                        // Wait for the response
                        val upstreamBuffer = ByteArray(512)
                        val upstreamResponsePacket = DatagramPacket(upstreamBuffer, upstreamBuffer.size)
                        upstreamSocket.receive(upstreamResponsePacket)

                        // Forward the response back to the client
                        val responsePacket = DatagramPacket(
                            upstreamResponsePacket.data,
                            upstreamResponsePacket.length,
                            packet.address,
                            packet.port
                        )
                        socket?.send(responsePacket)
                        upstreamSocket.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    /**
     * Stops the DNS proxy.
     */
    fun stop() {
        isRunning = false
        socket?.close()
        socket = null
    }

    /**
     * Represents a DNS request.
     */
    data class DnsRequest(
        val id: Int,
        val domain: String
    ) {
        companion object {
            fun parse(data: ByteArray): DnsRequest {
                val id = (data[0].toInt() and 0xFF) shl 8 or (data[1].toInt() and 0xFF)
                var position = 12 // Skip header

                // Read the domain name (compressed format)
                val domainBuilder = StringBuilder()
                var length = data[position++].toInt() and 0xFF
                while (length > 0) {
                    if (domainBuilder.isNotEmpty()) {
                        domainBuilder.append('.')
                    }
                    val start = position
                    position += length
                    domainBuilder.append(String(data, start, position, Charsets.UTF_8))
                    length = data[position++].toInt() and 0xFF
                }

                return DnsRequest(id, domainBuilder.toString())
            }
        }
    }

    /**
     * Represents a DNS response.
     */
    data class DnsResponse(
        val id: Int,
        val domain: String,
        val isBlocked: Boolean
    ) {
        companion object {
            fun createNxDomainResponse(request: DnsRequest): ByteArray {
                val response = ByteArray(512)

                // Set request ID
                response[0] = (request.id ushr 8).toByte()
                response[1] = (request.id and 0xFF).toByte()

                // Set response flags (QR = 1, Opcode = 0, AA = 0, TC = 0, RD = 0, RA = 0, Z = 0, RCODE = 3 (NXDOMAIN))
                response[2] = 0x80.toByte()
                response[3] = 0x03.toByte()

                // QDCOUNT = 1
                response[4] = 0x00.toByte()
                response[5] = 0x01.toByte()
                // ANCOUNT = 0
                response[6] = 0x00.toByte()
                response[7] = 0x00.toByte()
                // NSCOUNT = 0
                response[8] = 0x00.toByte()
                response[9] = 0x00.toByte()
                // ARCOUNT = 0
                response[10] = 0x00.toByte()
                response[11] = 0x00.toByte()

                // Copy the question section from the request
                System.arraycopy(
                    request.toByteArray(),
                    12,
                    response,
                    12,
                    request.toByteArray().size - 12
                )

                return response.copyOf(12 + (request.toByteArray().size - 12))
            }
        }
    }

    /**
     * Converts a DnsRequest to a byte array.
     */
    private fun DnsRequest.toByteArray(): ByteArray {
        val domainParts = domain.split('.')
        val domainBytes = domainParts.joinToByteArray(".") { part ->
            byteArrayOf(part.length.toByte()) + part.toByteArray(Charsets.UTF_8)
        } + byteArrayOf(0x00) // Null terminator

        val header = ByteArray(12)
        header[0] = (id ushr 8).toByte()
        header[1] = (id and 0xFF).toByte()
        header[2] = 0x01.toByte() // QR = 0 (query), Opcode = 0, AA = 0, TC = 0, RD = 1
        header[3] = 0x00.toByte()
        header[4] = 0x00.toByte() // QDCOUNT = 1
        header[5] = 0x01.toByte()
        header[6] = 0x00.toByte() // ANCOUNT = 0
        header[7] = 0x00.toByte()
        header[8] = 0x00.toByte() // NSCOUNT = 0
        header[9] = 0x00.toByte()
        header[10] = 0x00.toByte() // ARCOUNT = 0
        header[11] = 0x00.toByte()

        return header + domainBytes + byteArrayOf(
            0x00, 0x01, // QTYPE = A (1)
            0x00, 0x01  // QCLASS = IN (1)
        )
    }
}