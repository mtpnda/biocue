import XCTest
import BioCueCore
@testable import BioCue

@MainActor
final class WearerServiceTests: XCTestCase {
    private let uid = "wearer-1"
    private let t0 = Date(timeIntervalSince1970: 1_700_000_000)

    private func make() throws -> (WearerService, FakeLink, MemoryEventStore, MemoryWindowStore, FakeWearerCloud, FakeNotifier) {
        let link = FakeLink(), events = MemoryEventStore(), windows = MemoryWindowStore()
        let cloud = FakeWearerCloud(), notifier = FakeNotifier()
        let service = try WearerService(userId: uid, gate: RoleGate(role: .wearer), link: link, events: events,
                                        windows: windows, cloud: cloud, notifier: notifier, now: { self.t0 })
        return (service, link, events, windows, cloud, notifier)
    }

    private func frame(_ seq: Int, _ m: LinkMessage) -> DecodedFrame { DecodedFrame(seq: seq, message: m) }
    private var ms: Int64 { Int64(t0.timeIntervalSince1970 * 1000) }
    private func hot() -> LinkMessage { .sensorWindow(timestamp: ms, hrMean: 95, rmssd: 8, accMag: 0.1, repetitiveness: 0, hrSlope: 0, isOffBody: false, isExercising: false) }
    private func calm() -> LinkMessage { .sensorWindow(timestamp: ms, hrMean: 72, rmssd: 42, accMag: 0.1, repetitiveness: 0, hrSlope: 0, isOffBody: false, isExercising: false) }

    // MARK: roles

    func testCaregiverCannotBuildAnyWearerComponent() {
        let gate = RoleGate(role: .caregiver)
        XCTAssertThrowsError(try WatchLinkCentral(gate: gate, accountId: "c"))
        XCTAssertThrowsError(try WearerCircleViewModel(uid: "c", gate: gate, cloud: FakeWearerCloud()))
        XCTAssertThrowsError(try WearerService(userId: "c", gate: gate, link: FakeLink(), events: MemoryEventStore(),
                                               windows: MemoryWindowStore(), cloud: FakeWearerCloud(), notifier: FakeNotifier()))
    }

    func testWearerCannotBuildCaregiverViewModel() {
        XCTAssertThrowsError(try CaregiverViewModel(uid: "w", displayName: "W", gate: RoleGate(role: .wearer), cloud: FakeCaregiverCloud()))
    }

    // MARK: detection

    func testHotWindowOpensOneAutoEventAndSyncsIt() async throws {
        let (service, _, events, windows, cloud, notifier) = try make()
        await service.handle(frame(1, hot()))
        await service.handle(frame(2, hot()))   // still ongoing: must not open a second event
        let all = await events.all()
        XCTAssertEqual(all.count, 1)
        XCTAssertEqual(all.first?.triggerType, "AUTO")
        XCTAssertNil(all.first?.endTime)
        let storedWindows = await windows.count()
        XCTAssertEqual(storedWindows, 2)
        XCTAssertEqual(service.tier, 2)
        XCTAssertEqual(notifier.tier2Count, 1, "alert cool-down must prevent a notification per window")
        try await Task.sleep(nanoseconds: 200_000_000)
        XCTAssertGreaterThanOrEqual(cloud.synced.count, 1)
    }

    func testCalmWindowClosesTheOngoingEvent() async throws {
        let (service, _, events, _, _, _) = try make()
        await service.handle(frame(1, hot()))
        await service.handle(frame(2, calm()))
        let all = await events.all()
        XCTAssertEqual(all.first?.endTime, ms)
    }

    func testWindowsAreNeverAcknowledged() async throws {
        let (service, link, _, _, _, _) = try make()
        await service.handle(frame(1, hot()))
        XCTAssertEqual(link.acked, [])
    }

    // MARK: critical frames

    func testCriticalFramesAreStoredThenAcknowledged() async throws {
        let (service, link, events, _, _, _) = try make()
        await service.handle(frame(10, .eventStart(timestamp: ms, eventId: "e1", isManual: true)))
        let stored = await events.event(id: "e1")
        XCTAssertNotNil(stored)
        XCTAssertEqual(link.acked, [10])
        await service.handle(frame(11, .eventLabel(timestamp: ms, eventId: "e1", label: "OVERWHELMED")))
        await service.handle(frame(12, .eventEnd(timestamp: ms + 5000, eventId: "e1")))
        let done = await events.event(id: "e1")
        XCTAssertEqual(done?.label, "OVERWHELMED")
        XCTAssertEqual(done?.endTime, ms + 5000)
        XCTAssertEqual(link.acked, [10, 11, 12])
    }

    func testRetransmittedFramesAreAckedButDoNotCorruptState() async throws {
        let (service, link, events, _, _, _) = try make()
        await service.handle(frame(1, .eventStart(timestamp: ms, eventId: "e1", isManual: true)))
        await service.handle(frame(2, .eventLabel(timestamp: ms, eventId: "e1", label: "EXCITED")))
        await service.handle(frame(3, .eventEnd(timestamp: ms + 1, eventId: "e1")))
        await service.resolveReview(eventId: "e1", newLabelOrContext: "Loud Noises")  // wearer finishes the review
        // the watch never got the ACKs and replays everything after a reconnect:
        await service.handle(frame(1, .eventStart(timestamp: ms, eventId: "e1", isManual: true)))
        await service.handle(frame(2, .eventLabel(timestamp: ms, eventId: "e1", label: "OVERWHELMED")))
        await service.handle(frame(3, .eventEnd(timestamp: ms + 999, eventId: "e1")))
        let e = await events.event(id: "e1")
        XCTAssertEqual(e?.label, "EXCITED - Loud Noises", "a stale label replay must not overwrite a finished review")
        XCTAssertEqual(e?.isResolved, true, "a finished review must not be reopened by a stale replay")
        XCTAssertEqual(e?.endTime, ms + 1, "end time must not move")
        XCTAssertEqual(Set(link.acked), [1, 2, 3], "replays are still acknowledged so the watch can forget them")
    }

