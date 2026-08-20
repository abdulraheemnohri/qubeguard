package com.qubeguard.app.ml

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MLModule {

    @Provides
    @Singleton
    fun provideTransformerClassifier(
        modelDownloader: ModelDownloader
    ): TransformerUrlClassifier = TransformerUrlClassifier(modelDownloader)

    @Provides
    @Singleton
    fun provideMLClassifier(
        transformer: TransformerUrlClassifier
    ): MLClassifier = MLClassifier(transformer)

    @Provides
    @Singleton
    fun provideFeatureExtractor(): FeatureExtractor = FeatureExtractor()

    @Provides
    @Singleton
    fun provideModelDownloader(
        context: android.content.Context
    ): ModelDownloader = ModelDownloader(context)
}
