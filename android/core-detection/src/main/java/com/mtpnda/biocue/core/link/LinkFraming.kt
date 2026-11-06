package com.mtpnda.biocue.core.link

import com.mtpnda.biocue.core.link.LinkProtocol as P

/**
 * Splits a frame into BLE-sized chunks and puts them back together.
 *
 * A BLE notification/write can carry at most (ATT MTU - 3) bytes — 20 at the
 * default MTU of 23. Each chunk is `flags_u8 + data...`, flags bit0 = FIRST,
 * bit1 = LAST (a frame that fits in one chunk has both set = 0x03).
 */
object LinkFraming {

    /** @param maxChunkBytes the largest single BLE payload (usually ATT MTU - 3), >= 2 */
    fun split(frame: ByteArray, maxChunkBytes: Int): List<ByteArray> {
        require(maxChunkBytes >= 2) { "chunk size must be >= 2" }
        require(frame.isNotEmpty()) { "empty frame" }
        val dataPer = maxChunkBytes - 1
        val chunks = ArrayList<ByteArray>((frame.size + dataPer - 1) / dataPer)
        var offset = 0
        while (offset < frame.size) {
            val n = minOf(dataPer, frame.size - offset)
            val chunk = ByteArray(1 + n)
            var flags = 0
            if (offset == 0) flags = flags or P.CHUNK_FIRST
            if (offset + n == frame.size) flags = flags or P.CHUNK_LAST
            chunk[0] = flags.toByte()
            System.arraycopy(frame, offset, chunk, 1, n)
            chunks.add(chunk)
            offset += n
        }
        return chunks
    }
}

/**
 * Stateful reassembler — one instance per direction per connection.
 * Robust against a lost/garbled start: a FIRST chunk always resets, and chunks
 * that arrive with no FIRST are counted and dropped instead of corrupting state.
 */
class LinkReassembler(private val maxFrameBytes: Int = P.MAX_FRAME_BYTES) {
    private var buffer = java.io.ByteArrayOutputStream()
    private var started = false

    /** Chunks discarded because they were orphaned or oversized. For diagnostics/tests. */
    var droppedChunks: Int = 0
        private set

    /** @return the complete frame when [chunk] carried the LAST flag, else null. */
    fun accept(chunk: ByteArray): ByteArray? {
        if (chunk.isEmpty()) { droppedChunks++; return null }
        val flags = chunk[0].toInt() and 0xFF
        if ((flags and P.CHUNK_FIRST) != 0) {
            buffer = java.io.ByteArrayOutputStream()
            started = true
        } else if (!started) {
            droppedChunks++
            return null
        }
        buffer.write(chunk, 1, chunk.size - 1)
        if (buffer.size() > maxFrameBytes) {
            reset(); droppedChunks++
            return null
        }
        if ((flags and P.CHUNK_LAST) != 0) {
            val frame = buffer.toByteArray()
            reset()
            return frame
        }
        return null
    }

    fun reset() { buffer = java.io.ByteArrayOutputStream(); started = false }
}
