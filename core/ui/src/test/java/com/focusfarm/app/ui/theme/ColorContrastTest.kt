package com.focusfarm.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {
    @Test
    fun textAndActionPairsMeetAaContrastOnTheirThemeSurfaces() {
        assertAa(
            GrowthEmerald to CozyBackground,
            SecondaryAction to CozyBackground,
            CozyOnSuccess to VividSprout,
            DeepForest to SoftMint,
            ForestMuted to SoftMint,
            CozyDanger to CozyBackground,
        )
    }

    @Test
    fun lightSchemeTextMeetsAa() = assertSchemeAa(LightFocusColorScheme)

    @Test
    fun darkSchemeTextMeetsAa() = assertSchemeAa(DarkFocusColorScheme)

    private fun assertSchemeAa(scheme: ColorScheme) = with(scheme) {
        assertAa(
            onBackground to background,
            onSurface to surface,
            onSurfaceVariant to surface,
            onSurfaceVariant to surfaceVariant,
            primary to background,
            primary to surface,
            onPrimary to primary,
            onPrimaryContainer to primaryContainer,
            onSecondary to secondary,
            error to surface,
            onErrorContainer to errorContainer,
        )
    }

    private fun assertAa(vararg pairs: Pair<Color, Color>) {
        pairs.forEach { (foreground, background) ->
            val ratio = ThemeContrast.ratio(foreground, background)
            assertTrue("Expected at least 4.5:1 for $foreground on $background, got $ratio", ratio >= 4.5)
        }
    }
}
