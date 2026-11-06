import Foundation
import Combine
import BioCueCore

// WEARER-ONLY. The iOS twin of Android's WatchDataReceiverService + WearerDashboardViewModel:
// receives watch frames, runs the shared detection pipeline, keeps the local event log, and syncs
// events (never raw vitals) to the cloud for the wearer's caregivers.

@MainActor
final class WearerService: ObservableObject {

    // MARK: UI state
    @Published private(set) var connection: ConnectionStatus = .searching
    @Published private(set) var linkState: WatchLinkState = .stopped
    @Published private(set) var tier = 0
    @Published private(set) var stressPercent = 0
    @Published private(set) var activityLabel = "🪑 Sitting Still"
    @Published private(set) var sosLocation: (latitude: Double, longitude: Double)?
    @Published private(set) var reviews: [ReviewEvent] = []
    @Published private(set) var journal: [JournalEntry] = []
    @Published private(set) var ongoingLabel: String?
    @Published private(set) var journalOffset = 0

    // MARK: Dependencies
    private let userId: String
    private let link: WatchLinking
    private let events: EventStore
    private let windows: WindowStore
    private let cloud: WearerCloud
    private let notifier: AlertNotifying
    private let now: () -> Date
    private let calendar: Calendar

    private var frameTask: Task<Void, Never>?
    private var tickTask: Task<Void, Never>?
    private var mockTask: Task<Void, Never>?
    private var linkSub: AnyCancellable?
    private var lastWindowAt: Date?
    private var lastSosRequestAt: Date?
    private var lastAlertAt: [Int: Date] = [:]

    /// Same fixed baseline as Android (no per-user calibration yet).
    private let baseline = Baseline.appDefault
    static let alertCooldown: TimeInterval = 5 * 60
    static let sosRequestCooldown: TimeInterval = 60
    static let disconnectedAfter: TimeInterval = 30

    /// Throws `RoleError` for a caregiver: this service cannot exist for the wrong role.
    init(userId: String, gate: RoleGate, link: WatchLinking, events: EventStore, windows: WindowStore,
         cloud: WearerCloud, notifier: AlertNotifying, now: @escaping () -> Date = Date.init,
         calendar: Calendar = .current) throws {
        try gate.require(\.linkWatch, action: "receive watch data")
        try gate.require(\.createEvents, action: "create stress events")
        self.userId = userId
        self.link = link
        self.events = events
        self.windows = windows
        self.cloud = cloud
        self.notifier = notifier
        self.now = now
        self.calendar = calendar
    }

    // MARK: Lifecycle

    func start() {
        guard frameTask == nil else { return }
        linkSub = link.statePublisher.sink { [weak self] state in
            Task { @MainActor in self?.linkState = state }
        }
        link.start()
        Task { await notifier.requestAuthorization() }

        // Frames are processed strictly one at a time, in arrival order: an event's start must be
        // applied before its label and end.
        frameTask = Task { [weak self] in
            guard let self = self else { return }
            for await frame in self.link.frames { await self.handle(frame) }
        }
        tickTask = Task { [weak self] in
            while !Task.isCancelled {
                self?.updateConnection()
                await self?.refresh()
                try? await Task.sleep(nanoseconds: 2_000_000_000)
            }
        }
    }

    func stop() {
        frameTask?.cancel(); tickTask?.cancel(); mockTask?.cancel()
        frameTask = nil; tickTask = nil; mockTask = nil
        linkSub = nil
        Task { await windows.flush() }
    }

    // MARK: Frames from the watch

