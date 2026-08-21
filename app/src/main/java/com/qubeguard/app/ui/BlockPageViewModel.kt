package com.qubeguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.qubeguard.app.data.blocklist.BlocklistDao
import com.qubeguard.app.data.blocklist.BlocklistRule
import com.qubeguard.app.engine.BlockingEngine
import com.qubeguard.app.policy.FeedbackCollector
import com.qubeguard.app.policy.UserFeedback
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import java.security.MessageDigest

@HiltViewModel
class BlockPageViewModel @Inject constructor(
    application: Application,
    private val blockingEngine: BlockingEngine,
    private val feedbackCollector: FeedbackCollector,
    private val blocklistDao: BlocklistDao
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

    fun allowOnce() {
        val url = _blockedUrl.value ?: return
        viewModelScope.launch {
            feedbackCollector.logFeedback(url, "allow_once")
        }
    }

    fun allowAlways() {
        val url = _blockedUrl.value ?: return
        viewModelScope.launch {
            feedbackCollector.logFeedback(url, UserFeedback.ALLOW_ALWAYS)
            val ruleId = "user_allow_" + sha256(url).substring(0, 12)
            val rule = BlocklistRule(
                id = ruleId,
                sourceId = "custom_allowlist",
                rule = url,
                type = "domain",
                category = "custom",
                isAllowlist = true,
                isCompiled = false,
                lastUpdated = System.currentTimeMillis().toString()
            )
            blocklistDao.insertRule(rule)
        }
    }

    fun keepBlocked() {
        val url = _blockedUrl.value ?: return
        viewModelScope.launch {
            feedbackCollector.logFeedback(url, "keep_blocked")
        }
    }

    fun reportFalsePositive() {
        val url = _blockedUrl.value ?: return
        viewModelScope.launch {
            feedbackCollector.logFeedback(url, UserFeedback.REPORT_FALSE_POSITIVE)
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
