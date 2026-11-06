import Foundation

/// Composition root. The three cloud INTERFACES are handed out separately on purpose: the wearer
/// host only ever receives `wearerCloud`, the caregiver host only `caregiverCloud`.
@MainActor
final class AppEnvironment {
    let auth: AuthServicing
    let profileCloud: ProfileCloud
    let wearerCloud: WearerCloud
    let caregiverCloud: CaregiverCloud

    init(auth: AuthServicing, profileCloud: ProfileCloud, wearerCloud: WearerCloud, caregiverCloud: CaregiverCloud) {
        self.auth = auth
        self.profileCloud = profileCloud
        self.wearerCloud = wearerCloud
        self.caregiverCloud = caregiverCloud
    }

    static func live() -> AppEnvironment {
        let cloud = FirebaseCloud()
        return AppEnvironment(auth: FirebaseAuthService(), profileCloud: cloud, wearerCloud: cloud, caregiverCloud: cloud)
    }
}