    func handle(_ frame: DecodedFrame) async {
        let clock = link.clock   // watch time -> phone time, so every comparison below uses ONE clock
        switch frame.message {
        case let .sensorWindow(ts, hr, rmssd, acc, rep, slope, off, ex):
            await process(SensorWindow(timestamp: clock.toPhoneTime(ts), hrMean: hr, rmssd: rmssd, accMag: acc, repetitiveness: rep,
                                       hrSlope: slope, isOffBody: off, isExercising: ex))

        case let .eventStart(ts, id, manual):
            let existing = await events.event(id: id)
            if let e = EventIngest.applyStart(existing: existing, id: id, userId: userId, timestamp: clock.toPhoneTime(ts), isManual: manual) {
                await events.upsert(e); sync(e)
            }
            link.acknowledge(seq: frame.seq)     // stored (or already had it): safe for the watch to forget

        case let .eventLabel(_, id, label):
            let existing = await events.event(id: id)
            if let e = EventIngest.applyLabel(existing: existing, label: label) {
                await events.upsert(e); sync(e)
            }
            link.acknowledge(seq: frame.seq)

        case let .eventEnd(ts, id):
            let existing = await events.event(id: id)
            if let e = EventIngest.applyEnd(existing: existing, timestamp: clock.toPhoneTime(ts)) {
                await events.upsert(e); sync(e)
            }
            link.acknowledge(seq: frame.seq)

        case let .sosLocation(_, lat, lng):
            sosLocation = (lat, lng)
            link.acknowledge(seq: frame.seq)

        default:
            break
        }
        await refresh()
    }

    private func process(_ window: SensorWindow) async {
        let nowDate = now()
        let nowMs = Int64(nowDate.timeIntervalSince1970 * 1000)
        let latest = await events.latest(userId: userId)
        let decision = DetectionPipeline.decide(
            window: window, baseline: baseline,
            latestEvent: latest.map { LatestEvent(id: $0.id, startTime: $0.startTime, endTime: $0.endTime) },
            nowMs: nowMs)

        await windows.append(window)
        lastWindowAt = nowDate
        tier = decision.tier
        stressPercent = DetectionPipeline.stressPercent(decision.score)
        activityLabel = Self.activityLabel(for: window)

        if decision.tier == 2 { requestSosLocationIfDue(nowDate) }
        alertIfDue(tier: decision.tier, at: nowDate)

        for action in decision.actions {
            switch action {
            case let .createAutoEvent(start):
                let e = StressEvent(id: UUID().uuidString, userId: userId, startTime: start, endTime: nil,
                                    triggerType: "AUTO", label: EventTypes.labelNone, isResolved: false)
                await events.upsert(e); sync(e)
            case let .closeEvent(id, end):
                if var e = await events.event(id: id) {
                    e.endTime = end
                    await events.upsert(e); sync(e)
                }
            }
        }
    }

    /// Cloud writes are fire-and-forget: the Firestore SDK queues them while offline, but an `await`
    /// on one would not return until the server confirms — blocking event processing (and the ACK
    /// to the watch) whenever the phone has no signal.
    private func sync(_ event: StressEvent) {
        let cloud = self.cloud, uid = userId
        Task.detached { try? await cloud.syncEvent(wearerUid: uid, event: event) }
    }

    private func alertIfDue(tier: Int, at date: Date) {
        guard tier >= 1 else { return }
        if let last = lastAlertAt[tier], date.timeIntervalSince(last) < Self.alertCooldown { return }
        lastAlertAt[tier] = date
        if tier == 2 { notifier.tier2(percent: stressPercent) } else { notifier.tier1(percent: stressPercent) }
    }

    private func requestSosLocationIfDue(_ date: Date) {
        if let last = lastSosRequestAt, date.timeIntervalSince(last) < Self.sosRequestCooldown { return }
        lastSosRequestAt = date
        link.requestSosLocation()
    }

    static func activityLabel(for w: SensorWindow) -> String {
        if w.isExercising { return "🏃 Exercising" }
        if w.repetitiveness > 0.5 { return "🔄 Pacing / Stimming" }
        if w.accMag > 0.2 { return "🚶 Active" }
        return "🪑 Sitting Still"
    }

    private func updateConnection() {
        guard let last = lastWindowAt else { connection = .searching; return }
        connection = now().timeIntervalSince(last) > Self.disconnectedAfter ? .disconnected : .connected
    }

    // MARK: Review + journal

