package com.qubeguard.app.engine

import com.qubeguard.app.policy.PolicyEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {
    @Provides
    @Singleton
    fun provideBlockingEngine(policyEngine: PolicyEngine): BlockingEngine =
        BlockingEngine(policyEngine)
}
