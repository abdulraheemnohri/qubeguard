package com.qubeguard.app.vpn

import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Bounded negative DNS cache for NXDOMAIN/NODATA responses. */
@Singleton
class DnsNegativeCache @Inject constructor() {
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
        val ttl = ttlSeconds.coerceAtMost(MAX_TTL_SECONDS).toLong() * 1000L
        synchronized(lock) { entries[key] = Entry(response.copyOf(), nowMs + ttl) }
    }

    fun clear() = synchronized(lock) { entries.clear() }

    companion object {
        private const val MAX_ENTRIES = 256
        private const val MAX_TTL_SECONDS = 300
    }
}