    func refresh() async {
        let unresolved = await events.unresolvedEnded(userId: userId)
        reviews = unresolved.map { e in
            ReviewEvent(eventId: e.id, time: TimeFormat.clock(e.startTime), triggerType: e.triggerType, currentLabel: e.label,
                        cues: [("🔄", e.triggerType == "AUTO" ? "Detected Spontaneous Spike" : "Manually Triggered"),
                               ("📍", sosLocation != nil ? "Location captured" : "Unknown Location")])
        }
        ongoingLabel = await events.ongoing(userId: userId)?.label

        let (start, end) = dayBounds(offset: journalOffset)
        let resolved = await events.between(userId: userId, start: start, end: end).filter { $0.isResolved }
        journal = EventClusterer.cluster(resolved).map { cluster in
            var labels: [String] = []   // distinct, first-seen order (Kotlin `.distinct()`)
            for event in cluster.events where event.label != "NONE" && !labels.contains(event.label) {
                labels.append(event.label)
            }
            let labelText = labels.isEmpty ? "Unknown" : labels.joined(separator: " + ")
            let hasAuto = cluster.events.contains { $0.triggerType == "AUTO" }
            let hasManual = cluster.events.contains { $0.triggerType == "MANUAL" }
            let activity = hasAuto && hasManual ? "Auto-detected + Manual Input" : (hasAuto ? "Auto-detected" : "Manual")
            return JournalEntry(time: TimeFormat.clock(cluster.startTime), activity: activity,
                                labelIcon: EventTypes.icon(forLabel: labelText),
                                labelText: labelText, colorHex: EventTypes.colorHex(forLabel: labelText))
        }
    }

    /// Spec 07: a caregiver's review is applied only while the event is unresolved (first resolution wins).
    func applyReview(_ review: Review) async {
        guard let event = await events.event(id: review.eventId),
              let updated = ReviewApplier.apply(event, review) else { return }
        await events.upsert(updated); sync(updated)
        await refresh()
    }

    func resolveReview(eventId: String, newLabelOrContext: String) async {
        guard var e = await events.event(id: eventId) else { return }
        let result = ResolveLogic.processResolution(currentLabel: e.label, newLabelOrContext: newLabelOrContext)
        e.label = result.label
        e.isResolved = result.resolved
        await events.upsert(e); sync(e)
        await refresh()
    }

    func changeJournalDate(delta: Int) {
        let next = journalOffset + delta
        guard next <= 0 else { return }
        journalOffset = next
        Task { await refresh() }
    }

    func endOngoingEvent() async {
        guard var e = await events.ongoing(userId: userId) else { return }
        e.endTime = Int64(now().timeIntervalSince1970 * 1000)
        await events.upsert(e); sync(e)
        await refresh()
    }

    // MARK: Helpers

    private func dayBounds(offset: Int) -> (Int64, Int64) {
        let day = calendar.date(byAdding: .day, value: offset, to: now()) ?? now()
        let start = calendar.startOfDay(for: day)
        let end = calendar.date(byAdding: .day, value: 1, to: start) ?? start.addingTimeInterval(86_400)
        return (Int64(start.timeIntervalSince1970 * 1000), Int64(end.timeIntervalSince1970 * 1000))
    }

    // MARK: Developer tools (no watch needed)

    #if DEBUG
    enum MockIntensity { case off, low, medium, high }

    /// Feeds synthetic windows through the REAL pipeline, like the Android "Continuous Sensor Mock".
    func setMockIntensity(_ intensity: MockIntensity) {
        mockTask?.cancel()
        guard intensity != .off else { return }
        mockTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self = self else { return }
                var hr = 110.0
                var hrv = 10.0
                if intensity == .low { hr = 72; hrv = 45 }
                else if intensity == .medium { hr = 88; hrv = 22 }
                let ts = Int64(self.now().timeIntervalSince1970 * 1000)
                await self.process(SensorWindow(timestamp: ts, hrMean: hr, rmssd: hrv, accMag: 0.1, repetitiveness: 0.1,
                                                hrSlope: 0, isOffBody: false, isExercising: false))
                await self.refresh()
                try? await Task.sleep(nanoseconds: 2_000_000_000)
            }
        }
    }

    func simulateManualEvent(label: String) async {
        let ms = Int64(now().timeIntervalSince1970 * 1000)
        let e = StressEvent(id: UUID().uuidString, userId: userId, startTime: ms, endTime: nil,
                            triggerType: "MANUAL", label: label, isResolved: false)
        await events.upsert(e); sync(e)
        await refresh()
    }
    #endif
}
