import XCTest
@testable import BioCueCore

final class LinkCodecTests: XCTestCase {

    private func bytes(_ hex: String) -> [UInt8] {
        var out: [UInt8] = []
        var i = hex.startIndex
        while i < hex.endIndex {
            let j = hex.index(i, offsetBy: 2)
            out.append(UInt8(hex[i..<j], radix: 16)!)
            i = j
        }
        return out
    }

    private func hex(_ b: [UInt8]) -> String { b.map { String(format: "%02x", $0) }.joined() }

    func testEveryGoldenVectorDecodesToTheExpectedMessage() throws {
        for v in LinkGoldenVectors.all {
            let d = try LinkCodec.decode(bytes(v.hex))
            XCTAssertEqual(d.seq, v.seq, v.name)
            XCTAssertEqual(d.message, v.message, v.name)
        }
    }

    func testEveryGoldenVectorEncodesToTheExactReferenceBytes() throws {
        for v in LinkGoldenVectors.all {
            if case .unknown = v.message { continue }   // cannot be produced by a sender
            XCTAssertEqual(hex(try LinkCodec.encode(seq: v.seq, message: v.message)), v.hex, v.name)
        }
    }

    func testUnknownTypeIsSurfacedNotFatal() throws {
        let v = LinkGoldenVectors.all.first { $0.name == "unknown_type" }!
        XCTAssertEqual(try LinkCodec.decode(bytes(v.hex)).message, .unknown(type: 0x7E))
    }

    func testShortFrameRejected() {
        XCTAssertThrowsError(try LinkCodec.decode([1, 1, 0])) { XCTAssertEqual($0 as? LinkProtocolError, .frameTooShort) }
    }

    func testWrongVersionRejected() {
        var b = bytes(LinkGoldenVectors.all.first { $0.name == "ping" }!.hex)
        b[0] = 2
        XCTAssertThrowsError(try LinkCodec.decode(b)) { XCTAssertEqual($0 as? LinkProtocolError, .unsupportedVersion(2)) }
    }

    func testLengthMismatchRejected() {
        let b = bytes(LinkGoldenVectors.all.first { $0.name == "sensor_window" }!.hex)
        XCTAssertThrowsError(try LinkCodec.decode(Array(b.dropLast())))
        XCTAssertThrowsError(try LinkCodec.decode(b + [0]))
    }

    func testPayloadShorterThanTypeRequiresIsRejected() {
        // SOS_LOCATION needs 24 bytes; give it a self-consistent 3-byte payload.
        let frame: [UInt8] = [1, 0x05, 0, 1, 0, 3, 9, 9, 9]
        XCTAssertThrowsError(try LinkCodec.decode(frame)) { XCTAssertEqual($0 as? LinkProtocolError, .truncatedPayload) }
    }

    func testStringOverLimitCannotBeEncoded() {
        let msg = LinkMessage.eventLabel(timestamp: 1, eventId: "id", label: String(repeating: "x", count: 256))
        XCTAssertThrowsError(try LinkCodec.encode(seq: 1, message: msg)) { XCTAssertEqual($0 as? LinkProtocolError, .stringTooLong) }
    }

    func testSeqOutOfRangeRejected() {
        XCTAssertThrowsError(try LinkCodec.encode(seq: 70000, message: .ping))
        XCTAssertThrowsError(try LinkCodec.encode(seq: -1, message: .ping))
    }

    func testUnknownRoleAndStatusRejected() {
        XCTAssertThrowsError(try LinkCodec.decode([1, 0x81, 0, 0, 0, 2, 1, 9]))
        XCTAssertThrowsError(try LinkCodec.decode([1, 0x06, 0, 0, 0, 5, 1, 9, 0, 0, 0]))
    }

    func testRandomGarbageNeverCrashesOrThrowsAnythingButProtocolErrors() {
        var rng = SystemRandomNumberGenerator()
        for _ in 0..<5000 {
            let n = Int.random(in: 0..<80, using: &rng)
            var b = (0..<n).map { _ in UInt8.random(in: 0...255, using: &rng) }
            if b.count >= 2 && Bool.random(using: &rng) {
                // structurally valid header around a random payload
                let payload = Array(b[2...])
                b = [1, b[1], 0, 7, UInt8(payload.count >> 8), UInt8(payload.count & 0xFF)] + payload
            }
            do { _ = try LinkCodec.decode(b) } catch is LinkProtocolError { /* expected */ } catch {
                XCTFail("unexpected error type: \(error)")
            }
        }
    }

