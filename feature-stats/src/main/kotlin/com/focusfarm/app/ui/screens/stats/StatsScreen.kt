package com.focusfarm.app.ui.screens.stats

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.data.local.ReflectionHistoryItem
import com.focusfarm.app.data.auth.AccountDeletionResult
import com.focusfarm.app.data.repository.DailyTrendPoint
import com.focusfarm.app.data.repository.TimeFilter
import com.focusfarm.app.data.repository.TagDuration
import com.focusfarm.app.data.repository.StatsSummary
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.domain.FocusTag
import com.focusfarm.app.ui.components.FocusBackdrop
import com.focusfarm.app.ui.navigation.ReleaseFeatureScope
import com.focusfarm.app.ui.theme.TimerFontFamily
import com.focusfarm.core.ui.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import com.focusfarm.app.domain.SessionStatus
import com.focusfarm.app.domain.PlantSpeciesCatalog
import com.focusfarm.app.domain.GardenPlantState
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.app.ui.components.PlantVectorRenderer
import com.focusfarm.app.ui.components.labelRes
import kotlin.math.roundToInt
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Flag

@Composable
fun StatsScreen(
    onOpenMissions: () -> Unit,
    onOpenStreak: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()
    val weeklySessions by viewModel.weeklySessions.collectAsStateWithLifecycle()
    val weeklyChallenge by viewModel.weeklyChallenge.collectAsStateWithLifecycle()
    val isWeeklyRewardClaimed by viewModel.isWeeklyRewardClaimed.collectAsStateWithLifecycle()
    val reflectionHistory by viewModel.reflectionHistory.collectAsStateWithLifecycle()

    FocusBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tab_stats),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // Daily Missions Banner Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenMissions() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.daily_quests_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.stats_missions_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(Icons.Rounded.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            // Weekly Challenge Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(seasonTitleRes(weeklyChallenge.seasonIndex)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(seasonBodyRes(weeklyChallenge.seasonIndex)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(
                            R.string.weekly_challenge_progress,
                            weeklyChallenge.currentSessions,
                            weeklyChallenge.targetSessions,
                            weeklyChallenge.currentMinutes,
                            weeklyChallenge.targetMinutes,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (weeklyChallenge.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    ProgressTrack(weeklyChallenge.overallRatio)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.weekly_reward_seeds, weeklyChallenge.rewardSeeds),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (isWeeklyRewardClaimed) {
                        Text(
                            text = stringResource(R.string.weekly_reward_claimed),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Time Filter Row
            TimeFilterSelector(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.onFilterSelected(it) }
            )

            // UI State Handling
            when (val state = uiState) {
                is StatsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is StatsUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Insights, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = stringResource(R.string.stats_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.stats_empty_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                is StatsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = stringResource(R.string.stats_error_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is StatsUiState.Success -> {
                    SuccessStatsContent(
                        summary = state.summary,
                        trend = state.trend,
                        tagDistribution = state.tagDistribution,
                        selectedFilter = selectedFilter,
                        onOpenStreak = onOpenStreak
                    )
                }
            }

            // Recent Sessions Section
            if (recentSessions.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.recent_sessions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        recentSessions.forEach { session ->
                            SessionRow(session)
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Reflection History Section
            if (reflectionHistory.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.reflection_history_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(8.dp))
                        reflectionHistory.forEach { item ->
                            ReflectionRow(item)
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

}

@Composable
private fun SuccessStatsContent(
    summary: StatsSummary,
    trend: List<DailyTrendPoint>,
    tagDistribution: List<TagDuration>,
    selectedFilter: TimeFilter,
    onOpenStreak: () -> Unit
) {
    // Four key metrics; the streak tile is the way into the streak screen.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                value = formatMinutes(summary.totalFocusDurationMinutes),
                label = stringResource(R.string.stats_focus_time),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                value = formatMinutes(summary.dailyAverageMinutes.roundToInt()),
                label = stringResource(R.string.stats_daily_avg),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                value = stringResource(R.string.stats_percent, (summary.successRate * 100).roundToInt()),
                label = stringResource(R.string.stats_success_rate),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                value = pluralStringResource(R.plurals.streak_days, summary.currentStreak, summary.currentStreak),
                label = stringResource(R.string.streak_current),
                modifier = Modifier.weight(1f),
                onClick = onOpenStreak
            )
        }
    }

    // Daily Focus Duration Trend Chart
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.stats_daily_chart),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            DailyFocusTrendChart(trend = trend, filter = selectedFilter)
        }
    }

    // Tag Distribution Section
    if (tagDistribution.isNotEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.stats_by_tag),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                TagDistributionChart(tagDistribution = tagDistribution)
            }
        }
    }
}

