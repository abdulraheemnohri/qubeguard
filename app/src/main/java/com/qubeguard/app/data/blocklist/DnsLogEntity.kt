package com.qubeguard.app.data.blocklist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dns_logs")
data class DnsLogEntity(
    @PrimaryKey
    val id: String,
    val domain: String,
    val isBlocked: Boolean,
    val reason: String,
    val timestamp: String
)
