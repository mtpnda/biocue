package com.mtpnda.biocue.mockemitter

import com.mtpnda.biocue.core.domain.model.SensorWindow

enum class ScenarioType(
    val title: String,
    val icon: String,
    val description: String,
    val hrMean: Double,
    val rmssd: Double,
    val accMag: Double,
    val repetitiveness: Double,
    val hrSlope: Double,
    val isOffBody: Boolean,
    val isExercising: Boolean
) {
    CALM(
        title = "Calm & Baseline",
        icon = "🧘",
        description = "Resting vitals, nominal HRV, steady state (Tier 0)",
        hrMean = 70.0,
        rmssd = 55.0,
        accMag = 0.05,
        repetitiveness = 0.02,
        hrSlope = 0.0,
        isOffBody = false,
        isExercising = false
    ),
    MILD_STRESS(
        title = "Mild Stress (Tier 1)",
        icon = "⚡",
        description = "Elevated HR, dropping HRV, positive slope -> Early Cue",
        hrMean = 92.0,
        rmssd = 30.0,
        accMag = 0.15,
        repetitiveness = 0.10,
        hrSlope = 1.2,
        isOffBody = false,
        isExercising = false
    ),
    OVERWHELMED(
        title = "Overwhelmed (Tier 2)",
        icon = "🚨",
        description = "Tachycardia, suppressed HRV, high stimming -> Tier 2 Meltdown Alert",
        hrMean = 128.0,
        rmssd = 16.0,
        accMag = 0.45,
        repetitiveness = 0.65,
        hrSlope = 2.8,
        isOffBody = false,
        isExercising = false
    ),
    EXERCISING(
        title = "Physical Exercise",
        icon = "🏃",
        description = "High HR + high motion; marked as exercising to suppress false alerts",
        hrMean = 140.0,
        rmssd = 22.0,
        accMag = 1.85,
        repetitiveness = 0.85,
        hrSlope = 1.5,
        isOffBody = false,
        isExercising = true
    ),
    OFF_BODY(
        title = "Watch Off Wrist",
        icon = "📴",
        description = "Watch removed from wrist, vitals nullified",
        hrMean = 0.0,
        rmssd = 0.0,
        accMag = 0.01,
        repetitiveness = 0.0,
        hrSlope = 0.0,
        isOffBody = true,
        isExercising = false
    ),
    CUSTOM(
        title = "Custom Manual",
        icon = "🎛️",
        description = "Fine-tuned parameters using manual sliders",
        hrMean = 75.0,
        rmssd = 40.0,
        accMag = 0.1,
        repetitiveness = 0.0,
        hrSlope = 0.0,
        isOffBody = false,
        isExercising = false
    );

    fun toSensorWindow(jitter: Boolean = false): SensorWindow {
        val hrJitter = if (jitter && !isOffBody) (Math.random() * 2.0 - 1.0) else 0.0
        val rmssJitter = if (jitter && !isOffBody) (Math.random() * 1.5 - 0.75) else 0.0
        return SensorWindow(
            timestamp = System.currentTimeMillis(),
            hrMean = (hrMean + hrJitter).coerceAtLeast(0.0),
            rmssd = (rmssd + rmssJitter).coerceAtLeast(0.0),
            accMag = accMag,
            repetitiveness = repetitiveness,
            hrSlope = hrSlope,
            isOffBody = isOffBody,
            isExercising = isExercising
        )
    }
}
