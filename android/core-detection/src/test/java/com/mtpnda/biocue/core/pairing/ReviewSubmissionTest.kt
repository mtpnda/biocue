package com.mtpnda.biocue.core.pairing

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingReviewStore : ReviewStore {
    val written = mutableListOf<Review>()
    override fun writeReview(review: Review) { written += review }
}

class ReviewSubmissionTest {
    private val store = RecordingReviewStore()
    private val clock = FakeClock(7_000L)
    private val submission = ReviewSubmission(store, clock)

    @Test fun `a caregiver whose pairing is not approved cannot submit a review`() {
        val result = submission.submit(pairing("c1", PairingStatus.PENDING), "e1", "NONE", "OVERWHELMED")
        assertEquals(ReviewResult.NotAllowed, result)
        assertEquals(emptyList<Review>(), store.written)
    }


    @Test fun `choosing overwhelmed for an unlabelled event records it but keeps the review open for context`() {
        val result = submission.submit(pairing("c1", PairingStatus.APPROVED), "e1", "NONE", "OVERWHELMED")
        assertEquals(ReviewResult.Submitted, result)
        assertEquals(listOf(Review("w_c1", "e1", "OVERWHELMED", false, 7_000L)), store.written)
    }


    @Test fun `adding a context to an overwhelmed event resolves it with a combined label`() {
        submission.submit(pairing("c1", PairingStatus.APPROVED), "e1", "OVERWHELMED", "Loud Noises")
        assertEquals(listOf(Review("w_c1", "e1", "OVERWHELMED - Loud Noises", true, 7_000L)), store.written)
    }
}
