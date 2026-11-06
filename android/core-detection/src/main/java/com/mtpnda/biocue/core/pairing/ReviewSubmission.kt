package com.mtpnda.biocue.core.pairing

/** A caregiver's review of one wearer event: `pairings/{pairingId}/reviews/{eventId}` (spec 07). */
data class Review(val pairingId: String, val eventId: String, val label: String, val isResolved: Boolean, val updatedAt: Long)

/** Boundary: where reviews are written (Firestore). Faked in tests. */
interface ReviewStore {
    fun writeReview(review: Review)
}

sealed interface ReviewResult {
    object Submitted : ReviewResult
    object NotAllowed : ReviewResult
}

class ReviewSubmission(private val store: ReviewStore, private val clock: Clock) {
    fun submit(pairing: Pairing, eventId: String, currentLabel: String, choice: String): ReviewResult {
        if (pairing.status != PairingStatus.APPROVED) return ReviewResult.NotAllowed
        val (label, resolved) = com.mtpnda.biocue.core.domain.ResolveLogic.processResolution(currentLabel, choice)
        store.writeReview(Review(pairing.id, eventId, label, resolved, clock.nowMs()))
        return ReviewResult.Submitted
    }
}
