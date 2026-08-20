package com.qubeguard.app.ml

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing the unified MLClassifier.
 */
@Module
@InstallIn(SingletonComponent::class)
object MLClassifierModule {

    @Provides
    @Singleton
    fun provideMLClassifier(
        tfLiteClassifier: TfLiteClassifier,
        huggingFaceClassifier: HuggingFaceClassifier
    ): MLClassifier {
        return MLClassifier(tfLiteClassifier, huggingFaceClassifier)
    }
}
