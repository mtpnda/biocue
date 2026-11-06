package com.mtpnda.biocue.core.link

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Random

class LinkFramingTest {

    private fun hex(s: String): ByteArray = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    @Test fun `chunks match the Python reference byte for byte`() {
        for (c in LinkGoldenVectors.chunkCases) {
            val frame = hex(LinkGoldenVectors.all.first { it.name == c.frameName }.hex)
            val chunks = LinkFraming.split(frame, c.maxChunk).map { it.toHex() }
            assertEquals("${c.frameName}@${c.maxChunk}", c.chunksHex, chunks)
        }
    }

    @Test fun `a frame that fits is a single chunk flagged first and last`() {
        val chunks = LinkFraming.split(byteArrayOf(1, 2, 3), 20)
        assertEquals(1, chunks.size)
        assertEquals(0x03, chunks[0][0].toInt())
    }

    @Test fun `no chunk ever exceeds the negotiated size`() {
        val frame = ByteArray(500) { it.toByte() }
        for (max in 2..60) for (c in LinkFraming.split(frame, max)) assertEquals(true, c.size <= max)
    }

    @Test fun `split then reassemble is lossless for every size`() {
        val rnd = Random(7)
        for (len in 1..300) for (max in listOf(2, 3, 5, 20, 23, 64, 185, 512)) {
            val frame = ByteArray(len).also { rnd.nextBytes(it) }
            val r = LinkReassembler()
            var out: ByteArray? = null
            for (chunk in LinkFraming.split(frame, max)) out = r.accept(chunk) ?: out
            assertArrayEquals("len=$len max=$max", frame, out)
        }
    }

    @Test fun `back to back frames reassemble independently`() {
        val r = LinkReassembler()
        val a = ByteArray(45) { 1 }; val b = ByteArray(7) { 2 }
        val got = mutableListOf<ByteArray>()
        for (c in LinkFraming.split(a, 20) + LinkFraming.split(b, 20)) r.accept(c)?.let { got += it }
        assertEquals(2, got.size); assertArrayEquals(a, got[0]); assertArrayEquals(b, got[1])
    }

    @Test fun `orphan chunk with no first is dropped and counted`() {
        val r = LinkReassembler()
        assertNull(r.accept(byteArrayOf(0x02, 9, 9)))
        assertEquals(1, r.droppedChunks)
    }

    @Test fun `a new FIRST discards a half-received frame`() {
        val r = LinkReassembler()
        val chunks = LinkFraming.split(ByteArray(50) { 5 }, 20)
        r.accept(chunks[0])                       // start frame A, never finish it
        val b = byteArrayOf(7, 7, 7)
        val out = LinkFraming.split(b, 20).map { r.accept(it) }.last()
        assertArrayEquals(b, out)                 // frame B is clean, not "A+B"
    }

    @Test fun `oversize frame is dropped and the reassembler recovers`() {
        val r = LinkReassembler(maxFrameBytes = 40)
        var out: ByteArray? = null
        for (c in LinkFraming.split(ByteArray(100), 20)) out = r.accept(c) ?: out
        assertNull(out)
        assertEquals(true, r.droppedChunks > 0)
        val ok = byteArrayOf(1, 2, 3)
        assertNotNull(LinkFraming.split(ok, 20).map { r.accept(it) }.last())
    }

    @Test fun `empty chunk is tolerated`() {
        val r = LinkReassembler()
        assertNull(r.accept(ByteArray(0)))
        assertEquals(1, r.droppedChunks)
    }

    @Test fun `full pipeline encode split reassemble decode for every golden vector`() {
        for (v in LinkGoldenVectors.all) {
            if (v.message is LinkMessage.Unknown) continue
            val frame = LinkCodec.encode(v.seq, v.message)
            for (max in listOf(20, 23, 185)) {
                val r = LinkReassembler(); var out: ByteArray? = null
                for (c in LinkFraming.split(frame, max)) out = r.accept(c) ?: out
                assertEquals(v.message, LinkCodec.decode(out!!).message)
            }
        }
    }
}
