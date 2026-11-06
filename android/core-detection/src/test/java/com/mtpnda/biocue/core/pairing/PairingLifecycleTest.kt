package com.mtpnda.biocue.core.pairing

import org.junit.Assert.assertEquals
import org.junit.Test

class PairingLifecycleTest {
    private val store = InMemoryPairingStore()
    private val clock = FakeClock()
    private val lifecycle = PairingLifecycle(store, clock)

    @Test fun `a wearer with two approved caregivers cannot generate a pairing code`() {
        store.put(pairing("c1", PairingStatus.APPROVED))
        store.put(pairing("c2", PairingStatus.APPROVED))
        assertEquals(CodeResult.MaxCaregiversReached, lifecycle.generateCode("w"))
    }

    @Test fun `below the limit a wearer gets a code that is valid for ten minutes`() {
        store.put(pairing("c1", PairingStatus.APPROVED))
        assertEquals(CodeResult.Issued("123456", clock.now + 10 * 60_000), lifecycle.generateCode("w"))
    }

    @Test fun `the code is consumed as soon as the first request arrives`() {
        lifecycle.generateCode("w")
        assertEquals(true, store.isCodeLive("123456"))
        store.put(pairing("c1", PairingStatus.PENDING))
        lifecycle.onPairingsChanged("w")
        assertEquals(false, store.isCodeLive("123456"))
    }

    @Test fun `the code stays valid while no request is pending`() {
        lifecycle.generateCode("w")
        store.put(pairing("c1", PairingStatus.APPROVED))   // an existing caregiver is not a new request
        lifecycle.onPairingsChanged("w")
        assertEquals(true, store.isCodeLive("123456"))
    }


    @Test fun `denying a request deletes the pairing document`() {
        store.put(pairing("c1", PairingStatus.PENDING))
        lifecycle.deny("w_c1")
        assertEquals(emptyList<Pairing>(), store.pairingsOfWearer("w"))
    }


    @Test fun `removing a caregiver marks the pairing REMOVED with the time of removal`() {
        store.put(pairing("c1", PairingStatus.APPROVED))
        clock.now = 5_000_000L
        lifecycle.remove("w_c1")
        assertEquals(listOf(pairing("c1", PairingStatus.REMOVED, removedAt = 5_000_000L)), store.pairingsOfWearer("w"))
    }


    @Test fun `approving a request gives the caregiver access`() {
        store.put(pairing("c1", PairingStatus.PENDING))
        assertEquals(ApprovalResult.Approved, lifecycle.approve("w", "w_c1"))
        assertEquals(PairingStatus.APPROVED, store.pairingsOfWearer("w").single().status)
    }


    @Test fun `a third caregiver cannot be approved and nothing changes`() {
        store.put(pairing("c1", PairingStatus.APPROVED))
        store.put(pairing("c2", PairingStatus.APPROVED))
        store.put(pairing("c3", PairingStatus.PENDING))
        assertEquals(ApprovalResult.MaxCaregiversReached, lifecycle.approve("w", "w_c3"))
        assertEquals(PairingStatus.PENDING, store.pairingsOfWearer("w").first { it.id == "w_c3" }.status)
    }


    @Test fun `a code nobody issued is not found`() {
        assertEquals(SubmitResult.NotFound, lifecycle.submitCode("c1", "Care c1", "999999"))
    }


    @Test fun `an expired code is not found`() {
        store.plantCode("111111", "w", expiresAt = clock.now - 1)
        assertEquals(SubmitResult.NotFound, lifecycle.submitCode("c1", "Care c1", "111111"))
    }


    @Test fun `a valid code creates a pending request carrying both names`() {
        store.plantCode("111111", "w", expiresAt = clock.now + 1000)
        assertEquals(SubmitResult.Requested, lifecycle.submitCode("c1", "Maya", "111111"))
        assertEquals(
            listOf(Pairing("w_c1", "w", "c1", "Maya", "Wearer", PairingStatus.PENDING, clock.now)),
            store.pairingsOfWearer("w"))
    }


    @Test fun `a removed caregiver can request again on the same pairing document`() {
        store.put(pairing("c1", PairingStatus.REMOVED, removedAt = 123L))
        store.plantCode("111111", "w", expiresAt = 99_000_000L)
        clock.now = 9_000_000L
        assertEquals(SubmitResult.Requested, lifecycle.submitCode("c1", "Maya", "111111"))
        val only = store.pairingsOfWearer("w").single()
        assertEquals("w_c1", only.id); assertEquals(PairingStatus.PENDING, only.status); assertEquals(null, only.removedAt)
        assertEquals(9_000_000L, only.createdAt)
    }


    @Test fun `asking again while a request is pending changes nothing`() {
        store.put(pairing("c1", PairingStatus.PENDING))
        store.plantCode("111111", "w", expiresAt = 99_000_000L)
        clock.now = 9_000_000L
        assertEquals(SubmitResult.AlreadyPending, lifecycle.submitCode("c1", "Maya", "111111"))
        assertEquals(1L, store.pairingsOfWearer("w").single().createdAt)
    }


    @Test fun `asking when already connected reports connected and changes nothing`() {
        store.put(pairing("c1", PairingStatus.APPROVED))
        store.plantCode("111111", "w", expiresAt = 99_000_000L)
        assertEquals(SubmitResult.AlreadyConnected, lifecycle.submitCode("c1", "Maya", "111111"))
        assertEquals(PairingStatus.APPROVED, store.pairingsOfWearer("w").single().status)
    }
}
