package com.qubeguard.app.data.blocklist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BlocklistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: BlocklistSource)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<BlocklistSource>)

    @Update
    suspend fun updateSource(source: BlocklistSource)

    @Query("SELECT * FROM blocklist_sources WHERE id = :id")
    suspend fun getSourceById(id: String): BlocklistSource?

    @Query("SELECT * FROM blocklist_sources WHERE enabled = 1")
    suspend fun getEnabledSources(): List<BlocklistSource>

    @Query("SELECT * FROM blocklist_sources")
    suspend fun getAllSources(): List<BlocklistSource>

    @Query("DELETE FROM blocklist_sources WHERE id = :id")
    suspend fun deleteSource(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: BlocklistRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<BlocklistRule>)

    @Update
    suspend fun updateRule(rule: BlocklistRule)

    @Query("SELECT * FROM blocklist_rules WHERE sourceId = :sourceId")
    suspend fun getRulesBySource(sourceId: String): List<BlocklistRule>

    @Query("SELECT COUNT(*) FROM blocklist_rules WHERE sourceId = :sourceId")
    suspend fun getRuleCountBySource(sourceId: String): Int

    @Query("SELECT COUNT(*) FROM blocklist_rules")
    suspend fun getTotalRuleCount(): Int

    @Query("SELECT * FROM blocklist_rules WHERE isAllowlist = 0")
    suspend fun getAllBlocklistRules(): List<BlocklistRule>

    @Query("SELECT * FROM blocklist_rules WHERE isAllowlist = 1")
    suspend fun getAllAllowlistRules(): List<BlocklistRule>

    @Query("SELECT * FROM blocklist_rules WHERE category = :category AND isAllowlist = 0")
    suspend fun getRulesByCategory(category: String): List<BlocklistRule>

    @Query("DELETE FROM blocklist_rules WHERE sourceId = :sourceId")
    suspend fun deleteRulesBySource(sourceId: String)

    @Query("DELETE FROM blocklist_rules WHERE id = :id")
    suspend fun deleteRule(id: String)

    @Query("""
        SELECT * FROM blocklist_rules
        WHERE (rule = :domain OR rule LIKE '%.' || :domain OR rule LIKE :domain || '.%')
        AND isAllowlist = 0
        LIMIT 1
    """)
    suspend fun getMatchingBlocklistRule(domain: String): BlocklistRule?

    @Query("""
        SELECT * FROM blocklist_rules
        WHERE (rule = :domain OR rule LIKE '%.' || :domain OR rule LIKE :domain || '.%')
        AND isAllowlist = 1
        LIMIT 1
    """)
    suspend fun getMatchingAllowlistRule(domain: String): BlocklistRule?

    // Local DNS Custom Records (Pi-hole style)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalDnsRecord(record: LocalDnsRecordEntity)

    @Query("SELECT * FROM local_dns_records WHERE enabled = 1")
    suspend fun getEnabledLocalDnsRecords(): List<LocalDnsRecordEntity>

    @Query("SELECT * FROM local_dns_records")
    suspend fun getAllLocalDnsRecords(): List<LocalDnsRecordEntity>

    @Query("SELECT * FROM local_dns_records WHERE domain = :domain AND enabled = 1 LIMIT 1")
    suspend fun getLocalDnsRecordForDomain(domain: String): LocalDnsRecordEntity?

    @Query("DELETE FROM local_dns_records WHERE id = :id")
    suspend fun deleteLocalDnsRecord(id: String)

    // DNS Query Logging
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDnsLog(log: DnsLogEntity)

    @Query("SELECT * FROM dns_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentDnsLogs(limit: Int = 100): List<DnsLogEntity>

    @Query("DELETE FROM dns_logs")
    suspend fun clearDnsLogs()
}
