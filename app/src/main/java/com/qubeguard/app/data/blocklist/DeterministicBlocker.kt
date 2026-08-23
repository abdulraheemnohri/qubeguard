package com.qubeguard.app.data.blocklist

import javax.inject.Inject

/**
 * Layer 1: Deterministic Blocker.
 * Uses compiled rules (Radix Tree, Bloom Filter, Regex Engine) for fast, deterministic blocking.
 */
class DeterministicBlocker @Inject constructor(
    private val ruleCompiler: RuleCompiler,
    private val blocklistDao: BlocklistDao
) {
    private var isInitialized = false

    /**
     * Initializes the Deterministic Blocker by compiling all blocklist rules.
     */
    suspend fun initialize() {
        if (isInitialized) return

        val blocklistRules = blocklistDao.getAllBlocklistRules()
        val allowlistRules = blocklistDao.getAllAllowlistRules()

        // Combine blocklist and allowlist rules
        val allRules = blocklistRules + allowlistRules

        // Compile rules into optimized data structures
        ruleCompiler.compileRules(allRules)

        isInitialized = true
    }

    /**
     * Fast non-blocking check using compiled in-memory structures.
     */
    fun isBlockedFast(input: String): Boolean {
        if (!isInitialized) return false
        if (ruleCompiler.isAllowed(input)) return false
        return ruleCompiler.isBlocked(input)
    }

    /**
     * Checks if a domain or URL is blocked by the deterministic rules.
     * @param input The domain or URL to check.
     * @return True if the input is blocked.
     */
    suspend fun isBlocked(input: String): Boolean {
        if (!isInitialized) {
            initialize()
        }

        // First, check if the input is explicitly allowed
        if (ruleCompiler.isAllowed(input)) {
            return false
        }

        // Then, check if the input is blocked
        return ruleCompiler.isBlocked(input)
    }

    /**
     * Checks if a domain or URL is allowed (whitelisted).
     * @param input The domain or URL to check.
     * @return True if the input is allowed.
     */
    suspend fun isAllowed(input: String): Boolean {
        if (!isInitialized) {
            initialize()
        }
        return ruleCompiler.isAllowed(input)
    }

    /**
     * Recompiles the rules after an update.
     */
    suspend fun recompileRules() {
        isInitialized = false
        initialize()
    }

    /**
     * Clears all compiled rules.
     */
    fun clear() {
        ruleCompiler.clear()
        isInitialized = false
    }
}
