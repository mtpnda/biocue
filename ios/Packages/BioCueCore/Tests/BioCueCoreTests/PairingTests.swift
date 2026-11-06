import XCTest
@testable import BioCueCore

// Mirrors core-detection/.../pairing/*Test.kt (same scenarios, same numbers). WRITTEN, NOT EXECUTED (no Swift toolchain
// was available) — run `swift test`. The Kotlin originals were written test-first and pass.

final class FakeClock: @unchecked Sendable {
    var now: Int64
    init(_ now: Int64 = 1_000_000) { self.now = now }
}

actor MemoryPairingStore: WearerPairingStore, CaregiverPairingStore {
    private var pairings: [String: Pairing] = [:]
    private var order: [String] = []
    private var codes: [String: CodeRecord] = [:]

    func put(_ p: Pairing) { if pairings[p.id] == nil { order.append(p.id) }; pairings[p.id] = p }
    func plantCode(_ code: String, wearerUid: String, expiresAt: Int64) { codes[code] = CodeRecord(wearerUid: wearerUid, wearerName: "Wearer", expiresAt: expiresAt) }
    func isCodeLive(_ code: String) -> Bool { codes[code] != nil }
    func all() -> [Pairing] { order.compactMap { pairings[$0] } }

    // WearerPairingStore
    func pairingsOfWearer(_ wearerUid: String) -> [Pairing] { all().filter { $0.wearerUid == wearerUid } }
    func issueCode(wearerUid: String, expiresAt: Int64) -> String { codes["123456"] = CodeRecord(wearerUid: wearerUid, wearerName: "Wearer", expiresAt: expiresAt); return "123456" }
    func consumeCodes(wearerUid: String) { codes = codes.filter { $0.value.wearerUid != wearerUid } }
    func deletePairing(_ pairingId: String) { pairings[pairingId] = nil; order.removeAll { $0 == pairingId } }
    func markRemoved(_ pairingId: String, removedAt: Int64) { pairings[pairingId]?.status = .removed; pairings[pairingId]?.removedAt = removedAt }
    func markApproved(_ pairingId: String) { pairings[pairingId]?.status = .approved }

    // CaregiverPairingStore
    func lookupCode(_ code: String) -> CodeRecord? { codes[code] }
    func pairing(_ pairingId: String) -> Pairing? { pairings[pairingId] }
    /// The real boundary refuses to overwrite (a caregiver `set` on an existing document is an update the rules deny).
    func createPairing(_ pairing: Pairing) { precondition(pairings[pairing.id] == nil, "create on existing document \(pairing.id)"); put(pairing) }
    func reRequest(_ pairingId: String, caregiverName: String, createdAt: Int64) {
        pairings[pairingId]?.status = .pending; pairings[pairingId]?.removedAt = nil
        pairings[pairingId]?.caregiverName = caregiverName; pairings[pairingId]?.createdAt = createdAt
    }
}

func pairing(_ caregiver: String, _ status: PairingStatus, wearer: String = "w", removedAt: Int64? = nil) -> Pairing {
    Pairing(id: "\(wearer)_\(caregiver)", wearerUid: wearer, caregiverUid: caregiver, caregiverName: "Care \(caregiver)",
            wearerName: "Wearer", status: status, createdAt: 1, removedAt: removedAt)
}

final class PairingLifecycleTests: XCTestCase {
    private let store = MemoryPairingStore()
    private let clock = FakeClock()
    private var wearer: WearerPairingLifecycle { WearerPairingLifecycle(store: store, nowMs: { [clock] in clock.now }) }
    private var caregiver: CaregiverPairingLifecycle { CaregiverPairingLifecycle(store: store, nowMs: { [clock] in clock.now }) }

    func testAWearerWithTwoApprovedCaregiversCannotGenerateAPairingCode() async throws {
        await store.put(pairing("c1", .approved)); await store.put(pairing("c2", .approved))
        let result = try await wearer.generateCode(wearerUid: "w")
        XCTAssertEqual(result, .maxCaregiversReached)
    }

