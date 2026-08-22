package com.qubeguard.app.data.blocklist

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local DNS mapping (Pi-hole style Custom DNS A/AAAA/CNAME record).
 */
@Entity(tableName = "local_dns_records")
data class LocalDnsRecordEntity(
    @PrimaryKey val id: String,
    val domain: String,
    val ipAddress: String,
    val recordType: String = "A", // "A", "AAAA", "CNAME"
    val enabled: Boolean = true,
    val description: String = ""
)
