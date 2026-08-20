package com.qubeguard.app.policy

import android.content.Context
import androidx.room.Room
import com.qubeguard.app.ml.MLClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PolicyModule {

    @Provides
    @Singleton
    fun provideFeedbackDatabase(@ApplicationContext context: Context): FeedbackDatabase =
        Room.databaseBuilder(
            context,
            FeedbackDatabase::class.java,
            "feedback_database"
        ).build()

    @Provides
    @Singleton
    fun provideFeedbackDao(database: FeedbackDatabase): FeedbackDao = database.feedbackDao()

    @Provides
    @Singleton
    fun provideFeedbackCollector(
        feedbackDao: FeedbackDao,
        mlClassifier: MLClassifier
    ): FeedbackCollector = FeedbackCollector(feedbackDao, mlClassifier)
}
