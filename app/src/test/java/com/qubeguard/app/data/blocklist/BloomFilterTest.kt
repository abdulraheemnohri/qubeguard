package com.qubeguard.app.data.blocklist

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BloomFilterTest {
    @Test
    fun insertedDomainIsPresent() {
        val filter = BloomFilter(size = 10_000)
        filter.add("Example.COM.")
        assertTrue(filter.mightContain("example.com"))
    }

    @Test
    fun emptyDomainIsRejected() {
        val filter = BloomFilter()
        assertFalse(filter.mightContain("   "))
    }
}
