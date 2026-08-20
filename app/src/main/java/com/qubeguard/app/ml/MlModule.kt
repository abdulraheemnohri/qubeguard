package com.qubeguard.app.ml

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing ML-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object MlModule {

    @Provides
    @Singleton
    fun provideFeatureExtractor(): FeatureExtractor {
        return FeatureExtractor()
    }

    @Provides
    @Singleton
    fun provideTfLiteClassifier(@ApplicationContext context: Context): TfLiteClassifier {
        return TfLiteClassifier(context)
    }

    @Provides
    @Singleton
    fun provideModelDownloader(@ApplicationContext context: Context): ModelDownloader {
        return ModelDownloader(context)
    }
}
