import SwiftUI

@main
struct BioCueApp: App {
    @StateObject private var session: SessionStore
    private let env: AppEnvironment

    init() {
        let env = AppEnvironment.live()
        self.env = env
        _session = StateObject(wrappedValue: SessionStore(auth: env.auth, profiles: env.profileCloud))
    }

    var body: some Scene {
        WindowGroup {
            RootView(session: session, env: env)
                .onAppear { session.start() }
                .tint(Palette.charcoal)
                .preferredColorScheme(.light)
        }
    }
}
