package com.qubeguard.app.policy

import com.qubeguard.app.data.blocklist.DeterministicBlocker
import com.qubeguard.app.ml.MLClassifier
import javax.inject.Inject

/**
 * Policy Engine for making final block/allow decisions.
 * Combines Layer 1 (Deterministic), Layer 2 (DNS/VPN), and Layer 3 (ML).
 * 
 * The ML layer can use either:
 * - Local TFLite model (offline, fast)
 * - Hugging Face API (online, more accurate with r3ddkahili/final-complete-malicious-url-model)
 */
class PolicyEngine @Inject constructor(
    private val deterministicBlocker: DeterministicBlocker,
    private val mlClassifier: MLClassifier
) {

    // Thresholds for ML-based blocking
    private val mlThresholds = mapOf(
        "Ad" to 0.7f,
        "Tracker" to 0.7f,
        "Malware" to 0.85f,
        "Phishing" to 0.8f,
        "Analytics" to 0.75f
    )

    /**
     * Makes a final decision on whether to block a URL/domain.
     * @param input The URL or domain to check.
     * @param isDnsRequest Whether this is a DNS request (Layer 2).
     * @return A BlockDecision object with the result and reason.
     */
    suspend fun decide(input: String, isDnsRequest: Boolean = false): BlockDecision {
        // Step 1: Check if the input is explicitly allowed (Layer 1)
        if (deterministicBlocker.isAllowed(input)) {
            return BlockDecision(
                isBlocked = false,
                reason = "Allowlisted (Layer 1)",
                layer = 1,
                confidence = 1.0f
            )
        }

        // Step 2: Check if the input is blocked by deterministic rules (Layer 1)
        if (deterministicBlocker.isBlocked(input)) {
            return BlockDecision(
                isBlocked = true,
                reason = "Blocked by deterministic rules (Layer 1)",
                layer = 1,
                confidence = 1.0f
            )
        }

        // Step 3: For DNS requests (Layer 2), use deterministic blocking only
        if (isDnsRequest) {
            return BlockDecision(
                isBlocked = false,
                reason = "Allowed (Layer 2 - DNS)",
                layer = 2,
                confidence = 1.0f
            )
        }

        // Step 4: Check ML-based blocking (Layer 3)
        val category = mlClassifier.classify(input)
        val confidenceScores = mlClassifier.getConfidenceScores(input)

        if (category != "Legitimate" && category != "Analytics") {
            val confidence = confidenceScores[category] ?: 0f
            val threshold = mlThresholds[category] ?: 0.5f

            if (confidence >= threshold) {
                return BlockDecision(
                    isBlocked = true,
                    reason = "Blocked by ML classifier (Layer 3 - $category)",
                    layer = 3,
                    confidence = confidence
                )
            }
        }

        // Step 5: Default decision (allow)
        return BlockDecision(
            isBlocked = false,
            reason = "Allowed (No match in any layer)",
            layer = 0,
            confidence = 0f
        )
    }

    /**
     * Checks if a URL/domain is blocked without providing a reason.
     * @param input The URL or domain to check.
     * @param isDnsRequest Whether this is a DNS request (Layer 2).
     * @return True if the input is blocked.
     */
    suspend fun isBlocked(input: String, isDnsRequest: Boolean = false): Boolean {
        return decide(input, isDnsRequest).isBlocked
    }

    /**
     * Gets the category of a URL/domain (e.g., "Ad", "Tracker", "Malware").
     * @param input The URL or domain to check.
     * @return The predicted category.
     */
    fun getCategory(input: String): String {
        return mlClassifier.classify(input)
    }

    /**
     * Gets the confidence scores for all categories.
     * @param input The URL or domain to check.
     * @return A map of category to confidence score.
     */
    fun getConfidenceScores(input: String): Map<String, Float> {
        return mlClassifier.getConfidenceScores(input)
    }

    /**
     * Enables Hugging Face API for ML classification.
     * Uses r3ddkahili/final-complete-malicious-url-model
     * Requires internet connectivity.
     */
    fun enableHuggingFace() {
        mlClassifier.enableHuggingFace()
    }

    /**
     * Disables Hugging Face API and uses local TFLite model.
     */
    fun disableHuggingFace() {
        mlClassifier.disableHuggingFace()
    }

    /**
     * Checks if Hugging Face API is enabled.
     */
    fun isHuggingFaceEnabled(): Boolean {
        return mlClassifier.isHuggingFaceEnabled()
    }

    /**
     * Sets the Hugging Face API token.
     * Get your token from: https://huggingface.co/settings/tokens
     */
    fun setHuggingFaceToken(token: String) {
        mlClassifier.setHuggingFaceToken(token)
    }

    /**
     * Represents a block/allow decision.
     */
    data class BlockDecision(
        val isBlocked: Boolean,
        val reason: String,
        val layer: Int, // 1 = Deterministic, 2 = DNS/VPN, 3 = ML
        val confidence: Float
    )
}
