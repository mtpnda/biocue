import Foundation

/// One analysis window from the watch. Mirrors Kotlin `SensorWindow`.
public struct SensorWindow: Equatable, Codable, Sendable {
    public var timestamp: Int64
    public var hrMean: Double
    public var rmssd: Double
    public var accMag: Double
    public var repetitiveness: Double
    public var hrSlope: Double
    public var isOffBody: Bool
    public var isExercising: Bool

    public init(timestamp: Int64, hrMean: Double, rmssd: Double, accMag: Double,
                repetitiveness: Double, hrSlope: Double, isOffBody: Bool, isExercising: Bool) {
        self.timestamp = timestamp
        self.hrMean = hrMean
        self.rmssd = rmssd
        self.accMag = accMag
        self.repetitiveness = repetitiveness
        self.hrSlope = hrSlope
        self.isOffBody = isOffBody
        self.isExercising = isExercising
    }
}

/// Personal baseline the score is measured against. Mirrors Kotlin `Baseline`.
public struct Baseline: Equatable, Sendable {
    public var hourOfDay: Int
    public var hrMean: Double
    public var hrStd: Double
    public var hrvMean: Double
    public var hrvStd: Double

    public init(hourOfDay: Int, hrMean: Double, hrStd: Double, hrvMean: Double, hrvStd: Double) {
        self.hourOfDay = hourOfDay
        self.hrMean = hrMean
        self.hrStd = hrStd
        self.hrvMean = hrvMean
        self.hrvStd = hrvStd
    }

    /// The fixed baseline the Android app currently uses (no per-user calibration yet).
    public static let appDefault = Baseline(hourOfDay: 12, hrMean: 75.0, hrStd: 5.0, hrvMean: 40.0, hrvStd: 8.0)
}

/// A stress event. Mirrors Kotlin `StressEventEntity` and the Firestore `events` document.
public struct StressEvent: Equatable, Codable, Identifiable, Sendable {
    public var id: String
    public var userId: String
    public var startTime: Int64
    public var endTime: Int64?
    /// "AUTO" or "MANUAL"
    public var triggerType: String
    /// "OVERWHELMED", "EXCITED", "ACTIVE", "NONE", or a compound like "OVERWHELMED - Loud Noises"
    public var label: String
    public var isResolved: Bool

    public init(id: String, userId: String, startTime: Int64, endTime: Int64?,
                triggerType: String, label: String, isResolved: Bool) {
        self.id = id
        self.userId = userId
        self.startTime = startTime
        self.endTime = endTime
        self.triggerType = triggerType
        self.label = label
        self.isResolved = isResolved
    }
}

public struct EventCluster: Equatable, Sendable {
    public var startTime: Int64
    public var endTime: Int64?
    public var events: [StressEvent]

    public init(startTime: Int64, endTime: Int64?, events: [StressEvent]) {
        self.startTime = startTime
        self.endTime = endTime
        self.events = events
    }
}
