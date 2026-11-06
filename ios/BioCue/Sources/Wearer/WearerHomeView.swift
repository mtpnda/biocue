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

                if !service.reviews.isEmpty {
                    ReviewCard(reviews: service.reviews, userName: "You") { id, label in
                        Task { await service.resolveReview(eventId: id, newLabelOrContext: label) }
                    }
                }

                caregivers
                pairingCode
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
        switch service.connection {
        case .connected: StatusPill(text: "Watch connected", palette: .calm)
        case .searching: StatusPill(text: "Searching for watch…", palette: .rising, textColor: Palette.inkPrimary)
        case .disconnected: StatusPill(text: "Watch disconnected", palette: .neutral)
        }
    }

    private var caregivers: some View {
        VStack(alignment: .leading, spacing: 6) {
            SectionHeader(text: "Connected caregivers · \(circle.connected.count) of \(WearerCircleViewModel.maxCaregivers)")
            SoftCard {
                if circle.connected.isEmpty && circle.pending.isEmpty {
                    Text("No caregivers connected yet.").font(AppFont.bodyMedium).foregroundStyle(Palette.inkSecondary)
                }
                ForEach(circle.pending) { p in
                    VStack(alignment: .leading, spacing: 10) {
                        HStack(spacing: 12) {
                            EmojiAvatar(emoji: "🤝", size: 44, fill: StatePalette.info.soft, border: StatePalette.info.chip)
                            VStack(alignment: .leading) {
                                Text(p.caregiverName)
                                    .font(AppFont.titleMedium)
                                    .foregroundStyle(Palette.inkPrimary)
                                Text("Wants to join your circle")
                                    .font(AppFont.bodySmall)
                                    .foregroundStyle(Palette.inkSecondary)
                            }
                        }
                        HStack(spacing: 10) {
                            OutlinedPillButton(title: "Deny", border: StatePalette.alert.chip, ink: StatePalette.alert.ink) { Task { await circle.deny(p) } }
                            PillButton(title: "Approve", disabled: !circle.canAddCaregiver) { Task { await circle.approve(p) } }
                        }
                    }
                    .padding(.vertical, 6)
                    Divider().overlay(Palette.line)
                }
                ForEach(Array(circle.connected.enumerated()), id: \.element.id) { index, c in
                    HStack(spacing: 12) {
                        EmojiAvatar(emoji: "👤", size: 44)
                        VStack(alignment: .leading) {
                            Text(c.caregiverName)
                                .font(AppFont.titleMedium)
                                .foregroundStyle(Palette.inkPrimary)
                            Text("Connected · reads stress events")
                                .font(AppFont.bodySmall)
                                .foregroundStyle(Palette.inkSecondary)
                        }
                        Spacer()
                        Button("Remove") { Task { await circle.remove(c) } }
                            .font(AppFont.labelLarge)
                            .foregroundStyle(StatePalette.alert.ink)
                            .frame(minHeight: Metrics.minTouch)
                    }
                    if index < circle.connected.count - 1 { Divider().overlay(Palette.line) }
                }
                if let message = circle.errorMessage {
                    InlineNotice(text: message).padding(.top, 8)
                }
            }
        }
    }

    private var pairingCode: some View {
        VStack(alignment: .leading, spacing: 6) {
            SectionHeader(text: "Add a caregiver")
            SoftCard {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Pairing code")
                        .font(AppFont.titleMedium)
                        .foregroundStyle(Palette.inkPrimary)
                    Text("Single-use · expires in 10 minutes. Share it out loud or in person.")
                        .font(AppFont.bodyMedium).foregroundStyle(Palette.inkSecondary)
                    PairingCodeView(code: circle.pairingCode)
                    Text(circle.pairingCode.isEmpty ? "No active code"
                         : String(format: "Expires in %d:%02d", circle.secondsRemaining / 60, circle.secondsRemaining % 60))
                        .font(AppFont.bodySmall).foregroundStyle(Palette.inkSecondary).frame(maxWidth: .infinity)
                    PillButton(title: circle.canAddCaregiver ? "Generate pairing code" : "Maximum caregivers reached",
                               loading: circle.isWorking, disabled: !circle.canAddCaregiver) {
                        Task { await circle.generateCode() }
                    }
                }
            }
        }
    }

    private var privacy: some View {
        VStack(alignment: .leading, spacing: 6) {
            SectionHeader(text: "What leaves this phone")
            SoftCard {
                privacyRow("📤", "Stress events", "Start, end, activity and your label — synced to your circle", .info)
                Divider().overlay(Palette.line)
                privacyRow("🔒", "Heart data & movement", "Stays on this phone, always", .neutral)
                Divider().overlay(Palette.line)
                privacyRow("📍", "Location", "Stays on this phone; only used to show where an SOS happened", .rising)
            }
        }
    }

    private func privacyRow(_ emoji: String, _ title: String, _ subtitle: String, _ tone: StatePalette) -> some View {
        HStack(spacing: 12) {
            EmojiAvatar(emoji: emoji, size: 44, fill: tone.soft, border: tone.chip)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(AppFont.titleMedium)
                    .foregroundStyle(Palette.inkPrimary)
                Text(subtitle)
                    .font(AppFont.bodySmall)
                    .foregroundStyle(Palette.inkSecondary)
            }
        }
        .padding(.vertical, 8)
        .accessibilityElement(children: .combine)
    }

    #if DEBUG
    private var developerTools: some View {
        SoftCard(container: Palette.surface2) {
            VStack(alignment: .leading, spacing: 10) {
                Text("DEVELOPER TOOLS").font(AppFont.labelSmall).foregroundStyle(Palette.inkSecondary)
                Text("Sensor mock (no watch needed)").font(AppFont.bodySmall).foregroundStyle(Palette.inkSecondary)
                HStack(spacing: 8) {
                    chip("Off") { service.setMockIntensity(.off) }
                    chip("Low") { service.setMockIntensity(.low) }
                    chip("Medium") { service.setMockIntensity(.medium) }
                    chip("High") { service.setMockIntensity(.high) }
                }
                HStack(spacing: 8) {
                    chip("Overwhelm") { Task { await service.simulateManualEvent(label: EventTypes.labelOverwhelmed) } }
                    chip("Excited") { Task { await service.simulateManualEvent(label: EventTypes.labelExcited) } }
                }
            }
        }
    }

    private func chip(_ title: String, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title).font(AppFont.labelMedium).frame(maxWidth: .infinity, minHeight: Metrics.minTouch)
                .background(Palette.card, in: Capsule()).overlay(Capsule().stroke(Palette.line, lineWidth: 1))
        }
        .foregroundStyle(Palette.inkPrimary)
    }
    #endif
}
