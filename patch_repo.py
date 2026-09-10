import re

with open('mobile/src/main/java/com/mtpnda/biocue/data/FirestoreRepository.kt', 'r') as f:
    content = f.read()

new_updatePairingCode = """    suspend fun updatePairingCode(wearerUid: String, code: String?): String? {
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
    }"""

# Actually, I shouldn't just replace it with python regex if it's too long and might fail. Let's use standard replacement.
import sys
start_idx = content.find("suspend fun updatePairingCode")
end_idx = content.find("suspend fun requestPairing")

if start_idx != -1 and end_idx != -1:
    content = content[:start_idx] + new_updatePairingCode + "\n\n    " + content[end_idx:]
    with open('mobile/src/main/java/com/mtpnda/biocue/data/FirestoreRepository.kt', 'w') as f:
        f.write(content)
    print("Replaced updatePairingCode.")
else:
    print("Could not find function bounds.")
