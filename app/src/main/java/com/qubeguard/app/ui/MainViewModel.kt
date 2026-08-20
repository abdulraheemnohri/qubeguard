package com.qubeguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.qubeguard.app.engine.QubeGuardService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for MainActivity.
 * Manages VPN state and blocked request count.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _isVpnRunning = MutableLiveData<Boolean>(false)
    val isVpnRunning: LiveData<Boolean> = _isVpnRunning

    private val _blockedCount = MutableLiveData<Int>(0)
    val blockedCount: LiveData<Int> = _blockedCount

    /**
     * Starts the VPN service.
     */
    fun startVpn() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, QubeGuardService::class.java).apply {
            action = QubeGuardService.ACTION_START
        }
        context.startService(intent)
        _isVpnRunning.value = true
    }

    /**
     * Stops the VPN service.
     */
    fun stopVpn() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, QubeGuardService::class.java).apply {
            action = QubeGuardService.ACTION_STOP
        }
        context.stopService(intent)
        _isVpnRunning.value = false
    }

    /**
     * Updates the blocked request count.
     */
    fun incrementBlockedCount() {
        _blockedCount.value = (_blockedCount.value ?: 0) + 1
    }

    /**
     * Resets the blocked request count.
     */
    fun resetBlockedCount() {
        _blockedCount.value = 0
    }
}
