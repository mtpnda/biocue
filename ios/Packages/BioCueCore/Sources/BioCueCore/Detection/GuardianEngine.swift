import Foundation

/// State of the Guardian Protocol monitoring an ongoing stress episode (Spec 19).
public enum GuardianState: Equatable, Sendable {
    /// No ongoing stress event or wearer is fully calm.
    case idle
    /// Active event, physiological tracking is underway, wearer is safe or recently interacted.
    case monitoring(durationMs: Int64)
    /// Event duration >= 7 minutes without wearer interaction: gentle safety check-in prompt.
    case safetyCheckDue(durationMs: Int64)
    /// Event duration >= 12 minutes, stress remains elevated, wearer unresponsive: 30s grace countdown.
    case graceCountdown(remainingSeconds: Int)
    /// Unresponsive crisis confirmed: emergency broadcast dispatched with GPS to caregivers.
    case crisisUnresponsive(durationMs: Int64)
    /// Wearer explicitly confirmed they are safe / coping.
    case acknowledgedSafe
}

/// Action recommendation emitted by GuardianEngine to the host service.
public enum GuardianAction: Equatable, Sendable {
    case none
    case promptSafetyCheck
    case soundGraceAlarm(remainingSeconds: Int)
    case triggerCrisisBroadcast(reason: String)
}

/// Input snapshot for evaluating Guardian Protocol state.
public struct GuardianInput: Sendable {
    public var eventStartTime: Int64
    public var currentTimeMs: Int64
    public var lastWearerInteractionMs: Int64
    public var currentTier: Int
    public var hrMean: Double
    public var hrBaseline: Double
    public var isSafeConfirmed: Bool

    public init(
        eventStartTime: Int64,
        currentTimeMs: Int64,
        lastWearerInteractionMs: Int64,
        currentTier: Int,
        hrMean: Double,
        hrBaseline: Double,
        isSafeConfirmed: Bool = false
    ) {
        self.eventStartTime = eventStartTime
        self.currentTimeMs = currentTimeMs
        self.lastWearerInteractionMs = lastWearerInteractionMs
        self.currentTier = currentTier
        self.hrMean = hrMean
        self.hrBaseline = hrBaseline
        self.isSafeConfirmed = isSafeConfirmed
    }
}

/// The Guardian Engine: prevents the "Silent Agony" blindspot where prolonged unmonitored stress
/// leads to crisis, incapacitation, or self-harm without anyone being informed.
public enum GuardianEngine {
    public static let safetyCheckThresholdMs: Int64 = 7 * 60 * 1000       // 7 minutes
    public static let graceAlarmThresholdMs: Int64 = 12 * 60 * 1000      // 12 minutes
    public static let crisisEscalationThresholdMs: Int64 = 15 * 60 * 1000 // 15 minutes
    public static let unresponsivenessWindowMs: Int64 = 10 * 60 * 1000    // 10 min without interaction
    public static let graceDurationSeconds: Int = 30

    /// Evaluates the current situation and returns the active Guardian state and action.
    public static func evaluate(input: GuardianInput) -> (state: GuardianState, action: GuardianAction) {
        let duration = max(0, input.currentTimeMs - input.eventStartTime)
        let silenceDuration = max(0, input.currentTimeMs - input.lastWearerInteractionMs)

        // 1. If wearer already confirmed safe during this ongoing episode, keep quiet monitoring
        if input.isSafeConfirmed {
            return (.acknowledgedSafe, .none)
        }

        // 2. Beyond 15 minutes with silence and elevated stress -> full crisis
        if duration >= crisisEscalationThresholdMs && silenceDuration >= unresponsivenessWindowMs {
            return (
                .crisisUnresponsive(durationMs: duration),
                .triggerCrisisBroadcast(reason: "Prolonged high stress (\(duration / 60000)m) with no response to safety check-ins.")
            )
        }

        // 3. Between 12 and 15 minutes with elevated stress and silence -> grace countdown alarm
        let isPhysiologicallyElevated = input.currentTier >= 1 || (input.hrMean > (input.hrBaseline + 15.0))
        if duration >= graceAlarmThresholdMs && silenceDuration >= unresponsivenessWindowMs && isPhysiologicallyElevated {
            let elapsedSince12m = duration - graceAlarmThresholdMs
            let secondsIntoGrace = Int(elapsedSince12m / 1000)
            let remaining = max(0, graceDurationSeconds - (secondsIntoGrace % (graceDurationSeconds + 1)))

            if remaining <= 0 {
                return (
                    .crisisUnresponsive(durationMs: duration),
                    .triggerCrisisBroadcast(reason: "Wearer unresponsive after 30-second safety grace alarm.")
                )
            }
            return (.graceCountdown(remainingSeconds: remaining), .soundGraceAlarm(remainingSeconds: remaining))
        }

        // 4. Beyond 7 minutes with silence -> safety check-in due
        if duration >= safetyCheckThresholdMs && silenceDuration >= safetyCheckThresholdMs {
            return (.safetyCheckDue(durationMs: duration), .promptSafetyCheck)
        }

        // 5. Normal active monitoring
        return (.monitoring(durationMs: duration), .none)
    }
}
