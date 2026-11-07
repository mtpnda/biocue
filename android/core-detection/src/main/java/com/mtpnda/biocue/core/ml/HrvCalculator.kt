package com.mtpnda.biocue.core.ml

import kotlin.math.sqrt

/**
 * Computes HRV RMSSD (Root Mean Square of Successive Differences) and HR slope.
 * Includes artifact rejection for non-physiological beat intervals.
 */
class HrvCalculator(
    private val minValidRrMs: Double = 300.0,  // 200 BPM
    private val maxValidRrMs: Double = 1500.0, // 40 BPM
    private val maxSuccessiveDiffMs: Double = 300.0
) {
    private val hrHistory = ArrayDeque<Pair<Long, Double>>() // (timestampMs, hr)

    /**
     * Compute RMSSD from a sequence of RR (inter-beat) intervals in milliseconds.
     */
    fun computeRmssd(rrIntervalsMs: List<Double>): Double {
        if (rrIntervalsMs.size < 2) return 42.0 // Return default healthy baseline if insufficient samples

        // Filter valid physiological intervals
        val validIntervals = mutableListOf<Double>()
        for (rr in rrIntervalsMs) {
            if (rr in minValidRrMs..maxValidRrMs) {
                validIntervals.add(rr)
            }
        }

        if (validIntervals.size < 2) return 42.0

        var sumSquaredDiffs = 0.0
        var count = 0

        for (i in 0 until validIntervals.size - 1) {
            val diff = validIntervals[i + 1] - validIntervals[i]
            if (kotlin.math.abs(diff) <= maxSuccessiveDiffMs) {
                sumSquaredDiffs += diff * diff
                count++
            }
        }

        return if (count > 0) sqrt(sumSquaredDiffs / count) else 42.0
    }

    /**
     * Convert instantaneous HR values to approximate RR intervals (60,000 / HR)
     * and compute RMSSD.
     */
    fun computeRmssdFromHrList(hrList: List<Double>): Double {
        val rrList = hrList.filter { it > 30.0 && it < 220.0 }.map { 60000.0 / it }
        return computeRmssd(rrList)
    }

    /**
     * Add a heart rate reading with timestamp for trailing slope computation.
     */
    fun addHrReading(timestampMs: Long, hr: Double) {
        hrHistory.addLast(timestampMs to hr)
        // Keep trailing 30 seconds max
        val cutoff = timestampMs - 30_000L
        while (hrHistory.isNotEmpty() && hrHistory.first().first < cutoff) {
            hrHistory.removeFirst()
        }
    }

    /**
     * Compute heart rate slope (change in BPM per second) over the trailing ~10-15s.
     */
    fun computeHrSlope(nowMs: Long): Double {
        if (hrHistory.size < 2) return 0.0
        val latest = hrHistory.last()
        val targetWindowStart = nowMs - 10_000L

        // Find the reading closest to 10s ago
        var oldestInWindow = hrHistory.first()
        for (item in hrHistory) {
            if (item.first >= targetWindowStart) {
                oldestInWindow = item
                break
            }
        }

        val dtSec = (latest.first - oldestInWindow.first) / 1000.0
        if (dtSec < 2.0) return 0.0 // Avoid noisy division by tiny time delta

        val dHr = latest.second - oldestInWindow.second
        return dHr / dtSec
    }
}
