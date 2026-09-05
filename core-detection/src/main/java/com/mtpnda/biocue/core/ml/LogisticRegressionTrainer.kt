package com.mtpnda.biocue.core.ml

/**
 * Lightweight Gradient Descent Trainer for on-device learning.
 * Runs entirely locally to ensure privacy (No Cloud).
 */
class LogisticRegressionTrainer(
    private val learningRate: Double = 0.01,
    private val epochs: Int = 1000
) {
    // Features: [bias, hr_z, hrv_z, repetitiveness, hr_slope]
    fun train(features: Array<DoubleArray>, labels: IntArray): DoubleArray {
        val weights = DoubleArray(5) { 0.0 } // Init with zeros
        
        for (epoch in 0 until epochs) {
            for (i in features.indices) {
                val x = features[i]
                val y = labels[i]
                
                // Prediction (Sigmoid)
                var z = weights[0]
                for (j in 1..4) z += weights[j] * x[j - 1]
                val yPred = 1.0 / (1.0 + Math.exp(-z))
                
                // Error gradient
                val error = yPred - y
                
                // Update weights
                weights[0] -= learningRate * error // Bias
                for (j in 1..4) {
                    weights[j] -= learningRate * error * x[j - 1]
                }
            }
        }
        return weights
    }
}