    func testBelowTheLimitAWearerGetsACodeThatIsValidForTenMinutes() async throws {
        await store.put(pairing("c1", .approved))
        let result = try await wearer.generateCode(wearerUid: "w")
        XCTAssertEqual(result, .issued(code: "123456", expiresAt: clock.now + 10 * 60_000))
    }

    func testTheCodeIsConsumedAsSoonAsTheFirstRequestArrives() async throws {
        _ = try await wearer.generateCode(wearerUid: "w")
        let before = await store.isCodeLive("123456")
        XCTAssertTrue(before)
        await store.put(pairing("c1", .pending))
        try await wearer.onPairingsChanged(wearerUid: "w")
        let after = await store.isCodeLive("123456")
        XCTAssertFalse(after)
    }

    func testTheCodeStaysValidWhileNoRequestIsPending() async throws {
        _ = try await wearer.generateCode(wearerUid: "w")
        await store.put(pairing("c1", .approved))          // an existing caregiver is not a new request
        try await wearer.onPairingsChanged(wearerUid: "w")
        let live = await store.isCodeLive("123456")
        XCTAssertTrue(live)
    }

    func testDenyingARequestDeletesThePairingDocument() async throws {
        await store.put(pairing("c1", .pending))
        try await wearer.deny("w_c1")
        let left = await store.pairingsOfWearer("w")
        XCTAssertEqual(left, [])
    }

    func testRemovingACaregiverMarksThePairingRemovedWithTheTimeOfRemoval() async throws {
        await store.put(pairing("c1", .approved))
        clock.now = 5_000_000
        try await wearer.remove("w_c1")
        let left = await store.pairingsOfWearer("w")
        XCTAssertEqual(left, [pairing("c1", .removed, removedAt: 5_000_000)])
    }

    func testApprovingARequestGivesTheCaregiverAccess() async throws {
        await store.put(pairing("c1", .pending))
        let result = try await wearer.approve(wearerUid: "w", pairingId: "w_c1")
        XCTAssertEqual(result, .approved)
        let status = await store.pairingsOfWearer("w").first?.status
        XCTAssertEqual(status, .approved)
    }

    func testAThirdCaregiverCannotBeApprovedAndNothingChanges() async throws {
        await store.put(pairing("c1", .approved)); await store.put(pairing("c2", .approved)); await store.put(pairing("c3", .pending))
        let result = try await wearer.approve(wearerUid: "w", pairingId: "w_c3")
        XCTAssertEqual(result, .maxCaregiversReached)
        let c3 = await store.pairing("w_c3")
        XCTAssertEqual(c3?.status, .pending)
    }

    func testACodeNobodyIssuedIsNotFound() async throws {
        let result = try await caregiver.submitCode(caregiverUid: "c1", caregiverName: "Care c1", code: "999999")
        XCTAssertEqual(result, .notFound)
    }

    func testAnExpiredCodeIsNotFound() async throws {
        await store.plantCode("111111", wearerUid: "w", expiresAt: clock.now - 1)
        let result = try await caregiver.submitCode(caregiverUid: "c1", caregiverName: "Care c1", code: "111111")
        XCTAssertEqual(result, .notFound)
    }

    func testAValidCodeCreatesAPendingRequestCarryingBothNames() async throws {
        await store.plantCode("111111", wearerUid: "w", expiresAt: clock.now + 1000)
        let result = try await caregiver.submitCode(caregiverUid: "c1", caregiverName: "Maya", code: "111111")
        XCTAssertEqual(result, .requested)
        let left = await store.pairingsOfWearer("w")
        XCTAssertEqual(left, [Pairing(id: "w_c1", wearerUid: "w", caregiverUid: "c1", caregiverName: "Maya", wearerName: "Wearer", status: .pending, createdAt: clock.now)])
    }

