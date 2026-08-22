package com.qubeguard.app.data.blocklist

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room Database for QubeGuard's blocklist system, local DNS records, and DNS logs.
 */
@Database(
    entities = [
        BlocklistSource::class,
        BlocklistRule::class,
        DnsLogEntity::class,
        LocalDnsRecordEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class BlocklistDatabase : RoomDatabase() {
    abstract fun blocklistDao(): BlocklistDao
}
