package com.mtpnda.biocue.core

import com.mtpnda.biocue.core.domain.model.Baseline
import com.mtpnda.biocue.core.domain.model.SensorWindow
import com.mtpnda.biocue.core.ml.LogisticRegressionTrainer
import com.mtpnda.biocue.core.ml.ScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionEngineTest {

    private val engine = ScoringEngine()

    @Test
    fun rule_based_score_correctly_identifies_meltdown_trajectory() {
        // Baseline: HR=80, HRV=50
        val baseline = Baseline(hourOfDay = 14, hrMean = 80.0, hrStd = 5.0, hrvMean = 50.0, hrvStd = 10.0)
        
        // Window: HR spiked to 95 (+3 std), HRV dropped to 20 (+3 std inverted), repetitive movement (0.8)
        val window = SensorWindow(
            timestamp = System.currentTimeMillis(),
            hrMean = 95.0, rmssd = 20.0, accMag = 0.5, 
            repetitiveness = 0.8, hrSlope = 2.0, 
            isOffBody = false, isExercising = false
        )

        val score = engine.computeRuleBasedScore(window, baseline, poorSleepFlag = false)
        
        // hrZ = 3.0, hrvZ = 3.0
        // s = (0.35 * 3) + (0.35 * 3) + (0.15 * 0.8) + (0.15 * 2.0) = 1.05 + 1.05 + 0.12 + 0.3 = 2.52
        assertEquals(2.52, score, 0.01)
        assertTrue("Score should trigger Tier 2 (>2.5)", score > 2.5)
    }

    @Test
    fun exercise_gate_suppresses_scoring() {
        val baseline = Baseline(14, 80.0, 5.0, 50.0, 10.0)
        val window = SensorWindow(
            timestamp = System.currentTimeMillis(),
            hrMean = 120.0, rmssd = 10.0, accMag = 1.5,
            repetitiveness = 0.1, hrSlope = 5.0,
            isOffBody = false, isExercising = true // EXERCISING
        )

        val score = engine.computeRuleBasedScore(window, baseline)
        assertEquals(0.0, score, 0.0) // Should be completely suppressed
    }

    @Test
    fun logistic_regression_converges_on_simple_linear_separable_data() {
        val trainer = LogisticRegressionTrainer(learningRate = 0.1, epochs = 2000)
        
        // Features: [hr_z, hrv_z, repetitiveness, hr_slope]
        val features = arrayOf(
            doubleArrayOf(3.0, 3.0, 0.8, 2.0), // Meltdown episode
            doubleArrayOf(0.1, 0.0, 0.1, 0.0)  // Calm episode
        )
        val labels = intArrayOf(1, 0)
        
        val weights = trainer.train(features, labels)
        
        // Test prediction
        val zMeltdown = weights[0] + (weights[1]*3.0) + (weights[2]*3.0) + (weights[3]*0.8) + (weights[4]*2.0)
        val predMeltdown = 1.0 / (1.0 + Math.exp(-zMeltdown))
        
        assertTrue("Model should predict meltdown (>0.9), got $predMeltdown", predMeltdown > 0.9)
    }
}
