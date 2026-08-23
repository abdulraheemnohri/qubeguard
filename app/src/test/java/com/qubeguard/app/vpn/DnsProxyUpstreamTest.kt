package com.qubeguard.app.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class DnsProxyUpstreamTest {
    @Test
    fun dnsRequestPreservesClass() {
        val packet = byteArrayOf(
            0x12, 0x34, 0x01, 0x00, 0, 1, 0, 0, 0, 0, 0, 0,
            7, 'e'.code.toByte(), 'x'.code.toByte(), 'a'.code.toByte(), 'm'.code.toByte(), 'p'.code.toByte(), 'l'.code.toByte(), 'e'.code.toByte(),
            3, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(), 0,
            0, 1, 0, 1
        )
        val request = DnsProxy.DnsRequest.parse(packet, packet.size)
        assertEquals(0x1234, request.id)
        assertEquals("example.com", request.domain)
        assertEquals(1, request.qType)
        assertEquals(1, request.qClass)
    }
}
