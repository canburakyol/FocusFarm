package com.focusfarm.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Amber,
    onPrimary = ForestDark,
    primaryContainer = AmberMuted,
    onPrimaryContainer = Cream,
    secondary = ForestLight,
    onSecondary = Cream,
    secondaryContainer = ForestMid,
    onSecondaryContainer = CreamDim,
    tertiary = Success,
    onTertiary = ForestDark,
    background = ForestDark,
    onBackground = Cream,
    surface = ForestMid,
    onSurface = Cream,
    surfaceVariant = ForestLight,
    onSurfaceVariant = CreamDim,
    error = Danger,
    onError = Cream,
    outline = ForestLight,
    outlineVariant = Soil,
)

@Composable
fun FocusFarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
