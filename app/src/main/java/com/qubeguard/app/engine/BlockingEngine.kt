package com.qubeguard.app.engine

import com.qubeguard.app.policy.PolicyEngine
import javax.inject.Inject

/**
 * Main QubeGuard blocking facade.
 * Layer 3 is the local Transformer runtime; there is no remote inference path.
 */
class BlockingEngine @Inject constructor(
    private val policyEngine: PolicyEngine
) {
    suspend fun checkUrl(input: String, isDnsRequest: Boolean = false): BlockResult {
        val decision = policyEngine.decide(input, isDnsRequest)
        return BlockResult(
            isBlocked = decision.isBlocked,
            reason = decision.reason,
            layer = decision.layer,
            confidence = decision.confidence,
            category = if (decision.isBlocked) policyEngine.getCategory(input) else null
        )
    }

    suspend fun isBlocked(input: String, isDnsRequest: Boolean = false): Boolean =
        checkUrl(input, isDnsRequest).isBlocked

    fun getCategory(input: String): String = policyEngine.getCategory(input)

    fun getConfidenceScores(input: String): Map<String, Float> =
        policyEngine.getConfidenceScores(input)

    data class BlockResult(
        val isBlocked: Boolean,
        val reason: String,
        val layer: Int,
        val confidence: Float,
        val category: String?
    )
}
