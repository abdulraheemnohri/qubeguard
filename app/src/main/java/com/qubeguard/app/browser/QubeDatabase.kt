package com.qubeguard.app.browser

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room Database for QubeGuard's Qube profiles.
 * Includes QubeProfile entity.
 */
@Database(
    entities = [QubeProfile::class],
    version = 1,
    exportSchema = false
)
abstract class QubeDatabase : RoomDatabase() {
    abstract fun qubeDao(): QubeDao
}
