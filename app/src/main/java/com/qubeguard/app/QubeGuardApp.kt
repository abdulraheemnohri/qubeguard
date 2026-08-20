package com.qubeguard.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.qubeguard.app.ml.ModelUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class QubeGuardApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // AI is optional and OFF by default. Layer 1/2 do not depend on model availability.
    }

    fun enableAutomaticModelUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        val initial = OneTimeWorkRequestBuilder<ModelUpdateWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            MODEL_INITIAL_WORK,
            ExistingWorkPolicy.KEEP,
            initial
        )

        val periodic = PeriodicWorkRequestBuilder<ModelUpdateWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            MODEL_PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic
        )
    }

    fun disableAutomaticModelUpdates() {
        WorkManager.getInstance(this).cancelUniqueWork(MODEL_INITIAL_WORK)
        WorkManager.getInstance(this).cancelUniqueWork(MODEL_PERIODIC_WORK)
    }

    companion object {
        const val MODEL_INITIAL_WORK = "qubeguard-transformer-model-initial-download"
        const val MODEL_PERIODIC_WORK = "qubeguard-transformer-model-update"
    }
}
