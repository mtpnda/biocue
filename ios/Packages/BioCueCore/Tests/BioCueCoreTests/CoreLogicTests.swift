import XCTest
@testable import BioCueCore

final class RoleCapabilitiesTests: XCTestCase {

    /// Pins the permission matrix documented in docs/SPEC.md §Roles and mirrored in firestore.rules.
    func testWearerCapabilities() {
        let c = UserRole.wearer.capabilities
        XCTAssertTrue(c.linkWatch)
        XCTAssertTrue(c.createEvents)
        XCTAssertTrue(c.manageCaregivers)
        XCTAssertFalse(c.requestPairing)
        XCTAssertFalse(c.readPairedWearerEvents)
        XCTAssertFalse(c.labelPairedWearerEvents)
    }

    func testCaregiverCapabilities() {
        let c = UserRole.caregiver.capabilities
        XCTAssertFalse(c.linkWatch)
        XCTAssertFalse(c.createEvents)
        XCTAssertFalse(c.manageCaregivers)
        XCTAssertTrue(c.requestPairing)
        XCTAssertTrue(c.readPairedWearerEvents)
        XCTAssertTrue(c.labelPairedWearerEvents)
    }

    func testRolesAreMutuallyExclusiveOnEveryCapability() {
        let w = UserRole.wearer.capabilities, c = UserRole.caregiver.capabilities
        XCTAssertNotEqual(w.linkWatch, c.linkWatch)
        XCTAssertNotEqual(w.createEvents, c.createEvents)
        XCTAssertNotEqual(w.manageCaregivers, c.manageCaregivers)
        XCTAssertNotEqual(w.requestPairing, c.requestPairing)
        XCTAssertNotEqual(w.readPairedWearerEvents, c.readPairedWearerEvents)
        XCTAssertNotEqual(w.labelPairedWearerEvents, c.labelPairedWearerEvents)
    }

    func testRoleGateThrowsForWrongRoleAndPassesForRightOne() {
        XCTAssertThrowsError(try RoleGate(role: .caregiver).require(\.linkWatch, action: "link watch")) {
            XCTAssertEqual($0 as? RoleError, .notPermitted(role: .caregiver, action: "link watch"))
        }
        XCTAssertThrowsError(try RoleGate(role: .wearer).require(\.requestPairing, action: "request pairing"))
        XCTAssertNoThrow(try RoleGate(role: .wearer).require(\.linkWatch, action: "link watch"))
        XCTAssertNoThrow(try RoleGate(role: .caregiver).require(\.labelPairedWearerEvents, action: "label"))
    }

    func testRawValuesMatchTheStringsStoredInFirestore() {
        XCTAssertEqual(UserRole.wearer.rawValue, "WEARER")
        XCTAssertEqual(UserRole.caregiver.rawValue, "CAREGIVER")
        XCTAssertEqual(UserRole(rawValue: "WEARER"), .wearer)
        XCTAssertNil(UserRole(rawValue: "wearer"))
    }

    func testLinkRoleMapping() {
        XCTAssertEqual(UserRole.wearer.linkRole, .wearer)
        XCTAssertEqual(UserRole.caregiver.linkRole, .caregiver)
        XCTAssertEqual(LinkRole.wearer.rawValue, 0)
        XCTAssertEqual(LinkRole.caregiver.rawValue, 1)
    }
}

final class EventIngestTests: XCTestCase {
    private func event(label: String = "NONE", end: Int64? = nil, resolved: Bool = false) -> StressEvent {
        StressEvent(id: "e1", userId: "u", startTime: 100, endTime: end, triggerType: "MANUAL", label: label, isResolved: resolved)
    }

    func testStartCreatesAnUnresolvedUnlabelledEvent() {
        let e = EventIngest.applyStart(existing: nil, id: "e1", userId: "u", timestamp: 5, isManual: false)
        XCTAssertEqual(e, StressEvent(id: "e1", userId: "u", startTime: 5, endTime: nil, triggerType: "AUTO", label: "NONE", isResolved: false))
        XCTAssertEqual(EventIngest.applyStart(existing: nil, id: "e1", userId: "u", timestamp: 5, isManual: true)?.triggerType, "MANUAL")
    }

