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
import com.focusfarm.app.ui.theme.CozyAccent
import com.focusfarm.app.ui.theme.CozyBackground
import com.focusfarm.app.ui.theme.CozySuccess
import com.focusfarm.app.ui.theme.CozySurface

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
                        Color(0xFFFCFAF5), // Çok açık fildişi
                        CozyBackground,
                        CozySurface,
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1400f, 2200f),
                ),
            ),
    ) {
        // Sol üst sıcak terracotta parıltısı
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CozyAccent.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        center = Offset(180f, 120f),
                        radius = 760f,
                    ),
                ),
        )
        // Sağ alt pastel adaçayı yeşili parıltısı
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CozySuccess.copy(alpha = 0.08f),
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
