import Foundation

/// Who an account is. Chosen once at sign-up and never changes (enforced by Firestore rules).
/// Raw values are the exact strings stored in `users/{uid}.role`.
public enum UserRole: String, Codable, CaseIterable, Sendable {
    case wearer = "WEARER"
    case caregiver = "CAREGIVER"
}

/// What each role is allowed to do. This single table is the source of truth for:
///  - the in-app `RoleGate` (UI + service guards, Android and iOS),
///  - the Firestore security rules (server-side mirror),
///  - the matrix in docs/SPEC.md.
/// If you change it, change all three (RoleCapabilitiesTests pins the table).
public struct RoleCapabilities: Equatable, Sendable {
    /// Open a Bluetooth/Data Layer link to the watch and read raw vitals.
    public let linkWatch: Bool
    /// Create/close events (watch-driven or detected) and write them to the cloud.
    public let createEvents: Bool
    /// Show a pairing code and approve/deny/remove caregivers.
    public let manageCaregivers: Bool
    /// Enter someone else's pairing code and request access.
    public let requestPairing: Bool
    /// Read a paired wearer's events.
    public let readPairedWearerEvents: Bool
    /// Edit label / resolved on a paired wearer's events (and nothing else on them).
    public let labelPairedWearerEvents: Bool

    public init(linkWatch: Bool, createEvents: Bool, manageCaregivers: Bool,
                requestPairing: Bool, readPairedWearerEvents: Bool, labelPairedWearerEvents: Bool) {
        self.linkWatch = linkWatch
        self.createEvents = createEvents
        self.manageCaregivers = manageCaregivers
        self.requestPairing = requestPairing
        self.readPairedWearerEvents = readPairedWearerEvents
        self.labelPairedWearerEvents = labelPairedWearerEvents
    }
}

public extension UserRole {
    var capabilities: RoleCapabilities {
        switch self {
        case .wearer:
            return RoleCapabilities(linkWatch: true, createEvents: true, manageCaregivers: true,
                                    requestPairing: false, readPairedWearerEvents: false, labelPairedWearerEvents: false)
        case .caregiver:
            return RoleCapabilities(linkWatch: false, createEvents: false, manageCaregivers: false,
                                    requestPairing: true, readPairedWearerEvents: true, labelPairedWearerEvents: true)
        }
    }

    /// The role byte a phone sends in the watch-link HELLO.
    var linkRole: LinkRole {
        switch self {
        case .wearer: return .wearer
        case .caregiver: return .caregiver
        }
    }
}

public enum RoleError: Error, Equatable {
    case notPermitted(role: UserRole, action: String)
}

/// Guards a role-restricted action. Features call this at their entry points so that a
/// wrong-role code path fails loudly in development instead of silently doing the wrong thing.
public struct RoleGate: Sendable {
    public let role: UserRole
    public init(role: UserRole) { self.role = role }

    public func require(_ keyPath: KeyPath<RoleCapabilities, Bool>, action: String) throws {
        if role.capabilities[keyPath: keyPath] == false {
            throw RoleError.notPermitted(role: role, action: action)
        }
    }

    public func allows(_ keyPath: KeyPath<RoleCapabilities, Bool>) -> Bool {
        role.capabilities[keyPath: keyPath]
    }
}
