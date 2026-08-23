package com.qubeguard.app.vpn

import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Small bounded DNS response cache. Only successful responses are cached. */
@Singleton
class DnsCache @Inject constructor() {
    data class Entry(val response: ByteArray, val expiresAtMs: Long)

    private val lock = Any()
    private val entries = object : LinkedHashMap<String, Entry>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean = size > MAX_ENTRIES
    }

    fun get(key: String, nowMs: Long = System.currentTimeMillis()): ByteArray? = synchronized(lock) {
        val entry = entries[key] ?: return@synchronized null
        if (entry.expiresAtMs <= nowMs) {
            entries.remove(key)
            return@synchronized null
        }
        entry.response.copyOf()
    }

    fun put(key: String, response: ByteArray, ttlSeconds: Int, nowMs: Long = System.currentTimeMillis()) {
        if (ttlSeconds <= 0 || response.isEmpty()) return
        val ttlMs = ttlSeconds.coerceAtMost(MAX_TTL_SECONDS).toLong() * 1000L
        synchronized(lock) {
            entries[key] = Entry(response.copyOf(), nowMs + ttlMs)
        }
    }

    fun clear() = synchronized(lock) { entries.clear() }

    fun size(): Int = synchronized(lock) { entries.size }

    companion object {
        private const val MAX_ENTRIES = 512
        private const val MAX_TTL_SECONDS = 3600
    }
}
