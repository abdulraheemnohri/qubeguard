package com.qubeguard.app.data.blocklist

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room Database for QubeGuard's blocklist system, local DNS records, DNS logs, and system event logs.
 */
@Database(
    entities = [
        BlocklistSource::class,
        BlocklistRule::class,
        DnsLogEntity::class,
        LocalDnsRecordEntity::class,
        SystemLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class BlocklistDatabase : RoomDatabase() {
    abstract fun blocklistDao(): BlocklistDao
}