    func testExtremeValuesRoundTrip() throws {
        let msgs: [LinkMessage] = [
            .sensorWindow(timestamp: Int64.max, hrMean: Double.greatestFiniteMagnitude, rmssd: Double.leastNonzeroMagnitude,
                          accMag: -0.0, repetitiveness: 1e-300, hrSlope: -1e300, isOffBody: true, isExercising: true),
            .sensorWindow(timestamp: Int64.min, hrMean: 0, rmssd: 0, accMag: 0, repetitiveness: 0, hrSlope: 0, isOffBody: false, isExercising: false),
            .eventLabel(timestamp: 5, eventId: String(repeating: "e", count: 255), label: String(repeating: "l", count: 255)),
            .helloAck(version: 1, status: .notInPairingWindow, watchName: "", batteryPercent: 0, watchTimeMs: Int64.min),
            .ack(ackedSeq: 0)
        ]
        for m in msgs {
            let back = try LinkCodec.decode(try LinkCodec.encode(seq: 65535, message: m))
            XCTAssertEqual(back.seq, 65535)
            XCTAssertEqual(back.message, m)
        }
    }

    func testNaNSurvivesTheWire() throws {
        let back = try LinkCodec.decode(try LinkCodec.encode(seq: 1, message: .sosLocation(timestamp: 1, latitude: .nan, longitude: 0)))
        if case let .sosLocation(_, lat, _) = back.message { XCTAssertTrue(lat.isNaN) } else { XCTFail("wrong case") }
    }

    func testCriticalFlagsMatchTheDeliveryContract() {
        XCTAssertTrue(LinkMessage.eventStart(timestamp: 1, eventId: "a", isManual: true).isCritical)
        XCTAssertTrue(LinkMessage.eventLabel(timestamp: 1, eventId: "a", label: "b").isCritical)
        XCTAssertTrue(LinkMessage.eventEnd(timestamp: 1, eventId: "a").isCritical)
        XCTAssertTrue(LinkMessage.sosLocation(timestamp: 1, latitude: 0, longitude: 0).isCritical)
        XCTAssertFalse(LinkMessage.sensorWindow(timestamp: 1, hrMean: 0, rmssd: 0, accMag: 0, repetitiveness: 0, hrSlope: 0, isOffBody: false, isExercising: false).isCritical)
        XCTAssertFalse(LinkMessage.pong.isCritical)
    }
}

final class LinkFramingTests: XCTestCase {

    private func bytes(_ hex: String) -> [UInt8] {
        var out: [UInt8] = []
        var i = hex.startIndex
        while i < hex.endIndex {
            let j = hex.index(i, offsetBy: 2)
            out.append(UInt8(hex[i..<j], radix: 16)!)
            i = j
        }
        return out
    }

    private func hex(_ b: [UInt8]) -> String { b.map { String(format: "%02x", $0) }.joined() }

    func testChunksMatchThePythonReferenceByteForByte() {
        for c in LinkGoldenVectors.chunkCases {
            let frame = bytes(LinkGoldenVectors.all.first { $0.name == c.frameName }!.hex)
            let chunks = LinkFraming.split(frame, maxChunkBytes: c.maxChunk).map(hex)
            XCTAssertEqual(chunks, c.chunksHex, "\(c.frameName)@\(c.maxChunk)")
        }
    }

    func testSingleChunkIsFlaggedFirstAndLast() {
        let chunks = LinkFraming.split([1, 2, 3], maxChunkBytes: 20)
        XCTAssertEqual(chunks.count, 1)
        XCTAssertEqual(chunks[0][0], 0x03)
    }

    func testNoChunkExceedsTheNegotiatedSize() {
        let frame = [UInt8](repeating: 7, count: 500)
        for max in 2...60 {
            for c in LinkFraming.split(frame, maxChunkBytes: max) { XCTAssertLessThanOrEqual(c.count, max) }
        }
    }

    func testSplitThenReassembleIsLosslessForEverySize() {
        for len in 1...300 {
            for max in [2, 3, 5, 20, 23, 64, 185, 512] {
                let frame = (0..<len).map { UInt8(truncatingIfNeeded: $0 &* 31 &+ 7) }
                var r = LinkReassembler()
                var out: [UInt8]?
                for chunk in LinkFraming.split(frame, maxChunkBytes: max) { out = r.accept(chunk) ?? out }
                XCTAssertEqual(out, frame, "len=\(len) max=\(max)")
            }
        }
    }

