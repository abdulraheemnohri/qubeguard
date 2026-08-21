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
        val labels = listOf(
            TransformerUrlClassifier.BENIGN,
            TransformerUrlClassifier.DEFACEMENT,
            TransformerUrlClassifier.PHISHING,
            TransformerUrlClassifier.MALWARE
        )
        return prediction.probabilities.mapIndexed { index, score ->
            labels.getOrElse(index) { "Unknown" } to score
        }.toMap()
    }

    fun isBlocked(url: String): Boolean = transformer.isBlocked(url)

    /** The selected model has no tracker class; deterministic lists handle trackers. */
    fun isTracker(url: String): Boolean = false

    fun isMalware(url: String): Boolean = classify(url) == TransformerUrlClassifier.MALWARE

    fun isPhishing(url: String): Boolean = classify(url) == TransformerUrlClassifier.PHISHING

    fun close() = transformer.close()
}
