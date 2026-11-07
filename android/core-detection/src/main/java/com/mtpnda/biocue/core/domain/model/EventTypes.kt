package com.mtpnda.biocue.core.domain.model

object EventTypes {
    const val LABEL_OVERWHELMED = "OVERWHELMED"
    const val LABEL_EXCITED = "EXCITED"
    const val LABEL_ACTIVE = "ACTIVE"
    const val LABEL_NONE = "NONE"
    
    const val CONTEXT_LOUD_NOISES = "Loud Noises"
    const val CONTEXT_CROWDED = "Crowded Space"
    const val CONTEXT_DEMAND = "Task Demand"
    const val CONTEXT_ROUTINE = "Routine Change"
    const val CONTEXT_UNKNOWN = "Unknown Context"
    
    val DEFAULT_LABELS = listOf(LABEL_OVERWHELMED, LABEL_EXCITED, LABEL_ACTIVE)
    val DEFAULT_CONTEXTS = listOf(CONTEXT_LOUD_NOISES, CONTEXT_CROWDED, CONTEXT_DEMAND, CONTEXT_ROUTINE, CONTEXT_UNKNOWN)
    
    fun getIconForLabel(label: String): String {
        if (label.contains(LABEL_OVERWHELMED)) return "🌩️"
        if (label.contains(LABEL_EXCITED)) return "✨"
        if (label.contains(LABEL_ACTIVE)) return "🏃"
        if (label.contains(CONTEXT_LOUD_NOISES)) return "🔊"
        if (label.contains(CONTEXT_CROWDED)) return "👥"
        if (label.contains(CONTEXT_DEMAND)) return "📋"
        if (label.contains(CONTEXT_ROUTINE)) return "🔄"
        if (label == LABEL_NONE || label == CONTEXT_UNKNOWN || label == "Unknown" || label.isEmpty() || label == "overwhelmed") {
            return "🤷"
        }

        val lower = label.lowercase()
        return when {
            lower.contains("hurt") || lower.contains("pain") || lower.contains("harm") || lower.contains("cut") || lower.contains("bleed") -> "🩹"
            lower.contains("sad") || lower.contains("cry") || lower.contains("lonely") || lower.contains("alone") || lower.contains("hopeless") -> "🌧️"
            lower.contains("loud") || lower.contains("noise") || lower.contains("sound") -> "🔊"
            lower.contains("crowd") || lower.contains("people") || lower.contains("party") -> "👥"
            else -> "💭"
        }
    }
    
    fun getColorHexForLabel(label: String): String {
        if (label.contains(LABEL_OVERWHELMED)) return "#a5b4fc"
        if (label.contains(LABEL_EXCITED)) return "#fcd34d"
        if (label.contains(LABEL_ACTIVE)) return "#cbd5e1"
        if (label.contains(CONTEXT_LOUD_NOISES)) return "#fca5a5"
        if (label.contains(CONTEXT_CROWDED)) return "#fdba74"
        if (label.contains(CONTEXT_DEMAND)) return "#6ee7b7"
        if (label.contains(CONTEXT_ROUTINE)) return "#93c5fd"
        if (label == LABEL_NONE || label == CONTEXT_UNKNOWN || label == "Unknown" || label.isEmpty() || label == "overwhelmed") {
            return "#94a3b8"
        }

        val lower = label.lowercase()
        return when {
            lower.contains("hurt") || lower.contains("pain") || lower.contains("harm") || lower.contains("cut") || lower.contains("bleed") -> "#f87171"
            lower.contains("sad") || lower.contains("cry") || lower.contains("lonely") || lower.contains("alone") || lower.contains("hopeless") -> "#93c5fd"
            lower.contains("loud") || lower.contains("noise") || lower.contains("sound") -> "#fca5a5"
            lower.contains("crowd") || lower.contains("people") || lower.contains("party") -> "#fdba74"
            else -> "#c084fc"
        }
    }
}
