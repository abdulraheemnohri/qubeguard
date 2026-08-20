package com.qubeguard.app.data.blocklist

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.qubeguard.app.data.blocklist.BlocklistDao
import com.qubeguard.app.data.blocklist.BlocklistSource
import com.qubeguard.app.data.blocklist.BlocklistRule
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for fetching and updating blocklists from their sources.
 * Downloads, normalizes, and stores blocklist rules in the database.
 */
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
        return@withContext try {
            // Fetch all enabled blocklist sources
            val sources = blocklistDao.getEnabledSources()
            if (sources.isEmpty()) {
                return@withContext Result.success()
            }

            // Process each source
            sources.forEach { source ->
                try {
                    fetchAndUpdateBlocklist(source)
                } catch (e: Exception) {
                    // Log error but continue with other sources
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /**
     * Fetches a blocklist from its URL, normalizes the rules, and updates the database.
     */
    private suspend fun fetchAndUpdateBlocklist(source: BlocklistSource) {
        val url = source.url
        val request = Request.Builder().url(url).build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to fetch blocklist: HTTP ${response.code}")
            }

            val rawContent = response.body?.string() ?: throw IOException("Empty response")
            val sha256Hash = sha256(rawContent)

            // Skip if the content hasn't changed
            if (source.sha256Hash == sha256Hash) {
                return
            }

            // Normalize the rules (convert to unified format)
            val normalizedRules = normalizeRules(rawContent, source.format, source.id)

            // Update the source metadata
            val updatedSource = source.copy(
                version = getVersionFromContent(rawContent),
                sha256Hash = sha256Hash,
                lastUpdated = java.time.Instant.now().toString()
            )
            blocklistDao.updateSource(updatedSource)

            // Delete old rules for this source
            blocklistDao.deleteRulesBySource(source.id)

            // Insert new rules
            blocklistDao.insertRules(normalizedRules)
        }
    }

    /**
     * Normalizes raw blocklist content into a list of BlocklistRule objects.
     * Supports AdBlock Plus, Hosts, and Regex formats.
     */
    private fun normalizeRules(
        rawContent: String,
        format: String,
        sourceId: String
    ): List<BlocklistRule> {
        val rules = mutableListOf<BlocklistRule>()
        val lines = rawContent.split("\n")

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("!") || trimmedLine.startsWith("#")) {
                continue // Skip comments and empty lines
            }

            val rule = when (format.lowercase()) {
                "adblock_plus", "adblock" -> normalizeAdBlockRule(trimmedLine, sourceId)
                "hosts" -> normalizeHostsRule(trimmedLine, sourceId)
                "regex" -> normalizeRegexRule(trimmedLine, sourceId)
                else -> normalizeGenericRule(trimmedLine, sourceId)
            }

            if (rule != null) {
                rules.add(rule)
            }
        }

        return rules
    }

    /**
     * Normalizes an AdBlock Plus rule into a BlocklistRule.
     */
    private fun normalizeAdBlockRule(line: String, sourceId: String): BlocklistRule? {
        // Example: ||example.com^ or @@||example.com^ (allowlist)
        val isAllowlist = line.startsWith("@@")
        val cleanRule = if (isAllowlist) line.substring(2) else line

        // Extract domain or pattern
        val rulePattern = when {
            cleanRule.startsWith("||") -> cleanRule.substring(2).replace("^".toRegex(), "")
            cleanRule.startsWith("|") -> cleanRule.substring(1)
            cleanRule.endsWith("^") -> cleanRule.replace("^".toRegex(), "")
            else -> cleanRule
        }

        return BlocklistRule(
            id = sha256(cleanRule),
            sourceId = sourceId,
            rule = rulePattern,
            type = if (rulePattern.contains("*") || rulePattern.contains("^")) "url" else "domain",
            category = "ads", // Default category, can be updated later
            isAllowlist = isAllowlist,
            isCompiled = false
        )
    }

    /**
     * Normalizes a Hosts file rule into a BlocklistRule.
     */
    private fun normalizeHostsRule(line: String, sourceId: String): BlocklistRule? {
        // Example: 127.0.0.1 example.com
        val parts = line.split("\s+".toRegex())
        if (parts.size < 2) return null

        val ip = parts[0]
        val domain = parts[1]

        return BlocklistRule(
            id = sha256(line),
            sourceId = sourceId,
            rule = domain,
            type = "domain",
            category = "ads", // Default category
            isAllowlist = false,
            isCompiled = false
        )
    }

    /**
     * Normalizes a regex rule into a BlocklistRule.
     */
    private fun normalizeRegexRule(line: String, sourceId: String): BlocklistRule? {
        return BlocklistRule(
            id = sha256(line),
            sourceId = sourceId,
            rule = line,
            type = "regex",
            category = "ads", // Default category
            isAllowlist = false,
            isCompiled = false
        )
    }

    /**
     * Normalizes a generic rule into a BlocklistRule.
     */
    private fun normalizeGenericRule(line: String, sourceId: String): BlocklistRule? {
        return BlocklistRule(
            id = sha256(line),
            sourceId = sourceId,
            rule = line,
            type = "domain",
            category = "ads", // Default category
            isAllowlist = false,
            isCompiled = false
        )
    }

    /**
     * Extracts version from blocklist content (if available).
     */
    private fun getVersionFromContent(content: String): String? {
        val versionRegex = Regex("version:\\s*(\\d+\\.\\d+)")
        return versionRegex.find(content)?.groupValues?.get(1)
    }

    /**
     * Computes SHA-256 hash of a string.
     */
    private fun sha256(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    companion object {
        const val WORK_NAME = "BlocklistFetcherWorker"
    }
}