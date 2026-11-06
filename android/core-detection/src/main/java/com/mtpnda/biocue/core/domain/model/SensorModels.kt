package com.mtpnda.biocue.core.domain.model

data class SensorWindow(
    val timestamp: Long,
    val hrMean: Double,
    val rmssd: Double, // HRV
    val accMag: Double,
    val repetitiveness: Double, // 0.5-4 Hz band power
    val hrSlope: Double,
    val isOffBody: Boolean,
    val isExercising: Boolean
)

data class Baseline(
    val hourOfDay: Int,
    val hrMean: Double,
    val hrStd: Double,
    val hrvMean: Double,
    val hrvStd: Double
)

data class ScoringResult(
    val timestamp: Long,
    val rawScore: Double,
    val adjustedScore: Double,
    val isTier1Alert: Boolean,
    val isTier2Alert: Boolean
)

/**
 * Single source of truth for the Wear <-> Phone communication schema.
 * Ensures that if a key changes, it breaks both sides at compile time rather than silently at runtime.
 */
object DataLayerSchema {
    const val PATH_SENSOR_WINDOW = "/sensor_window"
    const val PATH_EVENT_START = "/event_start"
    const val PATH_EVENT_LABEL = "/event_label"
    const val PATH_EVENT_END = "/event_end"
    
    // Window keys
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_HR_MEAN = "hrMean"
    const val KEY_RMSSD = "rmssd"
    const val KEY_ACC_MAG = "accMag"
    const val KEY_REPETITIVENESS = "repetitiveness"
    const val KEY_HR_SLOPE = "hrSlope"
    const val KEY_IS_OFF_BODY = "isOffBody"
    const val KEY_IS_EXERCISING = "isExercising"
    
    // Event keys
    const val KEY_EVENT_ID = "eventId"
    const val KEY_EVENT_TRIGGER_TYPE = "triggerType" // "MANUAL" or "AUTO"
    const val KEY_EVENT_LABEL = "label"
}
