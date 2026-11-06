package com.mtpnda.biocue.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Corner radii: generous and tactile. */
object AppRadius {
    val Sm = 12.dp
    val Md = 16.dp
    val Card = 20.dp
    val Lg = 22.dp
    val Xl = 28.dp
    val Pill = 999.dp
}

object AppMetrics {
    /** Minimum interactive target — non-negotiable for accessibility. */
    val MinTouchTarget = 48.dp
    val ButtonHeight = 52.dp
}

/** Warm tint (rgb 43,38,30) used for every shadow so elevation never looks grey. */
val ShadowTint = DlsTokens.shadowTint
