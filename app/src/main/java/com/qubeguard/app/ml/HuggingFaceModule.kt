package com.qubeguard.app.ml

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing Hugging Face classifier.
 */
@Module
@InstallIn(SingletonComponent::class)
object HuggingFaceModule {

    @Provides
    @Singleton
    fun provideHuggingFaceClassifier(@ApplicationContext context: Context): HuggingFaceClassifier {
        return HuggingFaceClassifier(context)
    }
}
