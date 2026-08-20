package com.qubeguard.app.data.blocklist

import java.util.BitSet

/**
 * A Bloom Filter implementation for fast negative lookups.
 * If a domain is NOT in the filter, it is definitely not blocked.
 * If a domain IS in the filter, it may or may not be blocked (false positives possible).
 */
class BloomFilter(
    private val size: Int = 1_000_000,
    private val numHashFunctions: Int = 7
) {
    private val bitSet = BitSet(size)

    /**
     * Adds a domain to the Bloom Filter.
     * @param domain The domain to add.
     */
    fun add(domain: String) {
        val normalizedDomain = domain.lowercase().trim()
        if (normalizedDomain.isEmpty()) return

        for (i in 0 until numHashFunctions) {
            val hash = hash(normalizedDomain, i)
            bitSet.set(Math.abs(hash % size))
        }
    }

    /**
     * Checks if a domain might be in the Bloom Filter.
     * @param domain The domain to check.
     * @return True if the domain might be in the filter (could be a false positive).
     */
    fun mightContain(domain: String): Boolean {
        val normalizedDomain = domain.lowercase().trim()
        if (normalizedDomain.isEmpty()) return false

        for (i in 0 until numHashFunctions) {
            val hash = hash(normalizedDomain, i)
            if (!bitSet.get(Math.abs(hash % size))) {
                return false
            }
        }
        return true
    }

    /**
     * Clears the Bloom Filter.
     */
    fun clear() {
        bitSet.clear()
    }

    /**
     * Computes a hash for a domain using a seed.
     */
    private fun hash(domain: String, seed: Int): Int {
        var hash = seed
        for (char in domain) {
            hash = hash * 31 + char.code
        }
        return hash
    }
}