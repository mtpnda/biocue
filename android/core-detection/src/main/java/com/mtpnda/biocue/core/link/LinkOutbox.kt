package com.mtpnda.biocue.core.link

import com.mtpnda.biocue.core.link.LinkProtocol as P

/**
 * Watch-side store-and-forward for CRITICAL frames (event start/label/end, SOS location).
 *
 * The Data Layer queued DataItems while the phone was away. BLE does not, so the watch keeps
 * un-acknowledged critical frames here and re-sends them (same bytes, same seq) after the next
 * successful handshake. Receivers therefore see AT-LEAST-ONCE delivery and must be idempotent.
 *
 * Bounded: when full the OLDEST frame is dropped (and counted) so a phone that is away for a
 * day cannot grow watch memory without limit. In-memory only — a watch reboot loses pending frames.
 */
/** One un-ACKed frame, as saved so it survives a watch reboot. */
class StoredFrame(val seq: Int, val bytes: ByteArray)

/** Boundary: where the outbox is saved (a file on the watch). Faked in tests. */
interface OutboxStorage {
    fun load(): List<StoredFrame>
    fun save(frames: List<StoredFrame>)
}

class LinkOutbox(
    private val capacity: Int = P.CRITICAL_OUTBOX_CAP,
    private val storage: OutboxStorage? = null,
) {
    private val pending = LinkedHashMap<Int, ByteArray>()
    private var nextSeq = 0

    init {
        runCatching { storage?.load() }.getOrNull()?.forEach { add(it.seq, it.bytes) }
        pending.keys.maxOrNull()?.let { nextSeq = (it + 1) and 0xFFFF }
    }

    private fun persist() { runCatching { storage?.save(pending.map { StoredFrame(it.key, it.value) }) } }

    /** Frames discarded because the outbox was full. */
    var droppedCount: Int = 0
        private set

    /** Allocates the next 16-bit sequence number (wraps). */
    @Synchronized fun allocateSeq(): Int { val s = nextSeq; nextSeq = (nextSeq + 1) and 0xFFFF; return s }

    @Synchronized fun add(seq: Int, frame: ByteArray) {
        if (pending.size >= capacity) {
            val oldest = pending.keys.first()
            pending.remove(oldest)
            droppedCount++
        }
        pending[seq] = frame
        persist()
    }

    /** @return true if [seq] was pending (i.e. this ACK mattered). */
    @Synchronized fun ack(seq: Int): Boolean {
        val removed = pending.remove(seq) != null
        if (removed) persist()
        return removed
    }

    /** Un-acked frames, oldest first. */
    @Synchronized fun pendingFrames(): List<ByteArray> = pending.values.toList()

    @Synchronized fun size(): Int = pending.size
}
