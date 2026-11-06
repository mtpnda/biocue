import SwiftUI
import BioCueCore

@MainActor
final class LoginViewModel: ObservableObject {
    @Published var email = ""
    @Published var password = ""
    @Published var fullName = ""
    @Published var isRegister = false
    @Published private(set) var isLoading = false
    @Published private(set) var errorMessage: String?

    private let auth: AuthServicing
    init(auth: AuthServicing) { self.auth = auth }

    func submit() async {
        if isRegister && fullName.trimmingCharacters(in: .whitespaces).isEmpty {
            errorMessage = "Please enter your full name."
            return
        }
        await run {
            if self.isRegister { try await self.auth.register(email: self.email, password: self.password, displayName: self.fullName) }
            else { try await self.auth.signIn(email: self.email, password: self.password) }
        }
    }

    func google() async { await run { try await self.auth.signInWithGoogle() } }
    func apple() async { await run { try await self.auth.signInWithApple() } }
    func quickDemo() async { await run { try await self.auth.signInQuickDemo() } }

    private func run(_ work: @escaping () async throws -> Void) async {
        isLoading = true; errorMessage = nil
        defer { isLoading = false }
        do { try await work() } catch { errorMessage = error.localizedDescription }
    }
}

struct LoginView: View {
    @StateObject var vm: LoginViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Text("🧠").font(.system(size: 44)).frame(width: 96, height: 96)
                    .background(Palette.charcoal, in: RoundedRectangle(cornerRadius: Radius.xl, style: .continuous))
                    .softShadow(radius: 6, y: 3).padding(.top, 56).accessibilityHidden(true)
                Text("BioCue")
                    .font(AppFont.displayMedium)
                    .foregroundStyle(Palette.inkPrimary)
                Text("Find your center.\nStay connected.")
                    .font(AppFont.bodyLarge)
                    .foregroundStyle(Palette.inkSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.bottom, 24)

                PillButton(title: "Continue with Google", loading: vm.isLoading) {
                    Task { await vm.google() }
                }

                OutlinedPillButton(title: "Sign in with Apple") {
                    Task { await vm.apple() }
                }

                GhostButton(title: "Quick Demo Access") {
                    Task { await vm.quickDemo() }
                }
                .padding(.top, 4)

                if let message = vm.errorMessage { InlineNotice(text: message) }
            }
            .padding(.horizontal, 28)
        }
        .calmBackground()
    }
}

struct RoleSelectionView: View {
    @ObservedObject var session: SessionStore

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Text("How will you use BioCue?")
                    .font(AppFont.headlineLarge)
                    .foregroundStyle(Palette.inkPrimary)
                    .multilineTextAlignment(.center)
                    .padding(.top, 48).padding(.bottom, 12)
                Text("This choice is permanent for your account.")
                    .font(AppFont.bodyMedium)
                    .foregroundStyle(Palette.inkSecondary)
                card("🩺", "I wear the watch", "Track my own stress, get gentle support, and choose who can see my day.", .calm, .wearer)
                card("🤝", "I support someone", "Follow a person's stress events and help label what happened.", .info, .caregiver)
                if let message = session.roleError { InlineNotice(text: message) }
            }
            .padding(.horizontal, 24)
        }
        .calmBackground()
    }

    private func card(_ emoji: String, _ title: String, _ body: String, _ tone: StatePalette, _ role: UserRole) -> some View {
        Button { Task { await session.chooseRole(role) } } label: {
            SoftCard(padding: 24) {
                VStack(spacing: 12) {
                    EmojiAvatar(emoji: emoji, size: 72, fill: tone.soft, border: tone.chip)
                    Text(title).font(AppFont.titleLarge).foregroundStyle(Palette.inkPrimary)
                    Text(body).font(AppFont.bodyMedium).foregroundStyle(Palette.inkSecondary).multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
            }
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
    }
}
