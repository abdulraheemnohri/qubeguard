package com.qubeguard.app.ml

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing all ML-related dependencies.
 * This includes both TFLite and Hugging Face classifiers.
 */
@Module
@InstallIn(SingletonComponent::class)
object MLModule {

    @Provides
    @Singleton
    fun provideTfLiteClassifier(@ApplicationContext context: Context): TfLiteClassifier {
        return TfLiteClassifier(context)
    }

    @Provides
    @Singleton
    fun provideHuggingFaceClassifier(@ApplicationContext context: Context): HuggingFaceClassifier {
        return HuggingFaceClassifier(context)
    }

    @Provides
    @Singleton
    fun provideMLClassifier(
        tfLiteClassifier: TfLiteClassifier,
        huggingFaceClassifier: HuggingFaceClassifier
    ): MLClassifier {
        return MLClassifier(tfLiteClassifier, huggingFaceClassifier)
    }

    @Provides
    @Singleton
    fun provideFeatureExtractor(): FeatureExtractor {
        return FeatureExtractor()
    }

    @Provides
    @Singleton
    fun provideModelDownloader(@ApplicationContext context: Context): ModelDownloader {
        return ModelDownloader(context)
    }
}
