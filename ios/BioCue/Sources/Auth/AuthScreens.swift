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
            VStack(spacing: 14) {
                Text("🧠").font(.system(size: 38)).frame(width: 88, height: 88)
                    .background(Palette.charcoal, in: RoundedRectangle(cornerRadius: Radius.xl, style: .continuous))
                    .softShadow(radius: 5, y: 3).padding(.top, 48).accessibilityHidden(true)
                Text("BioCue").font(AppFont.displayMedium)
                Text("Find your center.\nStay connected.").font(AppFont.bodyLarge)
                    .foregroundStyle(Palette.inkSecondary).multilineTextAlignment(.center).padding(.bottom, 16)

                if vm.isRegister { CalmTextField(label: "Full name", text: $vm.fullName, contentType: .name, capitalization: .words) }
                CalmTextField(label: "Email address", text: $vm.email, keyboard: .emailAddress, contentType: .emailAddress)
                CalmTextField(label: "Password", text: $vm.password, secure: true, contentType: vm.isRegister ? .newPassword : .password)

                PillButton(title: vm.isRegister ? "Create Account" : "Sign In", loading: vm.isLoading) { Task { await vm.submit() } }
                    .padding(.top, 8)

                HStack {
                    Rectangle().fill(Palette.line).frame(height: 1)
                    Text("or").font(AppFont.bodySmall).foregroundStyle(Palette.inkSecondary)
                    Rectangle().fill(Palette.line).frame(height: 1)
                }
                OutlinedPillButton(title: "Continue with Google") { Task { await vm.google() } }

                Button(vm.isRegister ? "Already have an account? Sign In" : "Don't have an account? Create one") { vm.isRegister.toggle() }
                    .font(AppFont.labelLarge).foregroundStyle(Palette.inkPrimary).frame(minHeight: Metrics.minTouch)

                if let message = vm.errorMessage { InlineNotice(text: message) }
            }
            .padding(.horizontal, 28)
        }
        .calmBackground()
        .scrollDismissesKeyboard(.interactively)
    }
}

struct RoleSelectionView: View {
    @ObservedObject var session: SessionStore

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Text("How will you use BioCue?").font(AppFont.headlineLarge).multilineTextAlignment(.center)
                    .padding(.top, 48).padding(.bottom, 12)
                Text("This choice is permanent for your account.").font(AppFont.bodyMedium).foregroundStyle(Palette.inkSecondary)
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
