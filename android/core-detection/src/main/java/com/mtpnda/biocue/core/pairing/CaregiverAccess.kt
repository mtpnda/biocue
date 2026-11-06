package com.mtpnda.biocue.core.pairing

sealed interface AccessState {
    object None : AccessState
    data class Pending(val wearerName: String) : AccessState
    data class Connected(val pairing: Pairing) : AccessState
    object Revoked : AccessState
}

enum class AccessEffect { WIPE_LOCAL_CACHE }

data class AccessUpdate(val state: AccessState, val effects: List<AccessEffect>)

/** The caregiver's view of access to ONE wearer. Feed it every snapshot of the caregiver's own pairings. */
/** @param acknowledgedRemovalAt the value last returned by [acknowledgeRevocation] and persisted by the app (0 = none). */
class CaregiverAccess(acknowledgedRemovalAt: Long = 0L) {
    private var connected: Pairing? = null
    private var revoked = false
    private var lastRemovalAt = 0L
    private var acknowledgedRemovalAt = acknowledgedRemovalAt

    /** @return the marker to persist so a dismissed removal stays dismissed after a restart. */
    fun acknowledgeRevocation(): Long {
        revoked = false
        acknowledgedRemovalAt = maxOf(acknowledgedRemovalAt, lastRemovalAt)
        return acknowledgedRemovalAt
    }

    fun onPairingsObserved(pairings: List<Pairing>): AccessUpdate {
        val approved = pairings.firstOrNull { it.status == PairingStatus.APPROVED }
        if (approved != null) {
            connected = approved
            revoked = false
            return AccessUpdate(AccessState.Connected(approved), emptyList())
        }
        val pending = pairings.firstOrNull { it.status == PairingStatus.PENDING }
        if (pending != null && connected == null) return AccessUpdate(AccessState.Pending(pending.wearerName), emptyList())
        if (revoked) return AccessUpdate(AccessState.Revoked, emptyList())
        val removal = pairings.filter { it.status == PairingStatus.REMOVED }.maxOfOrNull { it.removedAt ?: 1L } ?: 0L
        if (connected == null && removal <= acknowledgedRemovalAt) return AccessUpdate(AccessState.None, emptyList())
        connected = null
        revoked = true
        lastRemovalAt = removal
        return AccessUpdate(AccessState.Revoked, listOf(AccessEffect.WIPE_LOCAL_CACHE))
    }
}
