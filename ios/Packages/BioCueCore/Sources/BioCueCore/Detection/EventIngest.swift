import Foundation

/// Idempotent application of watch event messages to the local event store.
///
/// Why this exists: the Data Layer delivered each DataItem change once. A BLE link is
/// at-least-once (the watch re-sends un-ACKed critical frames after a reconnect), so the
/// same EVENT_START / LABEL / END can legitimately arrive twice and must not corrupt state.
public enum EventIngest {

    /// A duplicate start for an id we already have is ignored (it must not reset a label
    /// or reopen an ended event). Returns nil when there is nothing to store.
    public static func applyStart(existing: StressEvent?, id: String, userId: String,
                                  timestamp: Int64, isManual: Bool) -> StressEvent? {
        if existing != nil { return nil }
        return StressEvent(id: id, userId: userId, startTime: timestamp, endTime: nil,
                           triggerType: isManual ? "MANUAL" : "AUTO", label: EventTypes.labelNone, isResolved: false)
    }

    /// Same behaviour as the Android receiver (label set, review reopened) with one safeguard:
    /// a label for an event whose review is already complete is a stale retransmission — ignore it.
    public static func applyLabel(existing: StressEvent?, label: String) -> StressEvent? {
        guard var event = existing else { return nil }
        if event.isResolved { return nil }
        if event.label == label { return nil }
        event.label = label
        event.isResolved = false
        return event
    }

    /// Sets the end time once. A repeat (or earlier) end for an already-ended event is ignored.
    public static func applyEnd(existing: StressEvent?, timestamp: Int64) -> StressEvent? {
        guard var event = existing else { return nil }
        if event.endTime != nil { return nil }
        event.endTime = timestamp
        return event
    }
}
