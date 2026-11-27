package com.focusfarm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Calm page background: the theme background with one faint growth glow, in light and dark. */
@Composable
fun FocusBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(colors.primary.copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(1020f, 1820f),
                    radius = 1100f,
                ),
            ),
    ) {
        content()
    }
}
