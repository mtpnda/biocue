import SwiftUI
import BioCueCore

// Role-AGNOSTIC views used by both the wearer and caregiver apps. They take plain values and
// closures only — never a service, cloud protocol or role — so they cannot leak a capability.

struct StatusMeterCard: View {
    let userName: String
    let tier: Int
    let activity: String

    private var palette: StatePalette { .forTier(tier) }
    private var fraction: CGFloat { tier == 0 ? 0.18 : (tier == 1 ? 0.55 : 0.92) }
    private var title: String {
        switch tier {
        case 0: return "\(userName) is calm ✨"
        case 1: return "\(userName)'s stress is rising"
        default: return "\(userName) may need support"
        }
    }

    var body: some View {
        SoftCard {
            VStack(alignment: .leading, spacing: 14) {
                Text(title).font(AppFont.titleLarge).foregroundStyle(palette.ink)
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Palette.surface2).overlay(Capsule().stroke(Palette.line, lineWidth: 1))
                        Capsule().fill(palette.chip).frame(width: max(12, geo.size.width * fraction))
                            .animation(.easeOut(duration: 0.4), value: fraction)
                    }
                }
                .frame(height: 12)
                .accessibilityHidden(true)
                HStack {
                    Text(tier > 0 ? "🔄 Pacing" : activity)
                        .font(AppFont.labelMedium).foregroundStyle(Palette.inkPrimary)
                        .padding(.horizontal, 12).padding(.vertical, 7)
                        .background(Palette.surface2, in: Capsule())
                        .overlay(Capsule().stroke(Palette.line, lineWidth: 1))
                    Spacer()
                    Text("Updated just now").font(AppFont.bodySmall).foregroundStyle(Palette.inkSecondary)
                }
            }
        }
        .accessibilityElement(children: .combine)
    }
}

struct SosAlertCard: View {
    let userName: String
    var body: some View {
        SoftCard(container: StatePalette.alert.soft, border: StatePalette.alert.chip) {
            VStack(alignment: .leading, spacing: 6) {
                Text("High stress alert").font(AppFont.titleLarge).foregroundStyle(StatePalette.alert.ink)
                Text("\(userName) triggered a manual SOS or their stress has been elevated for 5 minutes. Please check in with them when you can.")
                    .font(AppFont.bodyLarge).foregroundStyle(Palette.inkPrimary)
            }
        }
    }
}

struct JournalCard: View {
    let entries: [JournalEntry]
    let offset: Int
    let onChangeDay: (Int) -> Void

    private var dayLabel: String {
        switch offset {
        case 0: return "Today"
        case -1: return "Yesterday"
        default: return "\(-offset) days ago"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .center) {
                Text("JOURNAL · \(dayLabel.uppercased())")
                    .font(AppFont.labelSmall).tracking(0.5)
                    .foregroundStyle(Palette.inkSecondary)
                    .padding(.leading, 4)
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                HStack(spacing: 8) {
                    CircleIconButton(systemName: "chevron.left", label: "Previous day") { onChangeDay(offset - 1) }
                    CircleIconButton(
                        systemName: "chevron.right",
                        label: "Next day",
                        disabled: offset >= 0
                    ) {
                        onChangeDay(offset + 1)
                    }
                }
            }
            SoftCard(padding: 16) {
                if entries.isEmpty {
                    HStack(spacing: 14) {
                        ZStack {
                            Circle()
                                .fill(StatePalette.calm.soft)
                                .frame(width: 40, height: 40)
                            Image(systemName: "sparkles")
                                .font(.system(size: 18, weight: .medium))
                                .foregroundStyle(StatePalette.calm.ink)
                        }
                        VStack(alignment: .leading, spacing: 3) {
                            Text("All calm on this day")
                                .font(AppFont.titleMedium)
                                .foregroundStyle(Palette.inkPrimary)
                            Text("No stress spikes detected or logged.")
                                .font(AppFont.bodySmall)
                                .foregroundStyle(Palette.inkSecondary)
                        }
                        Spacer()
                    }
                    .padding(.vertical, 4)
                } else {
                    VStack(spacing: 0) {
                        ForEach(Array(entries.enumerated()), id: \.element.id) { index, entry in
                            let tint = Color.fromHexString(entry.colorHex)
                            HStack(alignment: .top, spacing: 14) {
                                EmojiAvatar(emoji: entry.labelIcon, size: 40, fill: tint.opacity(0.18), border: tint.opacity(0.40))
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(entry.labelText)
                                        .font(AppFont.titleMedium)
                                        .foregroundStyle(Palette.inkPrimary)
                                        .fixedSize(horizontal: false, vertical: true)
                                    HStack(spacing: 6) {
                                        Text(entry.activity)
                                            .font(AppFont.bodySmall)
                                            .foregroundStyle(Palette.inkSecondary)
                                        Text("·")
                                            .font(AppFont.bodySmall)
                                            .foregroundStyle(Palette.inkMuted)
                                        Text(entry.time)
                                            .font(AppFont.bodySmall)
                                            .foregroundStyle(Palette.inkMuted)
                                    }
                                }
                                Spacer(minLength: 0)
                            }
                            .padding(.vertical, 10)
                            .accessibilityElement(children: .combine)
                            if index < entries.count - 1 {
                                Divider().overlay(Palette.line).padding(.leading, 54)
                            }
                        }
                    }
                }
            }
        }
    }
}

