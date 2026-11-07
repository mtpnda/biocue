package com.mtpnda.biocue.core.guardian

enum class BreathingRhythm(
    val title: String,
    val inhaleSeconds: Double,
    val holdSeconds: Double,
    val exhaleSeconds: Double
) {
    BOX_BREATHING("4-4-4-4 Box Breathing", 4.0, 4.0, 4.0),
    CALMING_EXHALE("4-7-8 Deep Exhale", 4.0, 7.0, 8.0),
    GENTLE_PACER("4-6 Soothing Pacer", 4.0, 2.0, 6.0)
}

data class CompanionGuidance(
    val phase: Int,
    val headline: String,
    val empatheticMessage: String,
    val suggestedRhythm: BreathingRhythm = BreathingRhythm.GENTLE_PACER,
    val primaryActionTitle: String,
    val isEmergencyEscalation: Boolean = false
)

object CompanionEngine {
    fun synthesize(
        durationMs: Long,
        tier: Int,
        hr: Double,
        hrBaseline: Double,
        activity: String,
        guardianState: GuardianState,
        hourOfDay: Int = 12
    ): CompanionGuidance {
        val minutes = (durationMs / 60000L).toInt()

        if (guardianState is GuardianState.CrisisUnresponsive) {
            return CompanionGuidance(
                phase = 3,
                headline = "🚨 Safety Alert Triggered",
                empatheticMessage = "High stress has persisted for over 15 minutes. We are alerting your circle so someone can be with you.",
                primaryActionTitle = "I'm Safe · Cancel Alert",
                isEmergencyEscalation = true
            )
        }

        if (guardianState is GuardianState.GraceCountdown) {
            return CompanionGuidance(
                phase = 3,
                headline = "⚠️ Checking in on you (${guardianState.remainingSeconds}s)",
                empatheticMessage = "Your body is carrying a very heavy load. Please tap to let me know you're safe.",
                primaryActionTitle = "I Am Safe · Still Coping",
                isEmergencyEscalation = true
            )
        }

        if (tier == 0 && minutes >= 1) {
            return CompanionGuidance(
                phase = 4,
                headline = "Heart rate is finding its calm ✨",
                empatheticMessage = "Your pulse is settling back near baseline. When you're ready, you can end this event or write a short note about what helped.",
                suggestedRhythm = BreathingRhythm.GENTLE_PACER,
                primaryActionTitle = "I'm Feeling Grounded Now"
            )
        }

        if (minutes >= 7) {
            return CompanionGuidance(
                phase = 3,
                headline = "I'm right here with you",
                empatheticMessage = "This wave is taking time to pass, and that's okay. You don't have to face this alone. Are you feeling physically safe right now?",
                suggestedRhythm = BreathingRhythm.BOX_BREATHING,
                primaryActionTitle = "I Am Safe · Just Need Time"
            )
        }

        if (minutes >= 3) {
            val movementHint = when {
                (hourOfDay >= 21 || hourOfDay < 6) && (activity.contains("Resting") || activity.contains("Bed") || activity.contains("Still")) ->
                    "Your body is resting in bed, but your nervous system is hot. You don't have to force sleep or stop thinking right now. Place a cool damp cloth on your forehead, keep your feet outside the blanket, and follow the gentle haptic rhythm."
                activity.contains("Pacing") || activity.contains("Stimming") ->
                    "Keep pacing if it helps your body regulate. Let your breath match the gentle orb below."
                activity.contains("Sitting") ->
                    "Feel the support of the chair beneath you. Soften your jaw and shoulders. You are safe in this moment."
                else -> "Let's take a slow breath together. Match your breath to the rhythm below."
            }

            return CompanionGuidance(
                phase = 2,
                headline = "Breathe through this wave",
                empatheticMessage = movementHint,
                suggestedRhythm = BreathingRhythm.CALMING_EXHALE,
                primaryActionTitle = "I'm Feeling Grounded Now"
            )
        }

        if ((hourOfDay >= 21 || hourOfDay < 6) && (activity.contains("Resting") || activity.contains("Bed") || activity.contains("Still"))) {
            return CompanionGuidance(
                phase = 1,
                headline = "Resting body, racing mind",
                empatheticMessage = "Your body is still, but your nervous system is in high gear. That cranial heat and racing thoughts are real. You are safe in bed; you don't need to force sleep right now.",
                suggestedRhythm = BreathingRhythm.GENTLE_PACER,
                primaryActionTitle = "Resting With It · I'm Safe"
            )
        }

        val timeHint = if (hourOfDay >= 22 || hourOfDay < 5) " The world is quiet right now." else ""
        return CompanionGuidance(
            phase = 1,
            headline = "Sensory spike noted",
            empatheticMessage = "I see your stress is rising.$timeHint Take things slow. You don't need to explain anything right now.",
            suggestedRhythm = BreathingRhythm.GENTLE_PACER,
            primaryActionTitle = "I'm Feeling Grounded Now"
        )
    }
}
