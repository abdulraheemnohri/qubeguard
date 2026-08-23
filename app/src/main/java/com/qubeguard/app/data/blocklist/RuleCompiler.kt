package com.qubeguard.app.data.blocklist

import javax.inject.Inject

/**
 * Compiles blocklist rules into optimized data structures for fast lookup.
 * Uses Radix Tree, Bloom Filter, and Regex Engine for efficient matching.
 */
class RuleCompiler @Inject constructor() {
    private val blocklistTree = RadixTree()
    private val allowlistTree = RadixTree()
    private val bloomFilter = BloomFilter()
    private val regexEngine = RegexEngine()

    /**
     * Compiles a list of BlocklistRule objects into optimized data structures.
     * @param rules The list of rules to compile.
     */
    fun compileRules(rules: List<BlocklistRule>) {
        clear()

        for (rule in rules) {
            if (rule.isAllowlist) {
                when (rule.type) {
                    "domain", "ip" -> allowlistTree.insert(rule.rule, true)
                    "url" -> {
                        val domain = extractDomain(rule.rule)
                        if (domain.isNotEmpty()) allowlistTree.insert(domain, true)
                        if (rule.rule.contains("*") || rule.rule.contains("^") || rule.rule.contains("||")) {
                            regexEngine.addPattern(convertGlobToRegex(rule.rule), isBlocked = false)
                        }
                    }
                    "regex" -> regexEngine.addPattern(rule.rule, isBlocked = false)
                }
            } else {
                when (rule.type) {
                    "domain", "ip" -> {
                        blocklistTree.insert(rule.rule, true)
                        bloomFilter.add(rule.rule)
                    }
                    "url" -> {
                        val domain = extractDomain(rule.rule)
                        if (domain.isNotEmpty()) {
                            blocklistTree.insert(domain, true)
                            bloomFilter.add(domain)
                        }
                        if (rule.rule.contains("*") || rule.rule.contains("^") || rule.rule.contains("||")) {
                            regexEngine.addPattern(convertGlobToRegex(rule.rule), isBlocked = true)
                        }
                    }
                    "regex" -> regexEngine.addPattern(rule.rule, isBlocked = true)
                }
            }
        }
    }

    /**
     * Checks if a domain or URL is blocked.
     * @param input The domain or URL to check.
     * @return True if the input is blocked.
     */
    fun isBlocked(input: String): Boolean {
        val domain = extractDomain(input)

        if (bloomFilter.mightContain(domain) && blocklistTree.isBlocked(domain)) {
            return true
        }

        if (regexEngine.isBlocked(input)) {
            return true
        }

        return false
    }

    /**
     * Checks if a domain or URL is allowed (whitelisted).
     * @param input The domain or URL to check.
     * @return True if the input is allowed.
     */
    fun isAllowed(input: String): Boolean {
        val domain = extractDomain(input)

        if (allowlistTree.isBlocked(domain)) {
            return true
        }

        if (regexEngine.isAllowed(input)) {
            return true
        }

        return false
    }

    /**
     * Clears all compiled rules.
     */
    fun clear() {
        blocklistTree.clear()
        allowlistTree.clear()
        bloomFilter.clear()
        regexEngine.clear()
    }

    private fun convertGlobToRegex(pattern: String): String {
        if (pattern.isBlank()) return ".*"
        var p = pattern.trim()
        var prefix = ""
        if (p.startsWith("||")) {
            prefix = "^https?://(?:[a-zA-Z0-9\\-]+\\.)*"
            p = p.substring(2)
        } else if (p.startsWith("|")) {
            prefix = "^"
            p = p.substring(1)
        }

        var suffix = ""
        if (p.endsWith("|")) {
            suffix = "$"
            p = p.dropLast(1)
        }

        val sb = StringBuilder(prefix)
        for (ch in p) {
            when (ch) {
                '*' -> sb.append(".*")
                '^' -> sb.append("(?:[^a-zA-Z0-9\\._\\-%]|$)")
                '.', '?', '+', '(', ')', '[', ']', '{', '}', '\\', '$' -> sb.append('\\').append(ch)
                else -> sb.append(ch)
            }
        }
        sb.append(suffix)
        return sb.toString()
    }

    /**
     * Extracts the domain from a URL or domain string.
     * @param input The URL or domain string.
     * @return The extracted domain.
     */
    private fun extractDomain(input: String): String {
        val normalizedInput = input.lowercase().trim()

        var domain = normalizedInput
            .replace("http://".toRegex(), "")
            .replace("https://".toRegex(), "")

        val slashIndex = domain.indexOf('/')
        if (slashIndex != -1) {
            domain = domain.substring(0, slashIndex)
        }

        val colonIndex = domain.indexOf(':')
        if (colonIndex != -1) {
            domain = domain.substring(0, colonIndex)
        }

        return domain
    }
}
