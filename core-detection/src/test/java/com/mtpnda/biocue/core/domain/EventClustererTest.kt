package com.mtpnda.biocue.core.domain

import com.mtpnda.biocue.core.data.local.StressEventEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class EventClustererTest {
    
    data class TestCase(
        val name: String,
        val input: List<StressEventEntity>,
        val expectedClusters: Int,
        val expectedStart: Long?,
        val expectedEnd: Long?
    )

    @Test
    fun runTableDrivenTests() {
        val tests = listOf(
            TestCase(
                name = "Separate Events (No Overlap)",
                input = listOf(
                    createEvent(1, 10, 20),
                    createEvent(2, 30, 40)
                ),
                expectedClusters = 2,
                expectedStart = 10, // Check first cluster
                expectedEnd = 20
            ),
            TestCase(
                name = "Overlapping Events (Auto 1-6, Manual 3-9)",
                input = listOf(
                    createEvent(1, 1, 6, "AUTO"),
                    createEvent(2, 3, 9, "MANUAL")
                ),
                expectedClusters = 1,
                expectedStart = 1,
                expectedEnd = 9
            ),
            TestCase(
                name = "Contained Event (1-10 contains 4-6)",
                input = listOf(
                    createEvent(1, 1, 10),
                    createEvent(2, 4, 6)
                ),
                expectedClusters = 1,
                expectedStart = 1,
                expectedEnd = 10
            ),
            TestCase(
                name = "Ongoing Event overlaps everything after it",
                input = listOf(
                    createEvent(1, 1, null),
                    createEvent(2, 5, 10),
                    createEvent(3, 15, 20)
                ),
                expectedClusters = 1,
                expectedStart = 1,
                expectedEnd = null
            ),
            TestCase(
                name = "Out of order input (Unsorted)",
                input = listOf(
                    createEvent(2, 3, 9, "MANUAL"),
                    createEvent(1, 1, 6, "AUTO")
                ),
                expectedClusters = 1,
                expectedStart = 1,
                expectedEnd = 9
            ),
            TestCase(
                name = "Multiple clusters with overlaps",
                input = listOf(
                    createEvent(1, 1, 5),
                    createEvent(2, 4, 8),
                    createEvent(3, 12, 15),
                    createEvent(4, 14, 20)
                ),
                expectedClusters = 2,
                expectedStart = 1, // Start of first cluster
                expectedEnd = 8    // End of first cluster
            )
        )

        for (tc in tests) {
            val clusters = EventClusterer.cluster(tc.input)
            assertEquals("Failed on ${tc.name}: Cluster count", tc.expectedClusters, clusters.size)
            
            if (clusters.isNotEmpty()) {
                assertEquals("Failed on ${tc.name}: Start time", tc.expectedStart, clusters[0].startTime)
                assertEquals("Failed on ${tc.name}: End time", tc.expectedEnd, clusters[0].endTime)
            }
        }
    }

    private fun createEvent(id: Int, start: Long, end: Long?, trigger: String = "AUTO"): StressEventEntity {
        return StressEventEntity(
            id = id.toString(),
            startTime = start,
            endTime = end,
            triggerType = trigger,
            label = "TEST",
            isResolved = true
        )
    }
}
