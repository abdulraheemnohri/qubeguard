package com.qubeguard.app.data.blocklist

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a source of blocklists (e.g., EasyList, AdGuard).
 * Each source provides a set of rules for blocking ads, trackers, or malware.
 */
@Entity(tableName = "blocklist_sources")
data class BlocklistSource(
    @PrimaryKey
    val id: String, // Unique identifier (e.g., "easylist_ads")
    val name: String, // Human-readable name (e.g., "EasyList")
    val category: String, // Category: ads, privacy, security, annoyances, social, custom
    val url: String, // URL to download the blocklist
    val format: String, // Format: adblock_plus, hosts, regex, etc.
    val license: String, // License (e.g., "GPLv3")
    val updateIntervalHours: Int, // How often to update (in hours)
    val version: String?, // Current version of the blocklist
    val sha256Hash: String?, // SHA-256 hash of the downloaded file
    val lastUpdated: String?, // Timestamp of last update (ISO 8601)
    val enabled: Boolean = true // Whether this source is enabled
)