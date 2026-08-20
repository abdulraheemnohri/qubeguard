package com.qubeguard.app.policy

import com.qubeguard.app.ml.TfLiteClassifier
import java.util.UUID
import javax.inject.Inject

/**
 * Collects and manages user feedback for blocked/allowed URLs.
 * Logs feedback locally and optionally uploads it to a server for improving the ML model.
 */
class FeedbackCollector @Inject constructor(
    private val feedbackDao: FeedbackDao,
    private val tfLiteClassifier: TfLiteClassifier
) {

    /**
     * Logs user feedback for a blocked/allowed URL.
     * @param url The URL or domain the feedback is for.
     * @param decision The user's decision (e.g., "allow_once", "allow_always", "report_false_positive").
     * @param qubeId The Qube ID this feedback was given in (optional).
     */
    suspend fun logFeedback(url: String, decision: String, qubeId: String? = null) {
        // Get the ML classifier's category and confidence scores
        val category = tfLiteClassifier.classify(url)
        val confidenceScores = tfLiteClassifier.getConfidenceScores(url)
        val confidence = confidenceScores[category] ?: 0f

        // Create a new feedback entry
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

        // Insert into the database
        feedbackDao.insertFeedback(feedback)
    }

    /**
     * Gets all unuploaded feedback entries.
     * @return A list of UserFeedback objects that haven't been uploaded yet.
     */
    suspend fun getUnuploadedFeedback(): List<UserFeedback> {
        return feedbackDao.getUnuploadedFeedback()
    }

    /**
     * Marks a feedback entry as uploaded.
     * @param feedbackId The ID of the feedback entry to mark as uploaded.
     */
    suspend fun markAsUploaded(feedbackId: String) {
        val feedback = feedbackDao.getFeedbackById(feedbackId)
        if (feedback != null) {
            feedbackDao.updateFeedback(feedback.copy(isUploaded = true))
        }
    }

    /**
     * Marks all feedback entries as uploaded.
     */
    suspend fun markAllAsUploaded() {
        val unuploadedFeedback = feedbackDao.getUnuploadedFeedback()
        for (feedback in unuploadedFeedback) {
            feedbackDao.updateFeedback(feedback.copy(isUploaded = true))
        }
    }

    /**
     * Deletes a feedback entry.
     * @param feedbackId The ID of the feedback entry to delete.
     */
    suspend fun deleteFeedback(feedbackId: String) {
        feedbackDao.deleteFeedback(feedbackId)
    }

    /**
     * Deletes all uploaded feedback entries.
     */
    suspend fun deleteUploadedFeedback() {
        feedbackDao.deleteUploadedFeedback()
    }

    /**
     * Gets the count of false positives reported by the user.
     * @return The number of false positives.
     */
    suspend fun getFalsePositiveCount(): Int {
        return feedbackDao.getFalsePositiveCount()
    }

    /**
     * Gets the count of "allow always" decisions.
     * @return The number of "allow always" decisions.
     */
    suspend fun getAllowAlwaysCount(): Int {
        return feedbackDao.getAllowAlwaysCount()
    }

    /**
     * Gets all feedback for a specific URL.
     * @param url The URL to get feedback for.
     * @return A list of UserFeedback objects for the URL.
     */
    suspend fun getFeedbackByUrl(url: String): List<UserFeedback> {
        return feedbackDao.getFeedbackByUrl(url)
    }

    /**
     * Gets all feedback for a specific decision type.
     * @param decision The decision type (e.g., "allow_once", "report_false_positive").
     * @return A list of UserFeedback objects for the decision type.
     */
    suspend fun getFeedbackByDecision(decision: String): List<UserFeedback> {
        return feedbackDao.getFeedbackByDecision(decision)
    }

    /**
     * Gets all feedback entries.
     * @return A list of all UserFeedback objects.
     */
    suspend fun getAllFeedback(): List<UserFeedback> {
        return feedbackDao.getAllFeedback()
    }
}
