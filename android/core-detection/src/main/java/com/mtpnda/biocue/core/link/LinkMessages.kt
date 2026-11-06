package com.mtpnda.biocue.core.link

/**
 * Who is on the other end of the link. The watch only ever talks to a WEARER's
 * phone — a caregiver phone has no business with raw vitals — so the very first
 * message a phone sends (HELLO) declares its role and the watch enforces it.
 */
enum class LinkRole(val wire: Int) {
    WEARER(0),
    CAREGIVER(1);

    companion object {
        fun fromWire(v: Int): LinkRole? = values().firstOrNull { it.wire == v }
    }
}

enum class HelloStatus(val wire: Int) {
    OK(0),
    ROLE_NOT_ALLOWED(1),
    VERSION_UNSUPPORTED(2),
    NOT_IN_PAIRING_WINDOW(3);

    companion object {
        fun fromWire(v: Int): HelloStatus? = values().firstOrNull { it.wire == v }
    }
}

/** Every message that can cross the link, in either direction. */
sealed interface LinkMessage {

    /** Critical messages are retained by the sender until ACKed (see [isCritical]). */
    val isCritical: Boolean get() = false

    // ---- watch -> phone ------------------------------------------------------

    /** Latest-wins telemetry. Not acknowledged, not retained. */
    data class SensorWindow(
        val timestamp: Long,
        val hrMean: Double,
        val rmssd: Double,
        val accMag: Double,
        val repetitiveness: Double,
        val hrSlope: Double,
        val isOffBody: Boolean,
        val isExercising: Boolean,
    ) : LinkMessage

    data class EventStart(val timestamp: Long, val eventId: String, val isManual: Boolean) : LinkMessage {
        override val isCritical get() = true
    }

    data class EventLabel(val timestamp: Long, val eventId: String, val label: String) : LinkMessage {
        override val isCritical get() = true
    }

    data class EventEnd(val timestamp: Long, val eventId: String) : LinkMessage {
        override val isCritical get() = true
    }

    data class SosLocation(val timestamp: Long, val latitude: Double, val longitude: Double) : LinkMessage {
        override val isCritical get() = true
    }

    data class HelloAck(
        val version: Int,
        val status: HelloStatus,
        val watchName: String,
        /** 0..100, or 255 when unknown. */
        val batteryPercent: Int,
        /** The watch's wall clock when it answered, so the phone can convert watch timestamps to its own time. */
        val watchTimeMs: Long,
    ) : LinkMessage

    object Pong : LinkMessage { override fun toString() = "Pong" }

    // ---- phone -> watch ------------------------------------------------------

    data class Hello(val maxVersion: Int, val role: LinkRole) : LinkMessage
    data class Ack(val ackedSeq: Int) : LinkMessage
    object Ping : LinkMessage { override fun toString() = "Ping" }
    object SosGpsRequest : LinkMessage { override fun toString() = "SosGpsRequest" }

    /** A well-formed frame with a type this build does not know (forward compatibility). */
    data class Unknown(val type: Int) : LinkMessage
}

/** A decoded frame: the sequence number travels with the message so it can be ACKed. */
data class DecodedFrame(val seq: Int, val message: LinkMessage)

class LinkProtocolException(message: String) : Exception(message)
