package com.qubeguard.app.data.blocklist

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room Database for QubeGuard's blocklist system.
 * Includes BlocklistSource and BlocklistRule entities.
 */
@Database(
    entities = [BlocklistSource::class, BlocklistRule::class],
    version = 1,
    exportSchema = false
)
abstract class BlocklistDatabase : RoomDatabase() {
    abstract fun blocklistDao(): BlocklistDao
}