    func testSosLocationIsStoredAndAcknowledged() async throws {
        let (service, link, _, _, _, _) = try make()
        await service.handle(frame(4, .sosLocation(timestamp: ms, latitude: 37.77, longitude: -122.41)))
        XCTAssertEqual(service.sosLocation?.latitude, 37.77)
        XCTAssertEqual(link.acked, [4])
    }

    func testOffBodyWindowNeverEscalates() async throws {
        let (service, _, events, _, _, _) = try make()
        await service.handle(frame(1, .sensorWindow(timestamp: ms, hrMean: 200, rmssd: 1, accMag: 1, repetitiveness: 1, hrSlope: 5, isOffBody: true, isExercising: false)))
        XCTAssertEqual(service.tier, 0)
        let n = await events.count()
        XCTAssertEqual(n, 0)
    }

    func testReviewFlowTwoStageForOverwhelmed() async throws {
        let (service, _, events, _, _, _) = try make()
        await service.handle(frame(1, .eventStart(timestamp: ms, eventId: "e1", isManual: false)))
        await service.handle(frame(2, .eventEnd(timestamp: ms + 1, eventId: "e1")))
        await service.resolveReview(eventId: "e1", newLabelOrContext: "OVERWHELMED")
        var e = await events.event(id: "e1")
        XCTAssertEqual(e?.isResolved, false, "OVERWHELMED continues to the context stage")
        await service.resolveReview(eventId: "e1", newLabelOrContext: "Loud Noises")
        e = await events.event(id: "e1")
        XCTAssertEqual(e?.label, "OVERWHELMED - Loud Noises")
        XCTAssertEqual(e?.isResolved, true)
    }
}

final class FileStoreTests: XCTestCase {
    private func tempDir() -> URL {
        let d = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try? FileManager.default.createDirectory(at: d, withIntermediateDirectories: true)
        return d
    }
    private func ev(_ id: String, _ uid: String, start: Int64, end: Int64? = nil, resolved: Bool = false) -> StressEvent {
        StressEvent(id: id, userId: uid, startTime: start, endTime: end, triggerType: "AUTO", label: "NONE", isResolved: resolved)
    }

    func testQuerySemanticsMatchRoom() async {
        let store = FileEventStore(directory: tempDir())
        await store.upsert(ev("a", "u", start: 100, end: 200))              // ended, unresolved -> needs review
        await store.upsert(ev("b", "u", start: 300))                         // ongoing
        await store.upsert(ev("c", "u", start: 50, end: 60, resolved: true))
        await store.upsert(ev("x", "other", start: 999))
        let latest = await store.latest(userId: "u")
        XCTAssertEqual(latest?.id, "b")
        let ongoing = await store.ongoing(userId: "u")
        XCTAssertEqual(ongoing?.id, "b")
        let review = await store.unresolvedEnded(userId: "u")
        XCTAssertEqual(review.map { $0.id }, ["a"], "ongoing and resolved events are not 'needs review'")
        let between = await store.between(userId: "u", start: 100, end: 300)
        XCTAssertEqual(between.map { $0.id }, ["a"], "start inclusive, end exclusive")
    }

    func testEventsSurviveARestart() async {
        let dir = tempDir()
        await FileEventStore(directory: dir).upsert(ev("a", "u", start: 1))
        let reopened = await FileEventStore(directory: dir).event(id: "a")
        XCTAssertEqual(reopened?.id, "a")
    }

    func testWindowsAreNotSharedBetweenAccounts() async {
        let dir = tempDir()
        let mine = FileWindowStore(ownerUid: "alice", directory: dir)
        await mine.append(SensorWindow(timestamp: 1, hrMean: 70, rmssd: 40, accMag: 0, repetitiveness: 0, hrSlope: 0, isOffBody: false, isExercising: false))
        await mine.flush()
        let sameUser = await FileWindowStore(ownerUid: "alice", directory: dir).latest()
        let otherUser = await FileWindowStore(ownerUid: "bob", directory: dir).latest()
        XCTAssertNotNil(sameUser)
        XCTAssertNil(otherUser, "a different account on the same phone must not see the previous person's vitals")
    }

    func testWindowRetentionDropsOldData() async {
        let store = FileWindowStore(ownerUid: "u", directory: tempDir())
        func w(_ t: Int64) -> SensorWindow { SensorWindow(timestamp: t, hrMean: 70, rmssd: 40, accMag: 0, repetitiveness: 0, hrSlope: 0, isOffBody: false, isExercising: false) }
        await store.append(w(1_000))
        await store.append(w(1_000 + FileWindowStore.retentionMs + 1))   // pushes the first one out of the 7-day window
        await store.flush()
        let latest = await store.latest()
        XCTAssertEqual(latest?.timestamp, 1_000 + FileWindowStore.retentionMs + 1)
    }
}

// CaregiverStateTests (the old `derive` test) was removed: that behaviour is now `CaregiverAccess`, covered by
// BioCueCore's CaregiverAccessTests (8 scenarios mirroring the tested Kotlin).
