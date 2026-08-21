package com.qubeguard.app.data.blocklist

import java.util.regex.Pattern

/**
 * A Regex Engine for matching complex URL patterns.
 * Used for rules that cannot be efficiently matched with Radix Tree or Bloom Filter.
 */
class RegexEngine {
    private val patterns = mutableListOf<PatternEntry>()

    /**
     * Adds a regex pattern to the engine.
     * @param pattern The regex pattern to add.
     * @param isBlocked Whether this pattern is blocked or allowed.
     */
    fun addPattern(pattern: String, isBlocked: Boolean) {
        try {
            val compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
            patterns.add(PatternEntry(compiledPattern, isBlocked))
        } catch (_: Exception) {
            // Log or ignore invalid regex pattern
        }
    }

    /**
     * Checks if a URL matches any of the blocked patterns.
     * @param url The URL to check.
     * @return True if the URL matches any blocked pattern.
     */
    fun isBlocked(url: String): Boolean {
        for (entry in patterns) {
            if (entry.isBlocked && entry.pattern.matcher(url).find()) {
                return true
            }
        }
        return false
    }

    /**
     * Checks if a URL matches any of the allowed patterns.
     * @param url The URL to check.
     * @return True if the URL matches any allowed pattern.
     */
    fun isAllowed(url: String): Boolean {
        for (entry in patterns) {
            if (!entry.isBlocked && entry.pattern.matcher(url).find()) {
                return true
            }
        }
        return false
    }

    /**
     * Clears all patterns from the engine.
     */
    fun clear() {
        patterns.clear()
    }

    private data class PatternEntry(
        val pattern: Pattern,
        val isBlocked: Boolean
    )
}
