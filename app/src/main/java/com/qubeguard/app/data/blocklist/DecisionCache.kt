package com.qubeguard.app.data.blocklist

import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fast in-memory LRU Decision Cache for Instant O(1) URL blocking decisions in WebView & DNS Proxy.
 */
@Singleton
class DecisionCache @Inject constructor() {
    private val cache = LruCache<String, Boolean>(2000)

    fun get(urlOrDomain: String): Boolean? {
        synchronized(cache) {
            return cache.get(urlOrDomain)
        }
    }

    fun put(urlOrDomain: String, isBlocked: Boolean) {
        synchronized(cache) {
            cache.put(urlOrDomain, isBlocked)
        }
    }

    fun clear() {
        synchronized(cache) {
            cache.evictAll()
        }
    }
}
