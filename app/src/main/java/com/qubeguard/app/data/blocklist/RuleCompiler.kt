package com.qubeguard.app.data.blocklist

import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject

/** Builds immutable compiled rule snapshots so readers never observe a partial rebuild. */
class RuleCompiler @Inject constructor() {
    private data class CompiledRules(
        val blocklistTree: RadixTree,
        val allowlistTree: RadixTree,
        val bloomFilter: BloomFilter,
        val regexEngine: RegexEngine
    )

    @Volatile
    private var snapshot = emptySnapshot()

    fun compileRules(rules: List<BlocklistRule>) {
        val blockTree = RadixTree()
        val allowTree = RadixTree()
        val bloom = BloomFilter()
        val regex = RegexEngine()

        for (rule in rules) {
            val value = rule.rule.trim()
            if (value.isEmpty()) continue
            val targetTree = if (rule.isAllowlist) allowTree else blockTree
            when (rule.type.lowercase()) {
                "domain", "ip" -> {
                    targetTree.insert(canonicalDomain(value), true)
                    if (!rule.isAllowlist) bloom.add(canonicalDomain(value))
                }
                "url" -> {
                    val domain = extractDomain(value)
                    if (domain.isNotEmpty()) {
                        targetTree.insert(domain, true)
                        if (!rule.isAllowlist) bloom.add(domain)
                    }
                    if (value.contains('*') || value.contains('^') || value.startsWith("||") || value.startsWith('|')) {
                        regex.addPattern(convertGlobToRegex(value), isBlocked = !rule.isAllowlist)
                    }
                }
                "regex" -> regex.addPattern(value, isBlocked = !rule.isAllowlist)
            }
        }
        snapshot = CompiledRules(blockTree, allowTree, bloom, regex)
    }

    fun isBlocked(input: String): Boolean {
        val current = snapshot
        val domain = extractDomain(input)
        if (current.allowlistTree.isBlocked(domain)) return false
        if (current.regexEngine.isAllowed(input)) return false
        if (current.bloomFilter.mightContain(domain) && current.blocklistTree.isBlocked(domain)) return true
        return current.regexEngine.isBlocked(input)
    }

    fun isAllowed(input: String): Boolean {
        val current = snapshot
        return current.allowlistTree.isBlocked(extractDomain(input)) || current.regexEngine.isAllowed(input)
    }

    fun clear() { snapshot = emptySnapshot() }

    private fun extractDomain(input: String): String {
        val value = input.trim().lowercase().trimEnd('.')
        if (value.isEmpty()) return ""
        return try {
            val uri = if (value.contains("://")) URI(value) else URI("https://$value")
            (uri.host ?: value.substringBefore('/').substringBefore('?').substringBefore('#'))
                .trim().trimEnd('.').lowercase()
                .removePrefix("[").removeSuffix("]")
        } catch (_: URISyntaxException) {
            value.substringBefore('/').substringBefore('?').substringBefore('#').substringBeforeLast(':').trimEnd('.')
        }
    }

    private fun canonicalDomain(value: String): String = extractDomain(value).ifEmpty { value.trim().trimEnd('.').lowercase() }

    private fun convertGlobToRegex(pattern: String): String {
        var p = pattern.trim()
        var prefix = ""
        if (p.startsWith("||")) {
            prefix = "^https?://(?:[^/?#]+\\.)*"
            p = p.substring(2)
        } else if (p.startsWith("|")) {
            prefix = "^"
            p = p.substring(1)
        }
        val suffix = if (p.endsWith("|")) { p = p.dropLast(1); "$" } else ""
        val out = StringBuilder(prefix)
        for (ch in p) {
            when (ch) {
                '*' -> out.append(".*")
                '^' -> out.append("(?:[^a-zA-Z0-9._%-]|$)")
                '.', '?', '+', '(', ')', '[', ']', '{', '}', '\\', '$' -> out.append('\\').append(ch)
                else -> out.append(ch)
            }
        }
        return out.append(suffix).toString()
    }

    private fun emptySnapshot() = CompiledRules(RadixTree(), RadixTree(), BloomFilter(), RegexEngine())
}
