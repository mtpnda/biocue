package com.mtpnda.biocue.wear.link

import android.content.Context

/**
 * Which kind of phone this watch talks to. Exactly one at a time — if both an Android phone
 * and an iPhone were fed the same watch, both would record the same events.
 *
 *  ANDROID -> Wearable Data Layer (Google Play services), the original transport
 *  IPHONE  -> BioCue Link v1 over BLE (the Data Layer cannot reach iOS)
 */
enum class PhoneTarget { ANDROID, IPHONE }

class PhoneTargetStore(private val context: Context) {
    /**
     * Defensive on purpose: a bad/absent value must fall back to ANDROID so the existing
     * behaviour (and the existing unit tests that use a mocked Context) is unchanged.
     */
    fun get(): PhoneTarget = runCatching {
        val raw = prefs().getString(KEY, null)
        if (raw.isNullOrEmpty()) PhoneTarget.ANDROID else PhoneTarget.valueOf(raw)
    }.getOrDefault(PhoneTarget.ANDROID)

    fun set(target: PhoneTarget) {
        runCatching { prefs().edit().putString(KEY, target.name).apply() }
    }

    private fun prefs() = context.getSharedPreferences("biocue_link", Context.MODE_PRIVATE)

    private companion object { const val KEY = "phone_target" }
}
