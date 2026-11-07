import XCTest
@testable import BioCueCore

final class OnDeviceIntelligenceTests: XCTestCase {

    func testGovernorPermitsSLMWhenBatteryHealthyAndThermallyNominal() {
        let state = DeviceHardwareState(batteryLevel: 0.85, isLowPowerMode: false, thermalState: 0, isCharging: false)
        XCTAssertTrue(state.isSlmExecutionPermitted)

        let reflection = OnDeviceIntelligenceEngine.synthesize(
            hardwareState: state,
            autonomicScore: 2.4,
            hrMean: 95.0,
            rmssd: 18.0,
            activity: "🛏️ Resting in Bed",
            note: "headache and head heat",
            hourOfDay: 23
        )

        XCTAssertEqual(reflection.executionPath, .gemma2BInt4)
        XCTAssertFalse(reflection.isThrottled)
        XCTAssertTrue(reflection.summary.contains("sympathetic heat"))
        XCTAssertTrue(reflection.somaticAnchorHint.contains("cool damp cloth"))
    }

    func testGovernorThrottlesWhenBatteryBelowTwentyPercent() {
        let lowBattery = DeviceHardwareState(batteryLevel: 0.15, isLowPowerMode: false, thermalState: 0, isCharging: false)
        XCTAssertFalse(lowBattery.isSlmExecutionPermitted)

        let reflection = OnDeviceIntelligenceEngine.synthesize(
            hardwareState: lowBattery,
            autonomicScore: 2.1,
            hrMean: 90.0,
            rmssd: 22.0,
            activity: "🛏️ Resting in Bed",
            note: "",
            hourOfDay: 22
        )

        XCTAssertEqual(reflection.executionPath, .lightRegexNLP)
        XCTAssertTrue(reflection.isThrottled)
        XCTAssertTrue(reflection.summary.contains("Throttled"))
    }

    func testGovernorThrottlesWhenInLowPowerMode() {
        let lowPower = DeviceHardwareState(batteryLevel: 0.80, isLowPowerMode: true, thermalState: 0, isCharging: false)
        XCTAssertFalse(lowPower.isSlmExecutionPermitted)

        let reflection = OnDeviceIntelligenceEngine.synthesize(
            hardwareState: lowPower,
            autonomicScore: 1.8,
            hrMean: 85.0,
            rmssd: 30.0,
            activity: "🪑 Sitting Still",
            note: "",
            hourOfDay: 14
        )

        XCTAssertEqual(reflection.executionPath, .lightRegexNLP)
        XCTAssertTrue(reflection.isThrottled)
    }

    func testGovernorThrottlesWhenThermalStateSeriousOrCritical() {
        let hotPhone = DeviceHardwareState(batteryLevel: 0.90, isLowPowerMode: false, thermalState: 2, isCharging: false)
        XCTAssertFalse(hotPhone.isSlmExecutionPermitted)

        let reflection = OnDeviceIntelligenceEngine.synthesize(
            hardwareState: hotPhone,
            autonomicScore: 2.5,
            hrMean: 100.0,
            rmssd: 15.0,
            activity: "🚶 Active",
            note: "",
            hourOfDay: 12
        )

        XCTAssertEqual(reflection.executionPath, .lightRegexNLP)
        XCTAssertTrue(reflection.isThrottled)
    }

    func testChargingAllowsExecutionEvenAtLowBattery() {
        let charging = DeviceHardwareState(batteryLevel: 0.18, isLowPowerMode: false, thermalState: 0, isCharging: true)
        XCTAssertTrue(charging.isSlmExecutionPermitted)

        let reflection = OnDeviceIntelligenceEngine.synthesize(
            hardwareState: charging,
            autonomicScore: 2.0,
            hrMean: 88.0,
            rmssd: 25.0,
            activity: "🪑 Sitting Still",
            note: "loud noise",
            hourOfDay: 15
        )

        XCTAssertEqual(reflection.executionPath, .gemma2BInt4)
        XCTAssertFalse(reflection.isThrottled)
    }
}
