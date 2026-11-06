import Foundation

public enum AccessState: Equatable, Sendable {
    case none
    case pending(wearerName: String)
    case connected(Pairing)
    case revoked
}

public enum AccessEffect: Equatable, Sendable {
    case wipeLocalCache
}

public struct AccessUpdate: Equatable, Sendable {
    public let state: AccessState
    public let effects: [AccessEffect]
    public init(state: AccessState, effects: [AccessEffect]) {
        self.state = state
        self.effects = effects
    }
}

/// The caregiver's view of access to ONE wearer. Feed it every snapshot of the caregiver's own pairings.
/// Twin of Kotlin `CaregiverAccess` (spec 07: on removal wipe cached data and show the revoked screen).
public final class CaregiverAccess {
    private var connected: Pairing?
    private var revoked = false
    private var lastRemovalAt: Int64 = 0
    private var acknowledgedRemovalAt: Int64

    /// - Parameter acknowledgedRemovalAt: the value last returned by `acknowledgeRevocation()` and persisted (0 = none).
    public init(acknowledgedRemovalAt: Int64 = 0) {
        self.acknowledgedRemovalAt = acknowledgedRemovalAt
    }

    /// - Returns: the marker to persist so a dismissed removal stays dismissed after a restart.
    @discardableResult
    public func acknowledgeRevocation() -> Int64 {
        revoked = false
        acknowledgedRemovalAt = max(acknowledgedRemovalAt, lastRemovalAt)
        return acknowledgedRemovalAt
    }

    public func onPairingsObserved(_ pairings: [Pairing]) -> AccessUpdate {
        if let approved = pairings.first(where: { $0.status == .approved }) {
            connected = approved
            revoked = false
            return AccessUpdate(state: .connected(approved), effects: [])
        }
        if let pending = pairings.first(where: { $0.status == .pending }), connected == nil {
            return AccessUpdate(state: .pending(wearerName: pending.wearerName), effects: [])
        }
        if revoked { return AccessUpdate(state: .revoked, effects: []) }
        var removal: Int64 = 0
        for p in pairings where p.status == .removed { removal = max(removal, p.removedAt ?? 1) }
        if connected == nil && removal <= acknowledgedRemovalAt { return AccessUpdate(state: .none, effects: []) }
        connected = nil
        revoked = true
        lastRemovalAt = removal
        return AccessUpdate(state: .revoked, effects: [.wipeLocalCache])
    }
}
