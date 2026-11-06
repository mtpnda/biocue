import Foundation
import UserNotifications

// WEARER-ONLY. Local alerts on the wearer's own phone (parity with Android `AlertManager`).
// Caregivers are NOT notified from here — see docs/SPEC.md "Known gaps" (no push yet).

protocol AlertNotifying {
    func requestAuthorization() async
    func tier1(percent: Int)
    func tier2(percent: Int)
}

final class LocalAlertNotifier: AlertNotifying {
    private let center = UNUserNotificationCenter.current()

    func requestAuthorization() async {
        _ = try? await center.requestAuthorization(options: [.alert, .sound])
    }

    func tier1(percent: Int) {
        post(id: "tier1", title: "Elevated Stress Signals",
             body: "Stress Level: \(percent)%. Wearer may need a break.", level: .active)
    }

    func tier2(percent: Int) {
        post(id: "tier2", title: "Sustained Distress Pattern",
             body: "Stress Level: \(percent)%. Caregiver attention required immediately.", level: .timeSensitive)
    }

    private func post(id: String, title: String, body: String, level: UNNotificationInterruptionLevel) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.interruptionLevel = level
        // Fixed identifier: a new alert REPLACES the previous one instead of stacking.
        center.add(UNNotificationRequest(identifier: id, content: content, trigger: nil))
    }
}
