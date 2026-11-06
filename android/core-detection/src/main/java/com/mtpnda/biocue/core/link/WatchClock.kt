package com.mtpnda.biocue.core.link

/**
 * Converts timestamps written by the WATCH's clock into the PHONE's clock.
 * The phone mixed the two (event times from the watch, "now" from the phone), so a watch clock off by more
 * than a few minutes broke the "is this event still ongoing?" rule. The offset is measured once per
 * connection from HELLO_ACK.watchTimeMs; the one-way Bluetooth delay (tens of ms) is ignored on purpose.
 */
class WatchClock private constructor(private val offsetMs: Long) {
    fun toPhoneTime(watchTimestampMs: Long): Long = watchTimestampMs + offsetMs

    companion object {
        fun fromHello(watchTimeMs: Long, phoneNowMs: Long): WatchClock = WatchClock(phoneNowMs - watchTimeMs)
    }
}
