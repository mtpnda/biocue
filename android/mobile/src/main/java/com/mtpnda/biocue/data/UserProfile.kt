package com.mtpnda.biocue.data

enum class UserRole { WEARER, CAREGIVER }

data class UserProfile(
    val uid: String = "",
    val role: UserRole = UserRole.WEARER,
    val displayName: String = "",
    val pairingCode: String? = null,
    val pairingCodeExpiresAt: Long? = null
)
