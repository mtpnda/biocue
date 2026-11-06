package com.mtpnda.biocue.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random
import android.util.Log

object FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun createUserProfile(uid: String, role: UserRole, displayName: String) {
        val userProfile = UserProfile(uid, role, displayName)
        db.collection("users").document(uid).set(userProfile).await()
    }

    fun getUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

        suspend fun updatePairingCode(wearerUid: String, code: String?): String? {
        if (code == null) {
            db.collection("users").document(wearerUid).update(
                "pairingCode", null,
                "pairingCodeExpiresAt", null
            ).await()
            return null
        } else {
            var uniqueCode: String? = null
            var success = false
            var attempts = 0
            
            while (!success && attempts < 10) {
                attempts++
                val candidateCode = String.format("%06d", kotlin.random.Random.nextInt(1000000))
                
                try {
                    val isUnique = db.runTransaction { transaction ->
                        // Query not allowed in transaction, but we can do a get() on a specific doc!
                        // Wait, users don't have document IDs as pairing codes.
                        // We must enforce this by using a separate collection "active_codes" where doc ID = pairingCode.
                        val codeRef = db.collection("active_codes").document(candidateCode)
                        val snapshot = transaction.get(codeRef)
                        
                        val now = System.currentTimeMillis()
                        if (!snapshot.exists() || (snapshot.getLong("expiresAt") ?: 0L) < now) {
                            // It's unique or expired, we can claim it.
                            val expiresAt = now + 10 * 60 * 1000 // 10 min
                            
                            val codeData = hashMapOf(
                                "wearerUid" to wearerUid,
                                "expiresAt" to expiresAt
                            )
                            transaction.set(codeRef, codeData)
                            
                            val userRef = db.collection("users").document(wearerUid)
                            transaction.update(userRef, "pairingCode", candidateCode, "pairingCodeExpiresAt", expiresAt)
                            
                            candidateCode
                        } else {
                            null
                        }
                    }.await()
                    
                    if (isUnique != null) {
                        uniqueCode = isUnique
                        success = true
                    }
                } catch (e: Exception) {
                    // Transaction failed, try again
                }
            }
            return uniqueCode
        }
    }

    suspend fun findWearerByCode(code: String): UserProfile? {
        val now = System.currentTimeMillis()
        val codeDoc = db.collection("active_codes").document(code).get().await()
        if (codeDoc.exists()) {
            val expiresAt = codeDoc.getLong("expiresAt") ?: 0L
            if (expiresAt > now) {
                val wearerUid = codeDoc.getString("wearerUid")
                if (wearerUid != null) {
                    val userDoc = db.collection("users").document(wearerUid).get().await()
                    return userDoc.toObject(UserProfile::class.java)
                }
            }
        }
        return null
    }

    suspend fun requestPairing(wearerUid: String, caregiverUid: String, caregiverName: String, wearerName: String): Boolean {
        // We cannot check if wearer has < 2 caregivers here because Caregiver doesn't have read access to other pairings.
        // We just create the PENDING request, and the wearer can decide whether to approve it.
        val pairingId = "${wearerUid}_${caregiverUid}"
        val pairing = Pairing(
            id = pairingId,
            wearerUid = wearerUid,
            caregiverUid = caregiverUid,
            caregiverName = caregiverName,
            wearerName = wearerName,
            status = PairingStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
        
        db.collection("pairings").document(pairingId).set(pairing).await()
        return true
    }

    suspend fun updatePairingStatus(pairingId: String, status: PairingStatus) {
        db.collection("pairings").document(pairingId).update("status", status.name).await()
    }

    fun getCaregiversForWearer(wearerUid: String): Flow<List<Pairing>> = callbackFlow {
        val listener = db.collection("pairings")
            .whereEqualTo("wearerUid", wearerUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val pairings = snapshot.documents.mapNotNull { it.toObject(Pairing::class.java) }
                    trySend(pairings)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getPairedWearer(caregiverUid: String): Flow<Pairing?> = callbackFlow {
        val listener = db.collection("pairings")
            .whereEqualTo("caregiverUid", caregiverUid)
            .whereEqualTo("status", PairingStatus.APPROVED.name)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val pairing = snapshot.documents[0].toObject(Pairing::class.java)
                    trySend(pairing)
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun syncEvent(wearerUid: String, eventId: String, startTime: Long, endTime: Long?, triggerType: String, label: String, isResolved: Boolean) {
        val eventData = mapOf(
            "id" to eventId,
            "startTime" to startTime,
            "endTime" to endTime,
            "triggerType" to triggerType,
            "label" to label,
            "isResolved" to isResolved
        )
        db.collection("wearerData").document(wearerUid).collection("events").document(eventId).set(eventData).await()
    }

    fun observeWearerEvents(wearerUid: String, dayStart: Long, dayEnd: Long): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = db.collection("wearerData").document(wearerUid).collection("events")
            .whereGreaterThanOrEqualTo("startTime", dayStart)
            .whereLessThanOrEqualTo("startTime", dayEnd)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val events = snapshot.documents.mapNotNull { it.data }
                    trySend(events)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateEventLabelAndStatus(wearerUid: String, eventId: String, finalLabel: String, isResolved: Boolean) {
        try {
            db.collection("wearerData").document(wearerUid)
                .collection("events").document(eventId)
                .update(mapOf(
                    "label" to finalLabel,
                    "isResolved" to isResolved
                ))
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ======== spec 07 (as amended): direct lookup, single-use codes, removal, re-pairing, reviews ========

    /** Direct `get` of the code, then of the wearer's profile. There is NO list/query on users or active_codes. */
    suspend fun lookupCode(code: String): com.mtpnda.biocue.core.pairing.CodeRecord? {
        val codeDoc = db.collection("active_codes").document(code).get().await()
        if (!codeDoc.exists()) return null
        val wearerUid = codeDoc.getString("wearerUid") ?: return null
        val expiresAt = codeDoc.getLong("expiresAt") ?: 0L
        val name = db.collection("users").document(wearerUid).get().await().getString("displayName") ?: ""
        return com.mtpnda.biocue.core.pairing.CodeRecord(wearerUid, name, expiresAt)
    }

    /** Single-use: delete the code's lookup record and clear the profile fields. */
    suspend fun consumeCodes(wearerUid: String) {
        val userRef = db.collection("users").document(wearerUid)
        val code = userRef.get().await().getString("pairingCode")
        if (code != null) db.collection("active_codes").document(code).delete().await()
        userRef.update("pairingCode", null, "pairingCodeExpiresAt", null).await()
    }

    suspend fun getPairing(id: String): Pairing? = db.collection("pairings").document(id).get().await().toObject(Pairing::class.java)

    suspend fun createPairing(p: Pairing) { db.collection("pairings").document(p.id).set(p).await() }

    /** 07 Deny: the document is deleted. */
    suspend fun deletePairing(id: String) { db.collection("pairings").document(id).delete().await() }

    /** 07 Removal: status REMOVED plus removedAt. */
    suspend fun markRemoved(id: String, removedAt: Long) {
        db.collection("pairings").document(id).update("status", PairingStatus.REMOVED.name, "removedAt", removedAt).await()
    }

    /** 07 Re-pairing: same document, REMOVED -> PENDING, removedAt cleared. */
    suspend fun reRequestPairing(id: String, caregiverName: String, createdAt: Long) {
        db.collection("pairings").document(id).update(
            "status", PairingStatus.PENDING.name,
            "removedAt", com.google.firebase.firestore.FieldValue.delete(),
            "createdAt", createdAt,
            "caregiverName", caregiverName
        ).await()
    }

    /** ALL of a caregiver's pairings (any status), so a removal is actually observed. */
    fun getCaregiverPairings(caregiverUid: String): Flow<List<Pairing>> = callbackFlow {
        val listener = db.collection("pairings").whereEqualTo("caregiverUid", caregiverUid)
            .addSnapshotListener { snap, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.toObject(Pairing::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    /** 07 review submission: pairings/{pairingId}/reviews/{eventId}. */
    suspend fun writeReview(r: com.mtpnda.biocue.core.pairing.Review) {
        db.collection("pairings").document(r.pairingId).collection("reviews").document(r.eventId)
            .set(mapOf("label" to r.label, "isResolved" to r.isResolved, "updatedAt" to r.updatedAt)).await()
    }

    fun observeReviews(pairingId: String): Flow<Map<String, com.mtpnda.biocue.core.pairing.Review>> = callbackFlow {
        val listener = db.collection("pairings").document(pairingId).collection("reviews")
            .addSnapshotListener { snap, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val map = HashMap<String, com.mtpnda.biocue.core.pairing.Review>()
                snap?.documents?.forEach { d ->
                    val label = d.getString("label") ?: return@forEach
                    map[d.id] = com.mtpnda.biocue.core.pairing.Review(
                        pairingId, d.id, label, d.getBoolean("isResolved") ?: false, d.getLong("updatedAt") ?: 0L)
                }
                trySend(map)
            }
        awaitClose { listener.remove() }
    }
}
