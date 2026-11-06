import Foundation
import BioCueCore

// CAREGIVER-ONLY. Only ever sees `CaregiverCloud`: it can read a paired wearer's events, write REVIEWS (never touch the
// wearer's events), and leave a pairing. All rules live in the tested BioCueCore types (`CaregiverAccess`,
// `CaregiverPairingLifecycle`, `ReviewSubmission`, `ReviewApplier`); this class drives them and publishes state.

@MainActor
final class CaregiverViewModel: ObservableObject {
    enum PairingState: Equatable {
        case none
        case searching
        case notFound
        case pending(wearerName: String)
        case connected(Pairing)
        /// The wearer removed this caregiver (spec 07).
        case revoked
        case failed(String)
    }

    @Published private(set) var pairingState: PairingState = .none
    @Published private(set) var reviews: [ReviewEvent] = []
    @Published private(set) var journal: [JournalEntry] = []
    @Published private(set) var hasOngoing = false
    @Published private(set) var journalOffset = 0

    private let uid: String
    private let displayName: String
    private let cloud: CaregiverCloud
    private let lifecycle: CaregiverPairingLifecycle
    private let submission: ReviewSubmission
    private let nowMs: @Sendable () -> Int64
    private let calendar: Calendar
    private let defaults: UserDefaults
    private var access: CaregiverAccess
    private var pairingToken: ListenerToken?
    private var eventsToken: ListenerToken?
    private var reviewsToken: ListenerToken?
    private var observedWearerUid: String?
    private var allEvents: [StressEvent] = []
    private var reviewsByEvent: [String: Review] = [:]

    /// Per ACCOUNT: the "dismissed removal" marker is keyed by uid, never shared between accounts on one phone.
    private var ackKey: String { "ack_removal_at_\(uid)" }
    static let purgeFlagKey = "purge_firestore_cache"

    init(uid: String, displayName: String, gate: RoleGate, cloud: CaregiverCloud,
         now: @escaping @Sendable () -> Date = Date.init, calendar: Calendar = .current,
         defaults: UserDefaults = .standard) throws {
        try gate.require(\.requestPairing, action: "request pairing")
        try gate.require(\.readPairedWearerEvents, action: "read a wearer's events")
        self.uid = uid
        self.displayName = displayName
        self.cloud = cloud
        self.calendar = calendar
        self.defaults = defaults
        let ms: @Sendable () -> Int64 = { Int64(now().timeIntervalSince1970 * 1000) }
        self.nowMs = ms
        self.lifecycle = CaregiverPairingLifecycle(store: cloud, nowMs: ms)
        self.submission = ReviewSubmission(store: cloud, nowMs: ms)
        let marker = (defaults.object(forKey: "ack_removal_at_\(uid)") as? NSNumber)?.int64Value ?? 0
        self.access = CaregiverAccess(acknowledgedRemovalAt: marker)
    }

    var wearerName: String? {
        if case .connected(let p) = pairingState { return p.wearerName }
        return nil
    }

    func start() {
        guard pairingToken == nil else { return }
        pairingToken = cloud.observePairings(caregiverUid: uid) { [weak self] result in
            Task { @MainActor in
                guard let self = self else { return }
                switch result {
                case .success(let list): self.apply(self.access.onPairingsObserved(list))
                case .failure(let error): self.pairingState = .failed(error.localizedDescription)
                }
            }
        }
    }

    func stop() { pairingToken = nil; stopObserving() }

    private func apply(_ update: AccessUpdate) {
        switch update.state {
        case .none:
            // never let a snapshot overwrite an in-flight "searching" / "not found" message
            if pairingState != .searching && pairingState != .notFound { pairingState = .none }
            stopObserving()
        case .pending(let name):
            pairingState = .pending(wearerName: name)
            stopObserving()
        case .connected(let p):
            pairingState = .connected(p)
            observe(wearerUid: p.wearerUid, pairingId: p.id)
        case .revoked:
            pairingState = .revoked
            stopObserving()
        }
        if update.effects.contains(.wipeLocalCache) { wipe() }
    }

    /// Spec 07: wipe everything cached about the wearer. In memory immediately; Firestore's on-disk cache can only be
    /// cleared before Firestore starts, so a flag makes the app do it at the next launch (see BioCueApp).
    private func wipe() {
        stopObserving()
        defaults.set(true, forKey: Self.purgeFlagKey)
    }

    private func stopObserving() {
        eventsToken = nil; reviewsToken = nil; observedWearerUid = nil
        allEvents = []; reviewsByEvent = [:]
        recompute()
    }

