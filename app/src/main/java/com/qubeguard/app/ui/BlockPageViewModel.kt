package com.qubeguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.qubeguard.app.engine.BlockingEngine
import com.qubeguard.app.policy.FeedbackCollector
import com.qubeguard.app.policy.UserFeedback
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for BlockPageActivity.
 * Manages the blocked URL and user feedback actions.
 */
@HiltViewModel
class BlockPageViewModel @Inject constructor(
    application: Application,
    private val blockingEngine: BlockingEngine,
    private val feedbackCollector: FeedbackCollector
) : AndroidViewModel(application) {

    private val _blockedUrl = MutableLiveData<String>("")
    val blockedUrl: LiveData<String> = _blockedUrl

    private val _blockReason = MutableLiveData<String>("")
    val blockReason: LiveData<String> = _blockReason

    private val _confidence = MutableLiveData<Float>(0f)
    val confidence: LiveData<Float> = _confidence

    private val _category = MutableLiveData<String>("")
    val category: LiveData<String> = _category

    /**
     * Initializes the ViewModel with the blocked URL and its details.
     */
    fun initialize(blockedUrl: String) {
        _blockedUrl.value = blockedUrl

        val scope = viewModelScope
        scope.launch {
            val result = blockingEngine.checkUrl(blockedUrl)
            _blockReason.value = result.reason
            _confidence.value = result.confidence
            _category.value = result.category ?: "Unknown"
        }
    }

    /**
     * Allows the URL once (temporary bypass).
     */
    fun allowOnce() {
        // In a real implementation, this would add the URL to a temporary allowlist
        // and close the block page
    }

    /**
     * Allows the URL always (adds to permanent allowlist).
     */
    fun allowAlways() {
        val url = _blockedUrl.value ?: return
        val scope = viewModelScope
        scope.launch {
            // Log feedback for "allow_always"
            feedbackCollector.logFeedback(url, UserFeedback.ALLOW_ALWAYS)
        }
    }

    /**
     * Keeps the URL blocked.
     */
    fun keepBlocked() {
        // Close the block page without any action
    }

    /**
     * Reports the URL as a false positive.
     */
    fun reportFalsePositive() {
        val url = _blockedUrl.value ?: return
        val scope = viewModelScope
        scope.launch {
            // Log feedback for "report_false_positive"
            feedbackCollector.logFeedback(url, UserFeedback.REPORT_FALSE_POSITIVE)
        }
    }
}
