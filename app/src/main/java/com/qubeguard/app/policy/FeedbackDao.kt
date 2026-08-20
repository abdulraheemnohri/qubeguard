package com.qubeguard.app.policy

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object (DAO) for UserFeedback.
 * Provides methods to interact with the Room Database for user feedback.
 */
@Dao
interface FeedbackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: UserFeedback)

    @Update
    suspend fun updateFeedback(feedback: UserFeedback)

    @Query("SELECT * FROM user_feedback WHERE id = :id")
    suspend fun getFeedbackById(id: String): UserFeedback?

    @Query("SELECT * FROM user_feedback WHERE url = :url ORDER BY timestamp DESC")
    suspend fun getFeedbackByUrl(url: String): List<UserFeedback>

    @Query("SELECT * FROM user_feedback WHERE decision = :decision ORDER BY timestamp DESC")
    suspend fun getFeedbackByDecision(decision: String): List<UserFeedback>

    @Query("SELECT * FROM user_feedback WHERE isUploaded = 0")
    suspend fun getUnuploadedFeedback(): List<UserFeedback>

    @Query("SELECT * FROM user_feedback ORDER BY timestamp DESC")
    suspend fun getAllFeedback(): List<UserFeedback>

    @Query("DELETE FROM user_feedback WHERE id = :id")
    suspend fun deleteFeedback(id: String)

    @Query("DELETE FROM user_feedback WHERE isUploaded = 1")
    suspend fun deleteUploadedFeedback()

    @Query("SELECT COUNT(*) FROM user_feedback WHERE decision = 'report_false_positive'")
    suspend fun getFalsePositiveCount(): Int

    @Query("SELECT COUNT(*) FROM user_feedback WHERE decision = 'allow_always'")
    suspend fun getAllowAlwaysCount(): Int
}
