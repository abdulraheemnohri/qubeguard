package com.qubeguard.app

import com.qubeguard.app.util.extractDomain
import com.qubeguard.app.util.isIpAddress
import com.qubeguard.app.util.isValidUrl
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for Kotlin extensions.
 */
class ExtensionsTest {

    @Test
    fun `test isValidUrl with valid URLs`() {
        assertEquals(true, "https://www.example.com".isValidUrl())
        assertEquals(true, "http://example.com/path".isValidUrl())
        assertEquals(true, "https://sub.example.com:8080/path?query=1".isValidUrl())
        assertEquals(true, "ftp://files.example.com".isValidUrl())
    }

    @Test
    fun `test isValidUrl with invalid URLs`() {
        assertEquals(false, "example.com".isValidUrl())
        assertEquals(false, "www.example.com".isValidUrl())
        assertEquals(false, "not a url".isValidUrl())
        assertEquals(false, "".isValidUrl())
    }

    @Test
    fun `test extractDomain from URLs`() {
        assertEquals("www.example.com", "https://www.example.com/path".extractDomain())
        assertEquals("example.com", "http://example.com".extractDomain())
        assertEquals("sub.example.com", "https://sub.example.com:8080/path".extractDomain())
        assertEquals("example.com", "example.com".extractDomain())
    }

    @Test
    fun `test extractDomain with ports and paths`() {
        assertEquals("example.com", "https://example.com:8080/path/to/page".extractDomain())
        assertEquals("example.com", "http://example.com/path?query=1".extractDomain())
    }

    @Test
    fun `test isIpAddress with valid IPs`() {
        assertEquals(true, "192.168.1.1".isIpAddress())
        assertEquals(true, "10.0.0.1".isIpAddress())
        assertEquals(true, "172.16.0.1".isIpAddress())
        assertEquals(true, "255.255.255.255".isIpAddress())
    }

    @Test
    fun `test isIpAddress with invalid IPs`() {
        assertEquals(false, "256.168.1.1".isIpAddress())
        assertEquals(false, "192.168.1".isIpAddress())
        assertEquals(false, "192.168.1.1.1".isIpAddress())
        assertEquals(false, "example.com".isIpAddress())
    }
}
