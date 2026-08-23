package com.qubeguard.app.engine

enum class HealthState {
    PROTECTED,
    DEGRADED,
    PAUSED,
    ERROR
}

/**
 * Data class representing the real-time health state of QubeGuard protection modules.
 */
data class ProtectionHealth(
    val state: HealthState = HealthState.PAUSED,
    val vpnActive: Boolean = false,
    val dnsEngineActive: Boolean = false,
    val blocklistsLoaded: Boolean = false,
    val activeRuleCount: Int = 0,
    val databaseHealthy: Boolean = true,
    val aiEngineReady: Boolean = false,
    val upstreamDnsReachable: Boolean = true
)
