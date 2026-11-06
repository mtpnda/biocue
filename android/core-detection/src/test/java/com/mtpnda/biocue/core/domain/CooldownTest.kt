package com.mtpnda.biocue.core.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CooldownTest {
    @Test fun `the first request is allowed and an immediate repeat is not`() {
        val c = Cooldown(60_000L)
        assertTrue(c.tryAcquire(1_000L))
        assertFalse(c.tryAcquire(2_000L))
    }


    @Test fun `it is allowed again once the period has passed, and not a moment before`() {
        val c = Cooldown(60_000L)
        assertTrue(c.tryAcquire(0L))
        assertFalse(c.tryAcquire(59_999L))
        assertTrue(c.tryAcquire(60_000L))
        assertFalse("the clock restarts from the last allowed request", c.tryAcquire(100_000L))
    }
}
