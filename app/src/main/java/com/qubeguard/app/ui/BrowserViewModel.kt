package com.qubeguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.qubeguard.app.browser.QubeManager
import com.qubeguard.app.browser.QubeProfile
import com.qubeguard.app.engine.BlockingEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class BrowserViewModel @Inject constructor(
    application: Application,
    private val qubeManager: QubeManager,
    private val blockingEngine: BlockingEngine
) : AndroidViewModel(application) {
    private val _currentUrl = MutableLiveData("https://www.example.com")
    val currentUrl: LiveData<String> = _currentUrl
    private val _canGoBack = MutableLiveData(false)
    val canGoBack: LiveData<Boolean> = _canGoBack
    private val _canGoForward = MutableLiveData(false)
    val canGoForward: LiveData<Boolean> = _canGoForward
    private val _selectedQube = MutableLiveData<QubeProfile?>(null)
    val selectedQube: LiveData<QubeProfile?> = _selectedQube
    private val _qubes = MutableLiveData<List<QubeProfile>>(emptyList())
    val qubes: LiveData<List<QubeProfile>> = _qubes

    init { loadQubes() }

    private fun loadQubes() {
        viewModelScope.launch {
            _qubes.value = qubeManager.getAllQubes()
            _selectedQube.value = qubeManager.getDefaultQube()
        }
    }

    fun setUrl(url: String) { _currentUrl.value = url }
    fun updateNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        _canGoBack.value = canGoBack
        _canGoForward.value = canGoForward
    }
    fun selectQube(qube: QubeProfile) { _selectedQube.value = qube }
    fun createQube(name: String, color: Int = QubeProfile.predefinedColors.random()) {
        viewModelScope.launch { qubeManager.createQube(name, color); loadQubes() }
    }
    fun deleteQube(qubeId: String) {
        viewModelScope.launch { qubeManager.deleteQube(qubeId); loadQubes() }
    }
    suspend fun isBlocked(url: String): Boolean = blockingEngine.isBlocked(url)
    fun getCategory(url: String): String = blockingEngine.getCategory(url)
    fun getConfidenceScores(url: String): Map<String, Float> = blockingEngine.getConfidenceScores(url)
}
