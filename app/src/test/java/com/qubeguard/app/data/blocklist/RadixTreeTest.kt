package com.qubeguard.app.data.blocklist

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadixTreeTest {
    @Test
    fun parentDomainBlocksSubdomains() {
        val tree = RadixTree()
        tree.insert("example.com", true)
        assertTrue(tree.isBlocked("ads.example.com"))
    }

    @Test
    fun unrelatedDomainIsNotBlocked() {
        val tree = RadixTree()
        tree.insert("example.com", true)
        assertFalse(tree.isBlocked("example.net"))
    }
}
