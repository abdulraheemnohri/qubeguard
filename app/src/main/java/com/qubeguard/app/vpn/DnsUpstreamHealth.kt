package com.qubeguard.app.vpn

import javax.inject.Inject
import javax.inject.Singleton

/** Lightweight circuit breaker for DNS upstreams. */
@Singleton
class DnsUpstreamHealth @Inject constructor() {
    private data class State(var failures: Int = 0, var unhealthyUntilMs: Long = 0L)
    private val lock = Any()
    private val states = HashMap<String, State>()

    fun isAvailable(server: String, nowMs: Long = System.currentTimeMillis()): Boolean = synchronized(lock) {
        val state = states[server] ?: return@synchronized true
        state.unhealthyUntilMs <= nowMs
    }

    fun recordSuccess(server: String) = synchronized(lock) {
        states[server] = State()
    }

    fun recordFailure(server: String, nowMs: Long = System.currentTimeMillis()) = synchronized(lock) {
        val state = states.getOrPut(server) { State() }
        state.failures = (state.failures + 1).coerceAtMost(MAX_FAILURES)
        if (state.failures >= FAILURE_THRESHOLD) {
            val exponent = (state.failures - FAILURE_THRESHOLD).coerceAtMost(4)
            val cooldown = BASE_COOLDOWN_MS shl exponent
            state.unhealthyUntilMs = nowMs + cooldown
        }
    }

    fun reset() = synchronized(lock) { states.clear() }

    companion object {
        private const val FAILURE_THRESHOLD = 2
        private const val MAX_FAILURES = 6
        private const val BASE_COOLDOWN_MS = 5_000L
    }
}
