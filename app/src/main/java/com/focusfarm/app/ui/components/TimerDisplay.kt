package com.focusfarm.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusfarm.app.R
import com.focusfarm.app.ui.theme.Amber
import com.focusfarm.app.ui.theme.CreamDim
import com.focusfarm.app.ui.theme.ForestLight
import com.focusfarm.app.ui.theme.TimerFontFamily

@Composable
fun TimerDisplay(
    remainingSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = "%02d:%02d".format(minutes, seconds)
    val minutesLeft = (remainingSeconds + 59) / 60

    val progress by animateFloatAsState(
        targetValue = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 1f,
        animationSpec = tween(300),
        label = "timerProgress",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = timeText,
            fontFamily = TimerFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 64.sp,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 4.sp,
        )

        Text(
            text = if (minutesLeft <= 1) {
                stringResource(R.string.last_minute)
            } else {
                stringResource(R.string.minutes_remaining, minutesLeft)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = CreamDim,
        )

        // Progress bar
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ForestLight),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Amber),
            )
        }
    }
}
