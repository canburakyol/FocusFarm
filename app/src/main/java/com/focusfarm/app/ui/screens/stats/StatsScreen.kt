package com.focusfarm.app.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.R
import com.focusfarm.app.data.local.ReflectionHistoryItem
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.ui.components.FocusBackdrop
import com.focusfarm.app.ui.theme.Amber
import com.focusfarm.app.ui.theme.CreamDim
import com.focusfarm.app.ui.theme.ForestDark
import com.focusfarm.app.ui.theme.ForestLight
import com.focusfarm.app.ui.theme.ForestMid
import com.focusfarm.app.ui.theme.TimerFontFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val totalMinutes by viewModel.totalMinutes.collectAsStateWithLifecycle()
    val sessionCount by viewModel.sessionCount.collectAsStateWithLifecycle()
    val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()
    val weeklySessions by viewModel.weeklySessions.collectAsStateWithLifecycle()
    val streakDays by viewModel.streakDays.collectAsStateWithLifecycle()
    val weeklyChallenge by viewModel.weeklyChallenge.collectAsStateWithLifecycle()
    val isWeeklyRewardClaimed by viewModel.isWeeklyRewardClaimed.collectAsStateWithLifecycle()
    val reflectionHistory by viewModel.reflectionHistory.collectAsStateWithLifecycle()

    val totalHours = totalMinutes / 60
    val todayMinutes = remember(weeklySessions) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        weeklySessions.filter { it.completedAt >= todayStart }.sumOf { it.durationMinutes }
    }

    FocusBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        Text(stringResource(R.string.tab_stats), style = MaterialTheme.typography.displayMedium)

        // Summary row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(
                stringResource(R.string.streak_days_short, streakDays),
                stringResource(R.string.current_streak),
                Modifier.weight(1f),
            )
            SummaryCard(stringResource(R.string.total_hours_short, totalHours), stringResource(R.string.total_focus), Modifier.weight(1f))
            SummaryCard(stringResource(R.string.total_focus_minutes, todayMinutes), stringResource(R.string.today), Modifier.weight(1f))
        }
        SummaryCard(
            value = "$sessionCount",
            label = stringResource(R.string.total_sessions),
            modifier = Modifier.fillMaxWidth(),
        )

        Card(colors = CardDefaults.cardColors(containerColor = ForestMid), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    weeklyChallenge.seasonTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CreamDim,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    weeklyChallenge.seasonSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = CreamDim,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(
                        R.string.weekly_challenge_progress,
                        weeklyChallenge.currentSessions,
                        weeklyChallenge.targetSessions,
                        weeklyChallenge.currentMinutes,
                        weeklyChallenge.targetMinutes,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (weeklyChallenge.isCompleted) Amber else CreamDim,
                )
                Spacer(Modifier.height(8.dp))
                ProgressTrack(weeklyChallenge.overallRatio)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.weekly_reward_seeds, weeklyChallenge.rewardSeeds),
                    style = MaterialTheme.typography.labelSmall,
                    color = Amber,
                )
                if (isWeeklyRewardClaimed) {
                    Text(
                        text = stringResource(R.string.weekly_reward_claimed),
                        style = MaterialTheme.typography.labelSmall,
                        color = CreamDim,
                    )
                }
            }
        }

        // Weekly bar chart
        Card(colors = CardDefaults.cardColors(containerColor = ForestMid), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.this_week), style = MaterialTheme.typography.bodyMedium, color = CreamDim, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                WeeklyChart(weeklySessions)
            }
        }

        // Recent sessions
        if (recentSessions.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = ForestMid), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.recent_sessions), style = MaterialTheme.typography.bodyMedium, color = CreamDim, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    recentSessions.forEach { session ->
                        SessionRow(session)
                        HorizontalDivider(color = ForestLight, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }

        if (reflectionHistory.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = ForestMid), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.reflection_history_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CreamDim,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    reflectionHistory.forEach { item ->
                        ReflectionRow(item)
                        HorizontalDivider(color = ForestLight, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }

        if (sessionCount == 0) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("📊", fontSize = 56.sp)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.no_data_title), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.no_data_desc), style = MaterialTheme.typography.bodyMedium, color = CreamDim, textAlign = TextAlign.Center)
            }
        }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WeeklyChart(sessions: List<FocusSession>) {
    val context = LocalContext.current
    val dayLabels = listOf(
        context.getString(R.string.day_mon_short),
        context.getString(R.string.day_tue_short),
        context.getString(R.string.day_wed_short),
        context.getString(R.string.day_thu_short),
        context.getString(R.string.day_fri_short),
        context.getString(R.string.day_sat_short),
        context.getString(R.string.day_sun_short),
    )

    val weeklyMinutes = remember(sessions) {
        val result = IntArray(7)
        sessions.forEach { session ->
            val cal = Calendar.getInstance().apply { timeInMillis = session.completedAt }
            val dayIndex = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            result[dayIndex] += session.durationMinutes
        }
        result.toList()
    }
    val maxMinutes = (weeklyMinutes.maxOrNull() ?: 1).coerceAtLeast(1)
    val todayIndex = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7

    Row(
        modifier = Modifier.fillMaxWidth().height(130.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        weeklyMinutes.forEachIndexed { index, minutes ->
            val barHeight = ((minutes.toFloat() / maxMinutes) * 100).dp.coerceAtLeast(4.dp)
            val isToday = index == todayIndex
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                if (minutes > 0) {
                    Text("$minutes", fontFamily = TimerFontFamily, fontSize = 9.sp, color = CreamDim)
                    Spacer(Modifier.height(2.dp))
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isToday) Amber else ForestLight),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    dayLabels[index],
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) Amber else CreamDim,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun SessionRow(session: FocusSession) {
    val dateFormat = remember { SimpleDateFormat("d MMM HH:mm", Locale.forLanguageTag("tr")) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(session.plantEmoji, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(session.plantName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(dateFormat.format(Date(session.completedAt)), style = MaterialTheme.typography.bodySmall, color = CreamDim)
        }
        Text(
            stringResource(R.string.duration_minutes_short, session.durationMinutes),
            fontFamily = TimerFontFamily,
            fontSize = 13.sp,
            color = Amber,
        )
    }
}

@Composable
private fun SummaryCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = ForestMid), shape = RoundedCornerShape(20.dp), modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            Text(value, fontFamily = TimerFontFamily, fontSize = 22.sp, color = Amber, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = CreamDim, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ReflectionRow(item: ReflectionHistoryItem) {
    val dateFormat = remember { SimpleDateFormat("d MMM HH:mm", Locale.forLanguageTag("tr")) }
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
                "${item.plantName} - ${item.durationMinutes} dk",
                style = MaterialTheme.typography.bodySmall,
                color = CreamDim,
            )
            Spacer(Modifier.weight(1f))
            Text(
                dateFormat.format(Date(item.completedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = CreamDim,
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
            .background(ForestLight),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Amber),
        )
    }
}


