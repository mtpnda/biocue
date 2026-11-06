package com.mtpnda.biocue.core.domain

/**
 * Lets an action happen at most once per [periodMs]. Used so a GPS fix is requested once per SOS (spec 03: GPS stays
 * off by default, one fix per ping) instead of on every 2-second poll, and so a caregiver is pushed once per event.
 */
class Cooldown(private val periodMs: Long) {
    private var last: Long? = null

    fun tryAcquire(nowMs: Long): Boolean {
        val previous = last
        if (previous != null && nowMs - previous < periodMs) return false
        last = nowMs
        return true
    }
}
