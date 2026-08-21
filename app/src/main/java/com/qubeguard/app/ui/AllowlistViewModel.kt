package com.qubeguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.qubeguard.app.data.blocklist.BlocklistDao
import com.qubeguard.app.data.blocklist.BlocklistRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class AllowlistViewModel @Inject constructor(
    application: Application,
    private val blocklistDao: BlocklistDao
) : AndroidViewModel(application) {

    private val _allowlistRules = MutableLiveData<List<BlocklistRule>>(emptyList())
    val allowlistRules: LiveData<List<BlocklistRule>> = _allowlistRules

    init {
        loadAllowlistRules()
    }

    fun loadAllowlistRules() {
        viewModelScope.launch {
            _allowlistRules.value = blocklistDao.getAllAllowlistRules()
        }
    }

    fun addAllowlistRule(domain: String) {
        val trimmed = domain.lowercase().trim().removePrefix("http://").removePrefix("https://").substringBefore("/")
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            val ruleId = "custom_allow_" + sha256(trimmed).substring(0, 10)
            val rule = BlocklistRule(
                id = ruleId,
                sourceId = "custom_allowlist",
                rule = trimmed,
                type = "domain",
                category = "custom",
                isAllowlist = true,
                isCompiled = false,
                lastUpdated = System.currentTimeMillis().toString()
            )
            blocklistDao.insertRule(rule)
            loadAllowlistRules()
        }
    }

    fun removeAllowlistRule(ruleId: String) {
        viewModelScope.launch {
            blocklistDao.deleteRule(ruleId)
            loadAllowlistRules()
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
