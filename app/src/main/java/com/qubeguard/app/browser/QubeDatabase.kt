package com.qubeguard.app.browser

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room Database for QubeGuard's Qube profiles, bookmarks, and history.
 */
@Database(
    entities = [QubeProfile::class, BookmarkEntity::class, HistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class QubeDatabase : RoomDatabase() {
    abstract fun qubeDao(): QubeDao
}
