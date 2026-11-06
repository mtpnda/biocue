package com.mtpnda.biocue.core.link

import com.mtpnda.biocue.core.link.LinkProtocol as P
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Encodes/decodes one application frame (before chunking):
 *
 *   [ver u8][type u8][seq u16][payloadLen u16][payload ...]     (big-endian)
 *
 * Payload layouts (normative; the Swift implementation must match byte-for-byte):
 *
 *   SENSOR_WINDOW  i64 ts, f64 hrMean, f64 rmssd, f64 accMag, f64 repetitiveness,
 *                  f64 hrSlope, u8 flags(bit0 offBody, bit1 exercising)        49 B
 *   EVENT_START    i64 ts, u8 trigger(0 manual,1 auto), str eventId
 *   EVENT_LABEL    i64 ts, str eventId, str label
 *   EVENT_END      i64 ts, str eventId
 *   SOS_LOCATION   i64 ts, f64 lat, f64 lng                                     24 B
 *   HELLO_ACK      u8 version, u8 status, str watchName, u8 battery, i64 watchTimeMs
 *   PONG           (empty)
 *   HELLO          u8 maxVersion, u8 role
 *   ACK            u16 ackedSeq
 *   PING / SOS_GPS_REQUEST  (empty)
 *
 *   str = [len u8][UTF-8 bytes]
 */
object LinkCodec {

    fun encode(seq: Int, message: LinkMessage): ByteArray {
        require(seq in 0..0xFFFF) { "seq out of range: $seq" }
        val (type, payload) = encodePayload(message)
        val total = P.FRAME_HEADER_BYTES + payload.size
        if (total > P.MAX_FRAME_BYTES) throw LinkProtocolException("frame too large: $total")
        val buf = ByteBuffer.allocate(total).order(ByteOrder.BIG_ENDIAN)
        buf.put(P.VERSION.toByte())
        buf.put(type.toByte())
        buf.putShort(seq.toShort())
        buf.putShort(payload.size.toShort())
        buf.put(payload)
        return buf.array()
    }

    fun decode(frame: ByteArray): DecodedFrame {
        if (frame.size < P.FRAME_HEADER_BYTES) throw LinkProtocolException("frame shorter than header")
        val buf = ByteBuffer.wrap(frame).order(ByteOrder.BIG_ENDIAN)
        val ver = buf.get().toInt() and 0xFF
        if (ver != P.VERSION) throw LinkProtocolException("unsupported frame version $ver")
        val type = buf.get().toInt() and 0xFF
        val seq = buf.short.toInt() and 0xFFFF
        val len = buf.short.toInt() and 0xFFFF
        if (frame.size != P.FRAME_HEADER_BYTES + len) {
            throw LinkProtocolException("length mismatch: header says $len, have ${frame.size - P.FRAME_HEADER_BYTES}")
        }
        val payload = ByteBuffer.wrap(frame, P.FRAME_HEADER_BYTES, len).slice().order(ByteOrder.BIG_ENDIAN)
        val message = try {
            decodePayload(type, payload)
        } catch (e: BufferUnderflowException) {
            throw LinkProtocolException("payload truncated for type 0x${type.toString(16)}")
        }
        return DecodedFrame(seq, message)
    }

    // ---- payloads ------------------------------------------------------------

