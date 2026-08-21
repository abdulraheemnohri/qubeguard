package com.qubeguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.qubeguard.app.browser.QubeManager
import com.qubeguard.app.browser.QubeProfile
import com.qubeguard.app.data.blocklist.BlocklistDao
import com.qubeguard.app.data.blocklist.BlocklistSource
import com.qubeguard.app.ml.MLClassifier
import com.qubeguard.app.ml.ModelDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val blocklistDao: BlocklistDao,
    private val qubeManager: QubeManager,
    private val mlClassifier: MLClassifier,
    private val modelDownloader: ModelDownloader
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

    init { loadBlocklistSources(); loadQubeProfiles() }
    private fun loadBlocklistSources() { viewModelScope.launch { _blocklistSources.value = blocklistDao.getAllSources() } }
    private fun loadQubeProfiles() { viewModelScope.launch { _qubeProfiles.value = qubeManager.getAllQubes() } }

    fun setBlocklistSourceEnabled(sourceId: String, enabled: Boolean) {
        viewModelScope.launch {
            blocklistDao.getSourceById(sourceId)?.let { blocklistDao.updateSource(it.copy(enabled = enabled)) }
            loadBlocklistSources()
        }
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

    companion object {
        private const val PREFERENCES = "qubeguard_settings"
        private const val KEY_AI_ENABLED = "ai_enabled"
        private const val KEY_AUTO_UPDATE = "ai_auto_update"
        private const val KEY_TELEMETRY = "telemetry_enabled"
    }
}
