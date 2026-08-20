package com.qubeguard.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
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
class QubeGuardApp : Application(), androidx.work.Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleModelUpdates()
    }

    private fun networkConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    private fun scheduleModelUpdates() {
        val initialRequest = OneTimeWorkRequestBuilder<ModelUpdateWorker>()
            .setConstraints(networkConstraints())
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "qubeguard-transformer-model-initial-download",
            ExistingWorkPolicy.KEEP,
            initialRequest
        )

        val periodicRequest = PeriodicWorkRequestBuilder<ModelUpdateWorker>(1, TimeUnit.DAYS)
            .setConstraints(networkConstraints())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "qubeguard-transformer-model-update",
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }
}