    func testBackToBackFramesReassembleIndependently() {
        var r = LinkReassembler()
        let a = [UInt8](repeating: 1, count: 45), b = [UInt8](repeating: 2, count: 7)
        var got: [[UInt8]] = []
        for c in LinkFraming.split(a, maxChunkBytes: 20) + LinkFraming.split(b, maxChunkBytes: 20) {
            if let f = r.accept(c) { got.append(f) }
        }
        XCTAssertEqual(got, [a, b])
    }

    func testOrphanChunkIsDroppedAndCounted() {
        var r = LinkReassembler()
        XCTAssertNil(r.accept([0x02, 9, 9]))
        XCTAssertEqual(r.droppedChunks, 1)
    }

    func testNewFirstDiscardsAHalfReceivedFrame() {
        var r = LinkReassembler()
        _ = r.accept(LinkFraming.split([UInt8](repeating: 5, count: 50), maxChunkBytes: 20)[0])
        var out: [UInt8]?
        for c in LinkFraming.split([7, 7, 7], maxChunkBytes: 20) { out = r.accept(c) ?? out }
        XCTAssertEqual(out, [7, 7, 7])
    }

    func testOversizeFrameIsDroppedAndTheReassemblerRecovers() {
        var r = LinkReassembler(maxFrameBytes: 40)
        var out: [UInt8]?
        for c in LinkFraming.split([UInt8](repeating: 0, count: 100), maxChunkBytes: 20) { out = r.accept(c) ?? out }
        XCTAssertNil(out)
        XCTAssertGreaterThan(r.droppedChunks, 0)
        var ok: [UInt8]?
        for c in LinkFraming.split([1, 2, 3], maxChunkBytes: 20) { ok = r.accept(c) ?? ok }
        XCTAssertEqual(ok, [1, 2, 3])
    }

    func testEmptyChunkIsTolerated() {
        var r = LinkReassembler()
        XCTAssertNil(r.accept([]))
        XCTAssertEqual(r.droppedChunks, 1)
    }

    func testFullPipelineForEveryGoldenVector() throws {
        for v in LinkGoldenVectors.all {
            if case .unknown = v.message { continue }
            let frame = try LinkCodec.encode(seq: v.seq, message: v.message)
            for max in [20, 23, 185] {
                var r = LinkReassembler()
                var out: [UInt8]?
                for c in LinkFraming.split(frame, maxChunkBytes: max) { out = r.accept(c) ?? out }
                XCTAssertEqual(try LinkCodec.decode(out!).message, v.message, "\(v.name)@\(max)")
            }
        }
    }

    // MARK: session helpers

    func testInboundDecodesFramesAndCountsCorruptOnesWithoutDying() throws {
        let inbound = LinkInbound()
        var outbound = LinkOutbound()
        // a good frame split to default-MTU chunks
        let msg = LinkMessage.eventLabel(timestamp: 9, eventId: "evt-1", label: "OVERWHELMED")
        var got: DecodedFrame?
        for c in try outbound.chunks(for: msg, maxChunkBytes: 20) { got = inbound.receive(chunk: c) ?? got }
        XCTAssertEqual(got?.message, msg)
        XCTAssertEqual(got?.seq, 0)
        // a structurally framed but undecodable payload (bad version)
        XCTAssertNil(inbound.receive(chunk: [0x03, 9, 9, 9, 9, 9, 9]))
        XCTAssertEqual(inbound.protocolErrors, 1)
        // ...and the link still works afterwards
        var again: DecodedFrame?
        for c in try outbound.chunks(for: .ping, maxChunkBytes: 20) { again = inbound.receive(chunk: c) ?? again }
        XCTAssertEqual(again?.message, .ping)
        XCTAssertEqual(again?.seq, 1)
    }

    func testOutboundSequenceNumbersWrapAtSixteenBits() throws {
        var outbound = LinkOutbound()
        let inbound = LinkInbound()
        var last = -1
        for _ in 0..<65_540 {
            var got: DecodedFrame?
            for c in try outbound.chunks(for: .ping, maxChunkBytes: 20) { got = inbound.receive(chunk: c) ?? got }
            last = got!.seq
        }
        XCTAssertEqual(last, (65_540 - 1) & 0xFFFF)
    }
}
