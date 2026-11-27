package com.focusfarm.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * Light: white cards on a soft cream-green page, emerald for the one primary action, mint only as an
 * accent container. Dark: the same roles on deep forest surfaces, for focusing at night.
 */
val LightFocusColorScheme = lightColorScheme(
    primary = GrowthEmerald,
    onPrimary = Color.White,
    primaryContainer = SoftMint,
    onPrimaryContainer = DeepForest,
    secondary = SecondaryAction,
    onSecondary = Color.White,
    secondaryContainer = SoftMint,
    onSecondaryContainer = DeepForest,
    tertiary = VividSprout,
    onTertiary = CozyOnSuccess,
    // Rare-reward accent (best streak, trophies): warm seed gold.
    tertiaryContainer = Color(0xFFFAEEDA),
    onTertiaryContainer = Color(0xFF633806),
    background = Color(0xFFF6F8F3),
    onBackground = Color(0xFF1B2B22),
    surface = Color.White,
    onSurface = Color(0xFF1B2B22),
    surfaceVariant = Color(0xFFEEF3EC),
    onSurfaceVariant = Color(0xFF4D5F55),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FBF7),
    surfaceContainer = Color(0xFFF1F5EF),
    surfaceContainerHigh = Color(0xFFEBF1E9),
    surfaceContainerHighest = Color(0xFFE5ECE3),
    error = CozyDanger,
    onError = Color.White,
    errorContainer = Color(0xFFFCE4E1),
    onErrorContainer = Color(0xFF7A1A12),
    outline = Color(0xFF8A9A90),
    outlineVariant = Color(0xFFD5DED7),
)

val DarkFocusColorScheme = darkColorScheme(
    primary = Color(0xFF7FD1A4),
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF1F4D37),
    onPrimaryContainer = Color(0xFFC9F0D9),
    secondary = Color(0xFF8FD5AE),
    onSecondary = Color(0xFF003822),
    secondaryContainer = Color(0xFF224B36),
    onSecondaryContainer = Color(0xFFC9F0D9),
    tertiary = VividSprout,
    onTertiary = Color(0xFF0B2B1C),
    tertiaryContainer = Color(0xFF412402),
    onTertiaryContainer = Color(0xFFFAC775),
    background = Color(0xFF0F1712),
    onBackground = Color(0xFFE3EBE5),
    surface = Color(0xFF16201A),
    onSurface = Color(0xFFE3EBE5),
    surfaceVariant = Color(0xFF1E2A23),
    onSurfaceVariant = Color(0xFFA9B8AE),
    surfaceContainerLowest = Color(0xFF0B110D),
    surfaceContainerLow = Color(0xFF141D17),
    surfaceContainer = Color(0xFF18221C),
    surfaceContainerHigh = Color(0xFF1E2A23),
    surfaceContainerHighest = Color(0xFF25322A),
    error = Color(0xFFFFB4A9),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF6F8076),
    outlineVariant = Color(0xFF2E3B33),
)

@Composable
fun FocusFarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkFocusColorScheme else LightFocusColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
