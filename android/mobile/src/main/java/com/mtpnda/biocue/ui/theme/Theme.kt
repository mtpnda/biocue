package com.mtpnda.biocue.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val CalmColorScheme = lightColorScheme(
    primary = PrimaryCharcoal,
    onPrimary = OnPrimaryCharcoal,
    primaryContainer = Surface2,
    onPrimaryContainer = InkPrimary,
    secondary = InkSecondary,
    onSecondary = OnPrimaryCharcoal,
    secondaryContainer = Surface2,
    onSecondaryContainer = InkPrimary,
    tertiary = CalmGreenInk,
    onTertiary = OnPrimaryCharcoal,
    tertiaryContainer = CalmGreenSoft,
    onTertiaryContainer = CalmGreenInk,
    background = BgColor,
    onBackground = InkPrimary,
    surface = SurfaceCard,
    onSurface = InkPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = InkSecondary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = SurfaceCard,
    surfaceContainerLow = SurfaceCard,
    surfaceContainer = SurfaceCard,
    surfaceContainerHigh = SurfaceCard,
    surfaceContainerHighest = Surface2,
    outline = LineStrong,
    outlineVariant = LineBorder,
    error = AlertCoralInk,
    onError = OnPrimaryCharcoal,
    errorContainer = AlertCoralSoft,
    onErrorContainer = AlertCoralInk
)

private val CalmShapes = Shapes(
    extraSmall = RoundedCornerShape(AppRadius.Sm),
    small = RoundedCornerShape(AppRadius.Sm),
    medium = RoundedCornerShape(AppRadius.Md),
    large = RoundedCornerShape(AppRadius.Lg),
    extraLarge = RoundedCornerShape(AppRadius.Xl)
)

private val CalmSelectionColors = TextSelectionColors(
    handleColor = PrimaryCharcoal,
    backgroundColor = RisingAmberChip.copy(alpha = 0.4f)
)

@Composable
fun BiocueTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CalmColorScheme,
        typography = AppTypography,
        shapes = CalmShapes
    ) {
        CompositionLocalProvider(LocalTextSelectionColors provides CalmSelectionColors) {
            // One warm surface under everything: default text is ink, default
            // background is oat, so no screen can fall back to white/black.
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                content = content
            )
        }
    }
}
