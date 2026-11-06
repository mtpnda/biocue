package com.mtpnda.biocue.core.pairing

import com.mtpnda.biocue.core.data.local.StressEventEntity

/** Applies a caregiver's [Review] to a wearer's event. First resolution wins (spec 07). */
object ReviewApplier {
    /** @return the updated event, or null when the review must be ignored. */
    /** What a caregiver's journal should display: the event with their own review overlaid, if it still applies. */
    fun effective(event: StressEventEntity, review: Review?): StressEventEntity =
        review?.let { apply(event, it) } ?: event

    fun apply(event: StressEventEntity, review: Review): StressEventEntity? {
        if (event.isResolved) return null
        return event.copy(label = review.label, isResolved = review.isResolved)
    }
}
