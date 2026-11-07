import Foundation

/// Pacing rhythms suggested by the Buddy for nervous system regulation.
public enum BreathingRhythm: String, Sendable {
    case boxBreathing = "4-4-4-4 Box Breathing"
    case calmingExhale = "4-7-8 Deep Exhale"
    case gentlePacer = "4-6 Soothing Pacer"

    public var title: String { rawValue }

    public var inhaleSeconds: Double { 4.0 }
    public var holdSeconds: Double {
        switch self {
        case .boxBreathing: return 4.0
        case .calmingExhale: return 7.0
        case .gentlePacer: return 2.0
        }
    }
    public var exhaleSeconds: Double {
        switch self {
        case .boxBreathing: return 4.0
        case .calmingExhale: return 8.0
        case .gentlePacer: return 6.0
        }
    }
}

/// Dynamic guidance synthesized on-device by BioCue Buddy (Spec 19).
public struct CompanionGuidance: Equatable, Sendable {
    public let phase: Int // 1: silent presence, 2: active co-regulation, 3: safety check, 4: recovery/closure
    public let headline: String
    public let empatheticMessage: String
    public let suggestedRhythm: BreathingRhythm
    public let primaryActionTitle: String
    public let isEmergencyEscalation: Bool

    public init(
        phase: Int,
        headline: String,
        empatheticMessage: String,
        suggestedRhythm: BreathingRhythm = .gentlePacer,
        primaryActionTitle: String,
        isEmergencyEscalation: Bool = false
    ) {
        self.phase = phase
        self.headline = headline
        self.empatheticMessage = empatheticMessage
        self.suggestedRhythm = suggestedRhythm
        self.primaryActionTitle = primaryActionTitle
        self.isEmergencyEscalation = isEmergencyEscalation
    }
}

/// BioCue Buddy Cognitive Guidance Engine:
/// Evaluates autonomic arousal, duration, and user interaction to deliver compassionate,
/// neurodiversity-affirming co-regulation without any cloud telemetry.
public enum CompanionEngine {

    public static func synthesize(
        durationMs: Int64,
        tier: Int,
        hr: Double,
        hrBaseline: Double,
        activity: String,
        guardianState: GuardianState,
        hourOfDay: Int = 12
    ) -> CompanionGuidance {
        let minutes = Int(durationMs / 60000)

        // 1. Guardian Crisis State
        if case .crisisUnresponsive = guardianState {
            return CompanionGuidance(
                phase: 3,
                headline: "🚨 Safety Alert Triggered",
                empatheticMessage: "High stress has persisted for over 15 minutes. We are alerting your circle so someone can be with you.",
                primaryActionTitle: "I'm Safe · Cancel Alert",
                isEmergencyEscalation: true
            )
        }

        if case .graceCountdown(let rem) = guardianState {
            return CompanionGuidance(
                phase: 3,
                headline: "⚠️ Checking in on you (\(rem)s)",
                empatheticMessage: "Your body is carrying a very heavy load. Please tap to let me know you're safe.",
                primaryActionTitle: "I Am Safe · Still Coping",
                isEmergencyEscalation: true
            )
        }

        // 2. Recovery / Return to Calm
        if tier == 0 && minutes >= 1 {
            return CompanionGuidance(
                phase: 4,
                headline: "Heart rate is finding its calm ✨",
                empatheticMessage: "Your pulse is settling back near baseline. When you're ready, you can end this event or write a short note about what helped.",
                suggestedRhythm: .gentlePacer,
                primaryActionTitle: "I'm Feeling Grounded Now"
            )
        }

        // 3. Phase 3: Prolonged Distress (7+ minutes)
        if minutes >= 7 {
            return CompanionGuidance(
                phase: 3,
                headline: "I'm right here with you",
                empatheticMessage: "This wave is taking time to pass, and that's okay. You don't have to face this alone. Are you feeling physically safe right now?",
                suggestedRhythm: .boxBreathing,
                primaryActionTitle: "I Am Safe · Just Need Time"
            )
        }

        // 4. Phase 2: Active Co-Regulation (3 - 7 minutes)
        if minutes >= 3 {
            let movementHint: String
            if (hourOfDay >= 21 || hourOfDay < 6) && (activity.contains("Resting") || activity.contains("Bed") || activity.contains("Still")) {
                movementHint = "Your body is resting in bed, but your nervous system is hot. You don't have to force sleep or stop thinking right now. Place a cool damp cloth on your forehead, keep your feet outside the blanket, and follow the gentle haptic rhythm."
            } else if activity.contains("Pacing") || activity.contains("Stimming") {
                movementHint = "Keep pacing if it helps your body regulate. Let your breath match the gentle orb below."
            } else if activity.contains("Sitting") {
                movementHint = "Feel the support of the chair beneath you. Soften your jaw and shoulders. You are safe in this moment."
            } else {
                movementHint = "Let's take a slow breath together. Match your breath to the rhythm below."
            }

            return CompanionGuidance(
                phase: 2,
                headline: "Breathe through this wave",
                empatheticMessage: movementHint,
                suggestedRhythm: .calmingExhale,
                primaryActionTitle: "I'm Feeling Grounded Now"
            )
        }

        // 5. Phase 1: Silent Presence & Validation (0 - 3 minutes)
        if (hourOfDay >= 21 || hourOfDay < 6) && (activity.contains("Resting") || activity.contains("Bed") || activity.contains("Still")) {
            return CompanionGuidance(
                phase: 1,
                headline: "Resting body, racing mind",
                empatheticMessage: "Your body is still, but your nervous system is in high gear. That cranial heat and racing thoughts are real. You are safe in bed; you don't need to force sleep right now.",
                suggestedRhythm: .gentlePacer,
                primaryActionTitle: "Resting With It · I'm Safe"
            )
        }

        let timeHint = (hourOfDay >= 22 || hourOfDay < 5) ? " The world is quiet right now." : ""
        return CompanionGuidance(
            phase: 1,
            headline: "Sensory spike noted",
            empatheticMessage: "I see your stress is rising.\(timeHint) Take things slow. You don't need to explain anything right now.",
            suggestedRhythm: .gentlePacer,
            primaryActionTitle: "I'm Feeling Grounded Now"
        )
    }
}
