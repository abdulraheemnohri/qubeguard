package com.qubeguard.app.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.qubeguard.app.data.blocklist.BlocklistDao
import com.qubeguard.app.engine.QubeGuardService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val blocklistDao: BlocklistDao
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("qubeguard_settings", Context.MODE_PRIVATE)

    private val _isVpnRunning = MutableLiveData(prefs.getBoolean("is_protection_active", false))
    val isVpnRunning: LiveData<Boolean> = _isVpnRunning

    private val _totalQueries = MutableLiveData(0)
    val totalQueries: LiveData<Int> = _totalQueries

    private val _blockedQueries = MutableLiveData(0)
    val blockedQueries: LiveData<Int> = _blockedQueries

    private val _adsCount = MutableLiveData(0)
    val adsCount: LiveData<Int> = _adsCount

    private val _trackersCount = MutableLiveData(0)
    val trackersCount: LiveData<Int> = _trackersCount

    private val _malwareCount = MutableLiveData(0)
    val malwareCount: LiveData<Int> = _malwareCount

    private val _estimatedSavedMb = MutableLiveData("0.0 MB")
    val estimatedSavedMb: LiveData<String> = _estimatedSavedMb

    private val _connectionTimeSeconds = MutableLiveData(0L)
    val connectionTimeSeconds: LiveData<Long> = _connectionTimeSeconds

    private val _formattedConnectionTime = MutableLiveData("00:00:00")
    val formattedConnectionTime: LiveData<String> = _formattedConnectionTime

    private var timerJob: Job? = null

    init {
        loadAnalytics()
        if (_isVpnRunning.value == true) {
            startConnectionTimer()
        }
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            _isVpnRunning.value = prefs.getBoolean("is_protection_active", false)
            val logs = blocklistDao.getRecentDnsLogs(limit = 500)
            _totalQueries.value = logs.size
            val blocked = logs.filter { it.isBlocked }
            _blockedQueries.value = blocked.size
            _adsCount.value = blocked.count { it.reason.contains("Ad", ignoreCase = true) || it.domain.contains("ad", ignoreCase = true) }
            _trackersCount.value = blocked.count { it.reason.contains("Tracker", ignoreCase = true) || it.domain.contains("analytics", ignoreCase = true) }
            _malwareCount.value = blocked.size - (_adsCount.value ?: 0) - (_trackersCount.value ?: 0)

            val savedBytes = blocked.size * 50 * 1024L
            val mb = savedBytes / (1024.0 * 1024.0)
            _estimatedSavedMb.value = "%.1f MB".format(mb)
        }
    }

    fun startVpn() {
        val context = getApplication<Application>().applicationContext
        prefs.edit().putBoolean("is_protection_active", true).apply()
        context.startService(Intent(context, QubeGuardService::class.java).apply {
            action = QubeGuardService.ACTION_START
        })
        _isVpnRunning.value = true
        startConnectionTimer()
        loadAnalytics()
    }

    fun stopVpn() {
        val context = getApplication<Application>().applicationContext
        prefs.edit().putBoolean("is_protection_active", false).apply()
        context.stopService(Intent(context, QubeGuardService::class.java).apply {
            action = QubeGuardService.ACTION_STOP
        })
        _isVpnRunning.value = false
        stopConnectionTimer()
    }

    private fun startConnectionTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val secs = (_connectionTimeSeconds.value ?: 0L) + 1
                _connectionTimeSeconds.value = secs
                val hours = secs / 3600
                val mins = (secs % 3600) / 60
                val s = secs % 60
                _formattedConnectionTime.value = "%02d:%02d:%02d".format(hours, mins, s)
            }
        }
    }

    private fun stopConnectionTimer() {
        timerJob?.cancel()
        timerJob = null
        _connectionTimeSeconds.value = 0L
        _formattedConnectionTime.value = "00:00:00"
    }
}
