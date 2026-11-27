package com.focusfarm.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/** WCAG contrast calculation for the semantic colors used in the app theme. */
object ThemeContrast {
    fun ratio(foreground: Color, background: Color): Double {
        val light = maxOf(luminance(foreground), luminance(background))
        val dark = minOf(luminance(foreground), luminance(background))
        return (light + 0.05) / (dark + 0.05)
    }

    private fun luminance(color: Color): Double =
        (0.2126 * linear(color.red)) +
            (0.7152 * linear(color.green)) +
            (0.0722 * linear(color.blue))

    private fun linear(channel: Float): Double =
        if (channel <= 0.04045f) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
}
