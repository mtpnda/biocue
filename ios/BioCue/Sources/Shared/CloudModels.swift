import Foundation
import BioCueCore

/// Mirrors Kotlin `UserProfile` / Firestore `users/{uid}`.
struct UserProfile: Equatable {
    var uid: String
    var role: UserRole
    var displayName: String
    var pairingCode: String?
    var pairingCodeExpiresAt: Int64?
}

// `Pairing` and `PairingStatus` now live in BioCueCore (with `CodeRecord` and `Review`) so the lifecycle logic that
// uses them is pure and unit-tested.

/// What the UI shows for the watch connection (same three states as Android `ConnectionStatus`).
enum ConnectionStatus: Equatable {
    case searching, connected, disconnected
}

struct JournalEntry: Equatable, Identifiable {
    var id: String { time + "|" + labelText + "|" + activity }
    let time: String
    let activity: String
    let labelIcon: String
    let labelText: String
    let colorHex: String
}

struct ReviewEvent: Equatable, Identifiable {
    var id: String { eventId }
    let eventId: String
    let time: String
    let triggerType: String
    let currentLabel: String
    let cues: [(icon: String, text: String)]

    static func == (l: ReviewEvent, r: ReviewEvent) -> Bool {
        l.eventId == r.eventId && l.time == r.time && l.triggerType == r.triggerType && l.currentLabel == r.currentLabel
            && l.cues.map { $0.icon + $0.text } == r.cues.map { $0.icon + $0.text }
    }
}

enum CloudError: Error, LocalizedError {
    case couldNotAllocateCode
    case notSignedIn
    var errorDescription: String? {
        switch self {
        case .couldNotAllocateCode: return "Could not create a pairing code. Please try again."
        case .notSignedIn: return "You are signed out."
        }
    }
}

/// Cancels a Firestore/Auth listener when released.
final class ListenerToken {
    private var handler: (() -> Void)?
    init(_ cancel: @escaping () -> Void) { handler = cancel }
    func cancel() { handler?(); handler = nil }
    deinit { cancel() }
}

// MARK: - Role-split cloud interfaces
//
// The protocols below are the iOS counterpart of "wearer and caregiver are separate". A feature is only ever handed the
// protocol for ITS role, so caregiver code has no method to create an event or approve a pairing, and wearer code has no
// method to read someone else's events. (The Firestore rules enforce the same split on the server.)

/// Needed by both roles: reading/creating the signed-in user's own profile.
protocol ProfileCloud {
    func observeUserProfile(uid: String, onChange: @escaping (Result<UserProfile?, Error>) -> Void) -> ListenerToken
    func createUserProfile(_ profile: UserProfile) async throws
}

/// WEARER only. Pairing operations come from `WearerPairingStore` (BioCueCore), which the tested
/// `WearerPairingLifecycle` drives; this adds events and listeners.
protocol WearerCloud: WearerPairingStore {
    func syncEvent(wearerUid: String, event: StressEvent) async throws
    func observeCaregivers(wearerUid: String, onChange: @escaping (Result<[Pairing], Error>) -> Void) -> ListenerToken
    /// Reviews a paired caregiver wrote for this wearer's events (spec 07).
    func observeReviews(pairingId: String, onChange: @escaping (Result<[Review], Error>) -> Void) -> ListenerToken
}

/// CAREGIVER only. Lookup / request / re-request come from `CaregiverPairingStore`; reviews are written through
/// `ReviewStore`. A caregiver never writes to the wearer's events (spec 07).
protocol CaregiverCloud: CaregiverPairingStore, ReviewStore {
    /// All of this caregiver's pairings (any status) so a removal is actually observed.
    func observePairings(caregiverUid: String, onChange: @escaping (Result<[Pairing], Error>) -> Void) -> ListenerToken
    func observeEvents(wearerUid: String, onChange: @escaping (Result<[StressEvent], Error>) -> Void) -> ListenerToken
    func observeReviews(pairingId: String, onChange: @escaping (Result<[Review], Error>) -> Void) -> ListenerToken
    /// Caregiver leaves: sets their OWN pairing to REMOVED (+ removedAt).
    func leavePairing(pairingId: String) async throws
}
