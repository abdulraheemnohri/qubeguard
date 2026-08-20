package com.qubeguard.app.ml

import kotlin.math.log2
import kotlin.math.pow

/**
 * Extracts features from URLs/domains for the TFLite AI classifier.
 * Features include lexical, structural, and contextual attributes.
 */
class FeatureExtractor {

    /**
     * Extracts features from a URL or domain.
     * @param input The URL or domain to analyze.
     * @return A float array of features for the TFLite model.
     */
    fun extractFeatures(input: String): FloatArray {
        val domain = extractDomain(input)
        val features = mutableListOf<Float>()

        // Lexical Features
        features.add(urlLengthFeature(input))
        features.add(subdomainDepthFeature(domain))
        features.add(entropyFeature(domain))
        features.add(hasNumericFeature(domain))
        features.add(hasHyphenFeature(domain))

        // Structural Features
        features.add(tldRarityFeature(domain))
        features.add(hasIpAddressFeature(domain))
        features.add(hasShortenedUrlFeature(input))

        // Contextual Features (if available)
        features.add(hasSuspiciousKeywordsFeature(domain))
        features.add(hasHttpFeature(input))

        return features.toFloatArray()
    }

    /**
     * Extracts the domain from a URL or domain string.
     */
    private fun extractDomain(input: String): String {
        val normalizedInput = input.lowercase().trim()
            .replace("http://".toRegex(), "")
            .replace("https://".toRegex(), "")

        val slashIndex = normalizedInput.indexOf('/')
        val domain = if (slashIndex != -1) normalizedInput.substring(0, slashIndex) else normalizedInput

        val colonIndex = domain.indexOf(':')
        return if (colonIndex != -1) domain.substring(0, colonIndex) else domain
    }

    // --- Lexical Features ---

    /**
     * URL length feature (normalized).
     */
    private fun urlLengthFeature(url: String): Float {
        val length = url.length
        return (length - 20f) / 100f // Normalize around 20-120 chars
    }

    /**
     * Subdomain depth feature (number of dots in domain).
     */
    private fun subdomainDepthFeature(domain: String): Float {
        val depth = domain.count { it == '.' }
        return depth.toFloat() / 5f // Normalize by max expected depth (5)
    }

    /**
     * Shannon entropy of the domain (measure of randomness).
     */
    private fun entropyFeature(domain: String): Float {
        if (domain.isEmpty()) return 0f

        val charCounts = mutableMapOf<Char, Int>()
        for (char in domain) {
            charCounts[char] = charCounts.getOrDefault(char, 0) + 1
        }

        var entropy = 0.0
        val length = domain.length
        for (count in charCounts.values) {
            val probability = count.toDouble() / length
            entropy -= probability * log2(probability)
        }

        return (entropy / 4f).toFloat() // Normalize by max entropy for 256 chars (log2(256) = 8)
    }

    /**
     * Whether the domain contains numeric characters.
     */
    private fun hasNumericFeature(domain: String): Float {
        return if (domain.any { it.isDigit() }) 1f else 0f
    }

    /**
     * Whether the domain contains hyphens.
     */
    private fun hasHyphenFeature(domain: String): Float {
        return if (domain.contains('-')) 1f else 0f
    }

    // --- Structural Features ---

    /**
     * TLD rarity feature (1 = rare TLD, 0 = common TLD).
     */
    private fun tldRarityFeature(domain: String): Float {
        val tld = extractTld(domain)
        val commonTlds = setOf("com", "org", "net", "io", "co", "uk", "us", "de", "fr", "jp")
        return if (tld in commonTlds) 0f else 1f
    }

    /**
     * Whether the domain is an IP address.
     */
    private fun hasIpAddressFeature(domain: String): Float {
        val ipv4Pattern = Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
        return if (ipv4Pattern.matches(domain)) 1f else 0f
    }

    /**
     * Whether the URL is shortened (e.g., bit.ly, goo.gl).
     */
    private fun hasShortenedUrlFeature(url: String): Float {
        val shortenedDomains = setOf("bit.ly", "goo.gl", "tinyurl.com", "ow.ly", "t.co", "is.gd")
        val domain = extractDomain(url)
        return if (shortenedDomains.any { domain.contains(it) }) 1f else 0f
    }

    // --- Contextual Features ---

    /**
     * Whether the domain contains suspicious keywords (e.g., "ad", "track", "analytics").
     */
    private fun hasSuspiciousKeywordsFeature(domain: String): Float {
        val suspiciousKeywords = setOf("ad", "track", "analytics", "click", "banner", "promo", "xyz", "top")
        return if (suspiciousKeywords.any { domain.contains(it) }) 1f else 0f
    }

    /**
     * Whether the URL uses HTTP (not HTTPS).
     */
    private fun hasHttpFeature(url: String): Float {
        return if (url.startsWith("http://")) 1f else 0f
    }

    /**
     * Extracts the TLD (Top-Level Domain) from a domain.
     */
    private fun extractTld(domain: String): String {
        val parts = domain.split('.')
        return if (parts.size >= 2) parts.last() else ""
    }
}