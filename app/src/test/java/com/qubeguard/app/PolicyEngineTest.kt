package com.qubeguard.app

import com.qubeguard.app.data.blocklist.DeterministicBlocker
import com.qubeguard.app.data.blocklist.RuleCompiler
import com.qubeguard.app.ml.FeatureExtractor
import com.qubeguard.app.ml.TfLiteClassifier
import com.qubeguard.app.policy.PolicyEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PolicyEngine.
 */
class PolicyEngineTest {

    private lateinit var policyEngine: PolicyEngine
    private val deterministicBlocker: DeterministicBlocker = mockk()
    private val tfLiteClassifier: TfLiteClassifier = mockk()

    @Before
    fun setup() {
        policyEngine = PolicyEngine(deterministicBlocker, tfLiteClassifier)
    }

    @Test
    fun `test decide with allowlisted URL`() = runBlocking {
        // Mock deterministicBlocker.isAllowed to return true
        coEvery { deterministicBlocker.isAllowed(any()) } returns true

        val decision = policyEngine.decide("example.com")

        assertEquals(false, decision.isBlocked)
        assertEquals("Allowlisted (Layer 1)", decision.reason)
        assertEquals(1, decision.layer)
        assertEquals(1.0f, decision.confidence, 0.01f)

        coVerify { deterministicBlocker.isAllowed("example.com") }
    }

    @Test
    fun `test decide with blocked URL in Layer 1`() = runBlocking {
        // Mock deterministicBlocker.isAllowed to return false
        coEvery { deterministicBlocker.isAllowed(any()) } returns false
        // Mock deterministicBlocker.isBlocked to return true
        coEvery { deterministicBlocker.isBlocked(any()) } returns true

        val decision = policyEngine.decide("ads.example.com")

        assertEquals(true, decision.isBlocked)
        assertEquals("Blocked by deterministic rules (Layer 1)", decision.reason)
        assertEquals(1, decision.layer)
        assertEquals(1.0f, decision.confidence, 0.01f)

        coVerify { deterministicBlocker.isBlocked("ads.example.com") }
    }

    @Test
    fun `test decide with DNS request`() = runBlocking {
        // Mock deterministicBlocker.isAllowed to return false
        coEvery { deterministicBlocker.isAllowed(any()) } returns false
        // Mock deterministicBlocker.isBlocked to return false
        coEvery { deterministicBlocker.isBlocked(any()) } returns false

        val decision = policyEngine.decide("example.com", isDnsRequest = true)

        assertEquals(false, decision.isBlocked)
        assertEquals("Allowed (Layer 2 - DNS)", decision.reason)
        assertEquals(2, decision.layer)
        assertEquals(1.0f, decision.confidence, 0.01f)
    }

    @Test
    fun `test decide with ML blocked URL`() = runBlocking {
        // Mock deterministicBlocker.isAllowed to return false
        coEvery { deterministicBlocker.isAllowed(any()) } returns false
        // Mock deterministicBlocker.isBlocked to return false
        coEvery { deterministicBlocker.isBlocked(any()) } returns false
        // Mock tfLiteClassifier.classify to return "Malware"
        coEvery { tfLiteClassifier.classify(any()) } returns "Malware"
        // Mock tfLiteClassifier.getConfidenceScores to return high confidence for Malware
        coEvery { tfLiteClassifier.getConfidenceScores(any()) } returns mapOf("Malware" to 0.9f)

        val decision = policyEngine.decide("malicious.example.com")

        assertEquals(true, decision.isBlocked)
        assertEquals("Blocked by ML classifier (Layer 3 - Malware)", decision.reason)
        assertEquals(3, decision.layer)
        assertEquals(0.9f, decision.confidence, 0.01f)

        coVerify { tfLiteClassifier.classify("malicious.example.com") }
        coVerify { tfLiteClassifier.getConfidenceScores("malicious.example.com") }
    }

    @Test
    fun `test decide with allowed URL`() = runBlocking {
        // Mock deterministicBlocker.isAllowed to return false
        coEvery { deterministicBlocker.isAllowed(any()) } returns false
        // Mock deterministicBlocker.isBlocked to return false
        coEvery { deterministicBlocker.isBlocked(any()) } returns false
        // Mock tfLiteClassifier.classify to return "Legitimate"
        coEvery { tfLiteClassifier.classify(any()) } returns "Legitimate"

        val decision = policyEngine.decide("legitimate.example.com")

        assertEquals(false, decision.isBlocked)
        assertEquals("Allowed (No match in any layer)", decision.reason)
        assertEquals(0, decision.layer)
        assertEquals(0f, decision.confidence, 0.01f)
    }

    @Test
    fun `test isBlocked`() = runBlocking {
        // Mock deterministicBlocker.isAllowed to return false
        coEvery { deterministicBlocker.isAllowed(any()) } returns false
        // Mock deterministicBlocker.isBlocked to return true
        coEvery { deterministicBlocker.isBlocked(any()) } returns true

        val isBlocked = policyEngine.isBlocked("ads.example.com")

        assertEquals(true, isBlocked)

        coVerify { deterministicBlocker.isBlocked("ads.example.com") }
    }

    @Test
    fun `test getCategory`() {
        // Mock tfLiteClassifier.classify to return "Tracker"
        coEvery { tfLiteClassifier.classify(any()) } returns "Tracker"

        val category = policyEngine.getCategory("tracker.example.com")

        assertEquals("Tracker", category)

        coVerify { tfLiteClassifier.classify("tracker.example.com") }
    }

    @Test
    fun `test getConfidenceScores`() {
        // Mock tfLiteClassifier.getConfidenceScores to return a map
        coEvery { tfLiteClassifier.getConfidenceScores(any()) } returns mapOf(
            "Legitimate" to 0.1f,
            "Ad" to 0.2f,
            "Tracker" to 0.7f
        )

        val scores = policyEngine.getConfidenceScores("example.com")

        assertEquals(0.1f, scores["Legitimate"], 0.01f)
        assertEquals(0.2f, scores["Ad"], 0.01f)
        assertEquals(0.7f, scores["Tracker"], 0.01f)

        coVerify { tfLiteClassifier.getConfidenceScores("example.com") }
    }
}
