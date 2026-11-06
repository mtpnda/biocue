import Foundation

/// Boundary to the cloud for the WEARER side. Faked in tests.
public protocol WearerPairingStore: Sendable {
    func pairingsOfWearer(_ wearerUid: String) async throws -> [Pairing]
    /// Claims a fresh unique 6-digit code for the wearer and returns it.
    func issueCode(wearerUid: String, expiresAt: Int64) async throws -> String
    /// Deletes every live code of the wearer (lookup record and profile fields).
    func consumeCodes(wearerUid: String) async throws
    func deletePairing(_ pairingId: String) async throws
    func markRemoved(_ pairingId: String, removedAt: Int64) async throws
    func markApproved(_ pairingId: String) async throws
}

public enum CodeResult: Equatable, Sendable {
    case maxCaregiversReached
    case issued(code: String, expiresAt: Int64)
}

public enum ApprovalResult: Equatable, Sendable {
    case approved
    case maxCaregiversReached
}

/// WEARER half of Kotlin `PairingLifecycle` (spec 07). Split from the caregiver half so the role boundary holds.
public struct WearerPairingLifecycle: Sendable {
    public static let maxCaregivers = 2
    public static let codeTtlMs: Int64 = 10 * 60_000

    private let store: WearerPairingStore
    private let nowMs: @Sendable () -> Int64

    public init(store: WearerPairingStore, nowMs: @escaping @Sendable () -> Int64) {
        self.store = store
        self.nowMs = nowMs
    }

    private func approvedCount(_ wearerUid: String) async throws -> Int {
        try await store.pairingsOfWearer(wearerUid).filter { $0.status == .approved }.count
    }

    public func generateCode(wearerUid: String) async throws -> CodeResult {
        if try await approvedCount(wearerUid) >= Self.maxCaregivers { return .maxCaregiversReached }
        let expiresAt = nowMs() + Self.codeTtlMs
        let code = try await store.issueCode(wearerUid: wearerUid, expiresAt: expiresAt)
        return .issued(code: code, expiresAt: expiresAt)
    }

    /// Spec 07 Deny: the pairing document is deleted.
    public func deny(_ pairingId: String) async throws { try await store.deletePairing(pairingId) }

    /// Spec 07 Removal: REMOVED plus the time.
    public func remove(_ pairingId: String) async throws { try await store.markRemoved(pairingId, removedAt: nowMs()) }

    public func approve(wearerUid: String, pairingId: String) async throws -> ApprovalResult {
        if try await approvedCount(wearerUid) >= Self.maxCaregivers { return .maxCaregiversReached }
        try await store.markApproved(pairingId)
        return .approved
    }

    /// Call whenever the wearer's pairings change. Single-use: a code is consumed as soon as a request is pending.
    public func onPairingsChanged(wearerUid: String) async throws {
        if try await store.pairingsOfWearer(wearerUid).contains(where: { $0.status == .pending }) {
            try await store.consumeCodes(wearerUid: wearerUid)
        }
    }
}
