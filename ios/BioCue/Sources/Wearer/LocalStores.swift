import Foundation
import BioCueCore

// WEARER-ONLY. Raw vitals and the event log live here, on the phone, and nowhere else
// ("Heart data & movement stays on this phone, always" — the app's privacy promise).

protocol EventStore: Sendable {
    func event(id: String) async -> StressEvent?
    func upsert(_ event: StressEvent) async
    /// Most recent event by start time (same as Room `getLatestEvent`).
    func latest(userId: String) async -> StressEvent?
    /// Newest event that has not ended.
    func ongoing(userId: String) async -> StressEvent?
    /// Needs review: unresolved AND ended (same as Room `getUnresolvedEvents`). Newest first.
    func unresolvedEnded(userId: String) async -> [StressEvent]
    /// startTime in [start, end), newest first (same as Room `getEventsBetween`).
    func between(userId: String, start: Int64, end: Int64) async -> [StressEvent]
}

protocol WindowStore: Sendable {
    func append(_ window: SensorWindow) async
    func latest() async -> SensorWindow?
    func flush() async
}

/// Where app data lives: Application Support, excluded from iCloud/iTunes backup (health-adjacent data).
enum AppStorage {
    static func directory() -> URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        var dir = base.appendingPathComponent("BioCue", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        try? dir.setResourceValues(values)
        return dir
    }
}

actor FileEventStore: EventStore {
    private let url: URL
    private var events: [String: StressEvent]

    init(directory: URL = AppStorage.directory()) {
        self.url = directory.appendingPathComponent("events.json")
        if let data = try? Data(contentsOf: url), let list = try? JSONDecoder().decode([StressEvent].self, from: data) {
            self.events = Dictionary(list.map { ($0.id, $0) }, uniquingKeysWith: { _, new in new })
        } else {
            self.events = [:]
        }
    }

    func event(id: String) -> StressEvent? { events[id] }

    func upsert(_ event: StressEvent) {
        events[event.id] = event
        persist()
    }

    func latest(userId: String) -> StressEvent? {
        events.values.filter { $0.userId == userId }.max { $0.startTime < $1.startTime }
    }

    func ongoing(userId: String) -> StressEvent? {
        events.values.filter { $0.userId == userId && $0.endTime == nil }.max { $0.startTime < $1.startTime }
    }

    func unresolvedEnded(userId: String) -> [StressEvent] {
        events.values.filter { $0.userId == userId && !$0.isResolved && $0.endTime != nil }
            .sorted { $0.startTime > $1.startTime }
    }

    func between(userId: String, start: Int64, end: Int64) -> [StressEvent] {
        events.values.filter { $0.userId == userId && $0.startTime >= start && $0.startTime < end }
            .sorted { $0.startTime > $1.startTime }
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(Array(events.values)) else { return }
        try? data.write(to: url, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
    }
}

/// Bounded ring of recent windows. Retention is 7 days / 5,000 windows so the phone cannot fill up.
/// Windows carry no user id, so the store remembers which account owns them and starts empty for a
/// different one (a shared phone must not show or keep the previous person's vitals).
actor FileWindowStore: WindowStore {
    static let maxCount = 5_000
    static let retentionMs: Int64 = 7 * 24 * 60 * 60 * 1000

    private struct Snapshot: Codable { var ownerUid: String; var windows: [SensorWindow] }

    private let url: URL
    private let ownerUid: String
    private var windows: [SensorWindow]
    private var unsaved = 0

    init(ownerUid: String, directory: URL = AppStorage.directory()) {
        self.url = directory.appendingPathComponent("windows.json")
        self.ownerUid = ownerUid
        if let data = try? Data(contentsOf: url),
           let snap = try? JSONDecoder().decode(Snapshot.self, from: data), snap.ownerUid == ownerUid {
            self.windows = snap.windows
        } else {
            self.windows = []
        }
    }

    func append(_ window: SensorWindow) {
        // Same primary-key semantics as Room's REPLACE on timestamp.
        if let i = windows.lastIndex(where: { $0.timestamp == window.timestamp }) { windows[i] = window }
        else { windows.append(window) }
        let cutoff = window.timestamp - Self.retentionMs
        windows.removeAll { $0.timestamp < cutoff }
        if windows.count > Self.maxCount { windows.removeFirst(windows.count - Self.maxCount) }
        unsaved += 1
        if unsaved >= 20 { persist() }
    }

    func latest() -> SensorWindow? { windows.max { $0.timestamp < $1.timestamp } }

    func flush() { persist() }

    private func persist() {
        unsaved = 0
        guard let data = try? JSONEncoder().encode(Snapshot(ownerUid: ownerUid, windows: windows)) else { return }
        try? data.write(to: url, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
    }
}
