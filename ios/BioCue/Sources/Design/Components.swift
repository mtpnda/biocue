import SwiftUI

// Shared calm UI atoms (the SwiftUI twin of ui/components/CalmComponents.kt).

extension View {
    /// Low, soft, warm-tinted elevation (never grey).
    func softShadow(radius: CGFloat = 4, y: CGFloat = 2) -> some View {
        shadow(color: DlsTokens.shadowTint.opacity(0.10), radius: radius, x: 0, y: y)
    }

    /// Full-screen warm background so no screen can fall back to white.
    func calmBackground() -> some View {
        background(Palette.bg.ignoresSafeArea())
    }
}

/// Parchment card with a gentle warm border.
struct SoftCard<Content: View>: View {
    var container: Color = Palette.card
    var border: Color = Palette.line
    var padding: CGFloat = 18
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(container, in: RoundedRectangle(cornerRadius: Radius.card, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: Radius.card, style: .continuous).stroke(border, lineWidth: 1))
            .softShadow()
    }
}

/// Primary action: warm charcoal pill with soft cream text.
struct PillButton: View {
    let title: String
    var loading = false
    var disabled = false
    let action: () -> Void

    var body: some View {
        Button(action: { if !loading { action() } }) {
            HStack(spacing: 10) {
                if loading { ProgressView().tint(Palette.onCharcoal) }
                Text(title).font(AppFont.labelLarge)
            }
            .frame(maxWidth: .infinity, minHeight: Metrics.buttonHeight)
            .foregroundStyle(disabled ? Palette.inkMuted : Palette.onCharcoal)
            .background(disabled ? Palette.line : Palette.charcoal, in: Capsule())
        }
        .disabled(disabled)
        .softShadow(radius: disabled ? 0 : 5, y: 3)
        .accessibilityAddTraits(.isButton)
    }
}

/// Secondary / destructive action: outlined pill.
struct OutlinedPillButton: View {
    let title: String
    var border: Color = Palette.lineStrong
    var ink: Color = Palette.inkPrimary
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title).font(AppFont.labelLarge)
                .frame(maxWidth: .infinity, minHeight: Metrics.buttonHeight)
                .foregroundStyle(ink)
                .background(Palette.card, in: Capsule())
                .overlay(Capsule().stroke(border, lineWidth: 1.5))
        }
    }
}

struct CoralOutlinedPillButton: View {
    let title: String
    let action: () -> Void
    var body: some View {
        OutlinedPillButton(title: title, border: StatePalette.alert.chip, ink: StatePalette.alert.ink, action: action)
    }
}

/// Circular icon button: 40pt visual circle inside a 48pt tap target.
struct CircleIconButton: View {
    let systemName: String
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(Palette.inkPrimary)
                .frame(width: 40, height: 40)
                .background(Palette.surface2, in: Circle())
                .overlay(Circle().stroke(Palette.line, lineWidth: 1))
                .frame(width: Metrics.minTouch, height: Metrics.minTouch)
                .contentShape(Circle())
        }
        .accessibilityLabel(label)
    }
}

/// Soft pill badge with an optional status dot.
struct StatusPill: View {
    let text: String
    let palette: StatePalette
    var textColor: Color? = nil
    var showDot = true

    var body: some View {
        HStack(spacing: 8) {
            if showDot { Circle().fill(palette.chip).frame(width: 8, height: 8) }
            Text(text).font(AppFont.labelMedium).foregroundStyle(textColor ?? palette.ink)
        }
        .padding(.horizontal, 12).padding(.vertical, 7)
        .background(palette.soft, in: Capsule())
        .overlay(Capsule().stroke(palette.chip.opacity(0.6), lineWidth: 1))
    }
}

/// Small tinted circle holding an emoji.
struct EmojiAvatar: View {
    let emoji: String
    var size: CGFloat = 40
    var fill: Color = Palette.surface2
    var border: Color = Palette.line

    var body: some View {
        Text(emoji).font(.system(size: size * 0.45))
            .frame(width: size, height: size)
            .background(fill, in: Circle())
            .overlay(Circle().stroke(border, lineWidth: 1))
            .accessibilityHidden(true)
    }
}

/// Uppercase, tracked section label (e.g. "JOURNAL · TODAY").
struct SectionHeader: View {
    let text: String
    var body: some View {
        Text(text.uppercased())
            .font(AppFont.labelSmall).tracking(0.5)
            .foregroundStyle(Palette.inkSecondary)
            .padding(.leading, 4).padding(.top, 8)
            .accessibilityAddTraits(.isHeader)
    }
}

struct CalmTextField: View {
    let label: String
    @Binding var text: String
    var secure = false
    var keyboard: UIKeyboardType = .default
    var contentType: UITextContentType? = nil
    var capitalization: TextInputAutocapitalization = .never

    var body: some View {
        Group {
            if secure { SecureField(label, text: $text) }
            else { TextField(label, text: $text).keyboardType(keyboard).textInputAutocapitalization(capitalization).autocorrectionDisabled() }
        }
        .textContentType(contentType)
        .font(AppFont.bodyLarge)
        .padding(.horizontal, 16)
        .frame(minHeight: 56)
        .background(Palette.card, in: RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: Radius.md, style: .continuous).stroke(Palette.line, lineWidth: 1))
        .accessibilityLabel(label)
    }
}

/// Pairing code in large serif type on a soft stone plate. VoiceOver reads it digit by digit.
struct PairingCodeView: View {
    let code: String
    var size: CGFloat = 40

    var body: some View {
        Text(code.isEmpty ? "······" : code)
            .font(AppFont.serif(size, relativeTo: .largeTitle))
            .tracking(6)
            .foregroundStyle(code.isEmpty ? Palette.inkMuted : Palette.inkPrimary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 18)
            .background(Palette.surface2, in: RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: Radius.md, style: .continuous).stroke(Palette.line, lineWidth: 1))
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(code.isEmpty ? "No active pairing code" : "Pairing code: " + code.map(String.init).joined(separator: " "))
    }
}

/// A friendly inline message box (errors, hints). `tone` picks the colour.
struct InlineNotice: View {
    let text: String
    var tone: StatePalette = .alert
    var body: some View {
        Text(text).font(AppFont.bodyMedium).foregroundStyle(Palette.inkPrimary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(tone.soft, in: RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: Radius.md, style: .continuous).stroke(tone.chip, lineWidth: 1))
            .accessibilityAddTraits(.updatesFrequently)
    }
}
