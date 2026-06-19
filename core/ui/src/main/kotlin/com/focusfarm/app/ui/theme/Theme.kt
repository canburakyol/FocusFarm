package com.focusfarm.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CozyColorScheme = lightColorScheme(
    primary = CozyAccent,
    onPrimary = CozyBackground,
    primaryContainer = CozySurface,
    onPrimaryContainer = CozyTextPrimary,
    secondary = CozyTextSecondary,
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
    MaterialTheme(
        colorScheme = CozyColorScheme,
        typography = Typography,
        content = content,
    )
}
