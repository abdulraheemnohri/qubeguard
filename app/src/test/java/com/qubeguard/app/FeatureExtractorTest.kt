package com.qubeguard.app

import com.qubeguard.app.ml.FeatureExtractor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for FeatureExtractor.
 */
class FeatureExtractorTest {

    private val featureExtractor = FeatureExtractor()

    @Test
    fun `test extractFeatures for legitimate URL`() {
        val url = "https://www.example.com"
        val features = featureExtractor.extractFeatures(url)

        // FeatureExtractor returns 10 features
        assertEquals(10, features.size)

        // First feature is URL length (normalized)
        // example.com has length 18 (https://www. = 12, example.com = 11, total = 23)
        // Normalized: (23 - 20) / 100 = 0.03
        assertEquals(0.03f, features[0], 0.01f)
    }

    @Test
    fun `test extractFeatures for ad URL`() {
        val url = "https://ads.example.com/banner"
        val features = featureExtractor.extractFeatures(url)

        assertEquals(10, features.size)

        // Check if suspicious keywords feature is triggered
        // "ads" is in the URL, so hasSuspiciousKeywordsFeature should be 1f
        assertEquals(1f, features[8], 0.01f)
    }

    @Test
    fun `test extractFeatures for HTTP URL`() {
        val url = "http://example.com"
        val features = featureExtractor.extractFeatures(url)

        assertEquals(10, features.size)

        // Check if HTTP feature is triggered
        assertEquals(1f, features[9], 0.01f)
    }

    @Test
    fun `test extractFeatures for URL with IP`() {
        val url = "http://192.168.1.1"
        val features = featureExtractor.extractFeatures(url)

        assertEquals(10, features.size)

        // Check if IP address feature is triggered
        assertEquals(1f, features[6], 0.01f)
    }

    @Test
    fun `test extractFeatures for shortened URL`() {
        val url = "https://bit.ly/abc123"
        val features = featureExtractor.extractFeatures(url)

        assertEquals(10, features.size)

        // Check if shortened URL feature is triggered
        assertEquals(1f, features[7], 0.01f)
    }

    @Test
    fun `test extractDomain`() {
        val url1 = "https://www.example.com/path?query=1"
        val domain1 = featureExtractor.extractDomain(url1)
        assertEquals("www.example.com", domain1)

        val url2 = "http://example.com"
        val domain2 = featureExtractor.extractDomain(url2)
        assertEquals("example.com", domain2)

        val url3 = "example.com"
        val domain3 = featureExtractor.extractDomain(url3)
        assertEquals("example.com", domain3)
    }
}
