package com.qubeguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.qubeguard.app.engine.BlockingEngine
import com.qubeguard.app.policy.FeedbackCollector
import com.qubeguard.app.policy.UserFeedback
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class BlockPageViewModel @Inject constructor(
    application: Application,
    private val blockingEngine: BlockingEngine,
    private val feedbackCollector: FeedbackCollector
) : AndroidViewModel(application) {
    private val _blockedUrl = MutableLiveData("")
    val blockedUrl: LiveData<String> = _blockedUrl
    private val _blockReason = MutableLiveData("")
    val blockReason: LiveData<String> = _blockReason
    private val _confidence = MutableLiveData(0f)
    val confidence: LiveData<Float> = _confidence
    private val _category = MutableLiveData("")
    val category: LiveData<String> = _category

    fun initialize(blockedUrl: String) {
        _blockedUrl.value = blockedUrl
        viewModelScope.launch {
            val result = blockingEngine.checkUrl(blockedUrl)
            _blockReason.value = result.reason
            _confidence.value = result.confidence
            _category.value = result.category ?: "Unknown"
        }
    }

    fun allowOnce() = Unit
    fun allowAlways() {
        val url = _blockedUrl.value ?: return
        viewModelScope.launch { feedbackCollector.logFeedback(url, UserFeedback.ALLOW_ALWAYS) }
    }
    fun keepBlocked() = Unit
    fun reportFalsePositive() {
        val url = _blockedUrl.value ?: return
        viewModelScope.launch { feedbackCollector.logFeedback(url, UserFeedback.REPORT_FALSE_POSITIVE) }
    }
}
