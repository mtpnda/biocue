package com.mtpnda.biocue.core

import com.mtpnda.biocue.core.domain.model.Baseline
import com.mtpnda.biocue.core.domain.model.SensorWindow
import com.mtpnda.biocue.core.ml.ScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringEngineEdgeCasesTest {

    private val engine = ScoringEngine()
    private val baseline = Baseline(hourOfDay = 12, hrMean = 70.0, hrStd = 5.0, hrvMean = 50.0, hrvStd = 10.0)

    @Test
    fun offBody_returnsZeroScore() {
        val window = SensorWindow(
            timestamp = System.currentTimeMillis(),
            hrMean = 110.0, rmssd = 15.0, accMag = 0.8,
            repetitiveness = 0.9, hrSlope = 3.0,
            isOffBody = true, // OFF BODY
            isExercising = false
        )

        val score = engine.computeRuleBasedScore(window, baseline)
        assertEquals(0.0, score, 0.0)
    }

    @Test
    fun calmState_returnsLowScore_Tier0() {
        val window = SensorWindow(
            timestamp = System.currentTimeMillis(),
            hrMean = 70.0, rmssd = 50.0, accMag = 0.05,
            repetitiveness = 0.1, hrSlope = 0.0,
            isOffBody = false, isExercising = false
        )

        val score = engine.computeRuleBasedScore(window, baseline)
        assertTrue("Calm score should be < 1.5 (Tier 0)", score < 1.5)
    }

    @Test
    fun moderateStress_returnsTier1Score() {
        val window = SensorWindow(
            timestamp = System.currentTimeMillis(),
            hrMean = 82.0, rmssd = 35.0, accMag = 0.2,
            repetitiveness = 0.4, hrSlope = 1.0,
            isOffBody = false, isExercising = false
        )

        val score = engine.computeRuleBasedScore(window, baseline)
        assertTrue("Moderate stress score should be > 1.0", score > 1.0)
    }
}
