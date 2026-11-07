package com.mtpnda.biocue.core.ml

import org.junit.Assert.*
import org.junit.Test

class OnDeviceIntelligenceEngineTest {

    @Test
    fun testGovernorPermitsSLMWhenBatteryHealthyAndThermallyNominal() {
        val state = DeviceHardwareState(batteryLevel = 0.85f, isPowerSaveMode = false, thermalStatus = 0, isCharging = false)
        assertTrue(state.isSlmExecutionPermitted)

        val reflection = OnDeviceIntelligenceEngine.synthesize(
            hardwareState = state,
            autonomicScore = 2.4,
            hrMean = 95.0,
            rmssd = 18.0,
            activity = "🛏️ Resting in Bed",
            note = "headache and head heat",
            hourOfDay = 23
        )

        assertEquals(OnChipModelFamily.GEMMA_2B_INT4, reflection.executionPath)
        assertFalse(reflection.isThrottled)
        assertTrue(reflection.summary.contains("sympathetic heat"))
        assertTrue(reflection.somaticAnchorHint.contains("cool damp cloth"))
    }

    @Test
    fun testGovernorThrottlesWhenBatteryBelowTwentyPercent() {
        val lowBattery = DeviceHardwareState(batteryLevel = 0.15f, isPowerSaveMode = false, thermalStatus = 0, isCharging = false)
        assertFalse(lowBattery.isSlmExecutionPermitted)

        val reflection = OnDeviceIntelligenceEngine.synthesize(
            hardwareState = lowBattery,
            autonomicScore = 2.1,
            hrMean = 90.0,
            rmssd = 22.0,
            activity = "🛏️ Resting in Bed",
            note = "",
            hourOfDay = 22
        )

        assertEquals(OnChipModelFamily.LIGHT_REGEX_NLP, reflection.executionPath)
        assertTrue(reflection.isThrottled)
        assertTrue(reflection.summary.contains("Throttled"))
    }

    @Test
    fun testGovernorThrottlesWhenInPowerSaveMode() {
        val powerSave = DeviceHardwareState(batteryLevel = 0.80f, isPowerSaveMode = true, thermalStatus = 0, isCharging = false)
        assertFalse(powerSave.isSlmExecutionPermitted)

        val reflection = OnDeviceIntelligenceEngine.synthesize(
            hardwareState = powerSave,
            autonomicScore = 1.8,
            hrMean = 85.0,
            rmssd = 30.0,
            activity = "🪑 Sitting Still",
            note = "",
            hourOfDay = 14
        )

        assertEquals(OnChipModelFamily.LIGHT_REGEX_NLP, reflection.executionPath)
        assertTrue(reflection.isThrottled)
    }

    @Test
    fun testChargingAllowsExecutionEvenAtLowBattery() {
        val charging = DeviceHardwareState(batteryLevel = 0.18f, isPowerSaveMode = false, thermalStatus = 0, isCharging = true)
        assertTrue(charging.isSlmExecutionPermitted)

        val reflection = OnDeviceIntelligenceEngine.synthesize(
            hardwareState = charging,
            autonomicScore = 2.0,
            hrMean = 88.0,
            rmssd = 25.0,
            activity = "🪑 Sitting Still",
            note = "loud noise",
            hourOfDay = 15
        )

        assertEquals(OnChipModelFamily.GEMMA_2B_INT4, reflection.executionPath)
        assertFalse(reflection.isThrottled)
    }
}
