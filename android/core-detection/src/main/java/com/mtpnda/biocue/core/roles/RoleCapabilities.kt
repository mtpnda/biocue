package com.mtpnda.biocue.core.roles

import com.mtpnda.biocue.core.link.LinkRole

/** Who an account is. Names match the strings stored in Firestore `users/{uid}.role`. */
enum class AccountRole { WEARER, CAREGIVER }

/**
 * What each role may do — the single source of truth for the in-app guards (Android + iOS),
 * the Firestore security rules (server-side mirror) and the matrix in docs/SPEC.md.
 * Pinned by RoleCapabilitiesTest here and RoleCapabilitiesTests.swift on iOS.
 */
data class RoleCapabilities(
    /** Open a link to the watch and read raw vitals. */
    val linkWatch: Boolean,
    /** Create/close events and write them to the cloud. */
    val createEvents: Boolean,
    /** Show a pairing code; approve / deny / remove caregivers. */
    val manageCaregivers: Boolean,
    /** Enter a wearer's pairing code and request access. */
    val requestPairing: Boolean,
    /** Read a paired wearer's events. */
    val readPairedWearerEvents: Boolean,
    /** Change label / resolved on a paired wearer's events (and nothing else on them). */
    val labelPairedWearerEvents: Boolean,
)

fun AccountRole.capabilities(): RoleCapabilities = when (this) {
    AccountRole.WEARER -> RoleCapabilities(
        linkWatch = true, createEvents = true, manageCaregivers = true,
        requestPairing = false, readPairedWearerEvents = false, labelPairedWearerEvents = false,
    )
    AccountRole.CAREGIVER -> RoleCapabilities(
        linkWatch = false, createEvents = false, manageCaregivers = false,
        requestPairing = true, readPairedWearerEvents = true, labelPairedWearerEvents = true,
    )
}

fun AccountRole.linkRole(): LinkRole = when (this) {
    AccountRole.WEARER -> LinkRole.WEARER
    AccountRole.CAREGIVER -> LinkRole.CAREGIVER
}

class RoleNotPermittedException(val role: AccountRole, val action: String) :
    IllegalStateException("$role is not permitted to: $action")

/** Fail loudly (in development) when a wrong-role code path runs, instead of silently doing the wrong thing. */
class RoleGate(val role: AccountRole) {
    fun require(action: String, capability: (RoleCapabilities) -> Boolean) {
        if (!capability(role.capabilities())) throw RoleNotPermittedException(role, action)
    }
    fun allows(capability: (RoleCapabilities) -> Boolean): Boolean = capability(role.capabilities())
}
