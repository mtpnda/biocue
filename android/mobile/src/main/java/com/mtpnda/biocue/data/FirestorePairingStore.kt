package com.mtpnda.biocue.data

import com.mtpnda.biocue.core.pairing.CodeRecord
import com.mtpnda.biocue.core.pairing.PairingStore
import com.mtpnda.biocue.core.pairing.Review
import com.mtpnda.biocue.core.pairing.ReviewStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.mtpnda.biocue.core.pairing.Pairing as DomainPairing
import com.mtpnda.biocue.core.pairing.PairingStatus as DomainStatus

/**
 * The real boundary behind the (test-first, fully unit-tested) pairing domain in :core-detection.
 * The domain is synchronous; callers MUST be off the main thread (the view models use Dispatchers.IO).
 * NOT unit-tested here (needs Firestore) — it only translates; all the rules live in the domain.
 */
class FirestorePairingStore(private val repo: FirestoreRepository = FirestoreRepository) : PairingStore, ReviewStore {

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }

    override fun pairingsOfWearer(wearerUid: String): List<DomainPairing> =
        io { repo.getCaregiversForWearer(wearerUid).first() }.map { it.toDomain() }

    override fun issueCode(wearerUid: String, expiresAt: Long): String =
        io { repo.updatePairingCode(wearerUid, "000000") } ?: error("could not allocate a pairing code")

    override fun lookupCode(code: String): CodeRecord? = io { repo.lookupCode(code) }

    override fun consumeCodes(wearerUid: String) = io { repo.consumeCodes(wearerUid) }

    override fun deletePairing(pairingId: String) = io { repo.deletePairing(pairingId) }

    override fun markRemoved(pairingId: String, removedAt: Long) = io { repo.markRemoved(pairingId, removedAt) }

    override fun markApproved(pairingId: String) = io { repo.updatePairingStatus(pairingId, PairingStatus.APPROVED) }

    override fun createPairing(pairing: DomainPairing) = io { repo.createPairing(pairing.toData()) }

    override fun pairing(pairingId: String): DomainPairing? = io { repo.getPairing(pairingId) }?.toDomain()

    override fun reRequest(pairingId: String, caregiverName: String, createdAt: Long) =
        io { repo.reRequestPairing(pairingId, caregiverName, createdAt) }

    override fun writeReview(review: Review) = io { repo.writeReview(review) }
}

fun Pairing.toDomain() = DomainPairing(id, wearerUid, caregiverUid, caregiverName, wearerName,
    DomainStatus.valueOf(status.name), createdAt, removedAt)

fun DomainPairing.toData() = Pairing(id, wearerUid, caregiverUid, caregiverName, wearerName,
    PairingStatus.valueOf(status.name), createdAt, removedAt)
