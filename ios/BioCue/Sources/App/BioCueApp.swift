import SwiftUI
import FirebaseCore
import FirebaseFirestore
import GoogleSignIn

@main
struct BioCueApp: App {
    @StateObject private var session: SessionStore
    private let env: AppEnvironment

    init() {
        FirebaseApp.configure()   // needs GoogleService-Info.plist in the app bundle (see ios/README.md)
        // Spec 07: a removed caregiver's on-disk cache can only be cleared before Firestore is first used, so a revocation
        // sets a flag and the clean-up happens here, at the start of the next launch.
        if UserDefaults.standard.bool(forKey: CaregiverViewModel.purgeFlagKey) {
            Firestore.firestore().clearPersistence { error in
                if error == nil { UserDefaults.standard.set(false, forKey: CaregiverViewModel.purgeFlagKey) }
            }
        }
        let env = AppEnvironment.live()
        self.env = env
        _session = StateObject(wrappedValue: SessionStore(auth: env.auth, profiles: env.profileCloud))
    }

    var body: some Scene {
        WindowGroup {
            RootView(session: session, env: env)
                .onAppear { session.start() }
                .onOpenURL { GIDSignIn.sharedInstance.handle($0) }
                .tint(Palette.charcoal)
        }
    }
}
