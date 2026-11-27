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

    var showSettings by remember { mutableStateOf(false) }

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
                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
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
                            text = "Günlük Görevler",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Bugünün görevlerini tamamla, ödül tohumlarını topla!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Text("🎯", fontSize = 28.sp)
                }
            }

            // Weekly Challenge Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = weeklyChallenge.seasonTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = weeklyChallenge.seasonSubtitle,
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
                        Text("📊", fontSize = 48.sp)
                        Text(
                            text = "Henüz Veri Yok",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Seçilen zaman aralığında tamamlanmış oturum bulunmamaktadır.",
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
                        Text("⚠️", fontSize = 48.sp)
                        Text(
                            text = "Hata Oluştu",
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

    if (showSettings) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettings = false }
        )
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
    // Metrics Grid
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                value = formatMinutes(summary.totalFocusDurationMinutes),
                label = "Toplam Odaklanma",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                value = "${summary.successfulSessionCount} / ${summary.failedSessionCount}",
                label = "Başarılı / Başarısız",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                value = String.format(Locale.getDefault(), "%% %.0f", summary.successRate * 100),
                label = "Başarı Oranı",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                value = String.format(Locale.getDefault(), "%.1f dk", summary.dailyAverageMinutes),
                label = "Günlük Ortalama",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                value = "${summary.longestSessionMinutes} dk",
                label = "En Uzun Oturum",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                value = "${summary.currentStreak} gün",
                label = "Aktif Seri",
                modifier = Modifier.weight(1f),
                onClick = onOpenStreak
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                value = "${summary.bestStreak} gün",
                label = "En İyi Seri",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                value = "${summary.totalPlantsGrown} adet",
                label = "Yetişen Bitki",
                modifier = Modifier.weight(1f)
            )
        }
        MetricCard(
            value = "${summary.totalSeedsEarned} Seed",
            label = "Kazanılan Toplam Seed",
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Daily Focus Duration Trend Chart
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Günlük Odaklanma Süresi (Dakika)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            DailyFocusTrendChart(trend = trend, filter = selectedFilter)
        }
    }

    // Completed Sessions Trend Chart
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tamamlanan Oturum Sayısı",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            CompletedSessionsChart(trend = trend, filter = selectedFilter)
        }
    }

    // Weekly Trend Chart (Grouped by Weeks for 30 days and above)
    if (selectedFilter != TimeFilter.TODAY && selectedFilter != TimeFilter.LAST_7_DAYS) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Haftalık Trend (Dakika)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(16.dp))
                WeeklyTrendChart(trend = trend)
            }
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
                    text = "Etiket Süre Dağılımı",
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
        TimeFilter.TODAY to "Bugün",
        TimeFilter.LAST_7_DAYS to "7 Gün",
        TimeFilter.LAST_30_DAYS to "30 Gün",
        TimeFilter.THIS_MONTH to "Bu Ay",
        TimeFilter.THIS_YEAR to "Bu Yıl",
        TimeFilter.ALL_TIME to "Hepsi"
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
                        Color(0xFF81C784), // Light Green
                        Color(0xFF388E3C)  // Dark Green
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
private fun CompletedSessionsChart(trend: List<DailyTrendPoint>, filter: TimeFilter) {
    val maxVal = (trend.maxOfOrNull { it.completedSessions } ?: 0).coerceAtLeast(1)
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "CompletedSessionsAnimation"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        val width = size.width
        val height = size.height
        val points = trend.size
        if (points < 2) {
            // Draw a single bar if only 1 point
            val x = width / 2
            val h = (trend.firstOrNull()?.completedSessions?.toFloat() ?: 0f) / maxVal * height * 0.8f
            drawRoundRect(
                color = Color(0xFF64B5F6),
                topLeft = Offset(x - 20.dp.toPx(), height - h),
                size = Size(40.dp.toPx(), h),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            return@Canvas
        }

        val stepX = width / (points - 1)
        val path = Path()

        trend.forEachIndexed { index, point ->
            val fraction = point.completedSessions.toFloat() / maxVal
            val x = index * stepX
            val y = height - (fraction * height * 0.8f * animatedProgress) - 10.dp.toPx()

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = Color(0xFF1E88E5),
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw points
        trend.forEachIndexed { index, point ->
            val fraction = point.completedSessions.toFloat() / maxVal
            val x = index * stepX
            val y = height - (fraction * height * 0.8f * animatedProgress) - 10.dp.toPx()

            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = Color(0xFF1E88E5),
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun WeeklyTrendChart(trend: List<DailyTrendPoint>) {
    // Group trend points by week number (since epochDay / 7)
    val weeklyMap = remember(trend) {
        trend.groupBy { it.localDay / 7 }.mapValues { (_, points) ->
            points.sumOf { it.totalMinutes }
        }.toList().sortedBy { it.first }.takeLast(4)
    }

    val maxVal = (weeklyMap.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "WeeklyTrendAnimation"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        val width = size.width
        val height = size.height
        val barCount = weeklyMap.size.coerceAtLeast(1)
        val spacing = 24.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(20.dp.toPx())

        weeklyMap.forEachIndexed { index, pair ->
            val fraction = pair.second.toFloat() / maxVal
            val barHeight = fraction * height * 0.8f * animatedProgress
            val x = index * (barWidth + spacing)
            val y = height - barHeight

            drawRoundRect(
                color = Color(0xFFFFB74D), // Orange
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight.coerceAtLeast(2.dp.toPx())),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // Draw text value above bar using native canvas
            drawContext.canvas.nativeCanvas.drawText(
                "${pair.second} dk",
                x + barWidth / 2f,
                y - 8.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 10.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
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

        val label = when (item.selectedTag) {
            FocusTag.WORK -> "Çalışma"
            FocusTag.STUDY -> "Ders Çalışma"
            FocusTag.READING -> "Okuma"
            FocusTag.EXERCISE -> "Egzersiz"
            FocusTag.OTHER -> "Diğer"
        }

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
                    text = "${item.totalMinutes} dk (%% ${String.format(Locale.getDefault(), "%.0f", percentage * 100)})",
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
        Text(session.plantEmoji, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(session.plantName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(dateFormat.format(Date(session.completedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            stringResource(R.string.duration_minutes_short, session.durationMinutes),
            fontFamily = TimerFontFamily,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
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

private fun formatMinutes(minutes: Int): String {
    val hrs = minutes / 60
    val mins = minutes % 60
    return if (hrs > 0) "${hrs}s ${mins}dk" else "${mins}dk"
}

@Composable
private fun SettingsDialog(
    viewModel: StatsViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }
    val scope = rememberCoroutineScope()

    val referralState by viewModel.referralState.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    var referralInput by remember { mutableStateOf("") }
    var referralSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isReferralLoading by remember { mutableStateOf(false) }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deletionResult by remember { mutableStateOf<AccountDeletionResult?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.settings_title),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Referral is retained in source but hidden until its code and wallet paths agree.
                if (ReleaseFeatureScope.REFERRAL_ENABLED) Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_referral_title),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.settings_referral_desc),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = referralInput,
                                onValueChange = { referralInput = it.uppercase() },
                                label = { Text(stringResource(R.string.settings_referral_label), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    if (referralInput.isNotBlank()) {
                                        isReferralLoading = true
                                        referralSuccessMessage = null
                                        scope.launch {
                                            val result = viewModel.verifyReferral(referralInput)
                                            isReferralLoading = false
                                            if (result.success) {
                                                referralSuccessMessage = result.message
                                                referralInput = ""
                                            }
                                        }
                                    }
                                },
                                enabled = referralInput.isNotBlank() && !isReferralLoading && !referralState.isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (isReferralLoading || referralState.isLoading) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(stringResource(R.string.settings_referral_apply))
                                }
                            }
                        }

                        referralSuccessMessage?.let { msg ->
                            Text(text = msg, color = Color.Green, style = MaterialTheme.typography.bodySmall)
                        }
                        referralState.errorMessage?.let { err ->
                            Text(text = err, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // SECTION 2: GDPR Consent Management
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_privacy_title),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.settings_privacy_desc),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Button(
                            onClick = {
                                activity?.let { act ->
                                    viewModel.showPrivacyOptions(act) { /* onDismiss */ }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.settings_privacy_preferences))
                        }

                        TextButton(
                            onClick = {
                                val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(PRIVACY_POLICY_URL),
                                )
                                context.startActivity(browserIntent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.settings_privacy_policy_button))
                        }
                    }
                }

                // SECTION 3: Account Deletion (Google Play Compliance)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_account_data_title),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(if (currentUserId == null) R.string.settings_clear_local_data_body else R.string.settings_account_delete_body),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Button(
                            onClick = {
                                deletionResult = null
                                showDeleteConfirmation = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(if (currentUserId == null) R.string.settings_clear_local_data_button else R.string.settings_delete_account_button))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(stringResource(R.string.paywall_close), color = MaterialTheme.colorScheme.primary)
            }
        }
    )

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirmation = false },
            title = { Text(stringResource(if (currentUserId == null) R.string.settings_clear_local_data_title else R.string.settings_delete_account_title), color = MaterialTheme.colorScheme.error) },
            containerColor = MaterialTheme.colorScheme.background,
            text = {
                Column {
                    Text(
                        text = stringResource(if (currentUserId == null) R.string.settings_clear_local_data_confirm_body else R.string.settings_delete_account_confirm_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    deletionResult?.takeIf { it != AccountDeletionResult.Success }?.let { result ->
                        Text(
                            text = stringResource(when (result) {
                                AccountDeletionResult.NoSignedInAccount -> R.string.settings_delete_no_account
                                AccountDeletionResult.RequiresRecentLogin -> R.string.settings_delete_reauth_required
                                AccountDeletionResult.RemoteDeletionFailed -> R.string.settings_delete_remote_failed
                                AccountDeletionResult.LocalCleanupFailed -> R.string.settings_clear_local_failed
                                AccountDeletionResult.AccountDeletedLocalCleanupFailed -> R.string.settings_delete_local_failed
                                AccountDeletionResult.Success -> R.string.settings_delete_remote_failed
                            }),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        scope.launch {
                            val result = viewModel.deleteAccountOrClearLocalData()
                            isDeleting = false
                            deletionResult = result
                            if (result == AccountDeletionResult.Success) {
                                showDeleteConfirmation = false
                                onDismiss()
                                activity?.finishAffinity()
                            }
                        }
                    },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    } else {
                        Text(stringResource(if (currentUserId == null) R.string.settings_clear_local_data_confirm else R.string.settings_delete_account_confirm), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    enabled = !isDeleting
                ) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

private const val PRIVACY_POLICY_URL = "https://canburakyol.github.io/focusfarm-privacy/"
