package com.qubeguard.app.data.blocklist

import java.util.BitSet

/** Fast negative lookup filter. A negative result is always authoritative; a positive result is only a candidate. */
class BloomFilter(
    private val size: Int = 1_000_000,
    private val numHashFunctions: Int = 7
) {
    private val bitSet = BitSet(size)

    fun add(domain: String) {
        val normalized = normalize(domain) ?: return
        repeat(numHashFunctions) { seed -> bitSet.set(index(hash(normalized, seed))) }
    }

    fun mightContain(domain: String): Boolean {
        val normalized = normalize(domain) ?: return false
        repeat(numHashFunctions) { seed ->
            if (!bitSet.get(index(hash(normalized, seed)))) return false
        }
        return true
    }

    fun clear() = bitSet.clear()

    private fun normalize(value: String): String? = value.trim().trimEnd('.').lowercase().takeIf { it.isNotEmpty() }

    private fun index(hash: Long): Int = (hash and Long.MAX_VALUE).rem(size.toLong()).toInt()

    private fun hash(domain: String, seed: Int): Long {
        var h = 0xCBF29CE484222325UL.toLong() xor seed.toLong()
        for (ch in domain) {
            h = h xor ch.code.toLong()
            h *= 1099511628211L
        }
        return h
    }
}
