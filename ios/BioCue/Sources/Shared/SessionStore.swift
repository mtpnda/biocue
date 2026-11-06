import Foundation
import BioCueCore

/// Owns "who is signed in and what role are they?". Everything role-specific hangs off `.ready`.
@MainActor
final class SessionStore: ObservableObject {
    enum Phase: Equatable {
        case starting
        case signedOut
        case loadingProfile
        /// Signed in, Firestore answered, and this account has no role yet.
        case needsRole
        case ready(UserProfile)
        case failed(String)
    }

    @Published private(set) var phase: Phase = .starting
    @Published var roleError: String?

    private let auth: AuthServicing
    private let profiles: ProfileCloud
    private var authToken: ListenerToken?
    private var profileToken: ListenerToken?
    private var currentUid: String?

    init(auth: AuthServicing, profiles: ProfileCloud) {
        self.auth = auth
        self.profiles = profiles
    }

    func start() {
        guard authToken == nil else { return }
        authToken = auth.addListener { [weak self] user in
            Task { @MainActor in self?.authChanged(user) }
        }
    }

    private func authChanged(_ user: AuthUser?) {
        // Always drop the previous account's listener first: a new sign-in must never inherit the
        // previous user's role or screens.
        profileToken = nil
        guard let user = user else {
            currentUid = nil
            phase = .signedOut
            return
        }
        currentUid = user.uid
        phase = .loadingProfile
        subscribe(uid: user.uid)
    }

    private func subscribe(uid: String) {
        profileToken = profiles.observeUserProfile(uid: uid) { [weak self] result in
            Task { @MainActor in self?.profileChanged(uid: uid, result) }
        }
    }

    private func profileChanged(uid: String, _ result: Result<UserProfile?, Error>) {
        guard uid == currentUid else { return } // a stale callback from a previous account
        switch result {
        case .success(let profile):
            if let profile = profile { phase = .ready(profile) } else { phase = .needsRole }
        case .failure(let error):
            phase = .failed(error.localizedDescription)
        }
    }

    func retry() {
        guard let uid = currentUid else { return }
        phase = .loadingProfile
        subscribe(uid: uid)
    }

    /// Roles are permanent (the server refuses to change one), so this only works from `.needsRole`.
    func chooseRole(_ role: UserRole) async {
        guard case .needsRole = phase, let user = auth.currentUser else { return }
        roleError = nil
        let profile = UserProfile(uid: user.uid, role: role, displayName: user.displayName,
                                  pairingCode: nil, pairingCodeExpiresAt: nil)
        do {
            try await profiles.createUserProfile(profile)
            // The profile listener moves us to .ready.
        } catch {
            roleError = "Could not save your role: \(error.localizedDescription)"
        }
    }

    func signOut() {
        profileToken = nil
        try? auth.signOut() // the auth listener moves us to .signedOut
    }

    var currentUser: AuthUser? { auth.currentUser }
}
