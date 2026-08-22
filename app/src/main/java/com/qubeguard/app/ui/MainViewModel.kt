package com.qubeguard.app.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.qubeguard.app.data.blocklist.BlocklistDao
import com.qubeguard.app.engine.QubeGuardService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val blocklistDao: BlocklistDao
) : AndroidViewModel(application) {

    private val _isVpnRunning = MutableLiveData(false)
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

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            val logs = blocklistDao.getRecentDnsLogs(limit = 500)
            _totalQueries.value = logs.size
            val blocked = logs.filter { it.isBlocked }
            _blockedQueries.value = blocked.size
            _adsCount.value = blocked.count { it.reason.contains("Ad", ignoreCase = true) || it.domain.contains("ad", ignoreCase = true) }
            _trackersCount.value = blocked.count { it.reason.contains("Tracker", ignoreCase = true) || it.domain.contains("analytics", ignoreCase = true) }
            _malwareCount.value = blocked.size - (_adsCount.value ?: 0) - (_trackersCount.value ?: 0)
        }
    }

    fun startVpn() {
        val context = getApplication<Application>().applicationContext
        context.startService(Intent(context, QubeGuardService::class.java).apply {
            action = QubeGuardService.ACTION_START
        })
        _isVpnRunning.value = true
        loadAnalytics()
    }

    fun stopVpn() {
        val context = getApplication<Application>().applicationContext
        context.stopService(Intent(context, QubeGuardService::class.java).apply {
            action = QubeGuardService.ACTION_STOP
        })
        _isVpnRunning.value = false
    }
}
