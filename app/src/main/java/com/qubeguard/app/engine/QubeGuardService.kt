package com.qubeguard.app.engine

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.qubeguard.app.data.blocklist.BlocklistFetcherWorker
import com.qubeguard.app.ml.MLClassifier
import com.qubeguard.app.ml.ModelDownloader
import com.qubeguard.app.vpn.VpnServiceImplementation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Coordinates protection. Optional AI never blocks deterministic protection. */
@AndroidEntryPoint
class QubeGuardService : Service() {

    @Inject lateinit var mlClassifier: MLClassifier
    @Inject lateinit var modelDownloader: ModelDownloader

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                setProtectionActive(false)
                stopSelf()
            }
            ACTION_START, null -> {
                setProtectionActive(true)
                startProtection()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        setProtectionActive(false)
        stopVpnService()
        mlClassifier.close()
        scope.cancel()
        super.onDestroy()
    }

    private fun setProtectionActive(active: Boolean) {
        val prefs = getSharedPreferences("qubeguard_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_protection_active", active).apply()
    }

    private fun startProtection() {
        if (running) return
        running = true
        startVpnService()
        scheduleBlocklistUpdates()

        scope.launch {
            runCatching {
                if (modelDownloader.isModelReady()) mlClassifier.loadModel()
            }
        }
    }

    private fun startVpnService() {
        startService(Intent(this, VpnServiceImplementation::class.java))
    }

    private fun stopVpnService() {
        stopService(Intent(this, VpnServiceImplementation::class.java))
    }

    private fun scheduleBlocklistUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<BlocklistFetcherWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            BlocklistFetcherWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun isRunning(): Boolean = running

    companion object {
        const val ACTION_START = "com.qubeguard.app.engine.QubeGuardService.ACTION_START"
        const val ACTION_STOP = "com.qubeguard.app.engine.QubeGuardService.ACTION_STOP"
    }
}
