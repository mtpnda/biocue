import Foundation

/// Port of Kotlin `EventTypes`. Labels can be compound ("OVERWHELMED - Loud Noises",
/// "EXCITED + ACTIVE"), so matching is by substring and ORDER MATTERS (first match wins).
public enum EventTypes {
    public static let labelOverwhelmed = "OVERWHELMED"
    public static let labelExcited = "EXCITED"
    public static let labelActive = "ACTIVE"
    public static let labelNone = "NONE"

    public static let contextLoudNoises = "Loud Noises"
    public static let contextCrowded = "Crowded Space"
    public static let contextDemand = "Task Demand"
    public static let contextRoutine = "Routine Change"
    public static let contextUnknown = "Unknown Context"

    public static let defaultLabels = [labelOverwhelmed, labelExcited, labelActive]
    public static let defaultContexts = [contextLoudNoises, contextCrowded, contextDemand, contextRoutine, contextUnknown]

    public static func icon(forLabel label: String) -> String {
        if label.contains(labelOverwhelmed) { return "🌩️" }
        if label.contains(labelExcited) { return "✨" }
        if label.contains(labelActive) { return "🏃" }
        if label.contains(contextLoudNoises) { return "🔊" }
        if label.contains(contextCrowded) { return "👥" }
        if label.contains(contextDemand) { return "📋" }
        if label.contains(contextRoutine) { return "🔄" }
        return "🤷"
    }

    public static func colorHex(forLabel label: String) -> String {
        if label.contains(labelOverwhelmed) { return "#a5b4fc" }
        if label.contains(labelExcited) { return "#fcd34d" }
        if label.contains(labelActive) { return "#cbd5e1" }
        if label.contains(contextLoudNoises) { return "#fca5a5" }
        if label.contains(contextCrowded) { return "#fdba74" }
        if label.contains(contextDemand) { return "#6ee7b7" }
        if label.contains(contextRoutine) { return "#93c5fd" }
        return "#94a3b8"
    }
}
