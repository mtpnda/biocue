package com.mtpnda.biocue.core.link

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryOutboxStorage : OutboxStorage {
    var saved: List<StoredFrame> = emptyList()
    override fun load(): List<StoredFrame> = saved
    override fun save(frames: List<StoredFrame>) { saved = frames.map { StoredFrame(it.seq, it.bytes.copyOf()) } }
}

class LinkOutboxPersistenceTest {
    private val storage = MemoryOutboxStorage()

    private fun frames(box: LinkOutbox) = box.pendingFrames().map { it.toList() }

    @Test fun `un-acknowledged events survive a watch reboot`() {
        val before = LinkOutbox(storage = storage)
        before.add(before.allocateSeq(), byteArrayOf(1, 2, 3))
        before.add(before.allocateSeq(), byteArrayOf(4, 5))
        val afterReboot = LinkOutbox(storage = storage)
        assertEquals(listOf(listOf<Byte>(1, 2, 3), listOf<Byte>(4, 5)), frames(afterReboot))
    }


    @Test fun `an acknowledged event does not come back after a reboot`() {
        val before = LinkOutbox(storage = storage)
        val a = before.allocateSeq(); before.add(a, byteArrayOf(1))
        val b = before.allocateSeq(); before.add(b, byteArrayOf(2))
        before.ack(a)
        assertEquals(listOf(listOf<Byte>(2)), frames(LinkOutbox(storage = storage)))
    }


    @Test fun `sequence numbers continue after a reboot instead of restarting at zero`() {
        val before = LinkOutbox(storage = storage)
        before.add(before.allocateSeq(), byteArrayOf(1))   // seq 0
        before.add(before.allocateSeq(), byteArrayOf(2))   // seq 1
        assertEquals(2, LinkOutbox(storage = storage).allocateSeq())
    }


    @Test fun `an unreadable storage file starts empty instead of crashing`() {
        val broken = object : OutboxStorage {
            override fun load(): List<StoredFrame> = throw java.io.IOException("corrupt")
            override fun save(frames: List<StoredFrame>) {}
        }
        assertEquals(0, LinkOutbox(storage = broken).size())
    }


    @Test fun `a saved outbox larger than the cap keeps the newest frames and counts the dropped ones`() {
        storage.saved = (1..5).map { StoredFrame(it, byteArrayOf(it.toByte())) }
        val box = LinkOutbox(capacity = 3, storage = storage)
        assertEquals(listOf(listOf<Byte>(3), listOf<Byte>(4), listOf<Byte>(5)), frames(box))
        assertEquals(2, box.droppedCount)
    }


    @Test fun `if saving fails the event is still queued in memory`() {
        val unwritable = object : OutboxStorage {
            override fun load(): List<StoredFrame> = emptyList()
            override fun save(frames: List<StoredFrame>) { throw java.io.IOException("disk full") }
        }
        val box = LinkOutbox(storage = unwritable)
        box.add(box.allocateSeq(), byteArrayOf(9))
        assertEquals(listOf(listOf<Byte>(9)), frames(box))
    }
}
