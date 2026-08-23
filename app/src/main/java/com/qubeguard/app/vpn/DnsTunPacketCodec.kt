package com.qubeguard.app.vpn

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal IPv4/UDP codec used by the DNS-only VPN.
 * It deliberately does not pretend to be a general IP router.
 */
object DnsTunPacketCodec {
    private const val IPV4 = 4
    private const val UDP = 17
    private const val UDP_HEADER = 8

    data class DnsPacket(
        val sourceAddress: Int,
        val destinationAddress: Int,
        val sourcePort: Int,
        val destinationPort: Int,
        val payload: ByteArray
    )

    fun extractDnsQuery(packet: ByteArray): DnsPacket? {
        if (packet.size < 20) return null
        val version = (packet[0].toInt() ushr 4) and 0x0F
        if (version != IPV4) return null
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (ihl < 20 || packet.size < ihl + UDP_HEADER) return null
        val totalLength = readU16(packet, 2)
        if (totalLength < ihl + UDP_HEADER || totalLength > packet.size) return null
        if (packet[9].toInt() and 0xFF != UDP) return null
        val src = readU32(packet, 12)
        val dst = readU32(packet, 16)
        val sourcePort = readU16(packet, ihl)
        val destinationPort = readU16(packet, ihl + 2)
        if (destinationPort != 53) return null
        val udpLength = readU16(packet, ihl + 4)
        if (udpLength < UDP_HEADER || ihl + udpLength > totalLength) return null
        val payloadLength = udpLength - UDP_HEADER
        if (payloadLength < 12) return null
        return DnsPacket(
            sourceAddress = src,
            destinationAddress = dst,
            sourcePort = sourcePort,
            destinationPort = destinationPort,
            payload = packet.copyOfRange(ihl + UDP_HEADER, ihl + UDP_HEADER + payloadLength)
        )
    }

    fun buildDnsResponse(request: DnsPacket, dnsResponse: ByteArray): ByteArray {
        require(dnsResponse.isNotEmpty()) { "DNS response must not be empty" }
        val ipHeaderLength = 20
        val udpLength = UDP_HEADER + dnsResponse.size
        val totalLength = ipHeaderLength + udpLength
        require(totalLength <= 65535) { "DNS response exceeds IPv4 packet size" }

        val out = ByteArray(totalLength)
        out[0] = 0x45
        out[1] = 0
        writeU16(out, 2, totalLength)
        writeU16(out, 4, 0)
        writeU16(out, 6, 0)
        out[8] = 64
        out[9] = UDP.toByte()
        writeU32(out, 12, request.destinationAddress)
        writeU32(out, 16, request.sourceAddress)
        writeU16(out, 10, checksum(out, 0, ipHeaderLength))

        writeU16(out, ipHeaderLength, request.destinationPort)
        writeU16(out, ipHeaderLength + 2, request.sourcePort)
        writeU16(out, ipHeaderLength + 4, udpLength)
        writeU16(out, ipHeaderLength + 6, 0)
        System.arraycopy(dnsResponse, 0, out, ipHeaderLength + UDP_HEADER, dnsResponse.size)

        val udpChecksum = udpChecksum(out, ipHeaderLength, udpLength, request.destinationAddress, request.sourceAddress)
        writeU16(out, ipHeaderLength + 6, if (udpChecksum == 0) 0xFFFF else udpChecksum)
        return out
    }

    private fun udpChecksum(packet: ByteArray, udpOffset: Int, udpLength: Int, src: Int, dst: Int): Int {
        var sum = 0L
        sum += (src ushr 16) and 0xFFFF
        sum += src and 0xFFFF
        sum += (dst ushr 16) and 0xFFFF
        sum += dst and 0xFFFF
        sum += UDP
        sum += udpLength
        var i = udpOffset
        val end = udpOffset + udpLength
        while (i + 1 < end) {
            if (i == udpOffset + 6) {
                i += 2
                continue
            }
            sum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < end) sum += (packet[i].toInt() and 0xFF) shl 8
        return fold(sum)
    }

    private fun checksum(packet: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        val end = offset + length
        while (i + 1 < end) {
            sum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < end) sum += (packet[i].toInt() and 0xFF) shl 8
        return fold(sum)
    }

    private fun fold(value: Long): Int {
        var sum = value
        while ((sum ushr 16) != 0L) sum = (sum and 0xFFFF) + (sum ushr 16)
        return sum.toInt().inv() and 0xFFFF
    }

    private fun readU16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    private fun readU32(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)

    private fun writeU16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value ushr 8).toByte()
        data[offset + 1] = value.toByte()
    }

    private fun writeU32(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value ushr 24).toByte()
        data[offset + 1] = (value ushr 16).toByte()
        data[offset + 2] = (value ushr 8).toByte()
        data[offset + 3] = value.toByte()
    }
}
