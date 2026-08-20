package com.qubeguard.app.policy

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing Policy Engine and Feedback System dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object PolicyModule {

    @Provides
    @Singleton
    fun provideFeedbackDatabase(@ApplicationContext context: Context): FeedbackDatabase {
        return Room.databaseBuilder(
            context,
            FeedbackDatabase::class.java,
            "feedback_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideFeedbackDao(database: FeedbackDatabase): FeedbackDao {
        return database.feedbackDao()
    }

    @Provides
    @Singleton
    fun provideFeedbackCollector(
        feedbackDao: FeedbackDao,
        tfLiteClassifier: com.qubeguard.app.ml.TfLiteClassifier
    ): FeedbackCollector {
        return FeedbackCollector(feedbackDao, tfLiteClassifier)
    }
}
