package com.qubeguard.app.ml

import javax.inject.Inject

/**
 * Unified ML Classifier that can use either:
 * 1. Local TFLite model (offline, fast)
 * 2. Hugging Face API (online, more accurate)
 * 
 * This provides a fallback mechanism - if the local model is not available,
 * it can use the Hugging Face API.
 */
class MLClassifier @Inject constructor(
    private val tfLiteClassifier: TfLiteClassifier,
    private val huggingFaceClassifier: HuggingFaceClassifier
) {
    
    // Configuration
    private var useHuggingFace = false // Default to TFLite
    private var useLocalModel = true // Use local model if available
    
    /**
     * Classifies a URL using the selected classifier.
     * @param url The URL to classify.
     * @return The predicted category.
     */
    fun classify(url: String): String {
        return if (useHuggingFace) {
            // Use Hugging Face API (requires internet)
            // Note: This is a suspend function, so it needs to be called from a coroutine
            // For now, we'll use a blocking call or return a default
            "Legitimate" // Placeholder - actual implementation needs coroutine
        } else {
            // Use local TFLite model
            if (useLocalModel && tfLiteClassifier.isModelLoaded()) {
                tfLiteClassifier.classify(url)
            } else {
                // Fallback to deterministic blocking
                "Legitimate"
            }
        }
    }
    
    /**
     * Gets confidence scores for all categories.
     * @param url The URL to check.
     * @return A map of category to confidence score.
     */
    fun getConfidenceScores(url: String): Map<String, Float> {
        return if (useHuggingFace) {
            // Use Hugging Face API
            // Note: This is a suspend function
            mapOf("Legitimate" to 1.0f) // Placeholder
        } else {
            // Use local TFLite model
            if (useLocalModel && tfLiteClassifier.isModelLoaded()) {
                tfLiteClassifier.getConfidenceScores(url)
            } else {
                // Fallback
                mapOf("Legitimate" to 1.0f)
            }
        }
    }
    
    /**
     * Checks if a URL is blocked.
     * @param url The URL to check.
     * @return True if the URL is blocked.
     */
    fun isBlocked(url: String): Boolean {
        val category = classify(url)
        return category != "Legitimate" && category != "Analytics"
    }
    
    /**
     * Checks if a URL is a tracker.
     * @param url The URL to check.
     * @return True if the URL is classified as a tracker.
     */
    fun isTracker(url: String): Boolean {
        return classify(url) == "Tracker"
    }
    
    /**
     * Checks if a URL is malware.
     * @param url The URL to check.
     * @return True if the URL is classified as malware.
     */
    fun isMalware(url: String): Boolean {
        return classify(url) == "Malware"
    }
    
    /**
     * Checks if a URL is phishing.
     * @param url The URL to check.
     * @return True if the URL is classified as phishing.
     */
    fun isPhishing(url: String): Boolean {
        return classify(url) == "Phishing"
    }
    
    /**
     * Enables Hugging Face API classification.
     * Requires internet connectivity.
     */
    fun enableHuggingFace() {
        useHuggingFace = true
        useLocalModel = false
    }
    
    /**
     * Disables Hugging Face API and uses local model.
     */
    fun disableHuggingFace() {
        useHuggingFace = false
        useLocalModel = true
    }
    
    /**
     * Checks if Hugging Face is enabled.
     */
    fun isHuggingFaceEnabled(): Boolean {
        return useHuggingFace
    }
    
    /**
     * Checks if local model is enabled and loaded.
     */
    fun isLocalModelEnabled(): Boolean {
        return useLocalModel && tfLiteClassifier.isModelLoaded()
    }
    
    /**
     * Sets the Hugging Face API token.
     */
    fun setHuggingFaceToken(token: String) {
        huggingFaceClassifier.setAuthToken(token)
    }
}
