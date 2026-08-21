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
import com.qubeguard.app.data.blocklist.BlocklistCatalog
import com.qubeguard.app.data.blocklist.BlocklistDao
import com.qubeguard.app.data.blocklist.BlocklistFetcherWorker
import com.qubeguard.app.ml.ModelUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class QubeGuardApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var blocklistDao: BlocklistDao

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        seedDefaultBlocklists()
        scheduleBlocklistUpdates()
        // AI is optional and OFF by default. Layer 1/2 do not depend on model availability.
    }

    private fun seedDefaultBlocklists() {
        applicationScope.launch {
            val existing = blocklistDao.getAllSources().associateBy { it.id }
            val missing = BlocklistCatalog.defaults.filter { it.id !in existing }
            if (missing.isNotEmpty()) {
                blocklistDao.insertSources(missing)
            }
        }
    }

    private fun scheduleBlocklistUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val initial = OneTimeWorkRequestBuilder<BlocklistFetcherWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            BlocklistFetcherWorker.WORK_NAME + "-initial",
            ExistingWorkPolicy.KEEP,
            initial
        )

        val periodic = PeriodicWorkRequestBuilder<BlocklistFetcherWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            BlocklistFetcherWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic
        )
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
