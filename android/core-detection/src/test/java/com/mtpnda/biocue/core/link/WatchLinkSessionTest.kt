package com.mtpnda.biocue.core.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchLinkSessionTest {
    private val WATCH_NOW = 1_700_000_006_000L


    private fun session(bonded: Boolean = true, pairing: Boolean = false, outbox: LinkOutbox = LinkOutbox()) =
        WatchLinkSession("Galaxy Watch4 Classic", { 80 }, { bonded }, { pairing }, outbox, { WATCH_NOW })

    private fun hello(role: LinkRole = LinkRole.WEARER, version: Int = 1) =
        DecodedFrame(0, LinkMessage.Hello(version, role))

    private fun WatchLinkSession.Output.messages() = frames.map { LinkCodec.decode(it).message }

    private val window = LinkMessage.SensorWindow(1, 70.0, 40.0, 0.1, 0.1, 0.0, false, false)

    @Test fun `wearer phone is accepted and receives HELLO_ACK OK`() {
        val s = session().also { it.onConnected() }
        val out = s.onDownlink(hello())
        assertEquals(WatchLinkSession.State.ESTABLISHED, s.state)
        assertFalse(out.disconnect)
        assertEquals(LinkMessage.HelloAck(1, HelloStatus.OK, "Galaxy Watch4 Classic", 80, WATCH_NOW), out.messages().single())
    }

    @Test fun `CAREGIVER phone is refused and disconnected`() {
        val s = session().also { it.onConnected() }
        val out = s.onDownlink(hello(role = LinkRole.CAREGIVER))
        assertEquals(WatchLinkSession.State.REJECTED, s.state)
        assertTrue(out.disconnect)
        assertEquals(HelloStatus.ROLE_NOT_ALLOWED, (out.messages().single() as LinkMessage.HelloAck).status)
        assertEquals(HelloStatus.ROLE_NOT_ALLOWED, s.lastRejection)
    }

    @Test fun `a refused caregiver never receives telemetry or events`() {
        val s = session().also { it.onConnected() }
        s.onDownlink(hello(role = LinkRole.CAREGIVER))
        assertTrue(s.submit(window).frames.isEmpty())
        assertTrue(s.submit(LinkMessage.EventStart(1, "e", true)).frames.isEmpty())
        assertTrue(s.onDownlink(DecodedFrame(1, LinkMessage.Ping)).frames.isEmpty())
    }

    @Test fun `unsupported version is refused`() {
        val s = session().also { it.onConnected() }
        val out = s.onDownlink(hello(version = 0))
        assertTrue(out.disconnect)
        assertEquals(HelloStatus.VERSION_UNSUPPORTED, (out.messages().single() as LinkMessage.HelloAck).status)
    }

    @Test fun `unbonded phone outside the pairing window is refused`() {
        val s = session(bonded = false, pairing = false).also { it.onConnected() }
        val out = s.onDownlink(hello())
        assertTrue(out.disconnect)
        assertEquals(HelloStatus.NOT_IN_PAIRING_WINDOW, (out.messages().single() as LinkMessage.HelloAck).status)
    }

    @Test fun `unbonded phone inside the pairing window is accepted`() {
        val s = session(bonded = false, pairing = true).also { it.onConnected() }
        s.onDownlink(hello())
        assertEquals(WatchLinkSession.State.ESTABLISHED, s.state)
    }

    @Test fun `nothing flows before HELLO`() {
        val s = session().also { it.onConnected() }
        assertTrue(s.submit(window).frames.isEmpty())
        assertTrue(s.onDownlink(DecodedFrame(1, LinkMessage.Ping)).frames.isEmpty())
        assertFalse(s.onDownlink(DecodedFrame(2, LinkMessage.SosGpsRequest)).sosGpsRequested)
    }

    @Test fun `HELLO timeout drops a silent connection`() {
        val s = session().also { it.onConnected() }
        assertTrue(s.onHelloTimeout().disconnect)
        assertEquals(WatchLinkSession.State.REJECTED, s.state)
        // once established, a stray timeout is harmless
        val t = session().also { it.onConnected(); it.onDownlink(hello()) }
        assertFalse(t.onHelloTimeout().disconnect)
    }

    @Test fun `established link streams telemetry and answers ping`() {
        val s = session().also { it.onConnected(); it.onDownlink(hello()) }
        assertEquals(window, s.submit(window).messages().single())
        assertEquals(LinkMessage.Pong, s.onDownlink(DecodedFrame(5, LinkMessage.Ping)).messages().single())
        assertTrue(s.onDownlink(DecodedFrame(6, LinkMessage.SosGpsRequest)).sosGpsRequested)
    }

    @Test fun `telemetry is never queued while disconnected`() {
        val s = session()
        s.submit(window); s.submit(window)
        s.onConnected()
        val out = s.onDownlink(hello())
        assertEquals(1, out.frames.size) // only the HELLO_ACK; no stale windows replayed
    }

    @Test fun `critical frames sent while away are replayed in order after the handshake`() {
        val s = session()
        s.submit(LinkMessage.EventStart(1, "e1", true))
        s.submit(LinkMessage.EventLabel(2, "e1", "OVERWHELMED"))
        s.submit(LinkMessage.EventEnd(3, "e1"))
        s.onConnected()
        val msgs = s.onDownlink(hello()).messages()
        assertTrue(msgs[0] is LinkMessage.HelloAck)
        assertEquals(listOf(
            LinkMessage.EventStart(1, "e1", true),
            LinkMessage.EventLabel(2, "e1", "OVERWHELMED"),
            LinkMessage.EventEnd(3, "e1"),
        ), msgs.drop(1))
    }

    @Test fun `ACKed critical frames are not replayed but un-ACKed ones are, with the original seq`() {
        val s = session().also { it.onConnected(); it.onDownlink(hello()) }
        val a = LinkCodec.decode(s.submit(LinkMessage.EventStart(1, "e1", true)).frames.single())
        val b = LinkCodec.decode(s.submit(LinkMessage.EventEnd(2, "e1")).frames.single())
        s.onDownlink(DecodedFrame(9, LinkMessage.Ack(a.seq)))     // phone confirms only the first
        s.onDisconnected(); s.onConnected()
        val replay = s.onDownlink(hello()).frames.map { LinkCodec.decode(it) }.drop(1)
        assertEquals(1, replay.size)
        assertEquals(b.seq, replay[0].seq)
        assertEquals(LinkMessage.EventEnd(2, "e1"), replay[0].message)
    }

    @Test fun `outbox drops the oldest when full and counts it`() {
        val box = LinkOutbox(capacity = 3)
        for (i in 1..5) box.add(box.allocateSeq(), byteArrayOf(i.toByte()))
        assertEquals(3, box.size())
        assertEquals(2, box.droppedCount)
        assertEquals(listOf<Byte>(3, 4, 5), box.pendingFrames().map { it[0] })
    }

    @Test fun `ack for an unknown seq is harmless`() {
        val box = LinkOutbox()
        assertFalse(box.ack(1234))
    }

    @Test fun `sequence numbers wrap at sixteen bits`() {
        val box = LinkOutbox()
        repeat(65536) { box.allocateSeq() }
        assertEquals(0, box.allocateSeq())
    }

    @Test fun `ack and hello sent the wrong way are ignored not fatal`() {
        val s = session().also { it.onConnected(); it.onDownlink(hello()) }
        val out = s.onDownlink(DecodedFrame(1, LinkMessage.Pong))   // an uplink type arriving downlink
        assertTrue(out.frames.isEmpty()); assertFalse(out.disconnect)
        assertEquals(WatchLinkSession.State.ESTABLISHED, s.state)
    }
}
