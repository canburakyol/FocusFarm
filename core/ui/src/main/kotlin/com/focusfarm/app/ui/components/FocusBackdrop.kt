package com.focusfarm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.focusfarm.app.ui.theme.Amber
import com.focusfarm.app.ui.theme.ForestDark
import com.focusfarm.app.ui.theme.ForestLight
import com.focusfarm.app.ui.theme.ForestMid

@Composable
fun FocusBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0C1511),
                        ForestDark,
                        ForestMid,
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1400f, 2200f),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Amber.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                        center = Offset(180f, 120f),
                        radius = 760f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ForestLight.copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                        center = Offset(1020f, 1820f),
                        radius = 980f,
                    ),
                ),
        )
        content()
    }
}
