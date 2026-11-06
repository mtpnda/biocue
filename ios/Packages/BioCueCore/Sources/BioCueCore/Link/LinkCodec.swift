import Foundation

/// Encode/decode one application frame (before chunking). Big-endian:
///   [ver u8][type u8][seq u16][payloadLen u16][payload ...]
/// Payload layouts are specified in LinkCodec.kt and docs/WATCH_PHONE_CONNECTION.md.
public enum LinkCodec {

    // MARK: - Encode

    public static func encode(seq: Int, message: LinkMessage) throws -> [UInt8] {
        guard seq >= 0 && seq <= 0xFFFF else { throw LinkProtocolError.seqOutOfRange }
        let (type, payload) = try encodePayload(message)
        let total = LinkProtocol.frameHeaderBytes + payload.count
        guard total <= LinkProtocol.maxFrameBytes else { throw LinkProtocolError.frameTooLarge }

        var w = ByteWriter()
        w.u8(Int(LinkProtocol.version))
        w.u8(Int(type))
        w.u16(seq)
        w.u16(payload.count)
        w.bytes(payload)
        return w.out
    }

    private static func encodePayload(_ m: LinkMessage) throws -> (UInt8, [UInt8]) {
        var w = ByteWriter()
        switch m {
        case let .sensorWindow(ts, hr, rmssd, acc, rep, slope, off, ex):
            w.i64(ts); w.f64(hr); w.f64(rmssd); w.f64(acc); w.f64(rep); w.f64(slope)
            w.u8((off ? 1 : 0) | (ex ? 2 : 0))
            return (LinkProtocol.tSensorWindow, w.out)
        case let .eventStart(ts, id, manual):
            w.i64(ts); w.u8(manual ? 0 : 1); try w.str(id)
            return (LinkProtocol.tEventStart, w.out)
        case let .eventLabel(ts, id, label):
            w.i64(ts); try w.str(id); try w.str(label)
            return (LinkProtocol.tEventLabel, w.out)
        case let .eventEnd(ts, id):
            w.i64(ts); try w.str(id)
            return (LinkProtocol.tEventEnd, w.out)
        case let .sosLocation(ts, lat, lng):
            w.i64(ts); w.f64(lat); w.f64(lng)
            return (LinkProtocol.tSosLocation, w.out)
        case let .helloAck(version, status, name, battery, watchTime):
            w.u8(version); w.u8(status.rawValue); try w.str(name)
            w.u8(min(255, max(0, battery)))
            w.i64(watchTime)
            return (LinkProtocol.tHelloAck, w.out)
        case .pong:
            return (LinkProtocol.tPong, [])
        case let .hello(maxVersion, role):
            w.u8(maxVersion); w.u8(role.rawValue)
            return (LinkProtocol.tHello, w.out)
        case let .ack(seq):
            w.u16(seq)
            return (LinkProtocol.tAck, w.out)
        case .ping:
            return (LinkProtocol.tPing, [])
        case .sosGpsRequest:
            return (LinkProtocol.tSosGpsRequest, [])
        case .unknown:
            throw LinkProtocolError.cannotEncodeUnknown
        }
    }

    // MARK: - Decode

    public static func decode(_ frame: [UInt8]) throws -> DecodedFrame {
        guard frame.count >= LinkProtocol.frameHeaderBytes else { throw LinkProtocolError.frameTooShort }
        var r = ByteReader(frame)
        let ver = try r.u8()
        guard ver == LinkProtocol.version else { throw LinkProtocolError.unsupportedVersion(ver) }
        let type = try r.u8()
        let seq = try r.u16()
        let len = try r.u16()
        guard frame.count == LinkProtocol.frameHeaderBytes + len else { throw LinkProtocolError.lengthMismatch }

        let payload = Array(frame[LinkProtocol.frameHeaderBytes...])
        var p = ByteReader(payload)
        let message = try decodePayload(type: type, &p)
        return DecodedFrame(seq: seq, message: message)
    }

