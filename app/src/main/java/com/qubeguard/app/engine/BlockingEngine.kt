package com.qubeguard.app.engine

import com.qubeguard.app.data.blocklist.DeterministicBlocker
import com.qubeguard.app.ml.MLClassifier
import com.qubeguard.app.policy.PolicyEngine
import javax.inject.Inject

/**
 * The main Blocking Engine for QubeGuard.
 * Combines Layer 1 (Deterministic), Layer 2 (DNS/VPN), and Layer 3 (ML).
 * 
 * The ML layer can use either:
 * - Local TFLite model (offline)
 * - Hugging Face API (r3ddkahili/final-complete-malicious-url-model)
 */
class BlockingEngine @Inject constructor(
    private val deterministicBlocker: DeterministicBlocker,
    private val mlClassifier: MLClassifier,
    private val policyEngine: PolicyEngine
) {

    /**
     * Checks if a URL/domain is blocked.
     * @param input The URL or domain to check.
     * @param isDnsRequest Whether this is a DNS request (Layer 2).
     * @return A BlockResult object with the decision and details.
     */
    suspend fun checkUrl(input: String, isDnsRequest: Boolean = false): BlockResult {
        // Use the Policy Engine to make the final decision
        val decision = policyEngine.decide(input, isDnsRequest)

        return BlockResult(
            isBlocked = decision.isBlocked,
            reason = decision.reason,
            layer = decision.layer,
            confidence = decision.confidence,
            category = if (decision.isBlocked) policyEngine.getCategory(input) else null
        )
    }

    /**
     * Checks if a URL/domain is blocked (simplified version).
     * @param input The URL or domain to check.
     * @param isDnsRequest Whether this is a DNS request (Layer 2).
     * @return True if the input is blocked.
     */
    suspend fun isBlocked(input: String, isDnsRequest: Boolean = false): Boolean {
        return checkUrl(input, isDnsRequest).isBlocked
    }

    /**
     * Gets the category of a URL/domain (e.g., "Ad", "Tracker", "Malware").
     * @param input The URL or domain to check.
     * @return The predicted category.
     */
    fun getCategory(input: String): String {
        return policyEngine.getCategory(input)
    }

    /**
     * Gets the confidence scores for all categories.
     * @param input The URL or domain to check.
     * @return A map of category to confidence score.
     */
    fun getConfidenceScores(input: String): Map<String, Float> {
        return policyEngine.getConfidenceScores(input)
    }

    /**
     * Enables Hugging Face API for ML classification.
     * Uses r3ddkahili/final-complete-malicious-url-model
     * Requires internet connectivity.
     */
    fun enableHuggingFace() {
        policyEngine.enableHuggingFace()
    }

    /**
     * Disables Hugging Face API and uses local TFLite model.
     */
    fun disableHuggingFace() {
        policyEngine.disableHuggingFace()
    }

    /**
     * Checks if Hugging Face API is enabled.
     */
    fun isHuggingFaceEnabled(): Boolean {
        return policyEngine.isHuggingFaceEnabled()
    }

    /**
     * Sets the Hugging Face API token.
     * Get your token from: https://huggingface.co/settings/tokens
     */
    fun setHuggingFaceToken(token: String) {
        policyEngine.setHuggingFaceToken(token)
    }

    /**
     * Represents the result of a block check.
     */
    data class BlockResult(
        val isBlocked: Boolean,
        val reason: String,
        val layer: Int, // 1 = Deterministic, 2 = DNS/VPN, 3 = ML
        val confidence: Float,
        val category: String? // e.g., "Ad", "Tracker", "Malware"
    )
}
