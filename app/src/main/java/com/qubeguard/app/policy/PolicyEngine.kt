package com.qubeguard.app.policy

import com.qubeguard.app.data.blocklist.DeterministicBlocker
import com.qubeguard.app.ml.MLClassifier
import com.qubeguard.app.ml.TransformerUrlClassifier
import javax.inject.Inject

/**
 * Final policy engine. Layer 3 is exclusively the local BERT Transformer
 * from r3ddkahili/final-complete-malicious-url-model.
 */
class PolicyEngine @Inject constructor(
    private val deterministicBlocker: DeterministicBlocker,
    private val mlClassifier: MLClassifier
) {
    private val mlThresholds = mapOf(
        TransformerUrlClassifier.DEFACEMENT to 0.80f,
        TransformerUrlClassifier.PHISHING to 0.80f,
        TransformerUrlClassifier.MALWARE to 0.85f
    )

    suspend fun decide(input: String, isDnsRequest: Boolean = false): BlockDecision {
        if (deterministicBlocker.isAllowed(input)) {
            return BlockDecision(false, "Allowlisted (Layer 1)", 1, 1.0f)
        }

        if (deterministicBlocker.isBlocked(input)) {
            return BlockDecision(true, "Blocked by deterministic rules (Layer 1)", 1, 1.0f)
        }

        if (isDnsRequest) {
            return BlockDecision(false, "Allowed (Layer 2 - DNS)", 2, 1.0f)
        }

        if (!mlClassifier.isModelLoaded()) {
            return BlockDecision(false, "ML model unavailable; deterministic policy only", 0, 0f)
        }

        val category = mlClassifier.classify(input)
        val confidence = mlClassifier.getConfidenceScores(input)[category] ?: 0f
        val threshold = mlThresholds[category]

        if (threshold != null && confidence >= threshold) {
            return BlockDecision(
                isBlocked = true,
                reason = "Blocked by local Transformer (Layer 3 - $category)",
                layer = 3,
                confidence = confidence
            )
        }

        return BlockDecision(false, "Allowed (no deterministic or Transformer match)", 0, confidence)
    }

    suspend fun isBlocked(input: String, isDnsRequest: Boolean = false): Boolean =
        decide(input, isDnsRequest).isBlocked

    fun getCategory(input: String): String =
        if (mlClassifier.isModelLoaded()) mlClassifier.classify(input) else "Unknown"

    fun getConfidenceScores(input: String): Map<String, Float> =
        if (mlClassifier.isModelLoaded()) mlClassifier.getConfidenceScores(input) else emptyMap()

    data class BlockDecision(
        val isBlocked: Boolean,
        val reason: String,
        val layer: Int,
        val confidence: Float
    )
}
