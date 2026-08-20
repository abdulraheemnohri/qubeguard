package com.qubeguard.app.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.qubeguard.app.engine.QubeGuardService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _isVpnRunning = MutableLiveData(false)
    val isVpnRunning: LiveData<Boolean> = _isVpnRunning

    private val _blockedCount = MutableLiveData(0)
    val blockedCount: LiveData<Int> = _blockedCount

    fun startVpn() {
        val context = getApplication<Application>().applicationContext
        context.startService(Intent(context, QubeGuardService::class.java).apply {
            action = QubeGuardService.ACTION_START
        })
        _isVpnRunning.value = true
    }

    fun stopVpn() {
        val context = getApplication<Application>().applicationContext
        context.stopService(Intent(context, QubeGuardService::class.java).apply {
            action = QubeGuardService.ACTION_STOP
        })
        _isVpnRunning.value = false
    }

    fun incrementBlockedCount() {
        _blockedCount.value = (_blockedCount.value ?: 0) + 1
    }

    fun resetBlockedCount() {
        _blockedCount.value = 0
    }
}
