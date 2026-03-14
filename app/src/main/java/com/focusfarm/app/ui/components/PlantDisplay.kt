package com.focusfarm.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.app.domain.Plant
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.ui.theme.plantAccentColor

@Composable
fun PlantDisplay(
    plant: Plant,
    stage: GrowthStage,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    breathing: Boolean = false,
) {
    val emoji = PlantCatalog.getStageEmoji(plant, stage)
    val fontSize = (size.value * 0.5f).sp
    val accentColor = plantAccentColor(plant.id)

    val scaleAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600),
        label = "plantScale",
    )

    val breathScale = if (breathing) {
        val infiniteTransition = rememberInfiniteTransition(label = "breath")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.97f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "breathScale",
        )
        scale
    } else {
        1f
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(scaleAnim * breathScale)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            fontSize = fontSize,
        )
    }
}
