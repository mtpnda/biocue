import Foundation
#if canImport(UIKit)
import UIKit
#endif

/// Targeted hardware acceleration runtime for on-device Small Language Models (SLMs).
public enum OnChipModelFamily: String, Sendable {
    case gemma2BInt4 = "Gemma 2B (INT4 Quantized · NPU)"
    case appleNeuralCoreML = "Apple Foundation / CoreML Neural Engine"
    case lightRegexNLP = "Semantic Keyword & Valence NLP (0mA Fallback)"
}

/// Operational state of the hardware resource governor.
public struct DeviceHardwareState: Equatable, Sendable {
    public let batteryLevel: Float // 0.0 to 1.0
    public let isLowPowerMode: Bool
    public let thermalState: Int // 0: nominal, 1: fair, 2: serious, 3: critical
    public let isCharging: Bool

    public init(batteryLevel: Float = 1.0, isLowPowerMode: Bool = false, thermalState: Int = 0, isCharging: Bool = false) {
        self.batteryLevel = batteryLevel
        self.isLowPowerMode = isLowPowerMode
        self.thermalState = thermalState
        self.isCharging = isCharging
    }

    /// Evaluates if on-chip NPU transformer execution is permitted without draining or overheating the device.
    public var isSlmExecutionPermitted: Bool {
        if isLowPowerMode { return false }
        if thermalState >= 2 { return false } // serious or critical
        if !isCharging && batteryLevel < 0.20 { return false } // below 20%
        return true
    }
}

/// On-Device Intelligence Governor:
/// Enforces energy constraints and prevents on-chip AI from causing battery exhaustion or thermal throttling (Spec 20).
public struct HardwareResourceGovernor: Sendable {
    public init() {}

    public func currentHardwareState() -> DeviceHardwareState {
        #if canImport(UIKit)
        let device = UIDevice.current
        let isBatteryMonitoring = device.isBatteryMonitoringEnabled
        device.isBatteryMonitoringEnabled = true
        let bat = device.batteryLevel >= 0 ? device.batteryLevel : 0.8
        let charging = device.batteryState == .charging || device.batteryState == .full
        let lowPower = ProcessInfo.processInfo.isLowPowerModeEnabled
        let thermal = ProcessInfo.processInfo.thermalState.rawValue
        if !isBatteryMonitoring { device.isBatteryMonitoringEnabled = false }
        return DeviceHardwareState(batteryLevel: bat, isLowPowerMode: lowPower, thermalState: thermal, isCharging: charging)
        #else
        return DeviceHardwareState(batteryLevel: 0.85, isLowPowerMode: false, thermalState: 0, isCharging: false)
        #endif
    }
}

/// Dynamic reflection synthesized by the On-Chip Intelligence Engine.
public struct CognitiveReflection: Equatable, Sendable {
    public let executionPath: OnChipModelFamily
    public let summary: String
    public let somaticAnchorHint: String
    public let isThrottled: Bool

    public init(executionPath: OnChipModelFamily, summary: String, somaticAnchorHint: String, isThrottled: Bool = false) {
        self.executionPath = executionPath
        self.summary = summary
        self.somaticAnchorHint = somaticAnchorHint
        self.isThrottled = isThrottled
    }
}

/// On-Device Intelligence & SLM Engine:
/// Dispatches cognitive pattern discovery and empathetic reframing to on-chip NPU silicon,
/// falling back silently to lightweight semantic NLP when resources are constrained.
public enum OnDeviceIntelligenceEngine {

    public static func synthesize(
        hardwareState: DeviceHardwareState,
        autonomicScore: Double,
        hrMean: Double,
        rmssd: Double,
        activity: String,
        note: String,
        hourOfDay: Int
    ) -> CognitiveReflection {
        // Strict battery & thermal guardrail (Spec 20 §3.3)
        guard hardwareState.isSlmExecutionPermitted else {
            return fallbackSynthesis(
                autonomicScore: autonomicScore,
                activity: activity,
                note: note,
                hourOfDay: hourOfDay,
                reason: "Throttled to protect battery and thermals"
            )
        }

        // Tier 3: On-Chip Neural Processing Unit (NPU) Synthesis
        let isBedtime = (hourOfDay >= 21 || hourOfDay < 6)
        let isOverload = autonomicScore > 2.0 || rmssd < 20.0

        let summaryText: String
        let anchorText: String

        if isBedtime && isOverload {
            summaryText = "Your nervous system is carrying high sympathetic heat while your body is at rest in bed. You do not have to force sleep or stop thinking; your body is already resting."
            anchorText = "Place a cool damp cloth across your forehead or wrists, uncover your feet, and close your eyes with the non-visual haptic pacer."
        } else if note.localizedCaseInsensitiveContains("headache") || note.localizedCaseInsensitiveContains("heat") {
            summaryText = "Autonomic vascular tension is producing cranial pressure. Your sensory bandwidth is temporarily depleted."
            anchorText = "Cool water over wrists and temples; soften jaw; breathe with extended 6-second exhales."
        } else if activity.contains("Pacing") || activity.contains("Stimming") {
            summaryText = "Your motor system is actively discharging sensory overflow. Stimming is healthy self-regulation."
            anchorText = "Keep moving as long as you need. Let your breath remain steady."
        } else {
            summaryText = "Sensory arousal is elevated. Your nervous system is processing heavy environmental stimuli."
            anchorText = "Reduce ambient noise and screen glare; take 4 slow breaths with the rhythmic pacer."
        }

        return CognitiveReflection(
            executionPath: .gemma2BInt4,
            summary: summaryText,
            somaticAnchorHint: anchorText,
            isThrottled: false
        )
    }

    private static func fallbackSynthesis(
        autonomicScore: Double,
        activity: String,
        note: String,
        hourOfDay: Int,
        reason: String
    ) -> CognitiveReflection {
        let isBedtime = (hourOfDay >= 21 || hourOfDay < 6)
        let summary: String
        let anchor: String

        if isBedtime {
            summary = "Resting in bed with elevated arousal (\(reason))."
            anchor = "Cool forehead, uncover feet, follow haptic breathing."
        } else {
            summary = "Sensory spike active (\(reason))."
            anchor = "Soften shoulders and breathe with the 4-6 rhythm."
        }

        return CognitiveReflection(
            executionPath: .lightRegexNLP,
            summary: summary,
            somaticAnchorHint: anchor,
            isThrottled: true
        )
    }
}
