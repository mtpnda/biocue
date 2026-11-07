import SwiftUI
import BioCueCore

// WEARER-ONLY home screen ("My Circle"): watch link, stress state, review, caregivers, privacy, journal.
struct WearerHomeView: View {
    @ObservedObject var service: WearerService
    @ObservedObject var circle: WearerCircleViewModel
    let link: WatchLinking
    let userName: String
    let onProfile: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                TopBar(pill: AnyView(connectionPill), onProfile: onProfile)

                VStack(alignment: .leading, spacing: 4) {
                    Text("My Circle")
                        .font(AppFont.headlineLarge)
                        .foregroundStyle(Palette.inkPrimary)
                        .accessibilityAddTraits(.isHeader)
                    Text("Hi \(userName) — you control who can see your day.")
                        .font(AppFont.bodyLarge)
                        .foregroundStyle(Palette.inkSecondary)
                }

                WatchLinkCard(state: service.linkState, onConnect: link.beginPairing, onForget: link.forgetWatch)

                if case .connected = service.linkState {
                    StatusMeterCard(userName: "You", tier: service.tier, activity: service.activityLabel)
                }

                // BioCue Buddy: On-device empathetic grounding & crisis guardian (Spec 19)
                if service.companionGuidance != nil || service.tier >= 1 {
                    BioCueBuddyCard(service: service)
                }

                if !service.reviews.isEmpty {
                    ReviewCard(reviews: service.reviews, userName: "You") { id, label in
                        Task { await service.resolveReview(eventId: id, newLabelOrContext: label) }
                    }
                }

                privacy

                JournalCard(entries: service.journal, offset: service.journalOffset) { delta in
                    service.changeJournalDate(delta: delta - service.journalOffset)
                }

                #if DEBUG
                developerTools
                #endif
            }
            .padding(.horizontal, 20).padding(.bottom, 32)
        }
        .calmBackground()
        .onAppear { circle.start() }
    }

    // MARK: sections

    @ViewBuilder private var connectionPill: some View {
        switch service.linkState {
        case .connected:
            StatusPill(text: "Watch connected", palette: .calm)
        case .connecting, .handshaking:
            StatusPill(text: "Connecting watch…", palette: .rising, textColor: Palette.inkPrimary)
        case .scanning:
            StatusPill(text: "Searching for watch…", palette: .rising, textColor: Palette.inkPrimary)
        default:
            StatusPill(text: "Watch disconnected", palette: .neutral)
        }
    }

    @State private var privacyExpanded = false

    private var privacy: some View {
        SoftCard(container: Palette.surface2, padding: 14) {
            VStack(alignment: .leading, spacing: 10) {
                Button(action: { withAnimation(.easeInOut(duration: 0.2)) { privacyExpanded.toggle() } }) {
                    HStack {
                        Image(systemName: "lock.shield")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Palette.inkSecondary)
                        Text("PRIVACY & DATA GUARANTEE")
                            .font(AppFont.labelSmall)
                            .tracking(0.5)
                            .foregroundStyle(Palette.inkSecondary)
                        Spacer()
                        Text(privacyExpanded ? "Hide" : "Review")
                            .font(AppFont.labelSmall)
                            .foregroundStyle(Palette.inkMuted)
                        Image(systemName: privacyExpanded ? "chevron.up" : "chevron.down")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(Palette.inkMuted)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                if privacyExpanded {
                    Divider().overlay(Palette.line)
                    VStack(alignment: .leading, spacing: 10) {
                        privacyRow("📤", "Stress events", "Start, end, and your label — shared only with approved circle", .info)
                        Divider().overlay(Palette.line)
                        privacyRow("🔒", "Heart data & movement", "Stays strictly on this phone and watch, always", .neutral)
                        Divider().overlay(Palette.line)
                        privacyRow("📍", "Emergency location", "Sent once, only if an emergency SOS is triggered", .rising)
                    }
                }
            }
        }
    }

    private func privacyRow(_ emoji: String, _ title: String, _ subtitle: String, _ tone: StatePalette) -> some View {
        HStack(spacing: 12) {
            EmojiAvatar(emoji: emoji, size: 36, fill: tone.soft, border: tone.chip)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(AppFont.titleMedium)
                    .foregroundStyle(Palette.inkPrimary)
                Text(subtitle)
                    .font(AppFont.bodySmall)
                    .foregroundStyle(Palette.inkSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
    }

    #if DEBUG
    @State private var devToolsExpanded = false

    private var developerTools: some View {
        SoftCard(container: Palette.surface2, padding: 14) {
            VStack(alignment: .leading, spacing: 12) {
                Button(action: { withAnimation(.easeInOut(duration: 0.2)) { devToolsExpanded.toggle() } }) {
                    HStack {
                        Image(systemName: "wrench.and.screwdriver")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Palette.inkSecondary)
                        Text("DEVELOPER TOOLS")
                            .font(AppFont.labelSmall)
                            .tracking(0.5)
                            .foregroundStyle(Palette.inkSecondary)
                        Spacer()
                        Image(systemName: devToolsExpanded ? "chevron.up" : "chevron.down")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(Palette.inkMuted)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                if devToolsExpanded {
                    Divider().overlay(Palette.line)

                    VStack(alignment: .leading, spacing: 10) {
                        Text("Sensor mock intensity")
                            .font(AppFont.bodySmall)
                            .foregroundStyle(Palette.inkSecondary)

                        HStack(spacing: 6) {
                            mockIntensityButton(title: "Off", intensity: .off)
                            mockIntensityButton(title: "Low", intensity: .low)
                            mockIntensityButton(title: "Medium", intensity: .medium)
                            mockIntensityButton(title: "High", intensity: .high)
                        }

                        Text("Simulate Spike")
                            .font(AppFont.bodySmall)
                            .foregroundStyle(Palette.inkSecondary)
                            .padding(.top, 4)

                        HStack(spacing: 8) {
                            simButton("Overwhelm", icon: "⚡️") {
                                Task { await service.simulateManualEvent(label: EventTypes.labelOverwhelmed) }
                            }
                            simButton("Excited", icon: "✨") {
                                Task { await service.simulateManualEvent(label: EventTypes.labelExcited) }
                            }
                        }
                    }
                }
            }
        }
    }

    private func mockIntensityButton(title: String, intensity: WearerService.MockIntensity) -> some View {
        let isSelected = service.mockIntensity == intensity
        return Button(action: { service.setMockIntensity(intensity) }) {
            Text(title)
                .font(AppFont.labelMedium)
                .foregroundStyle(isSelected ? Palette.onCharcoal : Palette.inkPrimary)
                .frame(maxWidth: .infinity, minHeight: 34)
                .background(isSelected ? Palette.charcoal : Palette.card, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 8, style: .continuous).stroke(Palette.line, lineWidth: isSelected ? 0 : 1))
        }
    }

    private func simButton(_ title: String, icon: String, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(icon).font(.system(size: 13))
                Text(title).font(AppFont.labelMedium)
            }
            .foregroundStyle(Palette.inkPrimary)
            .frame(maxWidth: .infinity, minHeight: 36)
            .background(Palette.card, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(Palette.line, lineWidth: 1))
        }
    }
    #endif
}
