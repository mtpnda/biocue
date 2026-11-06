package com.mtpnda.biocue.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mtpnda.biocue.R

// Both bundled fonts are variable fonts (wght 200–800). Declaring each weight
// with an explicit variation setting gives real weights instead of faux bold.
@OptIn(ExperimentalTextApi::class)
private fun atkinson(weight: Int) = Font(
    resId = R.font.atkinson,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

@OptIn(ExperimentalTextApi::class)
private fun newsreader(weight: Int) = Font(
    resId = R.font.newsreader,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

/** Body / labels / inputs — Atkinson Hyperlegible: unambiguous letterforms. */
val AtkinsonFont = FontFamily(atkinson(400), atkinson(600), atkinson(700), atkinson(800))

/** Titles / greetings — Newsreader: warm, thoughtful, human. */
val NewsreaderFont = FontFamily(newsreader(400), newsreader(500))

// NOTE: text styles intentionally carry no colour. Colour comes from
// LocalContentColor (set by BiocueTheme's Surface and by buttons), so the same
// style stays legible on both light surfaces and the dark charcoal button.
val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = NewsreaderFont, fontWeight = FontWeight.Medium, fontSize = 40.sp, lineHeight = 46.sp),
    displayMedium = TextStyle(fontFamily = NewsreaderFont, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 40.sp),
    displaySmall = TextStyle(fontFamily = NewsreaderFont, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = NewsreaderFont, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = NewsreaderFont, fontWeight = FontWeight.Medium, fontSize = 25.sp, lineHeight = 31.sp),
    headlineSmall = TextStyle(fontFamily = NewsreaderFont, fontWeight = FontWeight.Medium, fontSize = 23.sp, lineHeight = 29.sp),
    titleLarge = TextStyle(fontFamily = NewsreaderFont, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = AtkinsonFont, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = AtkinsonFont, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = AtkinsonFont, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = AtkinsonFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = AtkinsonFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = AtkinsonFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = AtkinsonFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = AtkinsonFont, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
)
