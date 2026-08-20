package com.qubeguard.app.policy

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents user feedback for a blocked/allowed URL/domain.
 * Used for improving the blocking engine and ML model.
 */
@Entity(tableName = "user_feedback")
data class UserFeedback(
    @PrimaryKey
    val id: String, // Unique identifier (e.g., UUID)
    val url: String, // The URL or domain the feedback is for
    val decision: String, // "allow_once", "allow_always", "keep_blocked", "report_false_positive"
    val category: String?, // The category assigned by the ML classifier (if available)
    val confidence: Float?, // The confidence score from the ML classifier (if available)
    val timestamp: String, // Timestamp of the feedback (ISO 8601)
    val qubeId: String?, // The Qube ID this feedback was given in (if applicable)
    val isUploaded: Boolean = false // Whether this feedback has been uploaded to the server
) {
    companion object {
        // Decision types
        const val ALLOW_ONCE = "allow_once"
        const val ALLOW_ALWAYS = "allow_always"
        const val KEEP_BLOCKED = "keep_blocked"
        const val REPORT_FALSE_POSITIVE = "report_false_positive"
    }
}