    private static func decodePayload(type: Int, _ r: inout ByteReader) throws -> LinkMessage {
        switch UInt8(truncatingIfNeeded: type) {
        case LinkProtocol.tSensorWindow:
            let ts = try r.i64()
            let hr = try r.f64()
            let rmssd = try r.f64()
            let acc = try r.f64()
            let rep = try r.f64()
            let slope = try r.f64()
            let flags = try r.u8()
            return .sensorWindow(timestamp: ts, hrMean: hr, rmssd: rmssd, accMag: acc, repetitiveness: rep,
                                 hrSlope: slope, isOffBody: (flags & 1) != 0, isExercising: (flags & 2) != 0)
        case LinkProtocol.tEventStart:
            let ts = try r.i64()
            let trigger = try r.u8()
            let id = try r.str()
            return .eventStart(timestamp: ts, eventId: id, isManual: trigger == 0)
        case LinkProtocol.tEventLabel:
            let ts = try r.i64()
            let id = try r.str()
            let label = try r.str()
            return .eventLabel(timestamp: ts, eventId: id, label: label)
        case LinkProtocol.tEventEnd:
            let ts = try r.i64()
            let id = try r.str()
            return .eventEnd(timestamp: ts, eventId: id)
        case LinkProtocol.tSosLocation:
            let ts = try r.i64()
            let lat = try r.f64()
            let lng = try r.f64()
            return .sosLocation(timestamp: ts, latitude: lat, longitude: lng)
        case LinkProtocol.tHelloAck:
            let v = try r.u8()
            let statusRaw = try r.u8()
            guard let status = HelloStatus(rawValue: statusRaw) else { throw LinkProtocolError.unknownStatus }
            let name = try r.str()
            let battery = try r.u8()
            let watchTime = try r.i64()
            return .helloAck(version: v, status: status, watchName: name, batteryPercent: battery, watchTimeMs: watchTime)
        case LinkProtocol.tPong:
            return .pong
        case LinkProtocol.tHello:
            let v = try r.u8()
            let roleRaw = try r.u8()
            guard let role = LinkRole(rawValue: roleRaw) else { throw LinkProtocolError.unknownRole }
            return .hello(maxVersion: v, role: role)
        case LinkProtocol.tAck:
            let acked = try r.u16()
            return .ack(ackedSeq: acked)
        case LinkProtocol.tPing:
            return .ping
        case LinkProtocol.tSosGpsRequest:
            return .sosGpsRequest
        default:
            return .unknown(type: type)
        }
    }
}

// MARK: - Byte helpers (arrays, not Data: Data slices keep a non-zero startIndex)

struct ByteWriter {
    var out: [UInt8] = []

    mutating func u8(_ v: Int) { out.append(UInt8(truncatingIfNeeded: v)) }

    mutating func u16(_ v: Int) {
        out.append(UInt8(truncatingIfNeeded: v >> 8))
        out.append(UInt8(truncatingIfNeeded: v))
    }

    mutating func i64(_ v: Int64) {
        let u = UInt64(bitPattern: v)
        var shift = 56
        while shift >= 0 {
            out.append(UInt8(truncatingIfNeeded: u >> UInt64(shift)))
            shift -= 8
        }
    }

    mutating func f64(_ v: Double) { i64(Int64(bitPattern: v.bitPattern)) }

    mutating func bytes(_ b: [UInt8]) { out.append(contentsOf: b) }

    mutating func str(_ s: String) throws {
        let utf8 = Array(s.utf8)
        guard utf8.count <= LinkProtocol.maxStringBytes else { throw LinkProtocolError.stringTooLong }
        out.append(UInt8(utf8.count))
        out.append(contentsOf: utf8)
    }
}

struct ByteReader {
    private let bytes: [UInt8]
    private var pos = 0

    init(_ bytes: [UInt8]) { self.bytes = bytes }

    mutating func u8() throws -> Int {
        guard pos < bytes.count else { throw LinkProtocolError.truncatedPayload }
        let v = Int(bytes[pos])
        pos += 1
        return v
    }

    mutating func u16() throws -> Int {
        let hi = try u8()
        let lo = try u8()
        return (hi << 8) | lo
    }

    mutating func i64() throws -> Int64 {
        guard bytes.count - pos >= 8 else { throw LinkProtocolError.truncatedPayload }
        var u: UInt64 = 0
        var i = 0
        while i < 8 {
            u = (u << 8) | UInt64(bytes[pos + i])
            i += 1
        }
        pos += 8
        return Int64(bitPattern: u)
    }

    mutating func f64() throws -> Double {
        let bits = try i64()
        return Double(bitPattern: UInt64(bitPattern: bits))
    }

    mutating func str() throws -> String {
        let len = try u8()
        guard bytes.count - pos >= len else { throw LinkProtocolError.truncatedPayload }
        let slice = bytes[pos..<(pos + len)]
        pos += len
        return String(decoding: slice, as: UTF8.self)
    }
}