    func testARemovedCaregiverCanRequestAgainOnTheSamePairingDocument() async throws {
        await store.put(pairing("c1", .removed, removedAt: 123))
        await store.plantCode("111111", wearerUid: "w", expiresAt: 99_000_000)
        clock.now = 9_000_000
        let result = try await caregiver.submitCode(caregiverUid: "c1", caregiverName: "Maya", code: "111111")
        XCTAssertEqual(result, .requested)
        let only = await store.pairingsOfWearer("w")
        XCTAssertEqual(only.count, 1)
        XCTAssertEqual(only[0].id, "w_c1"); XCTAssertEqual(only[0].status, .pending)
        XCTAssertNil(only[0].removedAt); XCTAssertEqual(only[0].createdAt, 9_000_000)
    }

    func testAskingAgainWhileARequestIsPendingChangesNothing() async throws {
        await store.put(pairing("c1", .pending))
        await store.plantCode("111111", wearerUid: "w", expiresAt: 99_000_000)
        clock.now = 9_000_000
        let result = try await caregiver.submitCode(caregiverUid: "c1", caregiverName: "Maya", code: "111111")
        XCTAssertEqual(result, .alreadyPending)
        let createdAt = await store.pairingsOfWearer("w").first?.createdAt
        XCTAssertEqual(createdAt, 1)
    }

    func testAskingWhenAlreadyConnectedReportsConnectedAndChangesNothing() async throws {
        await store.put(pairing("c1", .approved))
        await store.plantCode("111111", wearerUid: "w", expiresAt: 99_000_000)
        let result = try await caregiver.submitCode(caregiverUid: "c1", caregiverName: "Maya", code: "111111")
        XCTAssertEqual(result, .alreadyConnected)
        let status = await store.pairingsOfWearer("w").first?.status
        XCTAssertEqual(status, .approved)
    }
}

final class CaregiverAccessTests: XCTestCase {
    private let access = CaregiverAccess()

    func testWhenTheWearerRemovesTheCaregiverTheCacheIsWipedAndAccessIsRevoked() {
        _ = access.onPairingsObserved([pairing("c1", .approved)])
        let update = access.onPairingsObserved([pairing("c1", .removed, removedAt: 5)])
        XCTAssertEqual(update.state, .revoked)
        XCTAssertEqual(update.effects, [.wipeLocalCache])
    }

    func testACaregiverWhoNeverHadAccessHasNothingRevokedAndNothingWiped() {
        let update = access.onPairingsObserved([])
        XCTAssertEqual(update.state, .none)
        XCTAssertEqual(update.effects, [])
    }

    func testAPendingRequestIsReportedAsWaitingForTheWearer() {
        let update = access.onPairingsObserved([pairing("c1", .pending)])
        XCTAssertEqual(update.state, .pending(wearerName: "Wearer"))
        XCTAssertEqual(update.effects, [])
    }

    func testIfTheApprovedPairingSimplyDisappearsAccessIsRevokedAndTheCacheWiped() {
        _ = access.onPairingsObserved([pairing("c1", .approved)])
        let update = access.onPairingsObserved([])
        XCTAssertEqual(update.state, .revoked)
        XCTAssertEqual(update.effects, [.wipeLocalCache])
    }

    func testTheSameRemovalSeenTwiceWipesOnlyOnce() {
        _ = access.onPairingsObserved([pairing("c1", .approved)])
        let removed = [pairing("c1", .removed, removedAt: 5)]
        _ = access.onPairingsObserved(removed)
        let again = access.onPairingsObserved(removed)
        XCTAssertEqual(again.state, .revoked)
        XCTAssertEqual(again.effects, [])
    }

    func testAfterTheCaregiverAcknowledgesTheRevocationTheScreenReturnsToNeutral() {
        _ = access.onPairingsObserved([pairing("c1", .approved)])
        let removed = [pairing("c1", .removed, removedAt: 5)]
        _ = access.onPairingsObserved(removed)
        access.acknowledgeRevocation()
        let after = access.onPairingsObserved(removed)
        XCTAssertEqual(after.state, .none)
        XCTAssertEqual(after.effects, [])
    }

