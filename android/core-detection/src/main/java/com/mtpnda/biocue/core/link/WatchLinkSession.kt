package com.mtpnda.biocue.core.link

import com.mtpnda.biocue.core.link.LinkProtocol as P

/**
 * The watch's side of one BLE connection, as a pure state machine (no Android classes) so every
 * rule is unit-testable on a JVM. [com.mtpnda.biocue.wear.link.BleLinkServer] feeds it bytes and
 * performs the I/O it asks for.
 *
 * Rules enforced here:
 *  - The remote must say HELLO first. Until it does, nothing but the handshake flows.
 *  - Only a WEARER's phone is accepted. A CAREGIVER phone gets HELLO_ACK(ROLE_NOT_ALLOWED) and is dropped.
 *  - Unsupported protocol versions are refused.
 *  - A phone that is neither bonded nor inside the pairing window is refused.
 *  - Critical frames are retained until ACKed and replayed after the next successful handshake.
 *  - Non-critical telemetry is never queued: if nobody is listening it is dropped (latest-wins).
 */
class WatchLinkSession(
    private val watchName: String,
    private val batteryPercent: () -> Int,
    private val remoteIsBonded: () -> Boolean,
    private val pairingWindowOpen: () -> Boolean,
    val outbox: LinkOutbox = LinkOutbox(),
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    enum class State { CONNECTED_AWAITING_HELLO, ESTABLISHED, REJECTED, DISCONNECTED }

    /** What the caller must do after feeding the session an event. */
    class Output(
        /** Complete (un-chunked) frames to notify to the phone, in order. */
        val frames: List<ByteArray> = emptyList(),
        /** Drop the connection after sending [frames]. */
        val disconnect: Boolean = false,
        /** Phone asked for a GPS fix for an SOS. */
        val sosGpsRequested: Boolean = false,
    ) {
        companion object { val NONE = Output() }
    }

    var state: State = State.DISCONNECTED
        private set

    /** Reason the last HELLO was refused, for the UI / logs. */
    var lastRejection: HelloStatus? = null
        private set

    fun onConnected() {
        state = State.CONNECTED_AWAITING_HELLO
        lastRejection = null
    }

    fun onDisconnected() { state = State.DISCONNECTED }

    /** The caller's HELLO timer fired without a HELLO. */
    fun onHelloTimeout(): Output {
        if (state != State.CONNECTED_AWAITING_HELLO) return Output.NONE
        state = State.REJECTED
        return Output(disconnect = true)
    }

    fun onDownlink(frame: DecodedFrame): Output {
        if (state == State.DISCONNECTED || state == State.REJECTED) return Output.NONE
        return when (val m = frame.message) {
            is LinkMessage.Hello -> onHello(m)
            is LinkMessage.Ack -> { outbox.ack(m.ackedSeq); Output.NONE }
            LinkMessage.Ping -> if (state == State.ESTABLISHED) Output(listOf(encode(LinkMessage.Pong))) else Output.NONE
            LinkMessage.SosGpsRequest -> if (state == State.ESTABLISHED) Output(sosGpsRequested = true) else Output.NONE
            else -> Output.NONE // uplink-only or unknown types sent the wrong way: ignore
        }
    }

    /**
     * The watch app has something to send.
     * Critical messages are always retained; they go out immediately only if the link is established.
     */
    fun submit(message: LinkMessage): Output {
        val seq = outbox.allocateSeq()
        val frame = LinkCodec.encode(seq, message)
        if (message.isCritical) outbox.add(seq, frame)
        return if (state == State.ESTABLISHED) Output(listOf(frame)) else Output.NONE
    }

    private fun onHello(hello: LinkMessage.Hello): Output {
        val status = when {
            hello.role != LinkRole.WEARER -> HelloStatus.ROLE_NOT_ALLOWED
            hello.maxVersion < P.VERSION -> HelloStatus.VERSION_UNSUPPORTED
            !remoteIsBonded() && !pairingWindowOpen() -> HelloStatus.NOT_IN_PAIRING_WINDOW
            else -> HelloStatus.OK
        }
        val ack = encode(LinkMessage.HelloAck(P.VERSION, status, watchName, batteryPercent(), nowMs()))
        if (status != HelloStatus.OK) {
            state = State.REJECTED
            lastRejection = status
            return Output(listOf(ack), disconnect = true)
        }
        state = State.ESTABLISHED
        // Handshake first, then replay everything the phone has not confirmed yet.
        return Output(listOf(ack) + outbox.pendingFrames())
    }

    private fun encode(m: LinkMessage): ByteArray = LinkCodec.encode(outbox.allocateSeq(), m)
}
