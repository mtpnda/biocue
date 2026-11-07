import SwiftUI
import BioCueCore

/// WEARER-ONLY. Caregiver management and pairing code cards displayed inside Settings.
struct WearerCaregiversCard: View {
    @ObservedObject var circle: WearerCircleViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            SectionHeader(text: "Connected caregivers · \(circle.connected.count) of \(WearerCircleViewModel.maxCaregivers)")
            SoftCard {
                VStack(alignment: .leading, spacing: 0) {
                    if circle.connected.isEmpty && circle.pending.isEmpty {
                        HStack(spacing: 12) {
                            EmojiAvatar(emoji: "👥", size: 40)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("No caregivers connected yet")
                                    .font(AppFont.titleMedium)
                                    .foregroundStyle(Palette.inkPrimary)
                                Text("Generate a pairing code below to connect someone.")
                                    .font(AppFont.bodySmall)
                                    .foregroundStyle(Palette.inkSecondary)
                            }
                            Spacer()
                        }
                        .padding(.vertical, 4)
                    }

                    ForEach(circle.pending) { p in
                        VStack(alignment: .leading, spacing: 10) {
                            HStack(spacing: 12) {
                                EmojiAvatar(emoji: "🤝", size: 40, fill: StatePalette.info.soft, border: StatePalette.info.chip)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(p.caregiverName)
                                        .font(AppFont.titleMedium)
                                        .foregroundStyle(Palette.inkPrimary)
                                    Text("Wants to join your circle")
                                        .font(AppFont.bodySmall)
                                        .foregroundStyle(Palette.inkSecondary)
                                }
                            }
                            HStack(spacing: 10) {
                                OutlinedPillButton(title: "Deny", border: StatePalette.alert.chip, ink: StatePalette.alert.ink) {
                                    Task { await circle.deny(p) }
                                }
                                PillButton(title: "Approve", disabled: !circle.canAddCaregiver) {
                                    Task { await circle.approve(p) }
                                }
                            }
                        }
                        .padding(.vertical, 8)
                        Divider().overlay(Palette.line)
                    }

                    ForEach(Array(circle.connected.enumerated()), id: \.element.id) { index, c in
                        HStack(spacing: 12) {
                            EmojiAvatar(emoji: "👤", size: 40)
                            VStack(alignment: .leading, spacing: 2) {
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
                        .padding(.vertical, 8)
                        if index < circle.connected.count - 1 {
                            Divider().overlay(Palette.line)
                        }
                    }

                    if let message = circle.errorMessage {
                        InlineNotice(text: message).padding(.top, 8)
                    }
                }
            }
        }
    }
}

struct WearerPairingCodeCard: View {
    @ObservedObject var circle: WearerCircleViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            SectionHeader(text: "Add a caregiver")
            SoftCard {
                VStack(alignment: .leading, spacing: 12) {
                    HStack(spacing: 12) {
                        EmojiAvatar(emoji: "🤝", size: 40, fill: StatePalette.info.soft, border: StatePalette.info.chip)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Pairing Code")
                                .font(AppFont.titleMedium)
                                .foregroundStyle(Palette.inkPrimary)
                            Text("Share this code to connect a caregiver")
                                .font(AppFont.bodySmall)
                                .foregroundStyle(Palette.inkSecondary)
                        }
                    }

                    Text("Single-use · expires in 10 minutes. Share it out loud or in person.")
                        .font(AppFont.bodyMedium)
                        .foregroundStyle(Palette.inkSecondary)

                    PairingCodeView(code: circle.pairingCode)

                    Text(circle.pairingCode.isEmpty ? "No active code"
                         : String(format: "Expires in %d:%02d", circle.secondsRemaining / 60, circle.secondsRemaining % 60))
                        .font(AppFont.bodySmall)
                        .foregroundStyle(Palette.inkSecondary)
                        .frame(maxWidth: .infinity)

                    PillButton(title: circle.canAddCaregiver ? "Generate pairing code" : "Maximum caregivers reached",
                               loading: circle.isWorking, disabled: !circle.canAddCaregiver) {
                        Task { await circle.generateCode() }
                    }
                }
            }
        }
    }
}
