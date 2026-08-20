package com.qubeguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.qubeguard.app.browser.QubeManager
import com.qubeguard.app.browser.QubeProfile
import com.qubeguard.app.engine.BlockingEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for BrowserActivity.
 * Manages browser state, Qube selection, and URL navigation.
 */
@HiltViewModel
class BrowserViewModel @Inject constructor(
    application: Application,
    private val qubeManager: QubeManager,
    private val blockingEngine: BlockingEngine
) : AndroidViewModel(application) {

    private val _currentUrl = MutableLiveData<String>("https://www.example.com")
    val currentUrl: LiveData<String> = _currentUrl

    private val _canGoBack = MutableLiveData<Boolean>(false)
    val canGoBack: LiveData<Boolean> = _canGoBack

    private val _canGoForward = MutableLiveData<Boolean>(false)
    val canGoForward: LiveData<Boolean> = _canGoForward

    private val _selectedQube = MutableLiveData<QubeProfile?>(null)
    val selectedQube: LiveData<QubeProfile?> = _selectedQube

    private val _qubes = MutableLiveData<List<QubeProfile>>(emptyList())
    val qubes: LiveData<List<QubeProfile>> = _qubes

    init {
        loadQubes()
    }

    /**
     * Loads the list of Qube profiles.
     */
    private fun loadQubes() {
        val scope = viewModelScope
        scope.launch {
            _qubes.value = qubeManager.getAllQubes()
            _selectedQube.value = qubeManager.getDefaultQube()
        }
    }

    /**
     * Sets the current URL.
     */
    fun setUrl(url: String) {
        _currentUrl.value = url
    }

    /**
     * Updates the navigation state (back/forward).
     */
    fun updateNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        _canGoBack.value = canGoBack
        _canGoForward.value = canGoForward
    }

    /**
     * Selects a Qube profile for browsing.
     */
    fun selectQube(qube: QubeProfile) {
        _selectedQube.value = qube
    }

    /**
     * Creates a new Qube profile.
     */
    suspend fun createQube(name: String, color: Int = QubeProfile.predefinedColors.random()) {
        qubeManager.createQube(name, color)
        loadQubes()
    }

    /**
     * Deletes a Qube profile.
     */
    suspend fun deleteQube(qubeId: String) {
        qubeManager.deleteQube(qubeId)
        loadQubes()
    }

    /**
     * Checks if a URL is blocked.
     */
    suspend fun isBlocked(url: String): Boolean {
        return blockingEngine.isBlocked(url)
    }

    /**
     * Gets the category of a URL (e.g., "Ad", "Tracker", "Malware").
     */
    fun getCategory(url: String): String {
        return blockingEngine.getCategory(url)
    }

    /**
     * Gets the confidence scores for a URL.
     */
    fun getConfidenceScores(url: String): Map<String, Float> {
        return blockingEngine.getConfidenceScores(url)
    }
}
