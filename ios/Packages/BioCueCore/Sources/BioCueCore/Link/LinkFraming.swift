import Foundation

/// Splits a frame into BLE-sized chunks and reassembles them. See LinkFraming.kt.
/// Chunk = [flags u8][data...], flags bit0 = FIRST, bit1 = LAST.
public enum LinkFraming {

    /// - Parameter maxChunkBytes: the largest single BLE payload (usually ATT MTU - 3), must be >= 2.
    public static func split(_ frame: [UInt8], maxChunkBytes: Int) -> [[UInt8]] {
        precondition(maxChunkBytes >= 2, "chunk size must be >= 2")
        precondition(!frame.isEmpty, "empty frame")
        let dataPer = maxChunkBytes - 1
        var chunks: [[UInt8]] = []
        var offset = 0
        while offset < frame.count {
            let n = min(dataPer, frame.count - offset)
            var flags: UInt8 = 0
            if offset == 0 { flags |= LinkProtocol.chunkFirst }
            if offset + n == frame.count { flags |= LinkProtocol.chunkLast }
            var chunk: [UInt8] = [flags]
            chunk.append(contentsOf: frame[offset..<(offset + n)])
            chunks.append(chunk)
            offset += n
        }
        return chunks
    }
}

/// One per direction per connection. A FIRST chunk always resets; chunks that arrive with no
/// FIRST (or that blow the size cap) are counted and dropped instead of corrupting state.
public struct LinkReassembler {
    private var buffer: [UInt8] = []
    private var started = false
    private let maxFrameBytes: Int

    /// Chunks discarded because they were orphaned or oversized (diagnostics / tests).
    public private(set) var droppedChunks = 0

    public init(maxFrameBytes: Int = LinkProtocol.maxFrameBytes) {
        self.maxFrameBytes = maxFrameBytes
    }

    /// Returns the complete frame when `chunk` carried the LAST flag, otherwise nil.
    public mutating func accept(_ chunk: [UInt8]) -> [UInt8]? {
        guard let flags = chunk.first else {
            droppedChunks += 1
            return nil
        }
        if (flags & LinkProtocol.chunkFirst) != 0 {
            buffer = []
            started = true
        } else if !started {
            droppedChunks += 1
            return nil
        }
        buffer.append(contentsOf: chunk.dropFirst())
        if buffer.count > maxFrameBytes {
            reset()
            droppedChunks += 1
            return nil
        }
        if (flags & LinkProtocol.chunkLast) != 0 {
            let frame = buffer
            reset()
            return frame
        }
        return nil
    }

    public mutating func reset() {
        buffer = []
        started = false
    }
}

// MARK: - Session helpers

/// Receives raw BLE notification payloads, returns decoded frames. Protocol errors are counted, never thrown,
/// so a single corrupt frame cannot take the link down.
public final class LinkInbound {
    private var reassembler = LinkReassembler()
    public private(set) var protocolErrors = 0

    public init() {}

    public func receive(chunk: [UInt8]) -> DecodedFrame? {
        guard let frame = reassembler.accept(chunk) else { return nil }
        do {
            return try LinkCodec.decode(frame)
        } catch {
            protocolErrors += 1
            return nil
        }
    }

    public func reset() { reassembler.reset() }
}

/// Assigns sequence numbers and turns messages into ready-to-write chunks.
public struct LinkOutbound {
    private var nextSeq = 0

    public init() {}

    public mutating func chunks(for message: LinkMessage, maxChunkBytes: Int) throws -> [[UInt8]] {
        let seq = nextSeq
        nextSeq = (nextSeq + 1) & 0xFFFF
        let frame = try LinkCodec.encode(seq: seq, message: message)
        return LinkFraming.split(frame, maxChunkBytes: maxChunkBytes)
    }
}
