package com.focusfarm.app.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {
    @Test
    fun textAndActionPairsMeetAaContrastOnTheirThemeSurfaces() {
        val pairs = listOf(
            GrowthEmerald to CozyBackground,
            SecondaryAction to CozyBackground,
            CozyOnSuccess to VividSprout,
            DeepForest to SoftMint,
            ForestMuted to SoftMint,
            CozyDanger to CozyBackground,
        )

        pairs.forEach { (foreground, background) ->
            assertTrue(
                "Expected at least 4.5:1, got ${ThemeContrast.ratio(foreground, background)}",
                ThemeContrast.ratio(foreground, background) >= 4.5,
            )
        }
    }
}
