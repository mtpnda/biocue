package com.mtpnda.biocue.core.link

import java.io.File

/** Saves the outbox to one small file on the watch. Written atomically; corruption is detected, never trusted. */
class FileOutboxStorage(private val file: File) : OutboxStorage {
    override fun save(frames: List<StoredFrame>) {
        val out = java.io.ByteArrayOutputStream()
        val d = java.io.DataOutputStream(out)
        d.writeInt(MAGIC); d.writeShort(frames.size)
        for (f in frames) { d.writeShort(f.seq); d.writeShort(f.bytes.size); d.write(f.bytes) }
        val body = out.toByteArray()
        val crc = java.util.zip.CRC32().also { it.update(body) }.value.toInt()
        val tmp = File(file.path + ".tmp")
        tmp.writeBytes(body + java.nio.ByteBuffer.allocate(4).putInt(crc).array())
        check(tmp.renameTo(file) || (file.delete() && tmp.renameTo(file))) { "could not replace outbox file" }
    }

    override fun load(): List<StoredFrame> = try { read() } catch (e: java.io.IOException) { emptyList() }

    private fun read(): List<StoredFrame> {
        if (!file.exists()) return emptyList()
        val all = file.readBytes()
        if (all.size < 10) return emptyList()
        val body = all.copyOf(all.size - 4)
        val stored = java.nio.ByteBuffer.wrap(all, all.size - 4, 4).int
        if (java.util.zip.CRC32().also { it.update(body) }.value.toInt() != stored) return emptyList()
        val d = java.io.DataInputStream(body.inputStream())
        if (d.readInt() != MAGIC) return emptyList()
        return List(d.readUnsignedShort()) {
            val seq = d.readUnsignedShort(); val len = d.readUnsignedShort()
            StoredFrame(seq, ByteArray(len).also { b -> d.readFully(b) })
        }
    }

    private companion object { const val MAGIC = 0x42434F42 } // "BCOB"
}
