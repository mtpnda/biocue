package com.mtpnda.biocue.core.domain

import com.mtpnda.biocue.core.domain.model.Baseline
import com.mtpnda.biocue.core.domain.model.SensorWindow
import org.junit.Assert.assertEquals
import org.junit.Test

class DetectionPipelineTest {
    private val baseline = Baseline(12, 75.0, 5.0, 40.0, 8.0)
    private val now = 1_700_000_000_000L

    private fun window(hr: Double = 75.0, rmssd: Double = 40.0, rep: Double = 0.0, slope: Double = 0.0, off: Boolean = false) =
        SensorWindow(now, hr, rmssd, 0.1, rep, slope, off, false)

    // hrZ=4, hrvZ=4 -> 0.35*4 + 0.35*4 = 2.8  (> 2.5)
    private val tier2 = window(hr = 95.0, rmssd = 8.0)
    // hrZ=2, hrvZ=2.5 -> 0.7 + 0.875 = 1.575 (between 1.5 and 2.5)
    private val tier1 = window(hr = 85.0, rmssd = 20.0)

    @Test fun `tier thresholds`() {
        assertEquals(0, DetectionPipeline.tierFor(1.5))   // strictly greater than
        assertEquals(1, DetectionPipeline.tierFor(1.5001))
        assertEquals(1, DetectionPipeline.tierFor(2.5))
        assertEquals(2, DetectionPipeline.tierFor(2.5001))
        assertEquals(0, DetectionPipeline.tierFor(-3.0))
    }

    @Test fun `stress percent is clamped to 0 to 100`() {
        assertEquals(0, DetectionPipeline.stressPercent(-5.0))
        assertEquals(50, DetectionPipeline.stressPercent(1.5))
        assertEquals(100, DetectionPipeline.stressPercent(99.0))
        assertEquals(0, DetectionPipeline.stressPercent(Double.NaN))
    }

    @Test fun `tier 2 with no event opens an AUTO event`() {
        val d = DetectionPipeline.decide(tier2, baseline, null, now)
        assertEquals(2, d.tier)
        assertEquals(listOf<PipelineAction>(PipelineAction.CreateAutoEvent(now)), d.actions)
    }

    @Test fun `tier 2 does not duplicate an ongoing event`() {
        val ongoing = LatestEvent("e1", now - 60_000, null)
        assertEquals(emptyList<PipelineAction>(), DetectionPipeline.decide(tier2, baseline, ongoing, now).actions)
    }

    @Test fun `tier 2 replaces an orphaned event older than 15 minutes`() {
        val orphan = LatestEvent("e1", now - 15 * 60_000, null)   // exactly 15 min: no longer ongoing
        assertEquals(listOf<PipelineAction>(PipelineAction.CreateAutoEvent(now)),
            DetectionPipeline.decide(tier2, baseline, orphan, now).actions)
        val justUnder = LatestEvent("e1", now - 15 * 60_000 + 1, null)
        assertEquals(emptyList<PipelineAction>(), DetectionPipeline.decide(tier2, baseline, justUnder, now).actions)
    }

    @Test fun `tier 2 after a closed event starts a new one`() {
        val closed = LatestEvent("e1", now - 1000, now - 500)
        assertEquals(listOf<PipelineAction>(PipelineAction.CreateAutoEvent(now)),
            DetectionPipeline.decide(tier2, baseline, closed, now).actions)
    }

    @Test fun `tier 1 never changes events`() {
        val d = DetectionPipeline.decide(tier1, baseline, LatestEvent("e1", now - 1000, null), now)
        assertEquals(1, d.tier); assertEquals(emptyList<PipelineAction>(), d.actions)
    }

    @Test fun `calm closes the ongoing event at the window timestamp`() {
        val d = DetectionPipeline.decide(window(), baseline, LatestEvent("e1", now - 600_000, null), now)
        assertEquals(0, d.tier)
        assertEquals(listOf<PipelineAction>(PipelineAction.CloseEvent("e1", now)), d.actions)
    }

    @Test fun `calm with nothing ongoing does nothing`() {
        assertEquals(emptyList<PipelineAction>(), DetectionPipeline.decide(window(), baseline, null, now).actions)
        assertEquals(emptyList<PipelineAction>(),
            DetectionPipeline.decide(window(), baseline, LatestEvent("e1", 1, 2), now).actions)
    }

    @Test fun `off body is always calm even with extreme vitals`() {
        val d = DetectionPipeline.decide(window(hr = 200.0, rmssd = 1.0, off = true), baseline, null, now)
        assertEquals(0.0, d.score, 0.0); assertEquals(0, d.tier)
    }

    @Test fun `poor sleep multiplies the score by 1_2`() {
        val a = DetectionPipeline.decide(tier1, baseline, null, now, poorSleep = false).score
        val b = DetectionPipeline.decide(tier1, baseline, null, now, poorSleep = true).score
        assertEquals(a * 1.2, b, 1e-12)
    }
}
