package com.mtpnda.biocue.core.pairing

enum class PairingStatus { PENDING, APPROVED, REMOVED }

data class Pairing(
    val id: String, val wearerUid: String, val caregiverUid: String,
    val caregiverName: String, val wearerName: String,
    val status: PairingStatus, val createdAt: Long, val removedAt: Long? = null,
)

data class CodeRecord(val wearerUid: String, val wearerName: String, val expiresAt: Long)

fun interface Clock { fun nowMs(): Long }

/** Boundary to the cloud (Firestore on Android/iOS). Faked in tests. */
interface PairingStore {
    fun pairingsOfWearer(wearerUid: String): List<Pairing>
    fun deletePairing(pairingId: String)
    fun createPairing(pairing: Pairing)
    /** REMOVED → PENDING on the same document; clears removedAt. */
    fun reRequest(pairingId: String, caregiverName: String, createdAt: Long)
    fun pairing(pairingId: String): Pairing?
    fun markRemoved(pairingId: String, removedAt: Long)
    fun markApproved(pairingId: String)
    /** Claims a fresh unique 6-digit code for [wearerUid]; returns it. */
    fun issueCode(wearerUid: String, expiresAt: Long): String
    /** Direct lookup by code (no listing). */
    fun lookupCode(code: String): CodeRecord?
    /** Deletes every live code of [wearerUid] (lookup record and profile fields). */
    fun consumeCodes(wearerUid: String)
}

sealed interface ApprovalResult {
    object Approved : ApprovalResult
    object MaxCaregiversReached : ApprovalResult
}

sealed interface SubmitResult {
    object NotFound : SubmitResult
    object Requested : SubmitResult
    object AlreadyPending : SubmitResult
    object AlreadyConnected : SubmitResult
}

sealed interface CodeResult {
    object MaxCaregiversReached : CodeResult
    data class Issued(val code: String, val expiresAt: Long) : CodeResult
}

class PairingLifecycle(private val store: PairingStore, private val clock: Clock) {
    fun generateCode(wearerUid: String): CodeResult {
        val approved = store.pairingsOfWearer(wearerUid).count { it.status == PairingStatus.APPROVED }
        if (approved >= MAX_CAREGIVERS) return CodeResult.MaxCaregiversReached
        val expiresAt = clock.nowMs() + CODE_TTL_MS
        return CodeResult.Issued(store.issueCode(wearerUid, expiresAt), expiresAt)
    }

    fun deny(pairingId: String) = store.deletePairing(pairingId)

    fun remove(pairingId: String) = store.markRemoved(pairingId, clock.nowMs())

    fun approve(wearerUid: String, pairingId: String): ApprovalResult {
        val approved = store.pairingsOfWearer(wearerUid).count { it.status == PairingStatus.APPROVED }
        if (approved >= MAX_CAREGIVERS) return ApprovalResult.MaxCaregiversReached
        store.markApproved(pairingId)
        return ApprovalResult.Approved
    }

    fun submitCode(caregiverUid: String, caregiverName: String, code: String): SubmitResult {
        val record = store.lookupCode(code) ?: return SubmitResult.NotFound
        if (record.expiresAt <= clock.nowMs()) return SubmitResult.NotFound
        val id = "${record.wearerUid}_$caregiverUid"
        val existing = store.pairing(id)
        if (existing != null && existing.status == PairingStatus.REMOVED) {
            store.reRequest(id, caregiverName, clock.nowMs())
            return SubmitResult.Requested
        }
        if (existing != null && existing.status == PairingStatus.PENDING) return SubmitResult.AlreadyPending
        if (existing != null && existing.status == PairingStatus.APPROVED) return SubmitResult.AlreadyConnected
        store.createPairing(Pairing(id, record.wearerUid, caregiverUid, caregiverName, record.wearerName,
            PairingStatus.PENDING, clock.nowMs()))
        return SubmitResult.Requested
    }

    /** Call whenever the wearer's pairings change (snapshot listener). */
    fun onPairingsChanged(wearerUid: String) {
        if (store.pairingsOfWearer(wearerUid).any { it.status == PairingStatus.PENDING }) {
            store.consumeCodes(wearerUid)
        }
    }

    companion object {
        const val MAX_CAREGIVERS = 2
        const val CODE_TTL_MS = 10 * 60_000L
    }
}
