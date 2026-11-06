package com.mtpnda.biocue.core.domain

import com.mtpnda.biocue.core.domain.model.Baseline
import com.mtpnda.biocue.core.domain.model.SensorWindow
import com.mtpnda.biocue.core.ml.ScoringEngine

/** The parts of the most recent stress event the pipeline needs to decide. */
data class LatestEvent(val id: String, val startTime: Long, val endTime: Long?)

sealed interface PipelineAction {
    /** Open a new AUTO event starting at [startTime] (label NONE, unresolved). */
    data class CreateAutoEvent(val startTime: Long) : PipelineAction
    /** Close the ongoing event [eventId] at [endTime]. */
    data class CloseEvent(val eventId: String, val endTime: Long) : PipelineAction
}

data class PipelineDecision(
    val score: Double,
    /** 0 calm, 1 early warning, 2 escalation — same value the UI shows as the tier. */
    val tier: Int,
    val actions: List<PipelineAction>,
)

/**
 * Single source of truth for "what should the wearer's phone do with this sensor
 * window?". It is a pure function so Android (WatchDataReceiverService) and iOS
 * (BioCueCore.DetectionPipeline) can be checked against the same fixtures.
 *
 * Behaviour is exactly what WatchDataReceiverService.processWindow did inline:
 *   score > 2.5  -> tier 2; open an AUTO event unless one is already ongoing
 *   score > 1.5  -> tier 1; no event change
 *   otherwise    -> tier 0; close any ongoing event
 * An event counts as "ongoing" only while it has no end AND started < 15 min ago;
 * older open events are treated as orphaned so a new one may start.
 */
object DetectionPipeline {
    const val TIER1_THRESHOLD = 1.5
    const val TIER2_THRESHOLD = 2.5
    const val ORPHAN_AFTER_MS = 15L * 60 * 1000

    fun tierFor(score: Double): Int = when {
        score > TIER2_THRESHOLD -> 2
        score > TIER1_THRESHOLD -> 1
        else -> 0
    }

    /** 0..100 gauge used by the wearer dashboard and the tier alert text. */
    fun stressPercent(score: Double): Int = ((score / 3.0) * 100).toInt().coerceIn(0, 100)

    fun decide(
        window: SensorWindow,
        baseline: Baseline,
        latestEvent: LatestEvent?,
        nowMs: Long,
        poorSleep: Boolean = false,
        scoring: ScoringEngine = ScoringEngine(),
    ): PipelineDecision {
        val score = scoring.computeRuleBasedScore(window, baseline, poorSleep)
        val tier = tierFor(score)
        val actions = mutableListOf<PipelineAction>()
        when (tier) {
            2 -> {
                val ongoing = latestEvent != null &&
                    latestEvent.endTime == null &&
                    (nowMs - latestEvent.startTime < ORPHAN_AFTER_MS)
                if (!ongoing) actions += PipelineAction.CreateAutoEvent(window.timestamp)
            }
            1 -> Unit
            else -> {
                if (latestEvent != null && latestEvent.endTime == null) {
                    actions += PipelineAction.CloseEvent(latestEvent.id, window.timestamp)
                }
            }
        }
        return PipelineDecision(score, tier, actions)
    }
}
