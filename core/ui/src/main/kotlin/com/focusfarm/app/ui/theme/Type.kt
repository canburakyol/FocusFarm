package com.focusfarm.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.focusfarm.core.ui.R

/**
 * DM Sans is bundled (one variable font file, SIL OFL 1.1; licence in assets/licenses), so every
 * device renders the same type from the first launch. Scripts it lacks, such as Arabic, fall back
 * to the system font.
 */
@OptIn(ExperimentalTextApi::class)
private fun dmSans(weight: FontWeight) = Font(
    resId = R.font.dm_sans,
    weight = weight,
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val DmSans = FontFamily(
    dmSans(FontWeight.Normal),
    dmSans(FontWeight.Medium),
    dmSans(FontWeight.SemiBold),
    dmSans(FontWeight.Bold),
    dmSans(FontWeight.ExtraBold),
    dmSans(FontWeight.Black),
)

private val base = Typography()

private fun TextStyle.dm(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    letterSpacing: Double = 0.0,
) = copy(
    fontFamily = DmSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/** One family, one scale: Display 57/45/36, Headline 32/28/24, Title 22/16/14, Body 16/14/12, Label 14/12/11. */
val Typography = Typography(
    displayLarge = base.displayLarge.dm(FontWeight.SemiBold, 57, 64, -0.25),
    displayMedium = base.displayMedium.dm(FontWeight.SemiBold, 45, 52),
    displaySmall = base.displaySmall.dm(FontWeight.SemiBold, 36, 44),
    headlineLarge = base.headlineLarge.dm(FontWeight.SemiBold, 32, 40),
    headlineMedium = base.headlineMedium.dm(FontWeight.SemiBold, 28, 36),
    headlineSmall = base.headlineSmall.dm(FontWeight.SemiBold, 24, 32),
    titleLarge = base.titleLarge.dm(FontWeight.SemiBold, 22, 28),
    titleMedium = base.titleMedium.dm(FontWeight.Medium, 16, 24, 0.1),
    titleSmall = base.titleSmall.dm(FontWeight.Medium, 14, 20, 0.1),
    bodyLarge = base.bodyLarge.dm(FontWeight.Normal, 16, 24, 0.15),
    bodyMedium = base.bodyMedium.dm(FontWeight.Normal, 14, 20, 0.15),
    bodySmall = base.bodySmall.dm(FontWeight.Normal, 12, 16, 0.2),
    // Labels carry UI text in every language; keep tracking near zero so words do not fall apart.
    labelLarge = base.labelLarge.dm(FontWeight.SemiBold, 14, 20, 0.1),
    labelMedium = base.labelMedium.dm(FontWeight.Medium, 12, 16, 0.3),
    labelSmall = base.labelSmall.dm(FontWeight.Medium, 11, 16, 0.3),
)

/** Timers use the same family with tabular digits (set per text via fontFeatureSettings = "tnum"). */
val TimerFontFamily: FontFamily = DmSans
