package com.qubeguard.app

import com.qubeguard.app.data.blocklist.BloomFilter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for BloomFilter.
 */
class BloomFilterTest {

    @Test
    fun `test add and mightContain`() {
        val bloomFilter = BloomFilter(size = 1000, numHashFunctions = 3)

        // Add a domain
        bloomFilter.add("example.com")

        // Check if the domain might be in the filter
        assertEquals(true, bloomFilter.mightContain("example.com"))

        // Check for a domain that was not added (false positive possible but unlikely with small size)
        assertEquals(false, bloomFilter.mightContain("google.com"))
    }

    @Test
    fun `test clear`() {
        val bloomFilter = BloomFilter(size = 1000, numHashFunctions = 3)

        // Add a domain
        bloomFilter.add("example.com")

        // Clear the filter
        bloomFilter.clear()

        // Check if the domain is no longer in the filter
        assertEquals(false, bloomFilter.mightContain("example.com"))
    }

    @Test
    fun `test multiple additions`() {
        val bloomFilter = BloomFilter(size = 1000, numHashFunctions = 3)

        // Add multiple domains
        bloomFilter.add("example.com")
        bloomFilter.add("google.com")
        bloomFilter.add("github.com")

        // Check if all domains might be in the filter
        assertEquals(true, bloomFilter.mightContain("example.com"))
        assertEquals(true, bloomFilter.mightContain("google.com"))
        assertEquals(true, bloomFilter.mightContain("github.com"))
    }

    @Test
    fun `test case insensitivity`() {
        val bloomFilter = BloomFilter(size = 1000, numHashFunctions = 3)

        // Add a domain in lowercase
        bloomFilter.add("example.com")

        // Check with different cases
        assertEquals(true, bloomFilter.mightContain("EXAMPLE.COM"))
        assertEquals(true, bloomFilter.mightContain("Example.Com"))
    }
}
