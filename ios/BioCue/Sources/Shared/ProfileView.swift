import SwiftUI

/// Role-agnostic profile sheet. It receives plain values and closures; the role only decides which
/// optional rows the HOST passes in.
struct ProfileView: View {
    let userName: String
    let roleLabel: String
    let onClose: () -> Void
    let manageConnections: (() -> Void)?
    let onSignOut: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                CircleIconButton(systemName: "chevron.left", label: "Back", action: onClose)
                VStack(alignment: .leading, spacing: 0) {
                    Text("Profile").font(AppFont.headlineSmall).accessibilityAddTraits(.isHeader)
                    Text("Your account").font(AppFont.bodySmall).foregroundStyle(Palette.inkSecondary)
                }
                Spacer()
            }
            .padding(.horizontal, 16).padding(.vertical, 12)

            ScrollView {
                VStack(spacing: 20) {
                    VStack(spacing: 10) {
                        Text(String(userName.trimmingCharacters(in: .whitespaces).prefix(1)).uppercased().nonEmpty("?"))
                            .font(AppFont.serif(38)).foregroundStyle(Palette.onCharcoal)
                            .frame(width: 88, height: 88).background(Palette.charcoal, in: Circle()).softShadow(radius: 5, y: 3)
                        Text(userName).font(AppFont.titleLarge)
                        StatusPill(text: "Role: \(roleLabel)", palette: .neutral, showDot: false)
                    }
                    .padding(.top, 8)
                    .accessibilityElement(children: .combine)

                    if let manageConnections = manageConnections {
                        VStack(alignment: .leading, spacing: 6) {
                            SectionHeader(text: "Settings")
                            SoftCard(padding: 0) {
                                Button(action: manageConnections) {
                                    HStack(spacing: 14) {
                                        EmojiAvatar(emoji: "🔗", size: 44)
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text("Manage Connections").font(AppFont.titleMedium).foregroundStyle(Palette.inkPrimary)
                                            Text("Pair with a wearer").font(AppFont.bodySmall).foregroundStyle(Palette.inkSecondary)
                                        }
                                        Spacer()
                                        Image(systemName: "chevron.right").foregroundStyle(Palette.inkSecondary)
                                    }
                                    .padding(16).frame(minHeight: 72)
                                }
                            }
                        }
                    }
                }
                .padding(.horizontal, 20)
            }

            VStack(alignment: .leading, spacing: 6) {
                SectionHeader(text: "Account")
                CoralOutlinedPillButton(title: "Log out", action: onSignOut)
            }
            .padding(.horizontal, 20).padding(.vertical, 16)
        }
        .calmBackground()
    }
}

private extension String {
    func nonEmpty(_ fallback: String) -> String { isEmpty ? fallback : self }
}
