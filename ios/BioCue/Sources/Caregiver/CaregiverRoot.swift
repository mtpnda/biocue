import SwiftUI
import BioCueCore

/// CAREGIVER host. Builds only caregiver objects, from the CAREGIVER cloud interface.
/// If the account is not a caregiver, construction fails and an error is shown instead.
struct CaregiverRoot: View {
    let profile: UserProfile
    let env: AppEnvironment
    let onSignOut: () -> Void

    @StateObject private var holder: Holder
    @State private var showProfile = false
    @State private var showPairing = false

    @MainActor
    final class Holder: ObservableObject {
        let vm: CaregiverViewModel?
        let error: String?
        init(profile: UserProfile, env: AppEnvironment) {
            var built: CaregiverViewModel?
            var failure: String?
            do {
                let gate = RoleGate(role: profile.role)
                built = try CaregiverViewModel(uid: profile.uid, displayName: profile.displayName, gate: gate, cloud: env.caregiverCloud)
            } catch {
                failure = "This account is not a caregiver."
            }
            self.vm = built
            self.error = failure
        }
    }

    init(profile: UserProfile, env: AppEnvironment, onSignOut: @escaping () -> Void) {
        self.profile = profile; self.env = env; self.onSignOut = onSignOut
        _holder = StateObject(wrappedValue: Holder(profile: profile, env: env))
    }

    var body: some View {
        if let vm = holder.vm {
            Content(vm: vm, profile: profile, showProfile: $showProfile, showPairing: $showPairing, onSignOut: onSignOut)
        } else {
            Text(holder.error ?? "Something went wrong.").padding().calmBackground()
        }
    }

    private struct Content: View {
        @ObservedObject var vm: CaregiverViewModel
        let profile: UserProfile
        @Binding var showProfile: Bool
        @Binding var showPairing: Bool
        let onSignOut: () -> Void

        var body: some View {
            Group {
                if case .revoked = vm.pairingState {
                    AccessRevokedView { vm.acknowledgeRevocation() }
                } else if case .connected = vm.pairingState {
                    CaregiverHomeView(vm: vm) { showProfile = true }
                } else {
                    CaregiverPairingView(vm: vm)   // no wearer yet: pairing IS the home screen
                }
            }
            .onAppear { vm.start() }
            .sheet(isPresented: $showProfile) {
                ProfileView(userName: profile.displayName, roleLabel: "Caregiver", onClose: { showProfile = false },
                            manageConnections: { showProfile = false; showPairing = true }, onSignOut: onSignOut)
            }
            .sheet(isPresented: $showPairing) { CaregiverPairingView(vm: vm, onDone: { showPairing = false }) }
        }
    }
}
