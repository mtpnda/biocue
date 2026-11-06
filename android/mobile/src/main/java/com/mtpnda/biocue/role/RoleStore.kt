package com.mtpnda.biocue.role

import android.content.Context
import com.mtpnda.biocue.data.UserRole

/**
 * Small on-device cache of this account's role, for code that cannot wait for Firestore
 * (notably [com.mtpnda.biocue.WatchDataReceiverService], which wakes without any UI).
 * Firestore stays the source of truth; AppViewModel refreshes this on every profile load
 * and clears it on sign-out.
 */
object RoleStore {
    private const val PREFS = "biocue_role"
    private const val KEY = "role"

    fun save(context: Context, role: UserRole) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, role.name).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    fun get(context: Context): UserRole? = parse(
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
    )

    internal fun parse(raw: String?): UserRole? = runCatching { UserRole.valueOf(raw ?: "") }.getOrNull()

    /**
     * Only an explicit CAREGIVER is refused. An unknown role (an existing install that has not yet
     * cached one) is allowed, so upgrading never silently drops a wearer's watch data.
     */
    fun mayReceiveWatchData(role: UserRole?): Boolean = role != UserRole.CAREGIVER
}
