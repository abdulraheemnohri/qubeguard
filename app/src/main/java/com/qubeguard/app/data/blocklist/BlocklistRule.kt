package com.qubeguard.app.data.blocklist

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Represents a normalized deterministic blocking rule. */
@Entity(tableName = "blocklist_rules")
data class BlocklistRule(
    @PrimaryKey val id: String,
    val sourceId: String,
    val rule: String,
    val type: String,
    val category: String,
    val isAllowlist: Boolean = false,
    val isCompiled: Boolean = false,
    val lastUpdated: String? = null
)