    func testDuplicateStartIsIgnoredAndCannotResetAnEvent() {
        let resolved = event(label: "OVERWHELMED - Loud Noises", end: 200, resolved: true)
        XCTAssertNil(EventIngest.applyStart(existing: resolved, id: "e1", userId: "u", timestamp: 100, isManual: true))
    }

    func testLabelAppliesAndReopensReview() {
        let e = EventIngest.applyLabel(existing: event(), label: "EXCITED")
        XCTAssertEqual(e?.label, "EXCITED")
        XCTAssertEqual(e?.isResolved, false)
    }

    func testLabelForUnknownEventIsIgnored() { XCTAssertNil(EventIngest.applyLabel(existing: nil, label: "X")) }

    func testStaleLabelRetransmissionCannotOverwriteAFinishedReview() {
        let done = event(label: "OVERWHELMED - Crowded Space", resolved: true)
        XCTAssertNil(EventIngest.applyLabel(existing: done, label: "OVERWHELMED"))
    }

    func testSameLabelTwiceIsANoOp() { XCTAssertNil(EventIngest.applyLabel(existing: event(label: "EXCITED"), label: "EXCITED")) }

    func testEndSetsOnceAndRepeatIsIgnored() {
        let ended = EventIngest.applyEnd(existing: event(), timestamp: 300)
        XCTAssertEqual(ended?.endTime, 300)
        XCTAssertNil(EventIngest.applyEnd(existing: ended, timestamp: 999))
        XCTAssertNil(EventIngest.applyEnd(existing: nil, timestamp: 1))
    }
}

final class DetectionEdgeCaseTests: XCTestCase {
    func testStressPercentIsTotal() {
        XCTAssertEqual(DetectionPipeline.stressPercent(.nan), 0)
        XCTAssertEqual(DetectionPipeline.stressPercent(.infinity), 100)
        XCTAssertEqual(DetectionPipeline.stressPercent(-.infinity), 0)
        XCTAssertEqual(DetectionPipeline.stressPercent(1.5), 50)
        XCTAssertEqual(DetectionPipeline.stressPercent(-5), 0)
        XCTAssertEqual(DetectionPipeline.stressPercent(99), 100)
    }

    func testTierBoundariesAreStrict() {
        XCTAssertEqual(DetectionPipeline.tierFor(1.5), 0)
        XCTAssertEqual(DetectionPipeline.tierFor(1.5001), 1)
        XCTAssertEqual(DetectionPipeline.tierFor(2.5), 1)
        XCTAssertEqual(DetectionPipeline.tierFor(2.5001), 2)
    }

    func testOrphanBoundaryIsExactlyFifteenMinutes() {
        let now: Int64 = 1_700_000_000_000
        let hot = SensorWindow(timestamp: now, hrMean: 95, rmssd: 8, accMag: 0.1, repetitiveness: 0, hrSlope: 0, isOffBody: false, isExercising: false)
        let atEdge = LatestEvent(id: "e", startTime: now - 15 * 60_000, endTime: nil)
        let justInside = LatestEvent(id: "e", startTime: now - 15 * 60_000 + 1, endTime: nil)
        XCTAssertEqual(DetectionPipeline.decide(window: hot, baseline: .appDefault, latestEvent: atEdge, nowMs: now).actions, [.createAutoEvent(startTime: now)])
        XCTAssertEqual(DetectionPipeline.decide(window: hot, baseline: .appDefault, latestEvent: justInside, nowMs: now).actions, [])
    }

    func testOffBodyIsAlwaysCalm() {
        let w = SensorWindow(timestamp: 1, hrMean: 200, rmssd: 1, accMag: 1, repetitiveness: 1, hrSlope: 9, isOffBody: true, isExercising: false)
        let d = DetectionPipeline.decide(window: w, baseline: .appDefault, latestEvent: nil, nowMs: 1)
        XCTAssertEqual(d.score, 0)
        XCTAssertEqual(d.tier, 0)
    }

    func testClustererEmptyAndSingle() {
        XCTAssertEqual(EventClusterer.cluster([]), [])
        let e = StressEvent(id: "a", userId: "u", startTime: 1, endTime: 2, triggerType: "AUTO", label: "NONE", isResolved: false)
        XCTAssertEqual(EventClusterer.cluster([e]), [EventCluster(startTime: 1, endTime: 2, events: [e])])
    }
}
