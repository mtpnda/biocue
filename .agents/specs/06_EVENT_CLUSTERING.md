# Event Clustering & Deduplication Algorithm

## 1. Problem Statement
The system captures stress events via two primary vectors:
1. **Auto-detected (Algorithmic):** The `ScoringEngine` detects a physiological stress spike and logs a `StressEventEntity` (e.g., from T=1 to T=6).
2. **Manual Input (Wearer / Caregiver):** The user taps the watch or the caregiver inputs context via the dashboard, logging a `StressEventEntity` (e.g., from T=3 to T=9).

**The Loophole:** Currently, the system reads these directly from the database and maps them 1:1 to UI Journal entries. If a user manually logs an event *during* or *shortly after* an auto-detected event, the Caregiver Journal displays them as separate, overlapping events (e.g., an entry for 1-6, and another for 3-9). This is confusing and breaks the temporal narrative.

## 2. Expected Behavior
Events that overlap temporally must be grouped into a single "Cluster" before rendering the Journal. 
- **Time Bounds:** The cluster's `startTime` is the earliest `startTime` in the group. The cluster's `endTime` is the latest `endTime` (or `null` if any event is ongoing).
- **Metadata Fusion:** The resulting clustered journal entry should combine the metadata, stating both "Auto-detected" and "Manual Input" were present.
- **Labels:** Labels from all overlapping events should be preserved and combined (e.g., "OVERWHELMED + LOUD NOISES").

## 3. Algorithm Design
**Input:** `List<StressEventEntity>` (unsorted)
**Output:** `List<EventCluster>` 

1. Filter the input for resolved events (or unresolved depending on the target UI).
2. Sort the events ascending by `startTime`.
3. Iterate sequentially:
   - Maintain a `currentCluster` with `currentStart`, `currentEnd`, and `eventsList`.
   - For each `event` in sorted list:
     - If `event.startTime <= currentEnd` (treating `null` end times as infinity):
       - The event overlaps. Add it to `eventsList`.
       - Update `currentEnd = max(currentEnd, event.endTime)`. (If either is `null`, `currentEnd` becomes `null`).
     - Else:
       - No overlap. Push `currentCluster` to the results list.
       - Start a new `currentCluster` with the current `event`.
4. Return the results list.

## 4. Complexity
- **Time:** $O(N \log N)$ where $N$ is the number of events in the queried time window, due to the sorting step. The single pass iteration is $O(N)$.
- **Space:** $O(N)$ to store the output clusters.

## 5. Edge Cases
- **Ongoing Events:** An event with `endTime = null` represents an ongoing stress spike. This mathematically overlaps with *any* subsequent event that starts after it.
- **Contained Events:** A manual event (4-5) completely inside an auto event (1-10). The algorithm handles this because `max(10, 5) = 10`.
