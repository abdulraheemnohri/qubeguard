package com.qubeguard.app.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsResponseValidationTest {
    @Test
    fun rejectsWrongTransactionId() {
        val request = DnsProxy.DnsRequest(0x1234, "example.com", 1, 1)
        val response = queryResponse(0x9999, "example.com")
        assertFalse(DnsProxy.DnsResponse.isValidForRequest(response, request))
    }

    @Test
    fun acceptsMatchingResponse() {
        val request = DnsProxy.DnsRequest(0x1234, "example.com", 1, 1)
        val response = queryResponse(0x1234, "example.com")
        assertTrue(DnsProxy.DnsResponse.isValidForRequest(response, request))
    }

    private fun queryResponse(id: Int, domain: String): ByteArray {
        val labels = domain.split('.')
        val q = ArrayList<Byte>()
        q += (id ushr 8).toByte(); q += id.toByte()
        q += byteArrayOf(0x81.toByte(), 0x80.toByte(), 0, 1, 0, 0, 0, 0, 0, 0).toList()
        labels.forEach { label -> q += label.length.toByte(); label.toByteArray().forEach { q += it } }
        q += 0; q += byteArrayOf(0, 1, 0, 1).toList()
        return q.toByteArray()
    }
}
