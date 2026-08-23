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
import java.util.concurrent.TimeUnit

@HiltWorker
class BlocklistFetcherWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val blocklistDao: BlocklistDao
) : CoroutineWorker(context, workerParams) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sources = blocklistDao.getEnabledSources()
            if (sources.isEmpty()) return@withContext Result.success()

            var failures = 0
            sources.forEach { source ->
                try {
                    fetchAndUpdateBlocklist(source)
                } catch (_: Exception) {
                    failures++
                }
            }

            if (failures == sources.size) Result.retry() else Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun fetchAndUpdateBlocklist(source: BlocklistSource) {
        val request = Request.Builder()
            .url(source.url)
            .header("User-Agent", "QubeGuard/1.0 blocklist-updater")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to fetch ${source.id}: HTTP ${response.code}")
            }

            val rawContent = response.body?.string() ?: throw IOException("Empty response for ${source.id}")
            if (rawContent.isBlank()) throw IOException("Blank response for ${source.id}")

            val sha256Hash = sha256(rawContent)
            if (source.sha256Hash == sha256Hash) return

            val updatedAt = System.currentTimeMillis().toString()
            val normalizedRules = normalizeRules(
                rawContent = rawContent,
                format = source.format,
                category = source.category,
                sourceId = source.id,
                lastUpdated = updatedAt
            )

            val updatedSource = source.copy(
                version = getVersionFromContent(rawContent),
                sha256Hash = sha256Hash,
                lastUpdated = updatedAt
            )

            // Atomic database update with transaction rollback protection
            blocklistDao.replaceRulesForSourceAtomic(source.id, normalizedRules, updatedSource)
        }
    }

    private fun normalizeRules(
        rawContent: String,
        format: String,
        category: String,
        sourceId: String,
        lastUpdated: String
    ): List<BlocklistRule> = rawContent.lineSequence().mapNotNull { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("!") || line.startsWith("#")) return@mapNotNull null

        when (format.lowercase()) {
            "adblock_plus", "adblock" -> normalizeAdBlockRule(line, category, sourceId, lastUpdated)
            "hosts" -> normalizeHostsRule(line, category, sourceId, lastUpdated)
            "regex" -> normalizeRegexRule(line, category, sourceId, lastUpdated)
            else -> normalizeGenericRule(line, category, sourceId, lastUpdated)
        }
    }.toList()

    private fun normalizeAdBlockRule(
        line: String,
        category: String,
        sourceId: String,
        lastUpdated: String
    ): BlocklistRule? {
        val isAllowlist = line.startsWith("@@")
        val cleanRule = if (isAllowlist) line.substring(2) else line
        val withoutOptions = cleanRule.substringBefore("$")
        val rulePattern = when {
            withoutOptions.startsWith("||") -> withoutOptions.removePrefix("||").removeSuffix("^")
            withoutOptions.startsWith("|") -> withoutOptions.substring(1)
            withoutOptions.endsWith("^") -> withoutOptions.dropLast(1)
            else -> withoutOptions
        }.trim('*')

        if (rulePattern.isBlank()) return null

        val isUrlPattern = rulePattern.contains('*') || rulePattern.contains('^') || rulePattern.contains('/')

        return BlocklistRule(
            id = sha256(cleanRule),
            sourceId = sourceId,
            rule = rulePattern,
            type = if (isUrlPattern) "url" else "domain",
            category = category,
            isAllowlist = isAllowlist,
            isCompiled = false,
            lastUpdated = lastUpdated
        )
    }

    private fun normalizeHostsRule(
        line: String,
        category: String,
        sourceId: String,
        lastUpdated: String
    ): BlocklistRule? {
        val parts = line.split(Regex("\\s+"))
        if (parts.size < 2) return null
        val domain = parts[1].trim().trimEnd('.')
        if (domain.isBlank() || domain == "localhost" || domain == "broadcasthost") return null

        return BlocklistRule(
            id = sha256(line),
            sourceId = sourceId,
            rule = domain,
            type = "domain",
            category = category,
            isAllowlist = false,
            isCompiled = false,
            lastUpdated = lastUpdated
        )
    }

    private fun normalizeRegexRule(
        line: String,
        category: String,
        sourceId: String,
        lastUpdated: String
    ) = BlocklistRule(
        id = sha256(line),
        sourceId = sourceId,
        rule = line,
        type = "regex",
        category = category,
        isAllowlist = false,
        isCompiled = false,
        lastUpdated = lastUpdated
    )

    private fun normalizeGenericRule(
        line: String,
        category: String,
        sourceId: String,
        lastUpdated: String
    ) = BlocklistRule(
        id = sha256(line),
        sourceId = sourceId,
        rule = line,
        type = "domain",
        category = category,
        isAllowlist = false,
        isCompiled = false,
        lastUpdated = lastUpdated
    )

    private fun getVersionFromContent(content: String): String? {
        val versionRegex = Regex("version:\\s*(\\d+(?:\\.\\d+)+)", RegexOption.IGNORE_CASE)
        return versionRegex.find(content)?.groupValues?.getOrNull(1)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val WORK_NAME = "BlocklistFetcherWorker"
    }
}
