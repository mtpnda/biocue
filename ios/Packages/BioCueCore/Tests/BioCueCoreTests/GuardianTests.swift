import XCTest
@testable import BioCueCore

final class GuardianTests: XCTestCase {

    func testInitialMonitoringPhase() {
        let now: Int64 = 100_000
        let input = GuardianInput(
            eventStartTime: now - (2 * 60 * 1000), // 2 min ago
            currentTimeMs: now,
            lastWearerInteractionMs: now - (2 * 60 * 1000),
            currentTier: 2,
            hrMean: 110,
            hrBaseline: 70
        )
        let (state, action) = GuardianEngine.evaluate(input: input)
        XCTAssertEqual(state, .monitoring(durationMs: 2 * 60 * 1000))
        XCTAssertEqual(action, .none)
    }

    func testSafetyCheckTriggeredAtSevenMinutesWithoutInteraction() {
        let now: Int64 = 1_000_000
        let input = GuardianInput(
            eventStartTime: now - (8 * 60 * 1000), // 8 min ago
            currentTimeMs: now,
            lastWearerInteractionMs: now - (8 * 60 * 1000),
            currentTier: 2,
            hrMean: 105,
            hrBaseline: 70
        )
        let (state, action) = GuardianEngine.evaluate(input: input)
        XCTAssertEqual(state, .safetyCheckDue(durationMs: 8 * 60 * 1000))
        XCTAssertEqual(action, .promptSafetyCheck)
    }

    func testGraceAlarmAtTwelveMinutesWhenElevatedAndUnresponsive() {
        let now: Int64 = 1_000_000
        let input = GuardianInput(
            eventStartTime: now - (13 * 60 * 1000), // 13 min ago
            currentTimeMs: now,
            lastWearerInteractionMs: now - (13 * 60 * 1000),
            currentTier: 2,
            hrMean: 115,
            hrBaseline: 70
        )
        let (state, action) = GuardianEngine.evaluate(input: input)
        if case .graceCountdown(let rem) = state {
            XCTAssertGreaterThanOrEqual(rem, 0)
            XCTAssertLessThanOrEqual(rem, 30)
        } else {
            XCTFail("Expected grace countdown, got \(state)")
        }
        if case .soundGraceAlarm(let rem) = action {
            XCTAssertGreaterThanOrEqual(rem, 0)
        } else {
            XCTFail("Expected soundGraceAlarm, got \(action)")
        }
    }

    func testCrisisBroadcastTriggeredAtFifteenMinutesWhenUnresponsive() {
        let now: Int64 = 2_000_000
        let input = GuardianInput(
            eventStartTime: now - (16 * 60 * 1000), // 16 min ago
            currentTimeMs: now,
            lastWearerInteractionMs: now - (16 * 60 * 1000),
            currentTier: 2,
            hrMean: 110,
            hrBaseline: 70
        )
        let (state, action) = GuardianEngine.evaluate(input: input)
        XCTAssertEqual(state, .crisisUnresponsive(durationMs: 16 * 60 * 1000))
        if case .triggerCrisisBroadcast(let reason) = action {
            XCTAssertTrue(reason.contains("Prolonged high stress"))
        } else {
            XCTFail("Expected crisis broadcast, got \(action)")
        }
    }

    func testWearerAcknowledgedSafeSilencesCrisis() {
        let now: Int64 = 2_000_000
        let input = GuardianInput(
            eventStartTime: now - (18 * 60 * 1000),
            currentTimeMs: now,
            lastWearerInteractionMs: now - (18 * 60 * 1000),
            currentTier: 2,
            hrMean: 110,
            hrBaseline: 70,
            isSafeConfirmed: true
        )
        let (state, action) = GuardianEngine.evaluate(input: input)
        XCTAssertEqual(state, .acknowledgedSafe)
        XCTAssertEqual(action, .none)
    }

    func testCompanionGuidancePhases() {
        // Phase 1
        let g1 = CompanionEngine.synthesize(
            durationMs: 60_000,
            tier: 2,
            hr: 100,
            hrBaseline: 70,
            activity: "🪑 Sitting Still",
            guardianState: .monitoring(durationMs: 60_000)
        )
        XCTAssertEqual(g1.phase, 1)

        // Phase 2
        let g2 = CompanionEngine.synthesize(
            durationMs: 4 * 60_000,
            tier: 2,
            hr: 105,
            hrBaseline: 70,
            activity: "🔄 Pacing / Stimming",
            guardianState: .monitoring(durationMs: 4 * 60_000)
        )
        XCTAssertEqual(g2.phase, 2)
        XCTAssertTrue(g2.empatheticMessage.contains("Keep pacing"))

        // Phase 3 Safety check
        let g3 = CompanionEngine.synthesize(
            durationMs: 9 * 60_000,
            tier: 2,
            hr: 110,
            hrBaseline: 70,
            activity: "🪑 Sitting Still",
            guardianState: .safetyCheckDue(durationMs: 9 * 60_000)
        )
        XCTAssertEqual(g3.phase, 3)
        XCTAssertTrue(g3.empatheticMessage.contains("physically safe"))

        // Phase 4 Return to calm
        let g4 = CompanionEngine.synthesize(
            durationMs: 10 * 60_000,
            tier: 0,
            hr: 72,
            hrBaseline: 70,
            activity: "🪑 Sitting Still",
            guardianState: .acknowledgedSafe
        )
        XCTAssertEqual(g4.phase, 4)
        XCTAssertTrue(g4.headline.contains("calm"))

        // Nocturnal sensory overload in bed (11 PM)
        let gNight = CompanionEngine.synthesize(
            durationMs: 60_000,
            tier: 2,
            hr: 98,
            hrBaseline: 65,
            activity: "🛏️ Resting in Bed",
            guardianState: .monitoring(durationMs: 60_000),
            hourOfDay: 23
        )
        XCTAssertEqual(gNight.phase, 1)
        XCTAssertEqual(gNight.headline, "Resting body, racing mind")
        XCTAssertTrue(gNight.empatheticMessage.contains("cranial heat"))
    }
}
