import Foundation

/// Port of Kotlin `ScoringEngine.computeRuleBasedScore` (Stage 3 "Fusion Score").
/// The arithmetic order is deliberately identical to the Kotlin so results match to the last bit.
public struct ScoringEngine: Sendable {
    public init() {}

    public func computeRuleBasedScore(window: SensorWindow, baseline: Baseline, poorSleepFlag: Bool = false) -> Double {
        if window.isOffBody { return 0.0 }

        let hrZ = (window.hrMean - baseline.hrMean) / max(baseline.hrStd, 1.0)
        // Inverted: an HRV drop is a positive contribution.
        let hrvZ = (baseline.hrvMean - window.rmssd) / max(baseline.hrvStd, 1.0)

        let s = (0.35 * hrZ) + (0.35 * hrvZ) + (0.15 * window.repetitiveness) + (0.15 * window.hrSlope)
        return poorSleepFlag ? s * 1.2 : s
    }
}
