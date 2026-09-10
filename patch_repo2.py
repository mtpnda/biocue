with open('mobile/src/main/java/com/mtpnda/biocue/data/FirestoreRepository.kt', 'r') as f:
    content = f.read()

new_find = """    suspend fun findWearerByCode(code: String): UserProfile? {
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
        
        // Fallback for older method if the active_codes collection wasn't used yet (backward compatibility during transition)
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
    }"""

start_idx = content.find("suspend fun findWearerByCode")
end_idx = content.find("suspend fun requestPairing") # Wait, findWearerByCode is after requestPairing?
import re
# Let's just find findWearerByCode and replace until the next fun
pattern = re.compile(r"suspend fun findWearerByCode.*?return null\n    }", re.DOTALL)
content = pattern.sub(new_find, content)

with open('mobile/src/main/java/com/mtpnda/biocue/data/FirestoreRepository.kt', 'w') as f:
    f.write(content)
print("Updated findWearerByCode.")
