package com.qubeguard.app

import com.qubeguard.app.data.blocklist.DeterministicBlocker
import com.qubeguard.app.ml.MLClassifier
import com.qubeguard.app.ml.TransformerUrlClassifier
import com.qubeguard.app.policy.PolicyEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private val mlClassifier: MLClassifier = mockk()

    @Before
    fun setup() {
        policyEngine = PolicyEngine(deterministicBlocker, mlClassifier)
    }

    @Test
    fun `test decide with allowlisted URL`() = runBlocking {
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
        coEvery { deterministicBlocker.isAllowed(any()) } returns false
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
        coEvery { deterministicBlocker.isAllowed(any()) } returns false
        coEvery { deterministicBlocker.isBlocked(any()) } returns false

        val decision = policyEngine.decide("example.com", isDnsRequest = true)

        assertEquals(false, decision.isBlocked)
        assertEquals("Allowed (Layer 2 - DNS)", decision.reason)
        assertEquals(2, decision.layer)
        assertEquals(1.0f, decision.confidence, 0.01f)
    }

    @Test
    fun `test decide with ML blocked URL`() = runBlocking {
        coEvery { deterministicBlocker.isAllowed(any()) } returns false
        coEvery { deterministicBlocker.isBlocked(any()) } returns false
        every { mlClassifier.isModelLoaded() } returns true
        every { mlClassifier.classify(any()) } returns TransformerUrlClassifier.MALWARE
        every { mlClassifier.getConfidenceScores(any()) } returns mapOf(TransformerUrlClassifier.MALWARE to 0.9f)

        val decision = policyEngine.decide("malicious.example.com")

        assertEquals(true, decision.isBlocked)
        assertEquals("Blocked by local Transformer (Layer 3 - Malware)", decision.reason)
        assertEquals(3, decision.layer)
        assertEquals(0.9f, decision.confidence, 0.01f)

        verify { mlClassifier.classify("malicious.example.com") }
        verify { mlClassifier.getConfidenceScores("malicious.example.com") }
    }

    @Test
    fun `test decide with allowed URL`() = runBlocking {
        coEvery { deterministicBlocker.isAllowed(any()) } returns false
        coEvery { deterministicBlocker.isBlocked(any()) } returns false
        every { mlClassifier.isModelLoaded() } returns true
        every { mlClassifier.classify(any()) } returns TransformerUrlClassifier.BENIGN
        every { mlClassifier.getConfidenceScores(any()) } returns mapOf(TransformerUrlClassifier.BENIGN to 0.95f)

        val decision = policyEngine.decide("legitimate.example.com")

        assertEquals(false, decision.isBlocked)
        assertEquals("Allowed (no deterministic or Transformer match)", decision.reason)
        assertEquals(0, decision.layer)
        assertEquals(0.95f, decision.confidence, 0.01f)
    }

    @Test
    fun `test isBlocked`() = runBlocking {
        coEvery { deterministicBlocker.isAllowed(any()) } returns false
        coEvery { deterministicBlocker.isBlocked(any()) } returns true

        val isBlocked = policyEngine.isBlocked("ads.example.com")

        assertEquals(true, isBlocked)

        coVerify { deterministicBlocker.isBlocked("ads.example.com") }
    }

    @Test
    fun `test getCategory`() {
        every { mlClassifier.isModelLoaded() } returns true
        every { mlClassifier.classify(any()) } returns TransformerUrlClassifier.DEFACEMENT

        val category = policyEngine.getCategory("defaced.example.com")

        assertEquals(TransformerUrlClassifier.DEFACEMENT, category)

        verify { mlClassifier.classify("defaced.example.com") }
    }

    @Test
    fun `test getConfidenceScores`() {
        every { mlClassifier.isModelLoaded() } returns true
        every { mlClassifier.getConfidenceScores(any()) } returns mapOf(
            TransformerUrlClassifier.BENIGN to 0.1f,
            TransformerUrlClassifier.DEFACEMENT to 0.2f,
            TransformerUrlClassifier.PHISHING to 0.7f
        )

        val scores = policyEngine.getConfidenceScores("example.com")

        assertEquals(0.1f, scores[TransformerUrlClassifier.BENIGN] ?: 0f, 0.01f)
        assertEquals(0.2f, scores[TransformerUrlClassifier.DEFACEMENT] ?: 0f, 0.01f)
        assertEquals(0.7f, scores[TransformerUrlClassifier.PHISHING] ?: 0f, 0.01f)

        verify { mlClassifier.getConfidenceScores("example.com") }
    }
}