@Composable
private fun TimeFilterSelector(
    selectedFilter: TimeFilter,
    onFilterSelected: (TimeFilter) -> Unit
) {
    val filters = listOf(
        TimeFilter.TODAY to stringResource(R.string.today),
        TimeFilter.LAST_7_DAYS to stringResource(R.string.stats_filter_7d),
        TimeFilter.LAST_30_DAYS to stringResource(R.string.stats_filter_30d),
        TimeFilter.THIS_MONTH to stringResource(R.string.stats_filter_month),
        TimeFilter.THIS_YEAR to stringResource(R.string.stats_filter_year),
        TimeFilter.ALL_TIME to stringResource(R.string.garden_filter_all)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (filter, label) ->
            val isSelected = selectedFilter == filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = cardModifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = value,
                fontFamily = TimerFontFamily,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DailyFocusTrendChart(trend: List<DailyTrendPoint>, filter: TimeFilter) {
    val barTop = MaterialTheme.colorScheme.tertiary
    val barBottom = MaterialTheme.colorScheme.primary
    val maxVal = (trend.maxOfOrNull { it.totalMinutes } ?: 0).coerceAtLeast(1)
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "DailyFocusTrendAnimation"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        val width = size.width
        val height = size.height
        val barCount = when (filter) {
            TimeFilter.TODAY -> 1
            TimeFilter.LAST_7_DAYS -> 7
            TimeFilter.LAST_30_DAYS -> 30
            else -> trend.size.coerceAtLeast(7)
        }
        val spacing = 8.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(2.dp.toPx())

        trend.forEachIndexed { index, point ->
            val fraction = point.totalMinutes.toFloat() / maxVal
            val barHeight = fraction * height * 0.85f * animatedProgress
            val x = index * (barWidth + spacing)
            val y = height - barHeight

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        barTop,
                        barBottom
                    )
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight.coerceAtLeast(2.dp.toPx())),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}

@Composable
private fun TagDistributionChart(tagDistribution: List<TagDuration>) {
    val totalMinutes = tagDistribution.sumOf { it.totalMinutes }.coerceAtLeast(1)

    tagDistribution.sortedByDescending { it.totalMinutes }.forEach { item ->
        val percentage = (item.totalMinutes.toFloat() / totalMinutes)
        val animatedProgress by animateFloatAsState(
            targetValue = percentage,
            animationSpec = tween(durationMillis = 800),
            label = "TagDistributionAnimation"
        )

        val color = when (item.selectedTag) {
            FocusTag.WORK -> Color(0xFF4CAF50)
            FocusTag.STUDY -> Color(0xFF2196F3)
            FocusTag.READING -> Color(0xFFFF9800)
            FocusTag.EXERCISE -> Color(0xFFE91E63)
            FocusTag.OTHER -> Color(0xFF9E9E9E)
        }

        val label = stringResource(item.selectedTag.labelRes())

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.stats_tag_minutes, item.totalMinutes, (percentage * 100).roundToInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun SessionRow(session: FocusSession) {
    val dateFormat = remember { SimpleDateFormat("d MMM HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val withered = session.status != SessionStatus.COMPLETED
        PlantVectorRenderer(
            plantId = session.plantId,
            stage = if (withered) GrowthStage.SPROUT else GrowthStage.FULL,
            state = if (withered) GardenPlantState.WITHERED else GardenPlantState.BLOOMED,
            size = 36.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                PlantSpeciesCatalog.getById(session.plantId)?.displayName ?: session.plantName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(dateFormat.format(Date(session.completedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            if (withered) {
                stringResource(R.string.garden_withered)
            } else {
                stringResource(R.string.duration_minutes_short, session.durationMinutes)
            },
            fontFamily = TimerFontFamily,
            fontSize = 13.sp,
            color = if (withered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ReflectionRow(item: ReflectionHistoryItem) {
    val dateFormat = remember { SimpleDateFormat("d MMM HH:mm", Locale.getDefault()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.plantEmoji, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "${item.plantName} - ${stringResource(R.string.duration_minutes_short, item.durationMinutes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                dateFormat.format(Date(item.completedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = item.note,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ProgressTrack(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun formatMinutes(minutes: Int): String =
    if (minutes >= 60) {
        stringResource(R.string.total_focus_hours_minutes, minutes / 60, minutes % 60)
    } else {
        stringResource(R.string.total_focus_minutes, minutes)
    }

private fun seasonTitleRes(index: Int): Int = when (index) {
    0 -> R.string.season_0_title
    1 -> R.string.season_1_title
    2 -> R.string.season_2_title
    else -> R.string.season_3_title
}

private fun seasonBodyRes(index: Int): Int = when (index) {
    0 -> R.string.season_0_body
    1 -> R.string.season_1_body
    2 -> R.string.season_2_body
    else -> R.string.season_3_body
}

