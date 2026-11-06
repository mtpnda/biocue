import SwiftUI
import BioCueCore

/// WEARER host. Builds the watch link, detection service and circle manager — the only place those
/// are created. If the account is not a wearer, construction throws and an error is shown instead,
/// so a caregiver can never end up holding a Bluetooth link to the watch.
struct WearerRoot: View {
    let profile: UserProfile
    let env: AppEnvironment
    let onSignOut: () -> Void

    @StateObject private var holder: Holder
    @State private var showProfile = false
    @Environment(\.scenePhase) private var scenePhase

    @MainActor
    final class Holder: ObservableObject {
        let link: WatchLinkCentral?
        let service: WearerService?
        let circle: WearerCircleViewModel?
        let error: String?

        init(profile: UserProfile, env: AppEnvironment) {
            // Build into locals and assign each `let` exactly once (Swift forbids re-initialising a
            // `let` if a later `try` throws into the catch).
            var builtLink: WatchLinkCentral?
            var builtService: WearerService?
            var builtCircle: WearerCircleViewModel?
            var failure: String?
            do {
                let gate = RoleGate(role: profile.role)
                let newLink = try WatchLinkCentral(gate: gate, accountId: profile.uid)
                let newService = try WearerService(
                    userId: profile.uid, gate: gate, link: newLink,
                    events: FileEventStore(), windows: FileWindowStore(ownerUid: profile.uid),
                    cloud: env.wearerCloud, notifier: LocalAlertNotifier())
                let newCircle = try WearerCircleViewModel(uid: profile.uid, gate: gate, cloud: env.wearerCloud)
                builtLink = newLink; builtService = newService; builtCircle = newCircle
            } catch {
                failure = "This account is not a wearer."
            }
            self.link = builtLink
            self.service = builtService
            self.circle = builtCircle
            self.error = failure
        }
    }

    init(profile: UserProfile, env: AppEnvironment, onSignOut: @escaping () -> Void) {
        self.profile = profile; self.env = env; self.onSignOut = onSignOut
        _holder = StateObject(wrappedValue: Holder(profile: profile, env: env))
    }

    var body: some View {
        if let service = holder.service, let circle = holder.circle, let link = holder.link {
            WearerHomeView(service: service, circle: circle, link: link, userName: profile.displayName,
                           onProfile: { showProfile = true })
                .onAppear {
                    service.start()
                    circle.startReviewImport { review in await service.applyReview(review) }
                }
                .onChange(of: circle.connected) { _ in
                    circle.startReviewImport { review in await service.applyReview(review) }
                }
                .onChange(of: scenePhase) { phase in
                    if phase == .background { Task { await service.refresh() } }
                }
                .sheet(isPresented: $showProfile) {
                    ProfileView(userName: profile.displayName, roleLabel: "Wearer", onClose: { showProfile = false },
                                manageConnections: nil, onSignOut: {
                                    service.stop(); circle.stop(); link.shutdown(); onSignOut()
                                })
                }
        } else {
            Text(holder.error ?? "Something went wrong.").padding().calmBackground()
        }
    }
}
