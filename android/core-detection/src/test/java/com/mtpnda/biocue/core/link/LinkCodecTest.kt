package com.mtpnda.biocue.core.link

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Random

class LinkCodecTest {

    private fun hex(s: String): ByteArray = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    @Test fun `every golden vector decodes to the expected message`() {
        for (v in LinkGoldenVectors.all) {
            val decoded = LinkCodec.decode(hex(v.hex))
            assertEquals("seq of ${v.name}", v.seq, decoded.seq)
            assertEquals("message of ${v.name}", v.message, decoded.message)
        }
    }

    @Test fun `every golden vector encodes to the exact reference bytes`() {
        for (v in LinkGoldenVectors.all) {
            if (v.message is LinkMessage.Unknown) continue // cannot be produced by a sender
            assertEquals("bytes of ${v.name}", v.hex, LinkCodec.encode(v.seq, v.message).toHex())
        }
    }

    @Test fun `unknown message type is surfaced not fatal`() {
        val d = LinkCodec.decode(hex(LinkGoldenVectors.all.first { it.name == "unknown_type" }.hex))
        assertEquals(LinkMessage.Unknown(0x7E), d.message)
    }

    @Test fun `short frame is rejected`() = expectError { LinkCodec.decode(byteArrayOf(1, 1, 0)) }

    @Test fun `wrong version is rejected`() {
        val bytes = hex(LinkGoldenVectors.all.first { it.name == "ping" }.hex)
        bytes[0] = 2
        expectError { LinkCodec.decode(bytes) }
    }

    @Test fun `header length that disagrees with actual size is rejected`() {
        val bytes = hex(LinkGoldenVectors.all.first { it.name == "sensor_window" }.hex)
        expectError { LinkCodec.decode(bytes.copyOf(bytes.size - 1)) }
        expectError { LinkCodec.decode(bytes + byteArrayOf(0)) }
    }

    @Test fun `payload shorter than its type requires is rejected`() {
        // type SOS_LOCATION (needs 24 bytes) with a self-consistent 3 byte payload
        val frame = byteArrayOf(1, LinkProtocol.T_SOS_LOCATION.toByte(), 0, 1, 0, 3, 9, 9, 9)
        expectError { LinkCodec.decode(frame) }
    }

    @Test fun `string longer than 255 bytes cannot be encoded`() {
        expectError { LinkCodec.encode(1, LinkMessage.EventLabel(1L, "id", "x".repeat(256))) }
    }

    @Test fun `seq outside u16 is rejected`() {
        try { LinkCodec.encode(70000, LinkMessage.Ping); fail("expected IllegalArgumentException") }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun `unknown role and status bytes are rejected`() {
        expectError { LinkCodec.decode(byteArrayOf(1, LinkProtocol.T_HELLO.toByte(), 0, 0, 0, 2, 1, 9)) }
        expectError { LinkCodec.decode(byteArrayOf(1, LinkProtocol.T_HELLO_ACK.toByte(), 0, 0, 0, 5, 1, 9, 0, 0, 0)) }
    }

    @Test fun `random garbage never throws anything but a protocol error`() {
        val rnd = Random(42)
        repeat(5000) {
            val bytes = ByteArray(rnd.nextInt(80)).also { rnd.nextBytes(it) }
            // also try structurally valid headers around random payloads
            val framed = if (bytes.size >= 2 && rnd.nextBoolean()) {
                val payload = bytes.copyOfRange(2, bytes.size)
                byteArrayOf(1, bytes[1], 0, 7, (payload.size shr 8).toByte(), payload.size.toByte()) + payload
            } else bytes
            try { LinkCodec.decode(framed) } catch (_: LinkProtocolException) { /* fine */ }
        }
    }

    @Test fun `round trip with extreme values`() {
        val msgs = listOf(
            LinkMessage.SensorWindow(Long.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE, -0.0, 1e-300, -1e300, true, true),
            LinkMessage.SensorWindow(Long.MIN_VALUE, 0.0, 0.0, 0.0, 0.0, 0.0, false, false),
            LinkMessage.EventLabel(5L, "e".repeat(255), "l".repeat(255)),
            LinkMessage.HelloAck(1, HelloStatus.NOT_IN_PAIRING_WINDOW, "", 0, Long.MIN_VALUE),
            LinkMessage.Ack(0),
        )
        for (m in msgs) {
            val back = LinkCodec.decode(LinkCodec.encode(65535, m))
            assertEquals(65535, back.seq)
            assertEquals(m, back.message)
        }
    }

    @Test fun `NaN survives the wire`() {
        val back = LinkCodec.decode(LinkCodec.encode(1, LinkMessage.SosLocation(1L, Double.NaN, 0.0)))
        assertTrue((back.message as LinkMessage.SosLocation).latitude.isNaN())
    }

    @Test fun `critical flags match the delivery contract`() {
        assertTrue(LinkMessage.EventStart(1, "a", true).isCritical)
        assertTrue(LinkMessage.EventLabel(1, "a", "b").isCritical)
        assertTrue(LinkMessage.EventEnd(1, "a").isCritical)
        assertTrue(LinkMessage.SosLocation(1, 0.0, 0.0).isCritical)
        assertTrue(!LinkMessage.SensorWindow(1, 0.0, 0.0, 0.0, 0.0, 0.0, false, false).isCritical)
        assertTrue(!LinkMessage.Pong.isCritical)
    }

    private fun expectError(block: () -> Unit) {
        try { block(); fail("expected LinkProtocolException") } catch (_: LinkProtocolException) { }
    }
}
