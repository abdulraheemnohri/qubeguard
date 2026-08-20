package com.qubeguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.qubeguard.app.browser.QubeManager
import com.qubeguard.app.browser.QubeProfile
import com.qubeguard.app.data.blocklist.BlocklistDao
import com.qubeguard.app.data.blocklist.BlocklistSource
import com.qubeguard.app.engine.BlockingEngine
import com.qubeguard.app.ml.TfLiteClassifier
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for SettingsActivity.
 * Manages settings for blocklists, Qubes, ML, and feedback.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val blocklistDao: BlocklistDao,
    private val qubeManager: QubeManager,
    private val tfLiteClassifier: TfLiteClassifier,
    private val blockingEngine: BlockingEngine
) : AndroidViewModel(application) {

    private val _blocklistSources = MutableLiveData<List<BlocklistSource>>(emptyList())
    val blocklistSources: LiveData<List<BlocklistSource>> = _blocklistSources

    private val _qubeProfiles = MutableLiveData<List<QubeProfile>>(emptyList())
    val qubeProfiles: LiveData<List<QubeProfile>> = _qubeProfiles

    private val _isMlEnabled = MutableLiveData<Boolean>(true)
    val isMlEnabled: LiveData<Boolean> = _isMlEnabled

    private val _isHuggingFaceEnabled = MutableLiveData<Boolean>(false)
    val isHuggingFaceEnabled: LiveData<Boolean> = _isHuggingFaceEnabled

    private val _isTelemetryEnabled = MutableLiveData<Boolean>(false)
    val isTelemetryEnabled: LiveData<Boolean> = _isTelemetryEnabled

    init {
        loadBlocklistSources()
        loadQubeProfiles()
    }

    /**
     * Loads the list of blocklist sources.
     */
    private fun loadBlocklistSources() {
        val scope = viewModelScope
        scope.launch {
            _blocklistSources.value = blocklistDao.getAllSources()
        }
    }

    /**
     * Loads the list of Qube profiles.
     */
    private fun loadQubeProfiles() {
        val scope = viewModelScope
        scope.launch {
            _qubeProfiles.value = qubeManager.getAllQubes()
        }
    }

    /**
     * Enables or disables a blocklist source.
     */
    suspend fun setBlocklistSourceEnabled(sourceId: String, enabled: Boolean) {
        val source = blocklistDao.getSourceById(sourceId)
        if (source != null) {
            blocklistDao.updateSource(source.copy(enabled = enabled))
            loadBlocklistSources()
        }
    }

    /**
     * Creates a new Qube profile.
     */
    suspend fun createQube(name: String, color: Int = QubeProfile.predefinedColors.random()) {
        qubeManager.createQube(name, color)
        loadQubeProfiles()
    }

    /**
     * Deletes a Qube profile.
     */
    suspend fun deleteQube(qubeId: String) {
        qubeManager.deleteQube(qubeId)
        loadQubeProfiles()
    }

    /**
     * Sets the default Qube profile.
     */
    suspend fun setDefaultQube(qubeId: String) {
        qubeManager.setDefaultQube(qubeId)
        loadQubeProfiles()
    }

    /**
     * Enables or disables the ML classifier.
     */
    fun setMlEnabled(enabled: Boolean) {
        _isMlEnabled.value = enabled
    }

    /**
     * Enables or disables Hugging Face API.
     * When enabled, uses r3ddkahili/final-complete-malicious-url-model
     */
    fun setHuggingFaceEnabled(enabled: Boolean) {
        _isHuggingFaceEnabled.value = enabled
        if (enabled) {
            blockingEngine.enableHuggingFace()
        } else {
            blockingEngine.disableHuggingFace()
        }
    }

    /**
     * Disables local TFLite model.
     */
    fun disableLocalModel() {
        // Local model is automatically disabled when Hugging Face is enabled
    }

    /**
     * Enables local TFLite model.
     */
    fun enableLocalModel() {
        // Local model is automatically enabled when Hugging Face is disabled
    }

    /**
     * Sets the Hugging Face API token.
     * Get your token from: https://huggingface.co/settings/tokens
     */
    fun setHuggingFaceToken(token: String) {
        blockingEngine.setHuggingFaceToken(token)
    }

    /**
     * Enables or disables telemetry (feedback upload).
     */
    fun setTelemetryEnabled(enabled: Boolean) {
        _isTelemetryEnabled.value = enabled
    }

    /**
     * Loads the TFLite model.
     */
    fun loadModel() {
        tfLiteClassifier.loadModel()
    }

    /**
     * Checks if the TFLite model is loaded.
     */
    fun isModelLoaded(): Boolean {
        return tfLiteClassifier.isModelLoaded()
    }
}
