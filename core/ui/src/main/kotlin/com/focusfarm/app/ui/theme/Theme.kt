package com.focusfarm.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val NatureColorScheme = lightColorScheme(
    primary = GrowthEmerald,
    onPrimary = CozyBackground,
    primaryContainer = CozySurface,
    onPrimaryContainer = CozyTextPrimary,
    secondary = VividSprout,
    onSecondary = CozyBackground,
    secondaryContainer = CozySurface,
    onSecondaryContainer = CozyTextPrimary,
    tertiary = CozySuccess,
    onTertiary = CozyBackground,
    background = CozyBackground,
    onBackground = CozyTextPrimary,
    surface = CozySurface,
    onSurface = CozyTextPrimary,
    surfaceVariant = CozySurface,
    onSurfaceVariant = CozyTextSecondary,
    error = CozyDanger,
    onError = CozyBackground,
    outline = CozyTextSecondary,
    outlineVariant = CozySurface,
)

@Composable
fun FocusFarmTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = NatureColorScheme,
            typography = Typography,
            content = content,
        )
    }
}

