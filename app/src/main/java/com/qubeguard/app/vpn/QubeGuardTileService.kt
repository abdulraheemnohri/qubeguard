package com.qubeguard.app.vpn

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.qubeguard.app.engine.QubeGuardService

/**
 * Android Quick Settings Tile for toggling QubeGuard VPN protection.
 */
class QubeGuardTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState(isProtectionActive())
    }

    override fun onClick() {
        super.onClick()
        val isActive = isProtectionActive()
        val context = applicationContext
        if (isActive) {
            context.stopService(Intent(context, QubeGuardService::class.java).apply {
                action = QubeGuardService.ACTION_STOP
            })
            updateTileState(false)
        } else {
            context.startService(Intent(context, QubeGuardService::class.java).apply {
                action = QubeGuardService.ACTION_START
            })
            updateTileState(true)
        }
    }

    private fun isProtectionActive(): Boolean {
        return qsTile?.state == Tile.STATE_ACTIVE
    }

    private fun updateTileState(active: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (active) "QubeGuard On" else "QubeGuard Off"
        tile.updateTile()
    }
}