struct ReviewCard: View {
    let reviews: [ReviewEvent]
    let userName: String
    let onResolve: (_ eventId: String, _ labelOrContext: String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            SectionHeader(text: "Pattern Review")
            SoftCard {
                if let rev = reviews.first {
                    ReviewContent(review: rev, userName: userName, onResolve: onResolve)
                } else {
                    VStack(spacing: 4) {
                        Text("✨").font(.system(size: 28))
                        Text("All caught up").font(AppFont.titleLarge).foregroundStyle(StatePalette.calm.ink)
                        Text("BioCue is learning \(userName)'s patterns.").font(AppFont.bodyMedium)
                            .foregroundStyle(Palette.inkSecondary).multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity).padding(.vertical, 10)
                }
            }
        }
    }
}

private struct ReviewContent: View {
    let review: ReviewEvent
    let userName: String
    let onResolve: (String, String) -> Void

    @State private var customText = ""

    private var isUnlabeled: Bool { review.currentLabel == EventTypes.labelNone }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Circle().fill(StatePalette.rising.chip).frame(width: 8, height: 8)
                Text("Review needed · \(review.time)")
                    .font(AppFont.labelMedium)
                    .foregroundStyle(Palette.inkPrimary)
            }
            Text(isUnlabeled
                 ? "A stress spike was detected but \(userName) didn't label it on the watch."
                 : "\(userName) reported feeling \(review.currentLabel.lowercased().capitalized). Please add environmental context to help BioCue learn.")
                .font(AppFont.bodyLarge)
                .foregroundStyle(Palette.inkPrimary)

            VStack(alignment: .leading, spacing: 8) {
                Text("CONTEXT CUES").font(AppFont.labelSmall).foregroundStyle(Palette.inkSecondary)
                ForEach(Array(review.cues.enumerated()), id: \.offset) { _, cue in
                    HStack(spacing: 8) {
                        Text(cue.icon)
                        Text(cue.text).font(AppFont.bodyMedium).foregroundStyle(Palette.inkPrimary)
                    }
                    .padding(.horizontal, 12).padding(.vertical, 6)
                    .background(Palette.card, in: Capsule()).overlay(Capsule().stroke(Palette.line, lineWidth: 1))
                }
            }
            .padding(14).frame(maxWidth: .infinity, alignment: .leading)
            .background(Palette.surface2, in: RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: Radius.md, style: .continuous).stroke(Palette.line, lineWidth: 1))

            Text(isUnlabeled ? "💡 Ask \(userName) first if you can — they may remember what happened."
                             : "💡 What was happening around \(userName)?")
                .font(AppFont.bodySmall)
                .foregroundStyle(isUnlabeled ? StatePalette.rising.ink : StatePalette.info.ink)

            VStack(spacing: 8) {
                if isUnlabeled {
                    ForEach(EventTypes.defaultLabels, id: \.self) { label in choice(label) }
                    choice(EventTypes.labelNone, title: "Unsure / Wasn't Present", neutral: true)
                } else {
                    ForEach(EventTypes.defaultContexts.filter { $0 != EventTypes.contextUnknown }, id: \.self) { ctx in choice(ctx) }
                    choice(EventTypes.contextUnknown, neutral: true)
                }
            }

            // Free-Text Labeling & Context Input (Spec 19)
            VStack(alignment: .leading, spacing: 6) {
                Text("OR WRITE IN YOUR OWN WORDS")
                    .font(AppFont.labelSmall)
                    .foregroundStyle(Palette.inkSecondary)

                HStack(spacing: 8) {
                    TextField(isUnlabeled ? "What were you feeling or experiencing?" : "Add notes, triggers, or what helped...", text: $customText)
                        .font(AppFont.bodyMedium)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(Palette.surface2, in: RoundedRectangle(cornerRadius: Radius.sm, style: .continuous))
                        .overlay(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).stroke(Palette.line, lineWidth: 1))

                    Button(action: {
                        let trimmed = customText.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !trimmed.isEmpty else { return }
                        onResolve(review.eventId, trimmed)
                        customText = ""
                    }) {
                        Text("Save")
                            .font(AppFont.labelMedium)
                            .foregroundStyle(customText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? Palette.inkMuted : Palette.onCharcoal)
                            .padding(.horizontal, 16)
                            .frame(minHeight: Metrics.buttonHeight)
                            .background(customText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? Palette.surface2 : Palette.charcoal, in: Capsule())
                    }
                    .buttonStyle(.plain)
                    .disabled(customText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .padding(.top, 4)
        }
    }

    private func choice(_ value: String, title: String? = nil, neutral: Bool = false) -> some View {
        let tint = neutral ? StatePalette.neutral.chip : Color.fromHexString(EventTypes.colorHex(forLabel: value))
        return Button { onResolve(review.eventId, value) } label: {
            HStack(spacing: 12) {
                Text(EventTypes.icon(forLabel: value)).font(.system(size: 18))
                Text(title ?? value).font(AppFont.labelMedium).foregroundStyle(Palette.inkPrimary)
                Spacer()
            }
            .padding(.horizontal, 18).frame(minHeight: Metrics.buttonHeight)
            .background(tint.opacity(0.22), in: Capsule())
            .overlay(Capsule().stroke(tint.opacity(0.55), lineWidth: 1))
        }
    }
}

/// Connection pill + settings button shown at the top of both dashboards.
struct TopBar: View {
    var pill: AnyView
    var onProfile: () -> Void
    var body: some View {
        HStack {
            pill
            Spacer()
            CircleIconButton(systemName: "gearshape", label: "Settings and profile", action: onProfile)
        }
    }
}
