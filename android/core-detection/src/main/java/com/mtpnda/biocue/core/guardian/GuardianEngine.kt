package com.mtpnda.biocue.core.guardian

sealed class GuardianState {
    object Idle : GuardianState()
    data class Monitoring(val durationMs: Long) : GuardianState()
    data class SafetyCheckDue(val durationMs: Long) : GuardianState()
    data class GraceCountdown(val remainingSeconds: Int) : GuardianState()
    data class CrisisUnresponsive(val durationMs: Long) : GuardianState()
    object AcknowledgedSafe : GuardianState()
}

sealed class GuardianAction {
    object None : GuardianAction()
    object PromptSafetyCheck : GuardianAction()
    data class SoundGraceAlarm(val remainingSeconds: Int) : GuardianAction()
    data class TriggerCrisisBroadcast(val reason: String) : GuardianAction()
}

data class GuardianInput(
    val eventStartTime: Long,
    val currentTimeMs: Long,
    val lastWearerInteractionMs: Long,
    val currentTier: Int,
    val hrMean: Double,
    val hrBaseline: Double,
    val isSafeConfirmed: Boolean = false
)

object GuardianEngine {
    const val SAFETY_CHECK_THRESHOLD_MS: Long = 7 * 60 * 1000L       // 7 minutes
    const val GRACE_ALARM_THRESHOLD_MS: Long = 12 * 60 * 1000L      // 12 minutes
    const val CRISIS_ESCALATION_THRESHOLD_MS: Long = 15 * 60 * 1000L // 15 minutes
    const val UNRESPONSIVENESS_WINDOW_MS: Long = 10 * 60 * 1000L    // 10 minutes silence
    const val GRACE_DURATION_SECONDS: Int = 30

    fun evaluate(input: GuardianInput): Pair<GuardianState, GuardianAction> {
        val duration = (input.currentTimeMs - input.eventStartTime).coerceAtLeast(0L)
        val silenceDuration = (input.currentTimeMs - input.lastWearerInteractionMs).coerceAtLeast(0L)

        if (input.isSafeConfirmed) {
            return Pair(GuardianState.AcknowledgedSafe, GuardianAction.None)
        }

        // Beyond 15 min with silence -> crisis broadcast
        if (duration >= CRISIS_ESCALATION_THRESHOLD_MS && silenceDuration >= UNRESPONSIVENESS_WINDOW_MS) {
            return Pair(
                GuardianState.CrisisUnresponsive(duration),
                GuardianAction.TriggerCrisisBroadcast(
                    "Prolonged high stress (${duration / 60000}m) with no response to safety check-ins."
                )
            )
        }

        // Between 12 and 15 minutes with silence and elevated stress -> grace alarm countdown
        val isPhysiologicallyElevated = input.currentTier >= 1 || (input.hrMean > (input.hrBaseline + 15.0))
        if (duration >= GRACE_ALARM_THRESHOLD_MS && silenceDuration >= UNRESPONSIVENESS_WINDOW_MS && isPhysiologicallyElevated) {
            val elapsedSince12m = duration - GRACE_ALARM_THRESHOLD_MS
            val secondsIntoGrace = (elapsedSince12m / 1000L).toInt()
            val remaining = (GRACE_DURATION_SECONDS - (secondsIntoGrace % (GRACE_DURATION_SECONDS + 1))).coerceAtLeast(0)

            if (remaining <= 0) {
                return Pair(
                    GuardianState.CrisisUnresponsive(duration),
                    GuardianAction.TriggerCrisisBroadcast("Wearer unresponsive after 30-second safety grace alarm.")
                )
            }
            return Pair(GuardianState.GraceCountdown(remaining), GuardianAction.SoundGraceAlarm(remaining))
        }

        // Beyond 7 minutes with silence -> safety check-in prompt
        if (duration >= SAFETY_CHECK_THRESHOLD_MS && silenceDuration >= SAFETY_CHECK_THRESHOLD_MS) {
            return Pair(GuardianState.SafetyCheckDue(duration), GuardianAction.PromptSafetyCheck)
        }

        return Pair(GuardianState.Monitoring(duration), GuardianAction.None)
    }
}
