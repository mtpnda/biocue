package com.mtpnda.biocue.core.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockSkewEstimatorTest {
    private val twentyMin = 20 * 60_000L

    @Test fun `before any telemetry has been seen timestamps are left alone`() {
        assertEquals(12_345L, ClockSkewEstimator().toPhoneTime(12_345L))
    }


    @Test fun `a watch twenty minutes slow is corrected from prompt telemetry`() {
        val e = ClockSkewEstimator()
        e.observe(watchTimestampMs = 1_000_000L, phoneReceiptMs = 1_000_000L + twentyMin + 40)   // arrived 40 ms after it was sent
        assertEquals(500_000L + twentyMin + 40, e.toPhoneTime(500_000L))
    }


    @Test fun `a late delivery does not inflate the estimate because the smallest recent offset wins`() {
        val e = ClockSkewEstimator()
        e.observe(1_000L, 1_000L + twentyMin + 8_000)     // delivered 8 s late
        e.observe(2_000L, 2_000L + twentyMin + 40)        // prompt
        e.observe(3_000L, 3_000L + twentyMin + 180_000)   // delivered 3 minutes late
        assertEquals(10_000L + twentyMin + 40, e.toPhoneTime(10_000L))
    }


    @Test fun `only the most recent samples count so a clock that changes is followed`() {
        val e = ClockSkewEstimator(window = 3)
        repeat(3) { e.observe(it * 1_000L, it * 1_000L) }                          // clock exactly right...
        repeat(3) { e.observe(100_000L + it, 100_000L + it + twentyMin) }          // ...then it jumps 20 minutes behind
        assertEquals(5_000L + twentyMin, e.toPhoneTime(5_000L))
    }
}
