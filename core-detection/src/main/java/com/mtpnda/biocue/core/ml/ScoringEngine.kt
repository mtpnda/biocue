package com.mtpnda.biocue.core.ml

import com.mtpnda.biocue.core.domain.model.Baseline
import com.mtpnda.biocue.core.domain.model.SensorWindow
import kotlin.math.max

class ScoringEngine {
    
    // Stage 3: Rule-based Fusion Score
    fun computeRuleBasedScore(
        window: SensorWindow, 
        baseline: Baseline, 
        poorSleepFlag: Boolean = false
    ): Double {
        if (window.isOffBody || window.isExercising) return 0.0

        val hrZ = (window.hrMean - baseline.hrMean) / max(baseline.hrStd, 1.0)
        // Inverted: HRV drop -> positive score
        val hrvZ = (baseline.hrvMean - window.rmssd) / max(baseline.hrvStd, 1.0) 
        
        // Stage 3 Formula
        val s = (0.35 * hrZ) + (0.35 * hrvZ) + (0.15 * window.repetitiveness) + (0.15 * window.hrSlope)
        
        return if (poorSleepFlag) s * 1.2 else s
    }

    // Logistic Regression Inference (Used once ~30 labeled episodes are met)
    fun computeLogisticRegressionScore(
        window: SensorWindow,
        baseline: Baseline,
        weights: DoubleArray
    ): Double {
        if (window.isOffBody || window.isExercising) return 0.0
        
        val hrZ = (window.hrMean - baseline.hrMean) / max(baseline.hrStd, 1.0)
        val hrvZ = (baseline.hrvMean - window.rmssd) / max(baseline.hrvStd, 1.0)
        
        val z = weights[0] + (weights[1] * hrZ) + (weights[2] * hrvZ) + 
                (weights[3] * window.repetitiveness) + (weights[4] * window.hrSlope)
                
        return 1.0 / (1.0 + Math.exp(-z)) // Sigmoid
    }
}
