import Foundation

/// Boundary: where reviews are written (Firestore). Faked in tests.
public protocol ReviewStore: Sendable {
    func writeReview(_ review: Review) async throws
}

public enum ReviewResult: Equatable, Sendable {
    case submitted
    case notAllowed
}

/// CAREGIVER side: a review is only allowed on an APPROVED pairing and follows the two-stage rules in `ResolveLogic`.
public struct ReviewSubmission: Sendable {
    private let store: ReviewStore
    private let nowMs: @Sendable () -> Int64

    public init(store: ReviewStore, nowMs: @escaping @Sendable () -> Int64) {
        self.store = store
        self.nowMs = nowMs
    }

    public func submit(pairing: Pairing, eventId: String, currentLabel: String, choice: String) async throws -> ReviewResult {
        if pairing.status != .approved { return .notAllowed }
        let resolution = ResolveLogic.processResolution(currentLabel: currentLabel, newLabelOrContext: choice)
        try await store.writeReview(Review(pairingId: pairing.id, eventId: eventId, label: resolution.label,
                                           isResolved: resolution.resolved, updatedAt: nowMs()))
        return .submitted
    }
}

/// Applies a caregiver's review to a wearer event. First resolution wins (spec 07).
public enum ReviewApplier {
    /// - Returns: the updated event, or nil when the review must be ignored.
    public static func apply(_ event: StressEvent, _ review: Review) -> StressEvent? {
        if event.isResolved { return nil }
        var updated = event
        updated.label = review.label
        updated.isResolved = review.isResolved
        return updated
    }

    /// What a caregiver's journal should display: the event with their own review overlaid, if it still applies.
    public static func effective(_ event: StressEvent, _ review: Review?) -> StressEvent {
        guard let review = review, let applied = apply(event, review) else { return event }
        return applied
    }
}
