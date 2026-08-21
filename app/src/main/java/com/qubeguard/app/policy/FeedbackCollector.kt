package com.qubeguard.app.policy

import com.qubeguard.app.ml.MLClassifier
import java.util.UUID
import javax.inject.Inject

/**
 * Collects user feedback against the same local Transformer used by policy decisions.
 */
class FeedbackCollector @Inject constructor(
    private val feedbackDao: FeedbackDao,
    private val mlClassifier: MLClassifier
) {
    suspend fun logFeedback(url: String, decision: String, qubeId: String? = null) {
        val category = if (mlClassifier.isModelLoaded()) mlClassifier.classify(url) else "Unknown"
        val confidenceScores = if (mlClassifier.isModelLoaded()) {
            mlClassifier.getConfidenceScores(url)
        } else {
            emptyMap()
        }
        val confidence = confidenceScores[category] ?: 0f

        val feedback = UserFeedback(
            id = UUID.randomUUID().toString(),
            url = url,
            decision = decision,
            category = category,
            confidence = confidence,
            timestamp = java.time.Instant.now().toString(),
            qubeId = qubeId,
            isUploaded = false
        )
        feedbackDao.insertFeedback(feedback)
    }

    suspend fun getUnuploadedFeedback(): List<UserFeedback> = feedbackDao.getUnuploadedFeedback()

    suspend fun markAsUploaded(feedbackId: String) {
        feedbackDao.getFeedbackById(feedbackId)?.let {
            feedbackDao.updateFeedback(it.copy(isUploaded = true))
        }
    }

    suspend fun markAllAsUploaded() {
        feedbackDao.getUnuploadedFeedback().forEach {
            feedbackDao.updateFeedback(it.copy(isUploaded = true))
        }
    }

    suspend fun deleteFeedback(feedbackId: String) = feedbackDao.deleteFeedback(feedbackId)
    suspend fun deleteUploadedFeedback() = feedbackDao.deleteUploadedFeedback()
    suspend fun getFalsePositiveCount(): Int = feedbackDao.getFalsePositiveCount()
    suspend fun getAllowAlwaysCount(): Int = feedbackDao.getAllowAlwaysCount()
    suspend fun getFeedbackByUrl(url: String): List<UserFeedback> = feedbackDao.getFeedbackByUrl(url)
    suspend fun getFeedbackByDecision(decision: String): List<UserFeedback> = feedbackDao.getFeedbackByDecision(decision)
    suspend fun getAllFeedback(): List<UserFeedback> = feedbackDao.getAllFeedback()
}
