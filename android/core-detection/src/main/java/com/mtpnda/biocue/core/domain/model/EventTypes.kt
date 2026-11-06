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
        return when {
            label.contains(LABEL_OVERWHELMED) -> "🌩️"
            label.contains(LABEL_EXCITED) -> "✨"
            label.contains(LABEL_ACTIVE) -> "🏃"
            label.contains(CONTEXT_LOUD_NOISES) -> "🔊"
            label.contains(CONTEXT_CROWDED) -> "👥"
            label.contains(CONTEXT_DEMAND) -> "📋"
            label.contains(CONTEXT_ROUTINE) -> "🔄"
            else -> "🤷"
        }
    }
    
    fun getColorHexForLabel(label: String): String {
        return when {
            label.contains(LABEL_OVERWHELMED) -> "#a5b4fc"
            label.contains(LABEL_EXCITED) -> "#fcd34d"
            label.contains(LABEL_ACTIVE) -> "#cbd5e1"
            label.contains(CONTEXT_LOUD_NOISES) -> "#fca5a5"
            label.contains(CONTEXT_CROWDED) -> "#fdba74"
            label.contains(CONTEXT_DEMAND) -> "#6ee7b7"
            label.contains(CONTEXT_ROUTINE) -> "#93c5fd"
            else -> "#94a3b8"
        }
    }
}
