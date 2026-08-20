package com.qubeguard.app

import com.qubeguard.app.data.blocklist.RadixTree
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for RadixTree.
 */
class RadixTreeTest {

    @Test
    fun `test insert and isBlocked`() {
        val radixTree = RadixTree()

        // Insert a domain
        radixTree.insert("example.com", true)

        // Check if the domain is blocked
        assertEquals(true, radixTree.isBlocked("example.com"))
        assertEquals(false, radixTree.isBlocked("google.com"))
    }

    @Test
    fun `test subdomain matching`() {
        val radixTree = RadixTree()

        // Insert a domain
        radixTree.insert("example.com", true)

        // Check if subdomains are blocked
        assertEquals(true, radixTree.isBlockedOrSubdomainBlocked("sub.example.com"))
        assertEquals(true, radixTree.isBlockedOrSubdomainBlocked("deep.sub.example.com"))
    }

    @Test
    fun `test multiple domains`() {
        val radixTree = RadixTree()

        // Insert multiple domains
        radixTree.insert("example.com", true)
        radixTree.insert("google.com", false)
        radixTree.insert("ads.example.com", true)

        // Check blocked domains
        assertEquals(true, radixTree.isBlocked("example.com"))
        assertEquals(true, radixTree.isBlocked("ads.example.com"))

        // Check allowed domain
        assertEquals(false, radixTree.isBlocked("google.com"))
    }

    @Test
    fun `test case insensitivity`() {
        val radixTree = RadixTree()

        // Insert a domain in lowercase
        radixTree.insert("example.com", true)

        // Check with different cases
        assertEquals(true, radixTree.isBlocked("EXAMPLE.COM"))
        assertEquals(true, radixTree.isBlocked("Example.Com"))
    }

    @Test
    fun `test clear`() {
        val radixTree = RadixTree()

        // Insert a domain
        radixTree.insert("example.com", true)

        // Clear the tree
        radixTree.clear()

        // Check if the domain is no longer blocked
        assertEquals(false, radixTree.isBlocked("example.com"))
    }
}
