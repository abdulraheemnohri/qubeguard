package com.qubeguard.app.data.blocklist

import javax.inject.Inject

/**
 * Compiles blocklist rules into optimized data structures for fast lookup.
 * Uses Radix Tree, Bloom Filter, and Regex Engine for efficient matching.
 */
class RuleCompiler @Inject constructor() {
    private val radixTree = RadixTree()
    private val bloomFilter = BloomFilter()
    private val regexEngine = RegexEngine()

    /**
     * Compiles a list of BlocklistRule objects into optimized data structures.
     * @param rules The list of rules to compile.
     */
    fun compileRules(rules: List<BlocklistRule>) {
        clear()

        for (rule in rules) {
            when (rule.type) {
                "domain" -> {
                    radixTree.insert(rule.rule, rule.isAllowlist)
                    bloomFilter.add(rule.rule)
                }
                "url" -> {
                    // For URL patterns, use Radix Tree for domain part and Regex for path
                    val domain = extractDomain(rule.rule)
                    if (domain.isNotEmpty()) {
                        radixTree.insert(domain, rule.isAllowlist)
                        bloomFilter.add(domain)
                    }
                    if (rule.rule.contains("*") || rule.rule.contains("^")) {
                        regexEngine.addPattern(rule.rule, !rule.isAllowlist)
                    }
                }
                "regex" -> {
                    regexEngine.addPattern(rule.rule, !rule.isAllowlist)
                }
                "ip" -> {
                    // For IP-based rules, use Radix Tree or Regex
                    radixTree.insert(rule.rule, rule.isAllowlist)
                    bloomFilter.add(rule.rule)
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

        // First, check Bloom Filter for fast negative lookup
        if (!bloomFilter.mightContain(domain)) {
            return false
        }

        // Check Radix Tree for domain/subdomain matching
        if (radixTree.isBlockedOrSubdomainBlocked(domain)) {
            return true
        }

        // Check Regex Engine for complex patterns
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

        // Check Radix Tree for allowlist
        if (radixTree.isBlocked(domain)) {
            return true
        }

        // Check Regex Engine for allowlist patterns
        if (regexEngine.isAllowed(input)) {
            return true
        }

        return false
    }

    /**
     * Clears all compiled rules.
     */
    fun clear() {
        radixTree.clear()
        bloomFilter.clear()
        regexEngine.clear()
    }

    /**
     * Extracts the domain from a URL or domain string.
     * @param input The URL or domain string.
     * @return The extracted domain.
     */
    private fun extractDomain(input: String): String {
        val normalizedInput = input.lowercase().trim()

        // Remove protocol (http://, https://)
        var domain = normalizedInput
            .replace("http://".toRegex(), "")
            .replace("https://".toRegex(), "")

        // Remove path and query parameters
        val slashIndex = domain.indexOf('/')
        if (slashIndex != -1) {
            domain = domain.substring(0, slashIndex)
        }

        // Remove port number
        val colonIndex = domain.indexOf(':')
        if (colonIndex != -1) {
            domain = domain.substring(0, colonIndex)
        }

        return domain
    }
}