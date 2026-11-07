package com.mtpnda.biocue.core.ml

/**
 * Duty cycle states that control hardware sensor activation to conserve battery:
 * - OFF_BODY: Watch is off wrist. PPG and Accelerometer listeners are completely paused. Zero wasted battery.
 * - CALM_DUTY: Wearer is calm and resting. Duty-cycle PPG (30s sampling / 90s sleep) to save ~75% optical power.
 * - ACTIVE_HIGH: Elevated stress, high acceleration, or rapid HR rise. 100% continuous sampling.
 */
enum class DutyCycleState {
    OFF_BODY,
    CALM_DUTY,
    ACTIVE_HIGH
}

class DutyCycleEngine(
    val calmActiveSeconds: Int = 30,
    val calmSleepSeconds: Int = 90,
    val deescalationCalmSeconds: Int = 60
) {
    var currentState: DutyCycleState = DutyCycleState.CALM_DUTY
        private set

    /** True when optical PPG callbacks should be registered with the OS. */
    var isPpgActive: Boolean = true
        private set

    private var isOnBody: Boolean = true
    private var calmSecondsAccumulated: Int = 0

    /**
     * Hardware off-body event from Android TYPE_LOW_LATENCY_OFFBODY_DETECT.
     */
    fun onBodyChanged(isOnBody: Boolean) {
        this.isOnBody = isOnBody
        if (!isOnBody) {
            currentState = DutyCycleState.OFF_BODY
            isPpgActive = false
        } else {
            currentState = DutyCycleState.CALM_DUTY
            isPpgActive = true
            calmSecondsAccumulated = 0
        }
    }

    /**
     * Evaluates stress and motion metrics. If stress or agitation is elevated,
     * immediately escalate to ACTIVE_HIGH continuous sampling.
     */
    fun onStressEvaluated(tier: Int, hrSlope: Double, accMag: Double) {
        if (!isOnBody) return

        if (tier >= 1 || hrSlope > 0.8 || accMag > 0.8) {
            currentState = DutyCycleState.ACTIVE_HIGH
            isPpgActive = true
            calmSecondsAccumulated = 0
        } else if (currentState == DutyCycleState.ACTIVE_HIGH) {
            // In ACTIVE_HIGH waiting for sustained calm before returning to duty-cycle
            calmSecondsAccumulated += 5
            if (calmSecondsAccumulated >= deescalationCalmSeconds) {
                currentState = DutyCycleState.CALM_DUTY
                isPpgActive = true
                calmSecondsAccumulated = 0
            }
        }
    }

    /**
     * Called when confirmed calm has been maintained.
     */
    fun onCalmDuration(seconds: Int) {
        if (!isOnBody) return
        calmSecondsAccumulated += seconds
        if (currentState == DutyCycleState.ACTIVE_HIGH && calmSecondsAccumulated >= deescalationCalmSeconds) {
            currentState = DutyCycleState.CALM_DUTY
            isPpgActive = true
        }
    }

    /**
     * Toggle PPG state during CALM_DUTY cycles.
     */
    fun setPpgSamplingActive(active: Boolean) {
        if (currentState == DutyCycleState.OFF_BODY) {
            isPpgActive = false
        } else if (currentState == DutyCycleState.ACTIVE_HIGH) {
            isPpgActive = true
        } else {
            isPpgActive = active
        }
    }
}
