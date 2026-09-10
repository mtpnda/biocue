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
        
        // Fallback for older method if the active_codes collection wasn't used yet
        val snapshot = db.collection("users")
            .whereEqualTo("pairingCode", code)
            .limit(1)
            .get()
            .await()
        
        if (!snapshot.isEmpty) {
            val profile = snapshot.documents[0].toObject(UserProfile::class.java)
            if (profile != null && profile.pairingCodeExpiresAt != null && profile.pairingCodeExpiresAt > now) {
                return profile
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
}
