package com.qubeguard.app.vpn

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DnsCacheTest {
    @Test
    fun storesAndReturnsCopyBeforeExpiry() {
        val cache = DnsCache()
        val original = byteArrayOf(1, 2, 3)
        cache.put("example.com|1", original, ttlSeconds = 10, nowMs = 1000)
        original[0] = 9
        assertArrayEquals(byteArrayOf(1, 2, 3), cache.get("example.com|1", nowMs = 2000))
    }

    @Test
    fun expiresEntry() {
        val cache = DnsCache()
        cache.put("example.com|1", byteArrayOf(1), ttlSeconds = 1, nowMs = 1000)
        assertNull(cache.get("example.com|1", nowMs = 2001))
        assertEquals(0, cache.size())
    }
}
