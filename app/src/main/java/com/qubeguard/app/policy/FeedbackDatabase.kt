package com.qubeguard.app.policy

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room Database for QubeGuard's user feedback system.
 * Includes UserFeedback entity.
 */
@Database(
    entities = [UserFeedback::class],
    version = 1,
    exportSchema = false
)
abstract class FeedbackDatabase : RoomDatabase() {
    abstract fun feedbackDao(): FeedbackDao
}
