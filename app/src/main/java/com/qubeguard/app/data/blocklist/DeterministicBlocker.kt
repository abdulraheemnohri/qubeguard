package com.qubeguard.app.data.blocklist

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Layer 1 deterministic firewall. Compilation is serialized so concurrent requests cannot race initialization. */
@Singleton
class DeterministicBlocker @Inject constructor(
    private val ruleCompiler: RuleCompiler,
    private val blocklistDao: BlocklistDao
) {
    private val initializationMutex = Mutex()
    @Volatile private var isInitialized = false

    private suspend fun ensureInitialized() {
        if (isInitialized) return
        initializationMutex.withLock {
            if (isInitialized) return
            ruleCompiler.compileRules(
                blocklistDao.getAllBlocklistRules() + blocklistDao.getAllAllowlistRules()
            )
            isInitialized = true
        }
    }

    fun isBlockedFast(input: String): Boolean {
        if (!isInitialized) return false
        return ruleCompiler.isBlocked(input)
    }

    suspend fun isBlocked(input: String): Boolean {
        ensureInitialized()
        return ruleCompiler.isBlocked(input)
    }

    suspend fun isAllowed(input: String): Boolean {
        ensureInitialized()
        return ruleCompiler.isAllowed(input)
    }

    suspend fun recompileRules() {
        initializationMutex.withLock {
            ruleCompiler.compileRules(
                blocklistDao.getAllBlocklistRules() + blocklistDao.getAllAllowlistRules()
            )
            isInitialized = true
        }
    }

    fun clear() {
        ruleCompiler.clear()
        isInitialized = false
    }
}
