package com.qubeguard.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QubeGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize app-wide components here
    }
}