package com.qubeguard.app.data.blocklist

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.TimeUnit

@HiltWorker
class BlocklistFetcherWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val blocklistDao: BlocklistDao
) : CoroutineWorker(context, workerParams) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sources = blocklistDao.getEnabledSources()
            if (sources.isEmpty()) return@withContext Result.success()
            sources.forEach { source ->
                try {
                    fetchAndUpdateBlocklist(source)
                } catch (_: Exception) {
                    // Continue with other sources; a single unavailable list must not abort the update batch.
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun fetchAndUpdateBlocklist(source: BlocklistSource) {
        val request = Request.Builder().url(source.url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to fetch blocklist: HTTP ${response.code}")
            val rawContent = response.body?.string() ?: throw IOException("Empty response")
            val sha256Hash = sha256(rawContent)
            if (source.sha256Hash == sha256Hash) return

            val updatedAt = Instant.now().toString()
            val normalizedRules = normalizeRules(rawContent, source.format, source.id, updatedAt)
            blocklistDao.updateSource(
                source.copy(
                    version = getVersionFromContent(rawContent),
                    sha256Hash = sha256Hash,
                    lastUpdated = updatedAt
                )
            )
            blocklistDao.deleteRulesBySource(source.id)
            blocklistDao.insertRules(normalizedRules)
        }
    }

    private fun normalizeRules(
        rawContent: String,
        format: String,
        sourceId: String,
        lastUpdated: String
    ): List<BlocklistRule> {
        return rawContent.lineSequence().mapNotNull { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("!") || line.startsWith("#")) return@mapNotNull null
            when (format.lowercase()) {
                "adblock_plus", "adblock" -> normalizeAdBlockRule(line, sourceId, lastUpdated)
                "hosts" -> normalizeHostsRule(line, sourceId, lastUpdated)
                "regex" -> normalizeRegexRule(line, sourceId, lastUpdated)
                else -> normalizeGenericRule(line, sourceId, lastUpdated)
            }
        }.toList()
    }

    private fun normalizeAdBlockRule(line: String, sourceId: String, lastUpdated: String): BlocklistRule? {
        val isAllowlist = line.startsWith("@@")
        val cleanRule = if (isAllowlist) line.substring(2) else line
        val rulePattern = when {
            cleanRule.startsWith("||") -> cleanRule.removePrefix("||").removeSuffix("^")
            cleanRule.startsWith("|") -> cleanRule.substring(1)
            cleanRule.endsWith("^") -> cleanRule.dropLast(1)
            else -> cleanRule
        }
        if (rulePattern.isBlank()) return null
        return BlocklistRule(
            id = sha256(cleanRule), sourceId = sourceId, rule = rulePattern,
            type = if (rulePattern.contains('*') || rulePattern.contains('^')) "url" else "domain",
            category = "ads", isAllowlist = isAllowlist, isCompiled = false, lastUpdated = lastUpdated
        )
    }

    private fun normalizeHostsRule(line: String, sourceId: String, lastUpdated: String): BlocklistRule? {
        val parts = line.split(Regex("\\s+"))
        if (parts.size < 2) return null
        val domain = parts[1].trim().trimEnd('.')
        if (domain.isBlank()) return null
        return BlocklistRule(
            id = sha256(line), sourceId = sourceId, rule = domain, type = "domain",
            category = "ads", isAllowlist = false, isCompiled = false, lastUpdated = lastUpdated
        )
    }

    private fun normalizeRegexRule(line: String, sourceId: String, lastUpdated: String): BlocklistRule =
        BlocklistRule(
            id = sha256(line), sourceId = sourceId, rule = line, type = "regex",
            category = "ads", isAllowlist = false, isCompiled = false, lastUpdated = lastUpdated
        )

    private fun normalizeGenericRule(line: String, sourceId: String, lastUpdated: String): BlocklistRule =
        BlocklistRule(
            id = sha256(line), sourceId = sourceId, rule = line, type = "domain",
            category = "ads", isAllowlist = false, isCompiled = false, lastUpdated = lastUpdated
        )

    private fun getVersionFromContent(content: String): String? {
        val versionRegex = Regex("version:\\s*(\\d+\\.\\d+)")
        return versionRegex.find(content)?.groupValues?.getOrNull(1)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val WORK_NAME = "BlocklistFetcherWorker"
    }
}
