import XCTest
@testable import BioCueCore

/// Mirrors WatchClockTest.kt (same scenarios, same numbers).
final class WatchClockTests: XCTestCase {
    private let twentyMin: Int64 = 20 * 60_000

    func testAWatchRunningTwentyMinutesBehindHasItsTimestampsMovedForward() {
        let clock = WatchClock.fromHello(watchTimeMs: 1_000_000, phoneNowMs: 1_000_000 + twentyMin)
        XCTAssertEqual(clock.toPhoneTime(500_000), 500_000 + twentyMin)
    }

    func testAWatchWhoseClockIsExactlyRightChangesNothing() {
        XCTAssertEqual(WatchClock.fromHello(watchTimeMs: 42, phoneNowMs: 42).toPhoneTime(12_345), 12_345)
        XCTAssertEqual(WatchClock.identity.toPhoneTime(7), 7)
    }

    func testAnEventThatBegan14MinutesAgoIsStillOngoingEvenWhenTheWatchClockIs20MinutesSlow() {
        let phoneNow: Int64 = 10_000_000
        let watchNow = phoneNow - twentyMin
        let clock = WatchClock.fromHello(watchTimeMs: watchNow, phoneNowMs: phoneNow)
        let startedOnWatch = watchNow - 14 * 60_000                       // what the slow watch wrote
        let hot = SensorWindow(timestamp: phoneNow, hrMean: 95, rmssd: 8, accMag: 0.1, repetitiveness: 0, hrSlope: 0, isOffBody: false, isExercising: false)

        let converted = LatestEvent(id: "e1", startTime: clock.toPhoneTime(startedOnWatch), endTime: nil)
        let raw = LatestEvent(id: "e1", startTime: startedOnWatch, endTime: nil)

        XCTAssertEqual(DetectionPipeline.decide(window: hot, baseline: .appDefault, latestEvent: converted, nowMs: phoneNow).actions, [])
        XCTAssertEqual(DetectionPipeline.decide(window: hot, baseline: .appDefault, latestEvent: raw, nowMs: phoneNow).actions,
                       [.createAutoEvent(startTime: phoneNow)], "the old behaviour wrongly started a second event")
    }

    func testExtremeTimestampsNeverCrash() {
        let clock = WatchClock.fromHello(watchTimeMs: Int64.min, phoneNowMs: Int64.max)
        _ = clock.toPhoneTime(Int64.max)
    }
}
