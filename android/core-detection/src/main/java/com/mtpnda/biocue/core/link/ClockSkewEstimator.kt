package com.mtpnda.biocue.core.link

/**
 * Android Data Layer path: there is no handshake to measure how far the watch's clock is from the phone's, and stamping
 * messages "on receipt" is wrong for events (a DataItem can be delivered minutes late). So the offset is ESTIMATED from the
 * prompt telemetry windows: for each one, `receipt - watchTimestamp = skew + deliveryDelay`, and a delay is never
 * negative, so the SMALLEST recent value is the best estimate of the true skew (the same idea NTP uses). Only the last
 * [window] samples count, so a clock that is corrected or drifts is followed.
 */
class ClockSkewEstimator(private val window: Int = 9) {
    private val samples = ArrayDeque<Long>()
    private var offsetMs = 0L

    fun observe(watchTimestampMs: Long, phoneReceiptMs: Long) {
        samples.addLast(phoneReceiptMs - watchTimestampMs)
        while (samples.size > window) samples.removeFirst()
        offsetMs = samples.minOrNull() ?: 0L
    }

    fun toPhoneTime(watchTimestampMs: Long): Long = watchTimestampMs + offsetMs
}
