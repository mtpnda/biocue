import SwiftUI
import BioCueCore

/// The ONLY place that decides wearer vs caregiver. Each branch builds a self-contained host that
/// never references the other role's code.
struct RootView: View {
    @ObservedObject var session: SessionStore
    let env: AppEnvironment

    var body: some View {
        switch session.phase {
        case .starting, .loadingProfile:
            ProgressView().controlSize(.large).frame(maxWidth: .infinity, maxHeight: .infinity).calmBackground()
        case .signedOut:
            LoginView(vm: LoginViewModel(auth: env.auth))
        case .needsRole:
            RoleSelectionView(session: session)
        case .failed(let message):
            VStack(spacing: 16) {
                InlineNotice(text: message)
                PillButton(title: "Try again") { session.retry() }
                OutlinedPillButton(title: "Log out") { session.signOut() }
            }
            .padding(28).calmBackground()
        case .ready(let profile):
            switch profile.role {
            case .wearer:
                WearerRoot(profile: profile, env: env, onSignOut: session.signOut).id(profile.uid)
            case .caregiver:
                CaregiverRoot(profile: profile, env: env, onSignOut: session.signOut).id(profile.uid)
            }
        }
    }
}
