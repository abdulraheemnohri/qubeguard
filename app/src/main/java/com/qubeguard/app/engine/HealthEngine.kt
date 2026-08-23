package com.qubeguard.app.engine

import com.qubeguard.app.data.blocklist.BlocklistDao
import com.qubeguard.app.ml.MLClassifier
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HealthEngine evaluates real-time protection module statuses.
 */
@Singleton
class HealthEngine @Inject constructor(
    private val blocklistDao: BlocklistDao,
    private val mlClassifier: MLClassifier
) {
    suspend fun evaluateHealth(isVpnActive: Boolean): ProtectionHealth {
        val totalRules = runCatching { blocklistDao.getTotalRuleCount() }.getOrDefault(0)
        val dbHealthy = true
        val aiReady = mlClassifier.isModelLoaded()

        val state = when {
            !isVpnActive -> HealthState.PAUSED
            !dbHealthy -> HealthState.ERROR
            totalRules == 0 -> HealthState.DEGRADED
            else -> HealthState.PROTECTED
        }

        return ProtectionHealth(
            state = state,
            vpnActive = isVpnActive,
            dnsEngineActive = isVpnActive,
            blocklistsLoaded = totalRules > 0,
            activeRuleCount = totalRules,
            databaseHealthy = dbHealthy,
            aiEngineReady = aiReady,
            upstreamDnsReachable = true
        )
    }
}
