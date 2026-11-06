package com.mtpnda.biocue.core.pairing

class FakeClock(var now: Long = 1_000_000L) : Clock { override fun nowMs() = now }

class InMemoryPairingStore : PairingStore {
    private val pairings = LinkedHashMap<String, Pairing>()
    fun put(p: Pairing) { pairings[p.id] = p }
    override fun pairingsOfWearer(wearerUid: String) = pairings.values.filter { it.wearerUid == wearerUid }
    override fun deletePairing(pairingId: String) { pairings.remove(pairingId) }
    override fun markRemoved(pairingId: String, removedAt: Long) { pairings[pairingId]?.let { pairings[pairingId] = it.copy(status = PairingStatus.REMOVED, removedAt = removedAt) } }
    override fun markApproved(pairingId: String) { pairings[pairingId]?.let { pairings[pairingId] = it.copy(status = PairingStatus.APPROVED) } }
    override fun lookupCode(code: String) = codes[code]
    /** Contract of the real boundary: create fails on an existing document (the rules forbid a caregiver overwriting one). */
    override fun createPairing(pairing: Pairing) { check(!pairings.containsKey(pairing.id)) { "create on existing document ${pairing.id}" }; pairings[pairing.id] = pairing }
    override fun pairing(pairingId: String) = pairings[pairingId]
    override fun reRequest(pairingId: String, caregiverName: String, createdAt: Long) { pairings[pairingId]?.let { pairings[pairingId] = it.copy(status = PairingStatus.PENDING, removedAt = null, caregiverName = caregiverName, createdAt = createdAt) } }
    private val codes = HashMap<String, CodeRecord>()
    override fun issueCode(wearerUid: String, expiresAt: Long): String { codes["123456"] = CodeRecord(wearerUid, "Wearer", expiresAt); return "123456" }
    override fun consumeCodes(wearerUid: String) { codes.values.removeAll { it.wearerUid == wearerUid } }
    fun isCodeLive(code: String) = codes.containsKey(code)
    fun plantCode(code: String, wearerUid: String, expiresAt: Long) { codes[code] = CodeRecord(wearerUid, "Wearer", expiresAt) }
}

fun pairing(caregiver: String, status: PairingStatus, wearer: String = "w", removedAt: Long? = null) =
    Pairing("${wearer}_$caregiver", wearer, caregiver, "Care $caregiver", "Wearer", status, 1L, removedAt)
