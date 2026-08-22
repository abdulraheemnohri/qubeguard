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
import com.qubeguard.app.data.blocklist.BlocklistRule
import com.qubeguard.app.data.blocklist.BlocklistSource
import com.qubeguard.app.data.blocklist.DnsLogEntity
import com.qubeguard.app.ml.MLClassifier
import com.qubeguard.app.ml.ModelDownloader
import com.qubeguard.app.policy.FeedbackCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
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

    private val _upstreamDns = MutableLiveData(preferences.getString(KEY_UPSTREAM_DNS, "1.1.1.1") ?: "1.1.1.1")
    val upstreamDns: LiveData<String> = _upstreamDns

    private val _falsePositiveCount = MutableLiveData(0)
    val falsePositiveCount: LiveData<Int> = _falsePositiveCount
    private val _allowAlwaysCount = MutableLiveData(0)
    val allowAlwaysCount: LiveData<Int> = _allowAlwaysCount
    private val _totalRuleCount = MutableLiveData(0)
    val totalRuleCount: LiveData<Int> = _totalRuleCount

    private val _dnsLogs = MutableLiveData<List<DnsLogEntity>>(emptyList())
    val dnsLogs: LiveData<List<DnsLogEntity>> = _dnsLogs

    init {
        loadBlocklistSources()
        loadQubeProfiles()
        loadFeedbackStats()
        loadTotalRuleCount()
        loadDnsLogs()
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

    fun loadDnsLogs() {
        viewModelScope.launch {
            _dnsLogs.value = blocklistDao.getRecentDnsLogs()
        }
    }

    fun clearDnsLogs() {
        viewModelScope.launch {
            blocklistDao.clearDnsLogs()
            _dnsLogs.value = emptyList()
        }
    }

    fun setUpstreamDns(dnsIp: String) {
        preferences.edit().putString(KEY_UPSTREAM_DNS, dnsIp).apply()
        _upstreamDns.value = dnsIp
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

    suspend fun exportSettingsJson(): String {
        val sources = blocklistDao.getAllSources()
        val allowlist = blocklistDao.getAllAllowlistRules()
        val json = JSONObject().apply {
            put("upstreamDns", _upstreamDns.value)
            put("aiEnabled", _isMlEnabled.value)
            put("sources", JSONArray().apply {
                sources.filter { it.id.startsWith("custom_") }.forEach { s ->
                    put(JSONObject().apply {
                        put("name", s.name)
                        put("url", s.url)
                        put("category", s.category)
                        put("format", s.format)
                    })
                }
            })
            put("allowlist", JSONArray().apply {
                allowlist.forEach { r ->
                    put(r.rule)
                }
            })
        }
        return json.toString(2)
    }

    suspend fun importSettingsJson(jsonStr: String): Boolean {
        return try {
            val json = JSONObject(jsonStr)
            val dns = json.optString("upstreamDns", "1.1.1.1")
            setUpstreamDns(dns)

            val sourcesArr = json.optJSONArray("sources")
            if (sourcesArr != null) {
                for (i in 0 until sourcesArr.length()) {
                    val sObj = sourcesArr.getJSONObject(i)
                    addCustomBlocklistSource(
                        name = sObj.getString("name"),
                        url = sObj.getString("url"),
                        category = sObj.optString("category", "custom"),
                        format = sObj.optString("format", "adblock_plus")
                    )
                }
            }

            val allowlistArr = json.optJSONArray("allowlist")
            if (allowlistArr != null) {
                for (i in 0 until allowlistArr.length()) {
                    val ruleStr = allowlistArr.getString(i)
                    val ruleId = "custom_allow_" + sha256(ruleStr).substring(0, 10)
                    blocklistDao.insertRule(
                        BlocklistRule(
                            id = ruleId,
                            sourceId = "custom_allowlist",
                            rule = ruleStr,
                            type = "domain",
                            category = "custom",
                            isAllowlist = true,
                            isCompiled = false,
                            lastUpdated = System.currentTimeMillis().toString()
                        )
                    )
                }
            }
            true
        } catch (_: Exception) {
            false
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
        const val KEY_UPSTREAM_DNS = "upstream_dns_ip"
    }
}