    private fun encodePayload(m: LinkMessage): Pair<Int, ByteArray> = when (m) {
        is LinkMessage.SensorWindow -> P.T_SENSOR_WINDOW to build(49) {
            putLong(m.timestamp); putDouble(m.hrMean); putDouble(m.rmssd); putDouble(m.accMag)
            putDouble(m.repetitiveness); putDouble(m.hrSlope)
            put(((if (m.isOffBody) 1 else 0) or (if (m.isExercising) 2 else 0)).toByte())
        }
        is LinkMessage.EventStart -> {
            val id = utf8(m.eventId)
            P.T_EVENT_START to build(8 + 1 + 1 + id.size) {
                putLong(m.timestamp); put((if (m.isManual) 0 else 1).toByte()); putStr(id)
            }
        }
        is LinkMessage.EventLabel -> {
            val id = utf8(m.eventId); val label = utf8(m.label)
            P.T_EVENT_LABEL to build(8 + 1 + id.size + 1 + label.size) {
                putLong(m.timestamp); putStr(id); putStr(label)
            }
        }
        is LinkMessage.EventEnd -> {
            val id = utf8(m.eventId)
            P.T_EVENT_END to build(8 + 1 + id.size) { putLong(m.timestamp); putStr(id) }
        }
        is LinkMessage.SosLocation -> P.T_SOS_LOCATION to build(24) {
            putLong(m.timestamp); putDouble(m.latitude); putDouble(m.longitude)
        }
        is LinkMessage.HelloAck -> {
            val name = utf8(m.watchName)
            P.T_HELLO_ACK to build(2 + 1 + name.size + 1 + 8) {
                put(m.version.toByte()); put(m.status.wire.toByte()); putStr(name)
                put(m.batteryPercent.coerceIn(0, 255).toByte()); putLong(m.watchTimeMs)
            }
        }
        LinkMessage.Pong -> P.T_PONG to ByteArray(0)
        is LinkMessage.Hello -> P.T_HELLO to build(2) {
            put(m.maxVersion.toByte()); put(m.role.wire.toByte())
        }
        is LinkMessage.Ack -> P.T_ACK to build(2) { putShort(m.ackedSeq.toShort()) }
        LinkMessage.Ping -> P.T_PING to ByteArray(0)
        LinkMessage.SosGpsRequest -> P.T_SOS_GPS_REQUEST to ByteArray(0)
        is LinkMessage.Unknown -> throw LinkProtocolException("cannot encode Unknown")
    }

    private fun decodePayload(type: Int, b: ByteBuffer): LinkMessage = when (type) {
        P.T_SENSOR_WINDOW -> {
            val ts = b.long; val hr = b.double; val rm = b.double; val acc = b.double
            val rep = b.double; val slope = b.double; val flags = b.get().toInt() and 0xFF
            LinkMessage.SensorWindow(ts, hr, rm, acc, rep, slope, (flags and 1) != 0, (flags and 2) != 0)
        }
        P.T_EVENT_START -> {
            val ts = b.long; val trig = b.get().toInt() and 0xFF
            LinkMessage.EventStart(ts, b.str(), isManual = trig == 0)
        }
        P.T_EVENT_LABEL -> { val ts = b.long; val id = b.str(); LinkMessage.EventLabel(ts, id, b.str()) }
        P.T_EVENT_END -> { val ts = b.long; LinkMessage.EventEnd(ts, b.str()) }
        P.T_SOS_LOCATION -> LinkMessage.SosLocation(b.long, b.double, b.double)
        P.T_HELLO_ACK -> {
            val v = b.get().toInt() and 0xFF
            val st = HelloStatus.fromWire(b.get().toInt() and 0xFF)
                ?: throw LinkProtocolException("unknown hello status")
            val name = b.str(); val batt = b.get().toInt() and 0xFF
            LinkMessage.HelloAck(v, st, name, batt, b.long)
        }
        P.T_PONG -> LinkMessage.Pong
        P.T_HELLO -> {
            val v = b.get().toInt() and 0xFF
            val role = LinkRole.fromWire(b.get().toInt() and 0xFF)
                ?: throw LinkProtocolException("unknown role")
            LinkMessage.Hello(v, role)
        }
        P.T_ACK -> LinkMessage.Ack(b.short.toInt() and 0xFFFF)
        P.T_PING -> LinkMessage.Ping
        P.T_SOS_GPS_REQUEST -> LinkMessage.SosGpsRequest
        else -> LinkMessage.Unknown(type)
    }

    // ---- helpers -------------------------------------------------------------

    private fun utf8(s: String): ByteArray {
        val bytes = s.toByteArray(Charsets.UTF_8)
        if (bytes.size > P.MAX_STRING_BYTES) throw LinkProtocolException("string exceeds ${P.MAX_STRING_BYTES} bytes")
        return bytes
    }

    private inline fun build(size: Int, block: ByteBuffer.() -> Unit): ByteArray {
        val buf = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN)
        buf.block()
        check(!buf.hasRemaining()) { "payload size mismatch (${buf.remaining()} bytes unused)" }
        return buf.array()
    }

    private fun ByteBuffer.putStr(bytes: ByteArray) { put(bytes.size.toByte()); put(bytes) }

    private fun ByteBuffer.str(): String {
        val len = get().toInt() and 0xFF
        val out = ByteArray(len)
        get(out)
        return String(out, Charsets.UTF_8)
    }
}
