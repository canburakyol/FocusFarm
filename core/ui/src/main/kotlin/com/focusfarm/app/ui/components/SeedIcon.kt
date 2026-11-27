package com.focusfarm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.focusfarm.app.ui.theme.SeedGold

/** The one symbol for the Seed currency, everywhere a balance or reward is shown. */
@Composable
fun SeedIcon(modifier: Modifier = Modifier, size: Dp = 12.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(SeedGold),
    )
}
