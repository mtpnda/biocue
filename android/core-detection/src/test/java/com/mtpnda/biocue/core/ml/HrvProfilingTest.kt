package com.mtpnda.biocue.core.ml

import com.mtpnda.biocue.core.domain.model.Baseline
import com.mtpnda.biocue.core.domain.model.SensorWindow
import org.junit.Assert.*
import org.junit.Test

class HrvProfilingTest {

    @Test
    fun testHrvCalculatorRmssdCalculation() {
        val calculator = HrvCalculator()
        // Sequence of RR intervals in ms:
        // Consecutive differences:
        // 800 -> 820: +20 (diff^2 = 400)
        // 820 -> 790: -30 (diff^2 = 900)
        // 790 -> 810: +20 (diff^2 = 400)
        // 810 -> 800: -10 (diff^2 = 100)
        // Mean squared diff: (400 + 900 + 400 + 100) / 4 = 1800 / 4 = 450
        // RMSSD = sqrt(450) ≈ 21.213 ms
        val rrIntervals = listOf(800.0, 820.0, 790.0, 810.0, 800.0)
        val rmssd = calculator.computeRmssd(rrIntervals)
        assertEquals(21.21, rmssd, 0.05)
    }

    @Test
    fun testHrvCalculatorFiltersOutliers() {
        val calculator = HrvCalculator()
        // Include an impossible physiological artifact (e.g. 200 ms or 2500 ms)
        val rrWithArtifacts = listOf(800.0, 200.0, 820.0, 2500.0, 810.0)
        val rmssd = calculator.computeRmssd(rrWithArtifacts)
        assertTrue("RMSSD should be computed from valid intervals only", rmssd > 0.0)
    }

    @Test
    fun testHrSlopeCalculation() {
        val calculator = HrvCalculator()
        // 10 seconds of HR readings: 70 -> 80 over 10 seconds -> slope = +1.0 bpm/s
        calculator.addHrReading(1000L, 70.0)
        calculator.addHrReading(6000L, 75.0)
        calculator.addHrReading(11000L, 80.0)
        val slope = calculator.computeHrSlope(11000L)
        assertEquals(1.0, slope, 0.1)
    }

    @Test
    fun testBaselineAdaptationOnlyWhenCalm() {
        val profiler = HrvBaselineProfiler(
            initialBaseline = Baseline(hourOfDay = 12, hrMean = 72.0, hrStd = 8.0, hrvMean = 42.0, hrvStd = 10.0)
        )

        // Calm resting window: should adapt baseline
        val calmWindow = SensorWindow(
            timestamp = 1000L,
            hrMean = 68.0,
            rmssd = 45.0,
            accMag = 0.05,
            repetitiveness = 0.0,
            hrSlope = 0.0,
            isOffBody = false,
            isExercising = false
        )
        val accepted = profiler.incorporateWindow(calmWindow)
        assertTrue(accepted)
        assertEquals(1, profiler.profile.calibratedSamples)
        // Baseline hrMean should gently move towards 68.0
        assertTrue(profiler.profile.baseline.hrMean < 72.0)
        // Baseline hrvMean should gently move towards 45.0
        assertTrue(profiler.profile.baseline.hrvMean > 42.0)

        // Exercising window: must NOT corrupt baseline
        val exerciseWindow = SensorWindow(
            timestamp = 2000L,
            hrMean = 140.0,
            rmssd = 12.0,
            accMag = 1.8,
            repetitiveness = 0.8,
            hrSlope = 2.0,
            isOffBody = false,
            isExercising = true
        )
        val rejectedExercise = profiler.incorporateWindow(exerciseWindow)
        assertFalse(rejectedExercise)
        assertEquals(1, profiler.profile.calibratedSamples)

        // Off-body window: must NOT corrupt baseline
        val offBodyWindow = SensorWindow(
            timestamp = 3000L,
            hrMean = 80.0,
            rmssd = 30.0,
            accMag = 0.0,
            repetitiveness = 0.0,
            hrSlope = 0.0,
            isOffBody = true,
            isExercising = false
        )
        val rejectedOffBody = profiler.incorporateWindow(offBodyWindow)
        assertFalse(rejectedOffBody)
        assertEquals(1, profiler.profile.calibratedSamples)
    }

    @Test
    fun testStressLevelPercentageBounds() {
        val profiler = HrvBaselineProfiler()
        // Deep calm: stress should be low (< 25%)
        val calmWindow = SensorWindow(
            timestamp = 1000L, hrMean = 65.0, rmssd = 55.0, accMag = 0.05,
            repetitiveness = 0.0, hrSlope = -0.2, isOffBody = false, isExercising = false
        )
        val calmStress = profiler.computeStressPercent(calmWindow)
        assertTrue("Calm stress should be <= 25%, got $calmStress", calmStress <= 25)

        // High stress: stress should be high (>= 75%)
        val stressWindow = SensorWindow(
            timestamp = 2000L, hrMean = 105.0, rmssd = 15.0, accMag = 0.1,
            repetitiveness = 0.2, hrSlope = 2.0, isOffBody = false, isExercising = false
        )
        val highStress = profiler.computeStressPercent(stressWindow)
        assertTrue("High stress should be >= 75%, got $highStress", highStress >= 75)
    }

    @Test
    fun testProfileJsonSerializationRoundTrip() {
        val profiler = HrvBaselineProfiler(
            initialBaseline = Baseline(hourOfDay = 10, hrMean = 70.5, hrStd = 7.5, hrvMean = 43.0, hrvStd = 9.2)
        )
        val calmWindow = SensorWindow(
            timestamp = 1700000000000L, hrMean = 68.0, rmssd = 46.0, accMag = 0.05,
            repetitiveness = 0.0, hrSlope = 0.0, isOffBody = false, isExercising = false
        )
        profiler.incorporateWindow(calmWindow)

        val json = profiler.serializeToJson()
        assertTrue(json.contains("\"calibratedSamples\":1"))
        assertTrue(json.contains("\"hrMean\":"))

        val restored = HrvBaselineProfiler.deserializeFromJson(json)
        assertNotNull(restored)
        assertEquals(profiler.profile.calibratedSamples, restored!!.profile.calibratedSamples)
        assertEquals(profiler.profile.baseline.hrMean, restored.profile.baseline.hrMean, 0.01)
        assertEquals(profiler.profile.baseline.hrvMean, restored.profile.baseline.hrvMean, 0.01)
    }

    @Test
    fun testDutyCycleEngineStateTransitions() {
        val engine = DutyCycleEngine()
        assertEquals(DutyCycleState.CALM_DUTY, engine.currentState)
        assertTrue(engine.isPpgActive)

        // Watch taken off wrist
        engine.onBodyChanged(isOnBody = false)
        assertEquals(DutyCycleState.OFF_BODY, engine.currentState)
        assertFalse("PPG must be OFF when watch is off-body to save battery", engine.isPpgActive)

        // Watch put back on wrist
        engine.onBodyChanged(isOnBody = true)
        assertEquals(DutyCycleState.CALM_DUTY, engine.currentState)
        assertTrue(engine.isPpgActive)

        // Tier 1 stress detected -> switch immediately to ACTIVE_HIGH continuous sampling
        engine.onStressEvaluated(tier = 1, hrSlope = 1.2, accMag = 0.1)
        assertEquals(DutyCycleState.ACTIVE_HIGH, engine.currentState)
        assertTrue(engine.isPpgActive)

        // Calm confirmed for > 60 seconds
        engine.onCalmDuration(seconds = 65)
        assertEquals(DutyCycleState.CALM_DUTY, engine.currentState)
    }
}
