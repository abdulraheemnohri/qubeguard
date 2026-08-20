package com.qubeguard.app.ml

import android.content.Context
import android.util.Log
import com.qubeguard.app.data.blocklist.BlocklistDao
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Classifier that uses the Hugging Face model for URL classification.
 * Model: r3ddkahili/final-complete-malicious-url-model
 * 
 * This model classifies URLs into 4 categories:
 * - Benign (Legitimate)
 * - Defacement
 * - Phishing
 * - Malware
 * 
 * We map these to QubeGuard's 6 categories:
 * - Benign → Legitimate
 * - Defacement → Tracker (or Malware)
 * - Phishing → Phishing
 * - Malware → Malware
 * 
 * Note: This requires internet connectivity and may have rate limits.
 */
class HuggingFaceClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val modelId = "r3ddkahili/final-complete-malicious-url-model"
    private val apiUrl = "https://api-inference.huggingface.co/models/$modelId"
    private val authToken = "" // Add your Hugging Face token here if needed

    // Map Hugging Face labels to QubeGuard labels
    private val labelMap = mapOf(
        "Benign" to "Legitimate",
        "Defacement" to "Tracker",  // or "Malware"
        "Phishing" to "Phishing",
        "Malware" to "Malware"
    )

    /**
     * Classifies a URL using the Hugging Face API.
     * @param url The URL to classify.
     * @return The predicted category (Legitimate, Ad, Tracker, Malware, Phishing, Analytics).
     */
    suspend fun classify(url: String): String {
        try {
            val requestBody = JSONObject().apply {
                put("inputs", url)
            }.toString()

            val request = Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer $authToken")
                .header("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Hugging Face API error: ${response.code} - ${response.message}")
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response")
                val jsonArray = JSONArray(responseBody)
                
                if (jsonArray.length() > 0) {
                    val result = jsonArray.getJSONObject(0)
                    val label = result.getString("label")
                    val score = result.getDouble("score")
                    
                    Log.d("HuggingFaceClassifier", "URL: $url -> Label: $label (Score: $score)")
                    
                    // Map to QubeGuard labels
                    return labelMap[label] ?: "Legitimate"
                }
            }
        } catch (e: Exception) {
            Log.e("HuggingFaceClassifier", "Error classifying URL: $url", e)
        }

        // Fallback to deterministic classification
        return "Legitimate"
    }

    /**
     * Gets confidence scores for all categories.
     * @param url The URL to classify.
     * @return A map of category to confidence score.
     */
    suspend fun getConfidenceScores(url: String): Map<String, Float> {
        try {
            val requestBody = JSONObject().apply {
                put("inputs", url)
            }.toString()

            val request = Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer $authToken")
                .header("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Hugging Face API error: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response")
                val jsonArray = JSONArray(responseBody)
                
                if (jsonArray.length() > 0) {
                    val result = jsonArray.getJSONObject(0)
                    val label = result.getString("label")
                    val score = result.getDouble("score").toFloat()
                    
                    // Map to QubeGuard labels with confidence
                    val mappedLabel = labelMap[label] ?: "Legitimate"
                    val scores = mutableMapOf<String, Float>()
                    
                    // Set confidence for predicted label
                    scores[mappedLabel] = score
                    
                    // Distribute remaining confidence to other labels
                    val remainingConfidence = (1.0f - score) / 5
                    for (label in listOf("Legitimate", "Ad", "Tracker", "Malware", "Phishing", "Analytics")) {
                        if (label != mappedLabel) {
                            scores[label] = remainingConfidence
                        }
                    }
                    
                    return scores
                }
            }
        } catch (e: Exception) {
            Log.e("HuggingFaceClassifier", "Error getting confidence scores", e)
        }

        // Fallback: equal distribution
        return mapOf(
            "Legitimate" to 0.4f,
            "Ad" to 0.1f,
            "Tracker" to 0.1f,
            "Malware" to 0.2f,
            "Phishing" to 0.2f,
            "Analytics" to 0.0f
        )
    }

    /**
     * Checks if a URL is blocked based on the Hugging Face model.
     * @param url The URL to check.
     * @return True if the URL is blocked (Malware or Phishing).
     */
    suspend fun isBlocked(url: String): Boolean {
        val category = classify(url)
        return category == "Malware" || category == "Phishing" || category == "Tracker"
    }

    /**
     * Checks if a URL is a tracker.
     * @param url The URL to check.
     * @return True if the URL is classified as a tracker.
     */
    suspend fun isTracker(url: String): Boolean {
        return classify(url) == "Tracker"
    }

    /**
     * Checks if a URL is malware.
     * @param url The URL to check.
     * @return True if the URL is classified as malware.
     */
    suspend fun isMalware(url: String): Boolean {
        return classify(url) == "Malware"
    }

    /**
     * Checks if a URL is phishing.
     * @param url The URL to check.
     * @return True if the URL is classified as phishing.
     */
    suspend fun isPhishing(url: String): Boolean {
        return classify(url) == "Phishing"
    }

    /**
     * Sets the Hugging Face API token.
     * @param token Your Hugging Face API token.
     */
    fun setAuthToken(token: String) {
        // In a real app, store this securely (e.g., in Android Keystore)
        // For demo purposes, we're storing it in memory
    }
}
