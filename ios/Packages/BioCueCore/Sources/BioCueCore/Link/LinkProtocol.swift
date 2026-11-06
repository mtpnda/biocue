import Foundation

/// BioCue Link v1 constants. Must match `LinkProtocol.kt` (Wear OS) exactly.
/// Normative spec: docs/WATCH_PHONE_CONNECTION.md. Golden vectors: tools/gen_link_vectors.py.
public enum LinkProtocol {
    public static let version = 1

    // GATT layout
    public static let serviceUUID = "7a1c0001-8f3b-4c5e-9a52-b10c0e000001"
    /// watch -> phone, NOTIFY
    public static let uplinkUUID = "7a1c0002-8f3b-4c5e-9a52-b10c0e000001"
    /// phone -> watch, WRITE (with response)
    public static let downlinkUUID = "7a1c0003-8f3b-4c5e-9a52-b10c0e000001"
    /// READ, 2 bytes [maxVersion][flags]; flag bit0 = pairing window open
    public static let infoUUID = "7a1c0004-8f3b-4c5e-9a52-b10c0e000001"

    // Framing
    public static let frameHeaderBytes = 6
    public static let maxFrameBytes = 1024
    public static let maxStringBytes = 255
    public static let chunkFirst: UInt8 = 0x01
    public static let chunkLast: UInt8 = 0x02

    // Message type codes
    static let tSensorWindow: UInt8 = 0x01
    static let tEventStart: UInt8 = 0x02
    static let tEventLabel: UInt8 = 0x03
    static let tEventEnd: UInt8 = 0x04
    static let tSosLocation: UInt8 = 0x05
    static let tHelloAck: UInt8 = 0x06
    static let tPong: UInt8 = 0x07
    static let tHello: UInt8 = 0x81
    static let tAck: UInt8 = 0x82
    static let tPing: UInt8 = 0x83
    static let tSosGpsRequest: UInt8 = 0x84
}

/// Who is on the other end. The watch only talks to a WEARER's phone.
public enum LinkRole: Int, Equatable, Sendable {
    case wearer = 0
    case caregiver = 1
}

public enum HelloStatus: Int, Equatable, Sendable {
    case ok = 0
    case roleNotAllowed = 1
    case versionUnsupported = 2
    case notInPairingWindow = 3
}

public enum LinkMessage: Equatable, Sendable {
    // watch -> phone
    case sensorWindow(timestamp: Int64, hrMean: Double, rmssd: Double, accMag: Double,
                      repetitiveness: Double, hrSlope: Double, isOffBody: Bool, isExercising: Bool)
    case eventStart(timestamp: Int64, eventId: String, isManual: Bool)
    case eventLabel(timestamp: Int64, eventId: String, label: String)
    case eventEnd(timestamp: Int64, eventId: String)
    case sosLocation(timestamp: Int64, latitude: Double, longitude: Double)
    case helloAck(version: Int, status: HelloStatus, watchName: String, batteryPercent: Int, watchTimeMs: Int64)
    case pong
    // phone -> watch
    case hello(maxVersion: Int, role: LinkRole)
    case ack(ackedSeq: Int)
    case ping
    case sosGpsRequest
    /// A well-formed frame of a type this build does not know (forward compatibility).
    case unknown(type: Int)

    /// Critical messages are retained by the watch until ACKed, so the phone must ACK them
    /// (after persisting) and must treat them idempotently.
    public var isCritical: Bool {
        switch self {
        case .eventStart, .eventLabel, .eventEnd, .sosLocation: return true
        default: return false
        }
    }
}

public struct DecodedFrame: Equatable, Sendable {
    public let seq: Int
    public let message: LinkMessage
    public init(seq: Int, message: LinkMessage) {
        self.seq = seq
        self.message = message
    }
}

public enum LinkProtocolError: Error, Equatable {
    case frameTooShort
    case unsupportedVersion(Int)
    case lengthMismatch
    case truncatedPayload
    case frameTooLarge
    case stringTooLong
    case unknownRole
    case unknownStatus
    case seqOutOfRange
    case cannotEncodeUnknown
}
