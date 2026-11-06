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
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                SectionHeader(text: "Journal · \(dayLabel)")
                Spacer()
                CircleIconButton(systemName: "chevron.left", label: "Previous day") { onChangeDay(offset - 1) }
                if offset < 0 {
                    CircleIconButton(systemName: "chevron.right", label: "Next day") { onChangeDay(offset + 1) }
                } else {
                    Color.clear.frame(width: Metrics.minTouch, height: Metrics.minTouch)  // keep the header from shifting
                }
            }
            SoftCard(padding: 16) {
                if entries.isEmpty {
                    Text("No events on this day. All calm ✨").font(AppFont.bodyMedium)
                        .foregroundStyle(Palette.inkSecondary).padding(.vertical, 10)
                } else {
                    VStack(spacing: 0) {
                        ForEach(Array(entries.enumerated()), id: \.element.id) { index, entry in
                            let tint = Color.fromHexString(entry.colorHex)
                            HStack(alignment: .top, spacing: 12) {
                                EmojiAvatar(emoji: entry.labelIcon, fill: tint.opacity(0.30), border: tint.opacity(0.6))
                                VStack(alignment: .leading, spacing: 6) {
                                    Text(entry.labelText).font(AppFont.labelMedium).foregroundStyle(Palette.inkPrimary)
                                        .padding(.horizontal, 10).padding(.vertical, 4)
                                        .background(tint.opacity(0.28), in: Capsule())
                                        .overlay(Capsule().stroke(tint.opacity(0.6), lineWidth: 1))
                                    Text(entry.activity).font(AppFont.bodyMedium).foregroundStyle(Palette.inkSecondary)
                                }
                                Spacer(minLength: 8)
                                Text(entry.time).font(AppFont.bodySmall).foregroundStyle(Palette.inkMuted)
                            }
                            .padding(.vertical, 12)
                            .accessibilityElement(children: .combine)
                            if index < entries.count - 1 { Divider().overlay(Palette.line) }
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

    private var isUnlabeled: Bool { review.currentLabel == EventTypes.labelNone }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Circle().fill(StatePalette.rising.chip).frame(width: 8, height: 8)
                Text("Review needed · \(review.time)").font(AppFont.labelMedium)
            }
            Text(isUnlabeled
                 ? "A stress spike was detected but \(userName) didn't label it on the watch."
                 : "\(userName) reported feeling \(review.currentLabel.lowercased().capitalized). Please add environmental context to help BioCue learn.")
                .font(AppFont.bodyLarge)

            VStack(alignment: .leading, spacing: 8) {
                Text("CONTEXT CUES").font(AppFont.labelSmall).foregroundStyle(Palette.inkSecondary)
                ForEach(Array(review.cues.enumerated()), id: \.offset) { _, cue in
                    HStack(spacing: 8) {
                        Text(cue.icon); Text(cue.text).font(AppFont.bodyMedium)
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
