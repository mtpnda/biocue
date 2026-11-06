package com.mtpnda.biocue.core.domain

import com.mtpnda.biocue.core.data.local.StressEventEntity
import kotlin.math.max

data class EventCluster(
    val startTime: Long,
    val endTime: Long?,
    val events: List<StressEventEntity>
)

object EventClusterer {
    fun cluster(events: List<StressEventEntity>): List<EventCluster> {
        if (events.isEmpty()) return emptyList()

        val sorted = events.sortedBy { it.startTime }
        val clusters = mutableListOf<EventCluster>()

        var currentStart = sorted[0].startTime
        var currentEnd = sorted[0].endTime
        var currentEvents = mutableListOf(sorted[0])

        for (i in 1 until sorted.size) {
            val ev = sorted[i]
            val effectiveEnd = currentEnd ?: Long.MAX_VALUE

            if (ev.startTime <= effectiveEnd) {
                // overlap found, merge
                currentEvents.add(ev)
                // expand currentEnd if needed
                if (currentEnd != null) {
                    if (ev.endTime == null) {
                        currentEnd = null
                    } else {
                        currentEnd = max(currentEnd, ev.endTime)
                    }
                }
            } else {
                // push old cluster
                clusters.add(EventCluster(currentStart, currentEnd, currentEvents))
                currentStart = ev.startTime
                currentEnd = ev.endTime
                currentEvents = mutableListOf(ev)
            }
        }
        clusters.add(EventCluster(currentStart, currentEnd, currentEvents))
        return clusters
    }
}
