package com.qubeguard.app.data.blocklist

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * System-wide event log entity for recording app events:
 * VPN Connect/Disconnect, DNS Blocks, In-Browser Ad Blocks, Media Downloads, and AI Classifications.
 */
@Entity(tableName = "system_logs")
data class SystemLogEntity(
    @PrimaryKey val id: String,
    val category: String, // "VPN", "DNS", "BROWSER", "AI"
    val event: String,
    val details: String,
    val timestamp: String
)
