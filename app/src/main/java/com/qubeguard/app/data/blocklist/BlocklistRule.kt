package com.qubeguard.app.data.blocklist

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single rule from a blocklist (e.g., a domain, URL pattern, or IP).
 * Rules are compiled into optimized data structures for fast lookup.
 */
@Entity(tableName = "blocklist_rules")
data class BlocklistRule(
    @PrimaryKey
    val id: String, // Unique identifier (e.g., SHA-256 hash of the rule)
    val sourceId: String, // ID of the BlocklistSource this rule belongs to
    val rule: String, // The raw rule (e.g., "||example.com^" or "127.0.0.1 example.com")
    val type: String, // Type: domain, url, ip, regex
    val category: String, // Category: ads, privacy, security, etc.
    val isAllowlist: Boolean = false, // Whether this is an allowlist rule (whitelist)
    val isCompiled: Boolean = false, // Whether this rule is compiled into the fast lookup structures
    val lastUpdated: String? // Timestamp of last update (ISO 8601)
)