package com.qubeguard.app.ml

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified local ML facade.
 * The only ML inference backend is the on-device Transformer classifier.
 */
@Singleton
class MLClassifier @Inject constructor(
    private val transformer: TransformerUrlClassifier
) {
    fun loadModel(): Boolean = transformer.load()

    fun isModelLoaded(): Boolean = transformer.isLoaded()

    fun classify(url: String): String = transformer.classify(url).label

    fun getConfidenceScores(url: String): Map<String, Float> {
        val prediction = transformer.classify(url)
        return prediction.probabilities.mapIndexed { index, score ->
            TransformerUrlClassifier.run {
                when (index) {
                    0 -> BENIGN
                    1 -> DEFACEMENT
                    2 -> PHISHING
                    3 -> MALWARE
                    else -> "Unknown"
                }
            } to score
        }.toMap()
    }

    fun isBlocked(url: String): Boolean = transformer.isBlocked(url)

    fun isTracker(url: String): Boolean = false

    fun isMalware(url: String): Boolean = classify(url) == TransformerUrlClassifier.MALWARE

    fun isPhishing(url: String): Boolean = classify(url) == TransformerUrlClassifier.PHISHING

    fun close() = transformer.close()
}
