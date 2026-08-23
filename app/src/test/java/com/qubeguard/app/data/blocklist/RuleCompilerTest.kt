package com.qubeguard.app.data.blocklist

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleCompilerTest {

    private lateinit var ruleCompiler: RuleCompiler

    @Before
    fun setup() {
        ruleCompiler = RuleCompiler()
    }

    @Test
    fun `test domain blocklist match`() {
        val rules = listOf(
            BlocklistRule(id = "1", sourceId = "test", rule = "ads.example.com", type = "domain", category = "ads", isAllowlist = false)
        )
        ruleCompiler.compileRules(rules)

        assertTrue(ruleCompiler.isBlocked("ads.example.com"))
        assertTrue(ruleCompiler.isBlocked("http://ads.example.com/page"))
        assertFalse(ruleCompiler.isBlocked("example.com"))
    }

    @Test
    fun `test url pattern match when domain bloom filter misses`() {
        val rules = listOf(
            BlocklistRule(id = "1", sourceId = "test", rule = "||example.com/tracking/*", type = "url", category = "tracking", isAllowlist = false)
        )
        ruleCompiler.compileRules(rules)

        // URL matching regex pattern
        assertTrue(ruleCompiler.isBlocked("http://example.com/tracking/pixel.gif"))
    }

    @Test
    fun `test allowlist priority over blocklist`() {
        val rules = listOf(
            BlocklistRule(id = "1", sourceId = "test", rule = "example.com", type = "domain", category = "ads", isAllowlist = false),
            BlocklistRule(id = "2", sourceId = "test", rule = "sub.example.com", type = "domain", category = "ads", isAllowlist = true)
        )
        ruleCompiler.compileRules(rules)

        assertTrue(ruleCompiler.isBlocked("example.com"))
        assertFalse(ruleCompiler.isBlocked("sub.example.com"))
        assertTrue(ruleCompiler.isAllowed("sub.example.com"))
    }
}
