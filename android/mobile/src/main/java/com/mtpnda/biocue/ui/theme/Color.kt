package com.mtpnda.biocue.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// MindCompass-derived palette. Surfaces are warm and soft (never stark white,
// never cold slate); colour is reserved for meaning (calm / rising / support).

// Surfaces
val BgColor = DlsTokens.surfaceBg        // App background (warm oat)
val SurfaceCard = DlsTokens.surfaceCard    // Card / sheet surface
val Surface2 = DlsTokens.surfaceRaised       // Secondary inputs, chips
val LineBorder = DlsTokens.borderSubtle     // Delicate dividers
val LineStrong = DlsTokens.borderStrong     // Stronger borders

// Ink — text & icons
val InkPrimary = DlsTokens.textPrimary
val InkSecondary = DlsTokens.textSecondary
val InkMuted = DlsTokens.textMuted       // Placeholders, decorative captions

// Primary action — warm charcoal
val PrimaryCharcoal = DlsTokens.actionPrimary
val OnPrimaryCharcoal = DlsTokens.actionOnPrimary

// Tier 0 / Calm / Connected — sage green
val CalmGreenChip = DlsTokens.stateCalmChip
val CalmGreenSoft = DlsTokens.stateCalmSoft
val CalmGreenInk = DlsTokens.stateCalmInk

// Tier 1 / Rising / Searching — pastel amber
val RisingAmberChip = DlsTokens.stateRisingChip
val RisingAmberSoft = DlsTokens.stateRisingSoft
val RisingAmberInk = DlsTokens.stateRisingInk

// Tier 2 / High stress / SOS — soft coral (supportive, never alarming)
val AlertCoralChip = DlsTokens.stateAlertChip
val AlertCoralSoft = DlsTokens.stateAlertSoft
val AlertCoralInk = DlsTokens.stateAlertInk

// Info / low energy — soft sky blue
val InfoBlueChip = DlsTokens.stateInfoChip
val InfoBlueSoft = DlsTokens.stateInfoSoft
val InfoBlueInk = DlsTokens.stateInfoInk

// Neutral / unsure — soft stone gray
val NeutralGrayChip = DlsTokens.stateNeutralChip
val NeutralGraySoft = DlsTokens.stateNeutralSoft
val NeutralGrayInk = DlsTokens.stateNeutralInk

/** A chip / soft-background / ink triple for one emotional state. */
@Immutable
data class StatePalette(val chip: Color, val soft: Color, val ink: Color)

val CalmPalette = StatePalette(CalmGreenChip, CalmGreenSoft, CalmGreenInk)
val RisingPalette = StatePalette(RisingAmberChip, RisingAmberSoft, RisingAmberInk)
val AlertPalette = StatePalette(AlertCoralChip, AlertCoralSoft, AlertCoralInk)
val InfoPalette = StatePalette(InfoBlueChip, InfoBlueSoft, InfoBlueInk)
val NeutralPalette = StatePalette(NeutralGrayChip, NeutralGraySoft, NeutralGrayInk)

/** Stress tier → palette. 0 calm, 1 rising, 2+ needs support. */
fun paletteForTier(tier: Int): StatePalette = when (tier) {
    0 -> CalmPalette
    1 -> RisingPalette
    else -> AlertPalette
}
