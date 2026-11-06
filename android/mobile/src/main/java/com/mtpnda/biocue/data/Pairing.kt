package com.mtpnda.biocue.data

enum class PairingStatus { PENDING, APPROVED, REMOVED }

data class Pairing(
    val id: String = "",
    val wearerUid: String = "",
    val caregiverUid: String = "",
    val caregiverName: String = "",
    val wearerName: String = "",
    val status: PairingStatus = PairingStatus.PENDING,
    val createdAt: Long = 0L,
    /** Set when the wearer (or the caregiver) ends the pairing — spec 07. */
    val removedAt: Long? = null
)
