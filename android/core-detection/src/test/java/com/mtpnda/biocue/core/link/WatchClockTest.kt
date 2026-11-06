package com.mtpnda.biocue.core.link

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchClockTest {
    private val twentyMin = 20 * 60_000L

    @Test fun `a watch running twenty minutes behind has its timestamps moved forward`() {
        val clock = WatchClock.fromHello(watchTimeMs = 1_000_000L, phoneNowMs = 1_000_000L + twentyMin)
        assertEquals(500_000L + twentyMin, clock.toPhoneTime(500_000L))
    }


    @Test fun `a watch whose clock is exactly right changes nothing`() {
        val clock = WatchClock.fromHello(watchTimeMs = 42L, phoneNowMs = 42L)
        assertEquals(12_345L, clock.toPhoneTime(12_345L))
    }


    @Test fun `an event that began 14 minutes ago is still ongoing even when the watch clock is 20 minutes slow`() {
        val phoneNow = 10_000_000L
        val watchNow = phoneNow - twentyMin
        val clock = WatchClock.fromHello(watchNow, phoneNow)
        val startedOnWatch = watchNow - 14 * 60_000L                      // what the slow watch wrote
        val hot = com.mtpnda.biocue.core.domain.model.SensorWindow(phoneNow, 95.0, 8.0, 0.1, 0.0, 0.0, false, false)
        val base = com.mtpnda.biocue.core.domain.model.Baseline(12, 75.0, 5.0, 40.0, 8.0)

        val converted = com.mtpnda.biocue.core.domain.LatestEvent("e1", clock.toPhoneTime(startedOnWatch), null)
        val raw = com.mtpnda.biocue.core.domain.LatestEvent("e1", startedOnWatch, null)

        val withClock = com.mtpnda.biocue.core.domain.DetectionPipeline.decide(hot, base, converted, phoneNow)
        val withoutClock = com.mtpnda.biocue.core.domain.DetectionPipeline.decide(hot, base, raw, phoneNow)
        assertEquals("ongoing: no duplicate event", emptyList<com.mtpnda.biocue.core.domain.PipelineAction>(), withClock.actions)
        assertEquals("the old behaviour wrongly started a second event",
            listOf<com.mtpnda.biocue.core.domain.PipelineAction>(com.mtpnda.biocue.core.domain.PipelineAction.CreateAutoEvent(phoneNow)), withoutClock.actions)
    }
}
