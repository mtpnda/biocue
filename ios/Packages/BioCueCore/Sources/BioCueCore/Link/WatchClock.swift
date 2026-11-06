import Foundation

/// Converts timestamps written by the WATCH's clock into the PHONE's clock (twin of Kotlin `WatchClock`).
/// The phone used to mix the two — event times from the watch, "now" from the phone — so a watch clock off by more than a
/// few minutes broke the "is this event still ongoing?" rule. The offset is measured once per connection from
/// `HELLO_ACK.watchTimeMs`; the one-way Bluetooth delay (tens of ms) is ignored on purpose.
public struct WatchClock: Equatable, Sendable {
    private let offsetMs: Int64

    public static let identity = WatchClock(offsetMs: 0)

    public init(offsetMs: Int64) { self.offsetMs = offsetMs }

    public static func fromHello(watchTimeMs: Int64, phoneNowMs: Int64) -> WatchClock {
        WatchClock(offsetMs: phoneNowMs &- watchTimeMs)   // wrapping, like Kotlin: never trap on a garbage timestamp
    }

    public func toPhoneTime(_ watchTimestampMs: Int64) -> Int64 { watchTimestampMs &+ offsetMs }
}
