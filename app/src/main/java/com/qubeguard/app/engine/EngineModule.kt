package com.qubeguard.app.engine

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing Blocking Engine and QubeGuard Service dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideBlockingEngine(
        deterministicBlocker: com.qubeguard.app.data.blocklist.DeterministicBlocker,
        mlClassifier: com.qubeguard.app.ml.MLClassifier,
        policyEngine: com.qubeguard.app.policy.PolicyEngine
    ): BlockingEngine {
        return BlockingEngine(deterministicBlocker, mlClassifier, policyEngine)
    }
}
