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
import com.qubeguard.app.data.blocklist.BlocklistCatalog
import com.qubeguard.app.data.blocklist.BlocklistDao
import com.qubeguard.app.data.blocklist.BlocklistFetcherWorker
import com.qubeguard.app.data.blocklist.BlocklistRule
import com.qubeguard.app.data.blocklist.BlocklistSource
import com.qubeguard.app.data.blocklist.DnsLogEntity
import com.qubeguard.app.data.blocklist.LocalDnsRecordEntity
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

    private val _themeMode = MutableLiveData(preferences.getString(KEY_THEME_MODE, "system") ?: "system")
    val themeMode: LiveData<String> = _themeMode

    private val _sinkholeMode = MutableLiveData(preferences.getString(KEY_SINKHOLE_MODE, "NXDOMAIN") ?: "NXDOMAIN")
    val sinkholeMode: LiveData<String> = _sinkholeMode

    private val _dnssecEnabled = MutableLiveData(preferences.getBoolean(KEY_DNSSEC_ENABLED, false))
    val dnssecEnabled: LiveData<Boolean> = _dnssecEnabled

    private val _conditionalForwardingEnabled = MutableLiveData(preferences.getBoolean(KEY_CONDITIONAL_ENABLED, false))
    val conditionalForwardingEnabled: LiveData<Boolean> = _conditionalForwardingEnabled

    private val _conditionalDomain = MutableLiveData(preferences.getString(KEY_CONDITIONAL_DOMAIN, "home.arpa") ?: "home.arpa")
    val conditionalDomain: LiveData<String> = _conditionalDomain

    private val _conditionalTargetIp = MutableLiveData(preferences.getString(KEY_CONDITIONAL_IP, "192.168.1.1") ?: "192.168.1.1")
    val conditionalTargetIp: LiveData<String> = _conditionalTargetIp

    private val _bypassPackages = MutableLiveData(preferences.getStringSet(KEY_BYPASS_PACKAGES, emptySet()) ?: emptySet())
    val bypassPackages: LiveData<Set<String>> = _bypassPackages

    private val _falsePositiveCount = MutableLiveData(0)
    val falsePositiveCount: LiveData<Int> = _falsePositiveCount
    private val _allowAlwaysCount = MutableLiveData(0)
    val allowAlwaysCount: LiveData<Int> = _allowAlwaysCount
    private val _totalRuleCount = MutableLiveData(0)
    val totalRuleCount: LiveData<Int> = _totalRuleCount

    private val _dnsLogs = MutableLiveData<List<DnsLogEntity>>(emptyList())
    val dnsLogs: LiveData<List<DnsLogEntity>> = _dnsLogs

    private val _localDnsRecords = MutableLiveData<List<LocalDnsRecordEntity>>(emptyList())
    val localDnsRecords: LiveData<List<LocalDnsRecordEntity>> = _localDnsRecords

    init {
        loadBlocklistSources()
        loadQubeProfiles()
        loadFeedbackStats()
        loadTotalRuleCount()
        loadDnsLogs()
        loadLocalDnsRecords()
    }

    fun loadBlocklistSources() {
        viewModelScope.launch {
            var sources = blocklistDao.getAllSources()
            if (sources.isEmpty()) {
                blocklistDao.insertSources(BlocklistCatalog.defaults)
                sources = blocklistDao.getAllSources()
            }
            _blocklistSources.value = sources
        }
    }
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

    fun loadLocalDnsRecords() {
        viewModelScope.launch {
            _localDnsRecords.value = blocklistDao.getAllLocalDnsRecords()
        }
    }

    fun addLocalDnsRecord(domain: String, ipAddress: String, recordType: String = "A", description: String = "") {
        viewModelScope.launch {
            val id = "dns_rec_" + sha256(domain + ipAddress).substring(0, 10)
            val record = LocalDnsRecordEntity(
                id = id,
                domain = domain.trim().lowercase(),
                ipAddress = ipAddress.trim(),
                recordType = recordType,
                enabled = true,
                description = description
            )
            blocklistDao.insertLocalDnsRecord(record)
            loadLocalDnsRecords()
        }
    }

    fun deleteLocalDnsRecord(id: String) {
        viewModelScope.launch {
            blocklistDao.deleteLocalDnsRecord(id)
            loadLocalDnsRecords()
        }
    }

    fun setSinkholeMode(mode: String) {
        preferences.edit().putString(KEY_SINKHOLE_MODE, mode).apply()
        _sinkholeMode.value = mode
    }

    fun setDnssecEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DNSSEC_ENABLED, enabled).apply()
        _dnssecEnabled.value = enabled
    }

    fun setConditionalForwarding(enabled: Boolean, domain: String = "home.arpa", targetIp: String = "192.168.1.1") {
        preferences.edit()
            .putBoolean(KEY_CONDITIONAL_ENABLED, enabled)
            .putString(KEY_CONDITIONAL_DOMAIN, domain)
            .putString(KEY_CONDITIONAL_IP, targetIp)
            .apply()
        _conditionalForwardingEnabled.value = enabled
        _conditionalDomain.value = domain
        _conditionalTargetIp.value = targetIp
    }

    fun setUpstreamDns(dnsIp: String) {
        preferences.edit().putString(KEY_UPSTREAM_DNS, dnsIp).apply()
        _upstreamDns.value = dnsIp
    }

    fun setThemeMode(mode: String) {
        preferences.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setAppBypass(packageName: String, bypass: Boolean) {
        val current = (_bypassPackages.value ?: emptySet()).toMutableSet()
        if (bypass) current.add(packageName) else current.remove(packageName)
        preferences.edit().putStringSet(KEY_BYPASS_PACKAGES, current).apply()
        _bypassPackages.value = current
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
        val localDns = blocklistDao.getAllLocalDnsRecords()
        val json = JSONObject().apply {
            put("upstreamDns", _upstreamDns.value)
            put("themeMode", _themeMode.value)
            put("sinkholeMode", _sinkholeMode.value)
            put("dnssecEnabled", _dnssecEnabled.value)
            put("conditionalEnabled", _conditionalForwardingEnabled.value)
            put("conditionalDomain", _conditionalDomain.value)
            put("conditionalIp", _conditionalTargetIp.value)
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
            put("localDns", JSONArray().apply {
                localDns.forEach { r ->
                    put(JSONObject().apply {
                        put("domain", r.domain)
                        put("ipAddress", r.ipAddress)
                        put("recordType", r.recordType)
                        put("description", r.description)
                    })
                }
            })
            put("bypassPackages", JSONArray().apply {
                _bypassPackages.value?.forEach { put(it) }
            })
        }
        return json.toString(2)
    }

    suspend fun importSettingsJson(jsonStr: String): Boolean {
        return try {
            val json = JSONObject(jsonStr)
            val dns = json.optString("upstreamDns", "1.1.1.1")
            setUpstreamDns(dns)

            val theme = json.optString("themeMode", "system")
            setThemeMode(theme)

            val sinkhole = json.optString("sinkholeMode", "NXDOMAIN")
            setSinkholeMode(sinkhole)

            val dnssec = json.optBoolean("dnssecEnabled", false)
            setDnssecEnabled(dnssec)

            val condEnabled = json.optBoolean("conditionalEnabled", false)
            val condDomain = json.optString("conditionalDomain", "home.arpa")
            val condIp = json.optString("conditionalIp", "192.168.1.1")
            setConditionalForwarding(condEnabled, condDomain, condIp)

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

            val localDnsArr = json.optJSONArray("localDns")
            if (localDnsArr != null) {
                for (i in 0 until localDnsArr.length()) {
                    val recObj = localDnsArr.getJSONObject(i)
                    addLocalDnsRecord(
                        domain = recObj.getString("domain"),
                        ipAddress = recObj.getString("ipAddress"),
                        recordType = recObj.optString("recordType", "A"),
                        description = recObj.optString("description", "")
                    )
                }
            }

            val bypassArr = json.optJSONArray("bypassPackages")
            if (bypassArr != null) {
                val set = mutableSetOf<String>()
                for (i in 0 until bypassArr.length()) {
                    set.add(bypassArr.getString(i))
                }
                preferences.edit().putStringSet(KEY_BYPASS_PACKAGES, set).apply()
                _bypassPackages.value = set
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
        const val KEY_BYPASS_PACKAGES = "bypass_packages"
        const val KEY_THEME_MODE = "app_theme_mode"
        const val KEY_SINKHOLE_MODE = "pihole_sinkhole_mode"
        const val KEY_DNSSEC_ENABLED = "pihole_dnssec_enabled"
        const val KEY_CONDITIONAL_ENABLED = "pihole_conditional_enabled"
        const val KEY_CONDITIONAL_DOMAIN = "pihole_conditional_domain"
        const val KEY_CONDITIONAL_IP = "pihole_conditional_ip"
    }
}
