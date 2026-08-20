package com.qubeguard.app.engine

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.qubeguard.app.data.blocklist.BlocklistFetcherWorker
import com.qubeguard.app.ml.ModelDownloader
import com.qubeguard.app.ml.TfLiteClassifier
import com.qubeguard.app.vpn.VpnServiceImplementation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main service for QubeGuard.
 * Manages the VPN, DNS Proxy, Blocking Engine, and Blocklist Updates.
 */
@AndroidEntryPoint
class QubeGuardService : Service() {

    @Inject
    lateinit var tfLiteClassifier: TfLiteClassifier

    @Inject
    lateinit var modelDownloader: ModelDownloader

    private var isServiceRunning = false
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true

        // Initialize the TFLite model
        scope.launch {
            initializeModel()
        }

        // Start the VPN service
        startVpnService()

        // Schedule blocklist updates
        scheduleBlocklistUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        stopVpnService()
    }

    /**
     * Initializes the TFLite model.
     */
    private suspend fun initializeModel() {
        // Check if the model is already downloaded
        if (!modelDownloader.isModelDownloaded()) {
            // Download the model
            modelDownloader.downloadModel()
        }

        // Load the model into the classifier
        modelDownloader.getModelFile()?.let { modelFile ->
            tfLiteClassifier.loadModel(modelFile.path)
        }
    }

    /**
     * Starts the VPN service.
     */
    private fun startVpnService() {
        val intent = Intent(this, VpnServiceImplementation::class.java)
        startService(intent)
    }

    /**
     * Stops the VPN service.
     */
    private fun stopVpnService() {
        val intent = Intent(this, VpnServiceImplementation::class.java)
        stopService(intent)
    }

    /**
     * Schedules periodic blocklist updates.
     */
    private fun scheduleBlocklistUpdates() {
        // Use WorkManager to schedule periodic updates
        // This is a placeholder; actual implementation would use WorkManager
    }

    /**
     * Starts the blocklist fetcher worker.
     */
    fun startBlocklistUpdates() {
        // Start the BlocklistFetcherWorker using WorkManager
        // This is a placeholder; actual implementation would use WorkManager
    }

    /**
     * Stops the blocklist fetcher worker.
     */
    fun stopBlocklistUpdates() {
        // Stop the BlocklistFetcherWorker
        // This is a placeholder; actual implementation would use WorkManager
    }

    /**
     * Checks if the service is running.
     */
    fun isRunning(): Boolean {
        return isServiceRunning
    }

    companion object {
        const val ACTION_START = "com.qubeguard.app.engine.QubeGuardService.ACTION_START"
        const val ACTION_STOP = "com.qubeguard.app.engine.QubeGuardService.ACTION_STOP"
    }
}
