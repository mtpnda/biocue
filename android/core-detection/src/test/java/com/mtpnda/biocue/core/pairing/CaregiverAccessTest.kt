package com.mtpnda.biocue.core.pairing

import org.junit.Assert.assertEquals
import org.junit.Test

class CaregiverAccessTest {
    private val access = CaregiverAccess()

    @Test fun `when the wearer removes the caregiver the cache is wiped and access is revoked`() {
        access.onPairingsObserved(listOf(pairing("c1", PairingStatus.APPROVED)))
        val update = access.onPairingsObserved(listOf(pairing("c1", PairingStatus.REMOVED, removedAt = 5L)))
        assertEquals(AccessState.Revoked, update.state)
        assertEquals(listOf(AccessEffect.WIPE_LOCAL_CACHE), update.effects)
    }


    @Test fun `a caregiver who never had access has nothing revoked and nothing wiped`() {
        val update = access.onPairingsObserved(emptyList())
        assertEquals(AccessState.None, update.state)
        assertEquals(emptyList<AccessEffect>(), update.effects)
    }


    @Test fun `a pending request is reported as waiting for the wearer`() {
        val update = access.onPairingsObserved(listOf(pairing("c1", PairingStatus.PENDING)))
        assertEquals(AccessState.Pending("Wearer"), update.state)
        assertEquals(emptyList<AccessEffect>(), update.effects)
    }


    @Test fun `if the approved pairing simply disappears access is revoked and the cache wiped`() {
        access.onPairingsObserved(listOf(pairing("c1", PairingStatus.APPROVED)))
        val update = access.onPairingsObserved(emptyList())
        assertEquals(AccessState.Revoked, update.state)
        assertEquals(listOf(AccessEffect.WIPE_LOCAL_CACHE), update.effects)
    }


    @Test fun `the same removal seen twice wipes only once`() {
        access.onPairingsObserved(listOf(pairing("c1", PairingStatus.APPROVED)))
        val removed = listOf(pairing("c1", PairingStatus.REMOVED, removedAt = 5L))
        access.onPairingsObserved(removed)
        val again = access.onPairingsObserved(removed)
        assertEquals(AccessState.Revoked, again.state)
        assertEquals(emptyList<AccessEffect>(), again.effects)
    }


    @Test fun `after the caregiver acknowledges the revocation the screen returns to neutral`() {
        access.onPairingsObserved(listOf(pairing("c1", PairingStatus.APPROVED)))
        val removed = listOf(pairing("c1", PairingStatus.REMOVED, removedAt = 5L))
        access.onPairingsObserved(removed)
        access.acknowledgeRevocation()
        val after = access.onPairingsObserved(removed)
        assertEquals(AccessState.None, after.state)
        assertEquals(emptyList<AccessEffect>(), after.effects)
    }


    @Test fun `opening the app after a removal still revokes and wipes the cache`() {
        val update = CaregiverAccess().onPairingsObserved(listOf(pairing("c1", PairingStatus.REMOVED, removedAt = 5L)))
        assertEquals(AccessState.Revoked, update.state)
        assertEquals(listOf(AccessEffect.WIPE_LOCAL_CACHE), update.effects)
    }


    @Test fun `a dismissed removal does not come back after the app restarts`() {
        val removed = listOf(pairing("c1", PairingStatus.REMOVED, removedAt = 5L))
        val first = CaregiverAccess()
        first.onPairingsObserved(removed)
        val persisted = first.acknowledgeRevocation()
        val afterRestart = CaregiverAccess(persisted).onPairingsObserved(removed)
        assertEquals(AccessState.None, afterRestart.state)
        assertEquals(emptyList<AccessEffect>(), afterRestart.effects)
    }
}
