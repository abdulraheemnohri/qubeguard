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

    private val _blocklistSources = MutableLiveData<List<BlocklistSource>>(emptyList())
    val blocklistSources: LiveData<List<BlocklistSource>> = _blocklistSources

    private val _qubeProfiles = MutableLiveData<List<QubeProfile>>(emptyList())
    val qubeProfiles: LiveData<List<QubeProfile>> = _qubeProfiles

    private val _isMlEnabled = MutableLiveData(true)
    val isMlEnabled: LiveData<Boolean> = _isMlEnabled

    // Kept for settings-screen compatibility; remote HF inference is removed.
    private val _isHuggingFaceEnabled = MutableLiveData(false)
    val isHuggingFaceEnabled: LiveData<Boolean> = _isHuggingFaceEnabled

    private val _isTelemetryEnabled = MutableLiveData(false)
    val isTelemetryEnabled: LiveData<Boolean> = _isTelemetryEnabled

    init {
        loadBlocklistSources()
        loadQubeProfiles()
    }

    private fun loadBlocklistSources() {
        viewModelScope.launch { _blocklistSources.value = blocklistDao.getAllSources() }
    }

    private fun loadQubeProfiles() {
        viewModelScope.launch { _qubeProfiles.value = qubeManager.getAllQubes() }
    }

    suspend fun setBlocklistSourceEnabled(sourceId: String, enabled: Boolean) {
        blocklistDao.getSourceById(sourceId)?.let {
            blocklistDao.updateSource(it.copy(enabled = enabled))
            loadBlocklistSources()
        }
    }

    suspend fun createQube(name: String, color: Int = QubeProfile.predefinedColors.random()) {
        qubeManager.createQube(name, color)
        loadQubeProfiles()
    }

    suspend fun deleteQube(qubeId: String) {
        qubeManager.deleteQube(qubeId)
        loadQubeProfiles()
    }

    suspend fun setDefaultQube(qubeId: String) {
        qubeManager.setDefaultQube(qubeId)
        loadQubeProfiles()
    }

    fun setMlEnabled(enabled: Boolean) { _isMlEnabled.value = enabled }

    /** Remote Hugging Face inference is intentionally unsupported. */
    fun setHuggingFaceEnabled(enabled: Boolean) { _isHuggingFaceEnabled.value = false }
    fun disableLocalModel() { _isMlEnabled.value = false }
    fun enableLocalModel() { _isMlEnabled.value = true; loadModel() }
    fun setHuggingFaceToken(token: String) = Unit

    fun setTelemetryEnabled(enabled: Boolean) { _isTelemetryEnabled.value = enabled }

    fun loadModel() {
        viewModelScope.launch {
            if (!modelDownloader.isModelReady()) modelDownloader.ensureModel()
            mlClassifier.loadModel()
        }
    }

    fun updateModel() {
        viewModelScope.launch {
            if (modelDownloader.updateModel()) {
                mlClassifier.close()
                mlClassifier.loadModel()
            }
        }
    }

    fun isModelLoaded(): Boolean = mlClassifier.isModelLoaded()
    fun isModelDownloaded(): Boolean = modelDownloader.isModelReady()
}
