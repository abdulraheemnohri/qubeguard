package com.qubeguard.app.vpn

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DnsTunPacketCodecTest {
    @Test
    fun extractsIpv4UdpDnsQuery() {
        val dns = byteArrayOf(0x12, 0x34, 0x01, 0x00, 0, 1, 0, 0, 0, 0, 0, 0, 7, 'e'.code.toByte(), 'x'.code.toByte(), 'a'.code.toByte(), 'm'.code.toByte(), 'p'.code.toByte(), 'l'.code.toByte(), 'e'.code.toByte(), 3, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(), 0, 0, 1, 0, 1)
        val packet = buildUdpIpv4(0x0A000002, 0x0A000001, 53000, 53, dns)
        val parsed = DnsTunPacketCodec.extractDnsQuery(packet)
        assertNotNull(parsed)
        assertEquals(53000, parsed!!.sourcePort)
        assertEquals(53, parsed.destinationPort)
        assertArrayEquals(dns, parsed.payload)
    }

    @Test
    fun rejectsNonDnsUdp() {
        val packet = buildUdpIpv4(1, 2, 1234, 443, byteArrayOf(1, 2, 3))
        assertEquals(null, DnsTunPacketCodec.extractDnsQuery(packet))
    }

    private fun buildUdpIpv4(src: Int, dst: Int, srcPort: Int, dstPort: Int, payload: ByteArray): ByteArray {
        val total = 20 + 8 + payload.size
        val out = ByteArray(total)
        out[0] = 0x45
        out[8] = 64
        out[9] = 17
        out[2] = (total ushr 8).toByte(); out[3] = total.toByte()
        write32(out, 12, src); write32(out, 16, dst)
        write16(out, 20, srcPort); write16(out, 22, dstPort); write16(out, 24, 8 + payload.size)
        System.arraycopy(payload, 0, out, 28, payload.size)
        return out
    }

    private fun write16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value ushr 8).toByte(); data[offset + 1] = value.toByte()
    }

    private fun write32(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value ushr 24).toByte(); data[offset + 1] = (value ushr 16).toByte(); data[offset + 2] = (value ushr 8).toByte(); data[offset + 3] = value.toByte()
    }
}
