import Foundation

/// The parts of the most recent stress event the pipeline needs.
public struct LatestEvent: Equatable, Sendable {
    public var id: String
    public var startTime: Int64
    public var endTime: Int64?

    public init(id: String, startTime: Int64, endTime: Int64?) {
        self.id = id
        self.startTime = startTime
        self.endTime = endTime
    }
}

public enum PipelineAction: Equatable, Sendable {
    /// Open a new AUTO event (label NONE, unresolved) starting at `startTime`.
    case createAutoEvent(startTime: Int64)
    /// Close the ongoing event.
    case closeEvent(eventId: String, endTime: Int64)
}

public struct PipelineDecision: Equatable, Sendable {
    public var score: Double
    /// 0 calm, 1 early warning, 2 escalation.
    public var tier: Int
    public var actions: [PipelineAction]

    public init(score: Double, tier: Int, actions: [PipelineAction]) {
        self.score = score
        self.tier = tier
        self.actions = actions
    }
}

/// Port of Kotlin `DetectionPipeline`: "what should the wearer's phone do with this window?".
/// Pure function of its inputs — checked against fixtures generated from the Android code.
public enum DetectionPipeline {
    public static let tier1Threshold = 1.5
    public static let tier2Threshold = 2.5
    /// An open event older than this is treated as orphaned, so a new one may start.
    public static let orphanAfterMs: Int64 = 15 * 60 * 1000

    public static func tierFor(_ score: Double) -> Int {
        if score > tier2Threshold { return 2 }
        if score > tier1Threshold { return 1 }
        return 0
    }

    /// 0...100 gauge. Kotlin does `((score / 3.0) * 100).toInt().coerceIn(0, 100)`;
    /// Swift's `Int(Double)` traps on NaN/overflow, so clamp first (same result for every finite input).
    public static func stressPercent(_ score: Double) -> Int {
        let raw = (score / 3.0) * 100.0
        if raw.isNaN { return 0 }
        let clamped = min(100.0, max(0.0, raw))
        return Int(clamped)
    }

    public static func decide(
        window: SensorWindow,
        baseline: Baseline,
        latestEvent: LatestEvent?,
        nowMs: Int64,
        poorSleep: Bool = false,
        scoring: ScoringEngine = ScoringEngine()
    ) -> PipelineDecision {
        let score = scoring.computeRuleBasedScore(window: window, baseline: baseline, poorSleepFlag: poorSleep)
        let tier = tierFor(score)
        var actions: [PipelineAction] = []

        switch tier {
        case 2:
            var ongoing = false
            if let latest = latestEvent, latest.endTime == nil, (nowMs - latest.startTime) < orphanAfterMs {
                ongoing = true
            }
            if !ongoing { actions.append(.createAutoEvent(startTime: window.timestamp)) }
        case 1:
            break
        default:
            if let latest = latestEvent, latest.endTime == nil {
                actions.append(.closeEvent(eventId: latest.id, endTime: window.timestamp))
            }
        }
        return PipelineDecision(score: score, tier: tier, actions: actions)
    }
}
