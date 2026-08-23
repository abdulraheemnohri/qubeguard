package com.qubeguard.app.data.blocklist

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Layer 1 deterministic firewall. Compilation is serialized and decisions are cached briefly. */
@Singleton
class DeterministicBlocker @Inject constructor(
    private val ruleCompiler: RuleCompiler,
    private val blocklistDao: BlocklistDao,
    private val decisionCache: DecisionCache
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
            decisionCache.clear()
            isInitialized = true
        }
    }

    fun isBlockedFast(input: String): Boolean {
        if (!isInitialized) return false
        decisionCache.get(input)?.let { return it }
        val result = ruleCompiler.isBlocked(input)
        decisionCache.put(input, result)
        return result
    }

    suspend fun isBlocked(input: String): Boolean {
        ensureInitialized()
        decisionCache.get(input)?.let { return it }
        val result = ruleCompiler.isBlocked(input)
        decisionCache.put(input, result)
        return result
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
            decisionCache.clear()
            isInitialized = true
        }
    }

    fun clear() {
        ruleCompiler.clear()
        decisionCache.clear()
        isInitialized = false
    }
}
