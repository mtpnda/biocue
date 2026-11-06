import Foundation

/// Port of Kotlin `EventClusterer`: merges overlapping events into one journal entry.
/// An event with no end time is "still going" and swallows everything after it.
public enum EventClusterer {
    public static func cluster(_ events: [StressEvent]) -> [EventCluster] {
        if events.isEmpty { return [] }

        // Kotlin's `sortedBy` is stable. Swift's `sorted` does not promise that, so make it explicit:
        // ties on startTime keep their original order.
        let sorted = events.enumerated().sorted { a, b in
            if a.element.startTime != b.element.startTime { return a.element.startTime < b.element.startTime }
            return a.offset < b.offset
        }.map { $0.element }

        var clusters: [EventCluster] = []
        var currentStart = sorted[0].startTime
        var currentEnd = sorted[0].endTime
        var currentEvents: [StressEvent] = [sorted[0]]

        var i = 1
        while i < sorted.count {
            let ev = sorted[i]
            let effectiveEnd = currentEnd ?? Int64.max

            if ev.startTime <= effectiveEnd {
                currentEvents.append(ev)
                if let ce = currentEnd {
                    if let evEnd = ev.endTime {
                        currentEnd = max(ce, evEnd)
                    } else {
                        currentEnd = nil
                    }
                }
            } else {
                clusters.append(EventCluster(startTime: currentStart, endTime: currentEnd, events: currentEvents))
                currentStart = ev.startTime
                currentEnd = ev.endTime
                currentEvents = [ev]
            }
            i += 1
        }
        clusters.append(EventCluster(startTime: currentStart, endTime: currentEnd, events: currentEvents))
        return clusters
    }
}