    func testOpeningTheAppAfterARemovalStillRevokesAndWipesTheCache() {
        let update = CaregiverAccess().onPairingsObserved([pairing("c1", .removed, removedAt: 5)])
        XCTAssertEqual(update.state, .revoked)
        XCTAssertEqual(update.effects, [.wipeLocalCache])
    }

    func testADismissedRemovalDoesNotComeBackAfterTheAppRestarts() {
        let removed = [pairing("c1", .removed, removedAt: 5)]
        let first = CaregiverAccess()
        _ = first.onPairingsObserved(removed)
        let persisted = first.acknowledgeRevocation()
        let afterRestart = CaregiverAccess(acknowledgedRemovalAt: persisted).onPairingsObserved(removed)
        XCTAssertEqual(afterRestart.state, .none)
        XCTAssertEqual(afterRestart.effects, [])
    }
}

actor RecordingReviewStore: ReviewStore {
    private(set) var written: [Review] = []
    func writeReview(_ review: Review) { written.append(review) }
}

final class ReviewTests: XCTestCase {
    private let store = RecordingReviewStore()
    private var submission: ReviewSubmission { ReviewSubmission(store: store, nowMs: { 7_000 }) }

    func testACaregiverWhosePairingIsNotApprovedCannotSubmitAReview() async throws {
        let result = try await submission.submit(pairing: pairing("c1", .pending), eventId: "e1", currentLabel: "NONE", choice: "OVERWHELMED")
        XCTAssertEqual(result, .notAllowed)
        let written = await store.written
        XCTAssertEqual(written, [])
    }

    func testChoosingOverwhelmedForAnUnlabelledEventRecordsItButKeepsTheReviewOpenForContext() async throws {
        let result = try await submission.submit(pairing: pairing("c1", .approved), eventId: "e1", currentLabel: "NONE", choice: "OVERWHELMED")
        XCTAssertEqual(result, .submitted)
        let written = await store.written
        XCTAssertEqual(written, [Review(pairingId: "w_c1", eventId: "e1", label: "OVERWHELMED", isResolved: false, updatedAt: 7_000)])
    }

    func testAddingAContextToAnOverwhelmedEventResolvesItWithACombinedLabel() async throws {
        _ = try await submission.submit(pairing: pairing("c1", .approved), eventId: "e1", currentLabel: "OVERWHELMED", choice: "Loud Noises")
        let written = await store.written
        XCTAssertEqual(written, [Review(pairingId: "w_c1", eventId: "e1", label: "OVERWHELMED - Loud Noises", isResolved: true, updatedAt: 7_000)])
    }

    private func event(_ label: String = "NONE", resolved: Bool = false) -> StressEvent {
        StressEvent(id: "e1", userId: "w", startTime: 100, endTime: 200, triggerType: "AUTO", label: label, isResolved: resolved)
    }
    private func review(_ label: String, _ resolved: Bool) -> Review { Review(pairingId: "w_c1", eventId: "e1", label: label, isResolved: resolved, updatedAt: 5) }

    func testAReviewIsAppliedToAnUnresolvedEvent() {
        XCTAssertEqual(ReviewApplier.apply(event(), review("OVERWHELMED - Loud Noises", true)), event("OVERWHELMED - Loud Noises", resolved: true))
    }

    func testAReviewForAnEventThatIsAlreadyResolvedIsIgnoredTheFirstResolutionWins() {
        XCTAssertNil(ReviewApplier.apply(event("EXCITED", resolved: true), review("OVERWHELMED", false)))
    }

    func testTheCaregiversJournalShowsAReviewOnAnOpenEventButNeverOverridesAResolvedOne() {
        let open = event(), done = event("EXCITED", resolved: true)
        XCTAssertEqual(ReviewApplier.effective(open, review("OVERWHELMED", false)), event("OVERWHELMED", resolved: false))
        XCTAssertEqual(ReviewApplier.effective(done, review("OVERWHELMED", false)), done)
        XCTAssertEqual(ReviewApplier.effective(open, nil), open)
    }
}
