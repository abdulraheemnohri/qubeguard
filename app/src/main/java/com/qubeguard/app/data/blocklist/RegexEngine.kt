package com.qubeguard.app.data.blocklist

import java.util.regex.Pattern

/** Bounded regex matcher for trusted/normalized blocklist rules. */
class RegexEngine(
    private val maxPatternLength: Int = 4096,
    private val maxPatterns: Int = 50_000
) {
    private val patterns = ArrayList<PatternEntry>()

    fun addPattern(pattern: String, isBlocked: Boolean) {
        if (pattern.isBlank() || pattern.length > maxPatternLength || patterns.size >= maxPatterns) return
        runCatching {
            patterns += PatternEntry(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE), isBlocked)
        }
    }

    fun isBlocked(input: String): Boolean = matches(input, blocked = true)

    fun isAllowed(input: String): Boolean = matches(input, blocked = false)

    private fun matches(input: String, blocked: Boolean): Boolean {
        if (input.length > 32_768) return false
        for (entry in patterns) {
            if (entry.isBlocked == blocked && entry.pattern.matcher(input).find()) return true
        }
        return false
    }

    fun clear() = patterns.clear()

    private data class PatternEntry(val pattern: Pattern, val isBlocked: Boolean)
}
