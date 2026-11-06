import Foundation

/// Stored as the enum NAME in Firestore (same strings as Android).
public enum PairingStatus: String, Equatable, Sendable {
    case pending = "PENDING"
    case approved = "APPROVED"
    case removed = "REMOVED"
}

/// `pairings/{wearerUid}_{caregiverUid}` — twin of Kotlin `core.pairing.Pairing`.
public struct Pairing: Equatable, Identifiable, Sendable {
    public var id: String
    public var wearerUid: String
    public var caregiverUid: String
    public var caregiverName: String
    public var wearerName: String
    public var status: PairingStatus
    public var createdAt: Int64
    /// Set when the pairing is ended (spec 07).
    public var removedAt: Int64?

    public init(id: String, wearerUid: String, caregiverUid: String, caregiverName: String, wearerName: String,
                status: PairingStatus, createdAt: Int64, removedAt: Int64? = nil) {
        self.id = id
        self.wearerUid = wearerUid
        self.caregiverUid = caregiverUid
        self.caregiverName = caregiverName
        self.wearerName = wearerName
        self.status = status
        self.createdAt = createdAt
        self.removedAt = removedAt
    }
}

/// `active_codes/{code}` plus the wearer's display name.
public struct CodeRecord: Equatable, Sendable {
    public var wearerUid: String
    public var wearerName: String
    public var expiresAt: Int64
    public init(wearerUid: String, wearerName: String, expiresAt: Int64) {
        self.wearerUid = wearerUid
        self.wearerName = wearerName
        self.expiresAt = expiresAt
    }
}

/// A caregiver's review of one wearer event: `pairings/{pairingId}/reviews/{eventId}` (spec 07).
public struct Review: Equatable, Sendable {
    public var pairingId: String
    public var eventId: String
    public var label: String
    public var isResolved: Bool
    public var updatedAt: Int64
    public init(pairingId: String, eventId: String, label: String, isResolved: Bool, updatedAt: Int64) {
        self.pairingId = pairingId
        self.eventId = eventId
        self.label = label
        self.isResolved = isResolved
        self.updatedAt = updatedAt
    }
}
