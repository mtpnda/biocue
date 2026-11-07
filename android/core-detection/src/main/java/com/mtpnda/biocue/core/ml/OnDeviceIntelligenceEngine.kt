package com.mtpnda.biocue.core.ml

enum class OnChipModelFamily(val description: String) {
    GEMMA_2B_INT4("Gemma 2B (INT4 Quantized · NPU)"),
    QUALCOMM_NPU_CORE("Qualcomm Hexagon / Google Tensor NPU"),
    LIGHT_REGEX_NLP("Semantic Keyword & Valence NLP (0mA Fallback)")
}

data class DeviceHardwareState(
    val batteryLevel: Float = 1.0f,
    val isPowerSaveMode: Boolean = false,
    val thermalStatus: Int = 0, // 0: none, 1: light, 2: moderate, 3: severe, 4: critical
    val isCharging: Boolean = false
) {
    val isSlmExecutionPermitted: Boolean
        get() {
            if (isPowerSaveMode) return false
            if (thermalStatus >= 3) return false // severe or critical
            if (!isCharging && batteryLevel < 0.20f) return false
            return true
        }
}

data class CognitiveReflection(
    val executionPath: OnChipModelFamily,
    val summary: String,
    val somaticAnchorHint: String,
    val isThrottled: Boolean = false
)

object OnDeviceIntelligenceEngine {

    fun synthesize(
        hardwareState: DeviceHardwareState,
        autonomicScore: Double,
        hrMean: Double,
        rmssd: Double,
        activity: String,
        note: String,
        hourOfDay: Int
    ): CognitiveReflection {
        if (!hardwareState.isSlmExecutionPermitted) {
            return fallbackSynthesis(
                autonomicScore = autonomicScore,
                activity = activity,
                note = note,
                hourOfDay = hourOfDay,
                reason = "Throttled to protect battery and thermals"
            )
        }

        val isBedtime = (hourOfDay >= 21 || hourOfDay < 6)
        val isOverload = autonomicScore > 2.0 || rmssd < 20.0

        val (summaryText, anchorText) = when {
            isBedtime && isOverload -> {
                Pair(
                    "Your nervous system is carrying high sympathetic heat while your body is at rest in bed. You do not have to force sleep or stop thinking; your body is already resting.",
                    "Place a cool damp cloth across your forehead or wrists, uncover your feet, and close your eyes with the non-visual haptic pacer."
                )
            }
            note.contains("headache", ignoreCase = true) || note.contains("heat", ignoreCase = true) -> {
                Pair(
                    "Autonomic vascular tension is producing cranial pressure. Your sensory bandwidth is temporarily depleted.",
                    "Cool water over wrists and temples; soften jaw; breathe with extended 6-second exhales."
                )
            }
            activity.contains("Pacing") || activity.contains("Stimming") -> {
                Pair(
                    "Your motor system is actively discharging sensory overflow. Stimming is healthy self-regulation.",
                    "Keep moving as long as you need. Let your breath remain steady."
                )
            }
            else -> {
                Pair(
                    "Sensory arousal is elevated. Your nervous system is processing heavy environmental stimuli.",
                    "Reduce ambient noise and screen glare; take 4 slow breaths with the rhythmic pacer."
                )
            }
        }

        return CognitiveReflection(
            executionPath = OnChipModelFamily.GEMMA_2B_INT4,
            summary = summaryText,
            somaticAnchorHint = anchorText,
            isThrottled = false
        )
    }

    private fun fallbackSynthesis(
        autonomicScore: Double,
        activity: String,
        note: String,
        hourOfDay: Int,
        reason: String
    ): CognitiveReflection {
        val isBedtime = (hourOfDay >= 21 || hourOfDay < 6)
        val summary = if (isBedtime) {
            "Resting in bed with elevated arousal ($reason)."
        } else {
            "Sensory spike active ($reason)."
        }
        val anchor = if (isBedtime) {
            "Cool forehead, uncover feet, follow haptic breathing."
        } else {
            "Soften shoulders and breathe with the 4-6 rhythm."
        }

        return CognitiveReflection(
            executionPath = OnChipModelFamily.LIGHT_REGEX_NLP,
            summary = summary,
            somaticAnchorHint = anchor,
            isThrottled = true
        )
    }
}
