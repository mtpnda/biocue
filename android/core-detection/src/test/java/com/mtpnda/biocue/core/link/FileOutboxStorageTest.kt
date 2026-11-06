package com.mtpnda.biocue.core.link

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileOutboxStorageTest {
    private val file: File = Files.createTempDirectory("outbox").resolve("outbox.bin").toFile()

    private fun sample() = listOf(StoredFrame(7, byteArrayOf(1, 2, 3)), StoredFrame(8, ByteArray(300) { it.toByte() }))
    private fun summary(l: List<StoredFrame>) = l.map { it.seq to it.bytes.toList() }

    @Test fun `saved frames are read back exactly by a new instance`() {
        FileOutboxStorage(file).save(sample())
        assertEquals(summary(sample()), summary(FileOutboxStorage(file).load()))
    }


    @Test fun `garbage or a truncated file loads as empty instead of throwing`() {
        file.writeBytes(byteArrayOf(0, 1, 2, 3, 4, 5))
        assertEquals(0, FileOutboxStorage(file).load().size)
        FileOutboxStorage(file).save(sample())
        file.writeBytes(file.readBytes().copyOf(20))               // power loss mid-write
        assertEquals(0, FileOutboxStorage(file).load().size)
    }


    @Test fun `a single corrupted byte is detected and the file is not trusted`() {
        FileOutboxStorage(file).save(sample())
        val bytes = file.readBytes(); bytes[12] = (bytes[12] + 1).toByte(); file.writeBytes(bytes)
        assertEquals(0, FileOutboxStorage(file).load().size)
    }
}
