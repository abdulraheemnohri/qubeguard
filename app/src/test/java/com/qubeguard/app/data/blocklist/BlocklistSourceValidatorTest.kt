package com.qubeguard.app.data.blocklist

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class BlocklistSourceValidatorTest {
    @Test fun acceptsHttpsUrl() {
        assertTrue(BlocklistSourceValidator.validate("https://example.com/list.txt").isSuccess)
    }

    @Test fun rejectsHttpUrl() {
        assertFalse(BlocklistSourceValidator.validate("http://example.com/list.txt").isSuccess)
    }

    @Test fun rejectsUserInfo() {
        assertFalse(BlocklistSourceValidator.validate("https://user:pass@example.com/list.txt").isSuccess)
    }

    @Test fun rejectsFragment() {
        assertFalse(BlocklistSourceValidator.validate("https://example.com/list.txt#rules").isSuccess)
    }
}