    private func observe(wearerUid: String, pairingId: String) {
        guard observedWearerUid != wearerUid else { return }
        observedWearerUid = wearerUid
        eventsToken = cloud.observeEvents(wearerUid: wearerUid) { [weak self] result in
            Task { @MainActor in
                guard let self = self, case .success(let list) = result else { return }
                self.allEvents = list
                self.recompute()
            }
        }
        reviewsToken = cloud.observeReviews(pairingId: pairingId) { [weak self] result in
            Task { @MainActor in
                guard let self = self, case .success(let list) = result else { return }
                self.reviewsByEvent = Dictionary(list.map { ($0.eventId, $0) }, uniquingKeysWith: { _, new in new })
                self.recompute()
            }
        }
    }

    // MARK: Pairing actions

    func submitCode(_ raw: String) async {
        let code = raw.trimmingCharacters(in: .whitespaces)
        guard code.count == 6 else { return }
        pairingState = .searching
        do {
            switch try await lifecycle.submitCode(caregiverUid: uid, caregiverName: displayName, code: code) {
            case .notFound: pairingState = .notFound
            case .requested, .alreadyPending: pairingState = .pending(wearerName: "")   // the next snapshot carries the name
            case .alreadyConnected: break                                              // the snapshot will show it
            }
        } catch {
            pairingState = .failed("A network error occurred. Please try again.")
        }
    }

    func acknowledgeRevocation() {
        defaults.set(access.acknowledgeRevocation(), forKey: ackKey)
        pairingState = .none
    }

    func unlink() async {
        guard case .connected(let p) = pairingState else { return }
        do { try await cloud.leavePairing(pairingId: p.id) }
        catch { pairingState = .failed(error.localizedDescription) }
    }

    // MARK: Review + journal

    func resolve(eventId: String, newLabelOrContext: String) async {
        guard case .connected(let pairing) = pairingState,
              let event = effectiveEvents().first(where: { $0.id == eventId }) else { return }
        // Optimistic: show the review at once; the server snapshot confirms it.
        let resolution = ResolveLogic.processResolution(currentLabel: event.label, newLabelOrContext: newLabelOrContext)
        reviewsByEvent[eventId] = Review(pairingId: pairing.id, eventId: eventId, label: resolution.label,
                                         isResolved: resolution.resolved, updatedAt: nowMs())
        recompute()
        do {
            _ = try await submission.submit(pairing: pairing, eventId: eventId, currentLabel: event.label, choice: newLabelOrContext)
        } catch {
            reviewsByEvent[eventId] = nil   // the write failed: fall back to what the server holds
            recompute()
        }
    }

    func changeJournalDate(delta: Int) {
        let next = journalOffset + delta
        guard next <= 0 else { return }
        journalOffset = next
        recompute()
    }

    /// Wearer events with this caregiver's reviews overlaid (a finished review is never overridden).
    private func effectiveEvents() -> [StressEvent] {
        allEvents.map { ReviewApplier.effective($0, reviewsByEvent[$0.id]) }
    }

    private func recompute() {
        let events = effectiveEvents()
        let unresolved = events.filter { !$0.isResolved }.sorted { $0.startTime > $1.startTime }
        reviews = unresolved.map { e in
            ReviewEvent(eventId: e.id, time: TimeFormat.clock(e.startTime), triggerType: e.triggerType, currentLabel: e.label,
                        cues: [("🔄", e.triggerType == "AUTO" ? "Detected Spontaneous Spike" : "Manually Triggered"),
                               ("☁️", "Synced from Wearer")])
        }
        hasOngoing = unresolved.contains { $0.endTime == nil }

        let day = calendar.date(byAdding: .day, value: journalOffset, to: Date(timeIntervalSince1970: TimeInterval(nowMs()) / 1000)) ?? Date()
        let start = Int64(calendar.startOfDay(for: day).timeIntervalSince1970 * 1000)
        let end = start + 86_400_000
        journal = events.filter { $0.isResolved && $0.startTime >= start && $0.startTime < end }
            .sorted { $0.startTime > $1.startTime }
            .map { e in
                JournalEntry(time: TimeFormat.clock(e.startTime),
                             activity: e.triggerType == "MANUAL" ? "Manual Event" : "Detected Event",
                             labelIcon: EventTypes.icon(forLabel: e.label), labelText: e.label,
                             colorHex: EventTypes.colorHex(forLabel: e.label))
            }
    }
}
