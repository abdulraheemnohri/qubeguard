package com.qubeguard.app.data.blocklist

import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

/** Bounded in-memory cache for deterministic firewall decisions. */
@Singleton
class DecisionCache @Inject constructor() {
    private data class Entry(val blocked: Boolean, val expiresAt: Long)
    private val cache = LruCache<String, Entry>(2000)

    fun get(input: String): Boolean? {
        val key = normalize(input)
        synchronized(cache) {
            val entry = cache.get(key) ?: return null
            if (entry.expiresAt <= System.currentTimeMillis()) {
                cache.remove(key)
                return null
            }
            return entry.blocked
        }
    }

    fun put(input: String, blocked: Boolean, ttlMs: Long = 30_000L) {
        if (ttlMs <= 0) return
        synchronized(cache) {
            cache.put(normalize(input), Entry(blocked, System.currentTimeMillis() + ttlMs))
        }
    }

    fun clear() {
        synchronized(cache) { cache.evictAll() }
    }

    private fun normalize(value: String): String = value.trim().lowercase()
}
