package com.mtpnda.biocue.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightGlassColorScheme = lightColorScheme(
    primary = CalmBlue,
    background = CanvasA,
    surface = GlassCard,
    onPrimary = Color.White,
    onBackground = LabelColor,
    onSurface = LabelColor
)

@Composable
fun BiocueTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightGlassColorScheme,
        typography = AppTypography,
        content = content
    )
}
