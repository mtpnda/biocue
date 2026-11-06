import SwiftUI
import UIKit

// MindCompass-derived calm palette — identical tokens to the Android app (ui/theme/Color.kt).
// Surfaces are warm and soft; colour is reserved for meaning (calm / rising / support).
enum Palette {
    // Surfaces
    static let bg = DlsTokens.surfaceBg
    static let card = DlsTokens.surfaceCard
    static let surface2 = DlsTokens.surfaceRaised
    static let line = DlsTokens.borderSubtle
    static let lineStrong = DlsTokens.borderStrong
    // Ink
    static let inkPrimary = DlsTokens.textPrimary
    static let inkSecondary = DlsTokens.textSecondary
    static let inkMuted = DlsTokens.textMuted
    // Primary action
    static let charcoal = DlsTokens.actionPrimary
    static let onCharcoal = DlsTokens.actionOnPrimary
}

/// A chip / soft-background / ink triple for one emotional state.
struct StatePalette {
    let chip: Color
    let soft: Color
    let ink: Color

    static let calm = StatePalette(chip: DlsTokens.stateCalmChip, soft: DlsTokens.stateCalmSoft, ink: DlsTokens.stateCalmInk)
    static let rising = StatePalette(chip: DlsTokens.stateRisingChip, soft: DlsTokens.stateRisingSoft, ink: DlsTokens.stateRisingInk)
    static let alert = StatePalette(chip: DlsTokens.stateAlertChip, soft: DlsTokens.stateAlertSoft, ink: DlsTokens.stateAlertInk)
    static let info = StatePalette(chip: DlsTokens.stateInfoChip, soft: DlsTokens.stateInfoSoft, ink: DlsTokens.stateInfoInk)
    static let neutral = StatePalette(chip: DlsTokens.stateNeutralChip, soft: DlsTokens.stateNeutralSoft, ink: DlsTokens.stateNeutralInk)

    /// Stress tier -> palette. 0 calm, 1 rising, 2+ needs support.
    static func forTier(_ tier: Int) -> StatePalette {
        switch tier {
        case 0: return .calm
        case 1: return .rising
        default: return .alert
        }
    }
}

enum Radius {
    static let sm: CGFloat = 12
    static let md: CGFloat = 16
    static let card: CGFloat = 20
    static let xl: CGFloat = 28
}

enum Metrics {
    /// Minimum interactive target (accessibility).
    static let minTouch: CGFloat = 48
    static let buttonHeight: CGFloat = 52
}

extension Color {
    init(hex: UInt32, opacity: Double = 1) {
        self.init(.sRGB,
                  red: Double((hex >> 16) & 0xFF) / 255,
                  green: Double((hex >> 8) & 0xFF) / 255,
                  blue: Double(hex & 0xFF) / 255,
                  opacity: opacity)
    }

    /// Parses "#RRGGBB". Never crashes on bad data (returns neutral grey).
    static func fromHexString(_ s: String) -> Color {
        var t = s.trimmingCharacters(in: .whitespaces)
        if t.hasPrefix("#") { t.removeFirst() }
        guard t.count == 6, let v = UInt32(t, radix: 16) else { return StatePalette.neutral.chip }
        return Color(hex: v)
    }
}

/// Fonts: Newsreader (serif, titles) and Atkinson Hyperlegible Next (body, labels).
/// Both are VARIABLE fonts. iOS exposes their named instances by PostScript name, but that is
/// exactly the kind of thing that can differ between OS versions — so each lookup is probed and
/// falls back to the system serif / sans rather than silently rendering the wrong thing.
enum AppFont {
    private static func probe(_ psName: String, size: CGFloat) -> Bool { UIFont(name: psName, size: size) != nil }

    static func serif(_ size: CGFloat, relativeTo style: Font.TextStyle = .title) -> Font {
        let name = "Newsreader16pt-Medium"
        if probe(name, size: size) { return .custom(name, size: size, relativeTo: style) }
        return .system(size: size, weight: .medium, design: .serif)
    }

    static func sans(_ size: CGFloat, weight: Font.Weight = .regular, relativeTo style: Font.TextStyle = .body) -> Font {
        let suffix: String
        switch weight {
        case .bold, .heavy, .black: suffix = "Bold"
        case .semibold: suffix = "SemiBold"
        case .medium: suffix = "Medium"
        default: suffix = "Regular"
        }
        let name = "AtkinsonHyperlegibleNext-\(suffix)"
        if probe(name, size: size) { return .custom(name, size: size, relativeTo: style) }
        return .system(size: size, weight: weight, design: .default)
    }

    // Semantic styles (match the Android typography scale)
    static var displayMedium: Font { serif(34, relativeTo: .largeTitle) }
    static var headlineLarge: Font { serif(28, relativeTo: .title) }
    static var headlineSmall: Font { serif(23, relativeTo: .title2) }
    static var titleLarge: Font { serif(22, relativeTo: .title2) }
    static var titleMedium: Font { sans(17, weight: .bold, relativeTo: .headline) }
    static var bodyLarge: Font { sans(15, relativeTo: .body) }
    static var bodyMedium: Font { sans(14, relativeTo: .callout) }
    static var bodySmall: Font { sans(13, weight: .semibold, relativeTo: .footnote) }
    static var labelLarge: Font { sans(15.5, weight: .bold, relativeTo: .body) }
    static var labelMedium: Font { sans(13, weight: .bold, relativeTo: .footnote) }
    static var labelSmall: Font { sans(11.5, weight: .bold, relativeTo: .caption) }
}
