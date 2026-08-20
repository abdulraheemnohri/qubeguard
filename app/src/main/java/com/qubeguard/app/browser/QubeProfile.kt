package com.qubeguard.app.browser

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a Qube (isolated browser profile) in QubeGuard.
 * Each Qube has its own cookies, cache, and history.
 */
@Entity(tableName = "qube_profiles")
data class QubeProfile(
    @PrimaryKey
    val id: String, // Unique identifier for the Qube
    val name: String, // Human-readable name (e.g., "Work", "Personal", "Shopping")
    val color: Int, // Color for UI representation (e.g., 0xFF0000 for red)
    val icon: String?, // Optional icon identifier
    val isDefault: Boolean = false, // Whether this is the default Qube
    val isIncognito: Boolean = false, // Whether this Qube is incognito (no persistent data)
    val createdAt: String, // Timestamp of creation (ISO 8601)
    val lastUsedAt: String? // Timestamp of last use (ISO 8601)
) {
    companion object {
        // Default Qube IDs
        const val DEFAULT_QUBE_ID = "default"
        const val INCOGNITO_QUBE_ID = "incognito"

        // Predefined Qube colors
        val predefinedColors = listOf(
            0xFFE57373.toInt(), // Red
            0xFF9C27B0.toInt(), // Purple
            0xFF3F51B5.toInt(), // Indigo
            0xFF03A9F4.toInt(), // Blue
            0xFF4CAF50.toInt(), // Green
            0xFFFFC107.toInt(), // Amber
            0xFFFF9800.toInt(), // Orange
            0xFF795548.toInt()  // Brown
        )
    }
}
