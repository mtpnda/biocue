package com.mtpnda.biocue.core.pairing

import com.mtpnda.biocue.core.data.local.StressEventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewApplierTest {
    private fun event(label: String = "NONE", resolved: Boolean = false) =
        StressEventEntity("e1", "w", 100L, 200L, "AUTO", label, resolved)
    private fun review(label: String, resolved: Boolean, at: Long = 5L) = Review("w_c1", "e1", label, resolved, at)

    @Test fun `a review is applied to an unresolved event`() {
        val updated = ReviewApplier.apply(event(), review("OVERWHELMED - Loud Noises", true))
        assertEquals(event(label = "OVERWHELMED - Loud Noises", resolved = true), updated)
    }


    @Test fun `a review for an event that is already resolved is ignored - the first resolution wins`() {
        assertNull(ReviewApplier.apply(event(label = "EXCITED", resolved = true), review("OVERWHELMED", false)))
    }


    @Test fun `the caregivers journal shows a review on an open event but never overrides a resolved one`() {
        val open = event()
        val done = event(label = "EXCITED", resolved = true)
        assertEquals(event(label = "OVERWHELMED", resolved = false), ReviewApplier.effective(open, review("OVERWHELMED", false)))
        assertEquals(done, ReviewApplier.effective(done, review("OVERWHELMED", false)))
        assertEquals(open, ReviewApplier.effective(open, null))
    }
}
