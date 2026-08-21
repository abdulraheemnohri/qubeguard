package com.qubeguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.qubeguard.app.browser.QubeManager
import com.qubeguard.app.browser.QubeProfile
import com.qubeguard.app.data.blocklist.BlocklistDao
import com.qubeguard.app.data.blocklist.BlocklistFetcherWorker
import com.qubeguard.app.data.blocklist.BlocklistSource
import com.qubeguard.app.ml.MLClassifier
import com.qubeguard.app.ml.ModelDownloader
import com.qubeguard.app.policy.FeedbackCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val blocklistDao: BlocklistDao,
    private val qubeManager: QubeManager,
    private val mlClassifier: MLClassifier,
    private val modelDownloader: ModelDownloader,
    private val feedbackCollector: FeedbackCollector
) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences(PREFERENCES, Application.MODE_PRIVATE)
    private val _blocklistSources = MutableLiveData<List<BlocklistSource>>(emptyList())
    val blocklistSources: LiveData<List<BlocklistSource>> = _blocklistSources
    private val _qubeProfiles = MutableLiveData<List<QubeProfile>>(emptyList())
    val qubeProfiles: LiveData<List<QubeProfile>> = _qubeProfiles
    private val _isMlEnabled = MutableLiveData(preferences.getBoolean(KEY_AI_ENABLED, false))
    val isMlEnabled: LiveData<Boolean> = _isMlEnabled
    private val _isAutoModelUpdateEnabled = MutableLiveData(preferences.getBoolean(KEY_AUTO_UPDATE, false))
    val isAutoModelUpdateEnabled: LiveData<Boolean> = _isAutoModelUpdateEnabled
    private val _isTelemetryEnabled = MutableLiveData(preferences.getBoolean(KEY_TELEMETRY, false))
    val isTelemetryEnabled: LiveData<Boolean> = _isTelemetryEnabled

    private val _falsePositiveCount = MutableLiveData(0)
    val falsePositiveCount: LiveData<Int> = _falsePositiveCount
    private val _allowAlwaysCount = MutableLiveData(0)
    val allowAlwaysCount: LiveData<Int> = _allowAlwaysCount
    private val _totalRuleCount = MutableLiveData(0)
    val totalRuleCount: LiveData<Int> = _totalRuleCount

    init {
        loadBlocklistSources()
        loadQubeProfiles()
        loadFeedbackStats()
        loadTotalRuleCount()
    }

    fun loadBlocklistSources() { viewModelScope.launch { _blocklistSources.value = blocklistDao.getAllSources() } }
    private fun loadQubeProfiles() { viewModelScope.launch { _qubeProfiles.value = qubeManager.getAllQubes() } }
    fun loadFeedbackStats() {
        viewModelScope.launch {
            _falsePositiveCount.value = feedbackCollector.getFalsePositiveCount()
            _allowAlwaysCount.value = feedbackCollector.getAllowAlwaysCount()
        }
    }
    fun loadTotalRuleCount() {
        viewModelScope.launch {
            _totalRuleCount.value = blocklistDao.getTotalRuleCount()
        }
    }

    fun setBlocklistSourceEnabled(sourceId: String, enabled: Boolean) {
        viewModelScope.launch {
            blocklistDao.getSourceById(sourceId)?.let { blocklistDao.updateSource(it.copy(enabled = enabled)) }
            loadBlocklistSources()
        }
    }

    fun addCustomBlocklistSource(name: String, url: String, category: String = "custom", format: String = "adblock_plus") {
        viewModelScope.launch {
            val id = "custom_" + sha256(url).substring(0, 10)
            val source = BlocklistSource(
                id = id,
                name = name,
                category = category,
                url = url,
                format = format,
                license = "Custom",
                updateIntervalHours = 24,
                version = null,
                sha256Hash = null,
                lastUpdated = null,
                enabled = true
            )
            blocklistDao.insertSource(source)
            loadBlocklistSources()
        }
    }

    fun deleteBlocklistSource(sourceId: String) {
        viewModelScope.launch {
            blocklistDao.deleteRulesBySource(sourceId)
            blocklistDao.deleteSource(sourceId)
            loadBlocklistSources()
            loadTotalRuleCount()
        }
    }

    fun syncBlocklistsNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<BlocklistFetcherWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            BlocklistFetcherWorker.WORK_NAME + "-manual",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun createQube(name: String, color: Int = QubeProfile.predefinedColors.random()) {
        viewModelScope.launch { qubeManager.createQube(name, color); loadQubeProfiles() }
    }
    fun deleteQube(qubeId: String) { viewModelScope.launch { qubeManager.deleteQube(qubeId); loadQubeProfiles() } }
    fun setDefaultQube(qubeId: String) { viewModelScope.launch { qubeManager.setDefaultQube(qubeId); loadQubeProfiles() } }

    fun setTelemetryEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_TELEMETRY, enabled).apply()
        _isTelemetryEnabled.value = enabled
    }
    fun setMlEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AI_ENABLED, enabled).apply()
        _isMlEnabled.value = enabled
        if (enabled) loadModel() else mlClassifier.close()
    }
    fun setAutoModelUpdateEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply()
        _isAutoModelUpdateEnabled.value = enabled
        val app = getApplication<com.qubeguard.app.QubeGuardApp>()
        if (enabled && isMlEnabled.value == true) app.enableAutomaticModelUpdates() else app.disableAutomaticModelUpdates()
    }
    fun disableLocalModel() = setMlEnabled(false)
    fun enableLocalModel() = setMlEnabled(true)
    fun loadModel() {
        if (isMlEnabled.value != true) return
        viewModelScope.launch {
            if (!modelDownloader.isModelReady()) modelDownloader.ensureModel()
            if (modelDownloader.isModelReady()) mlClassifier.loadModel()
        }
    }
    fun updateModel() {
        if (isMlEnabled.value != true) return
        viewModelScope.launch {
            if (modelDownloader.updateModel()) { mlClassifier.close(); mlClassifier.loadModel() }
        }
    }
    fun deleteLocalModel() { mlClassifier.close(); modelDownloader.deleteModel() }
    fun isModelLoaded(): Boolean = mlClassifier.isModelLoaded()
    fun isModelDownloaded(): Boolean = modelDownloader.isModelReady()

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFERENCES = "qubeguard_settings"
        private const val KEY_AI_ENABLED = "ai_enabled"
        private const val KEY_AUTO_UPDATE = "ai_auto_update"
        private const val KEY_TELEMETRY = "telemetry_enabled"
    }
}
