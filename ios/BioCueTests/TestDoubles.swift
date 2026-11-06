import Foundation
import Combine
import BioCueCore
@testable import BioCue

final class FakeLink: WatchLinking {
    private let subject = CurrentValueSubject<WatchLinkState, Never>(.stopped)
    var statePublisher: AnyPublisher<WatchLinkState, Never> { subject.eraseToAnyPublisher() }
    let frames: AsyncStream<DecodedFrame>
    private let continuation: AsyncStream<DecodedFrame>.Continuation
    var clock: WatchClock = .identity
    private(set) var acked: [Int] = []
    private(set) var sosRequests = 0

    init() {
        var c: AsyncStream<DecodedFrame>.Continuation?
        frames = AsyncStream<DecodedFrame> { c = $0 }
        continuation = c!
    }
    func start() {}
    func beginPairing() {}
    func forgetWatch() {}
    func shutdown() {}
    func acknowledge(seq: Int) { acked.append(seq) }
    func requestSosLocation() { sosRequests += 1 }
}

actor MemoryEventStore: EventStore {
    var events: [String: StressEvent] = [:]
    func event(id: String) -> StressEvent? { events[id] }
    func upsert(_ e: StressEvent) { events[e.id] = e }
    func latest(userId: String) -> StressEvent? { events.values.filter { $0.userId == userId }.max { $0.startTime < $1.startTime } }
    func ongoing(userId: String) -> StressEvent? { events.values.filter { $0.userId == userId && $0.endTime == nil }.max { $0.startTime < $1.startTime } }
    func unresolvedEnded(userId: String) -> [StressEvent] { events.values.filter { $0.userId == userId && !$0.isResolved && $0.endTime != nil } }
    func between(userId: String, start: Int64, end: Int64) -> [StressEvent] { events.values.filter { $0.userId == userId && $0.startTime >= start && $0.startTime < end } }
    func count() -> Int { events.count }
    func all() -> [StressEvent] { Array(events.values) }
}

actor MemoryWindowStore: WindowStore {
    var windows: [SensorWindow] = []
    func append(_ w: SensorWindow) { windows.append(w) }
    func latest() -> SensorWindow? { windows.last }
    func flush() {}
    func count() -> Int { windows.count }
}

final class FakeWearerCloud: WearerCloud, @unchecked Sendable {
    private let lock = NSLock()
    private var _synced: [StressEvent] = []
    var synced: [StressEvent] { lock.lock(); defer { lock.unlock() }; return _synced }
    func syncEvent(wearerUid: String, event: StressEvent) async throws { lock.lock(); _synced.append(event); lock.unlock() }
    func observeCaregivers(wearerUid: String, onChange: @escaping (Result<[Pairing], Error>) -> Void) -> ListenerToken { ListenerToken {} }
    func observeReviews(pairingId: String, onChange: @escaping (Result<[Review], Error>) -> Void) -> ListenerToken { ListenerToken {} }
    // WearerPairingStore
    func pairingsOfWearer(_ wearerUid: String) async throws -> [Pairing] { [] }
    func issueCode(wearerUid: String, expiresAt: Int64) async throws -> String { "123456" }
    func consumeCodes(wearerUid: String) async throws {}
    func deletePairing(_ pairingId: String) async throws {}
    func markRemoved(_ pairingId: String, removedAt: Int64) async throws {}
    func markApproved(_ pairingId: String) async throws {}
}

final class FakeCaregiverCloud: CaregiverCloud, @unchecked Sendable {
    func observePairings(caregiverUid: String, onChange: @escaping (Result<[Pairing], Error>) -> Void) -> ListenerToken { ListenerToken {} }
    func observeEvents(wearerUid: String, onChange: @escaping (Result<[StressEvent], Error>) -> Void) -> ListenerToken { ListenerToken {} }
    func observeReviews(pairingId: String, onChange: @escaping (Result<[Review], Error>) -> Void) -> ListenerToken { ListenerToken {} }
    func leavePairing(pairingId: String) async throws {}
    // CaregiverPairingStore + ReviewStore
    func lookupCode(_ code: String) async throws -> CodeRecord? { nil }
    func pairing(_ pairingId: String) async throws -> Pairing? { nil }
    func createPairing(_ pairing: Pairing) async throws {}
    func reRequest(_ pairingId: String, caregiverName: String, createdAt: Int64) async throws {}
    func writeReview(_ review: Review) async throws {}
}

final class FakeNotifier: AlertNotifying {
    private(set) var tier1Count = 0, tier2Count = 0
    func requestAuthorization() async {}
    func tier1(percent: Int) { tier1Count += 1 }
    func tier2(percent: Int) { tier2Count += 1 }
}
