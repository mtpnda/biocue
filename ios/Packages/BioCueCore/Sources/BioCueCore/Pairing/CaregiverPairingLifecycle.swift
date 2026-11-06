import Foundation

/// Boundary to the cloud for the CAREGIVER side. Faked in tests.
public protocol CaregiverPairingStore: Sendable {
    /// Direct lookup by code (never a list/query).
    func lookupCode(_ code: String) async throws -> CodeRecord?
    func pairing(_ pairingId: String) async throws -> Pairing?
    /// Contract of the real boundary: fails if the document already exists (the rules forbid overwriting).
    func createPairing(_ pairing: Pairing) async throws
    /// REMOVED -> PENDING on the same document; clears removedAt.
    func reRequest(_ pairingId: String, caregiverName: String, createdAt: Int64) async throws
}

public enum SubmitResult: Equatable, Sendable {
    case notFound
    case requested
    case alreadyPending
    case alreadyConnected
}

/// CAREGIVER half of Kotlin `PairingLifecycle.submitCode`.
public struct CaregiverPairingLifecycle: Sendable {
    private let store: CaregiverPairingStore
    private let nowMs: @Sendable () -> Int64

    public init(store: CaregiverPairingStore, nowMs: @escaping @Sendable () -> Int64) {
        self.store = store
        self.nowMs = nowMs
    }

    public func submitCode(caregiverUid: String, caregiverName: String, code: String) async throws -> SubmitResult {
        guard let record = try await store.lookupCode(code) else { return .notFound }
        if record.expiresAt <= nowMs() { return .notFound }
        let id = "\(record.wearerUid)_\(caregiverUid)"
        if let existing = try await store.pairing(id) {
            switch existing.status {
            case .removed:
                try await store.reRequest(id, caregiverName: caregiverName, createdAt: nowMs())
                return .requested
            case .pending: return .alreadyPending
            case .approved: return .alreadyConnected
            }
        }
        try await store.createPairing(Pairing(id: id, wearerUid: record.wearerUid, caregiverUid: caregiverUid,
                                              caregiverName: caregiverName, wearerName: record.wearerName,
                                              status: .pending, createdAt: nowMs()))
        return .requested
    }
}
