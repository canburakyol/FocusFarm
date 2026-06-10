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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.app.Activity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.data.local.ReflectionHistoryItem
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.ui.components.FocusBackdrop
import com.focusfarm.app.ui.theme.Amber
import com.focusfarm.app.ui.theme.CreamDim
import com.focusfarm.app.ui.theme.ForestDark
import com.focusfarm.app.ui.theme.ForestLight
import com.focusfarm.app.ui.theme.ForestMid
import com.focusfarm.app.ui.theme.TimerFontFamily
import com.focusfarm.core.ui.R
import kotlinx.coroutines.launch
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

    var showSettings by remember { mutableStateOf(false) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.tab_stats), style = MaterialTheme.typography.displayMedium)
                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Amber
                    )
                }
            }

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

    if (showSettings) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettings = false }
        )
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
    val dateFormat = remember { SimpleDateFormat("d MMM HH:mm", Locale.getDefault()) }
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

@Composable
private fun SettingsDialog(
    viewModel: StatsViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }
    val scope = rememberCoroutineScope()

    val referralState by viewModel.referralState.collectAsStateWithLifecycle()
    var referralInput by remember { mutableStateOf("") }
    var referralSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isReferralLoading by remember { mutableStateOf(false) }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ayarlar",
                color = Amber,
                style = MaterialTheme.typography.titleLarge
            )
        },
        containerColor = ForestMid,
        shape = RoundedCornerShape(28.dp),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // SECTION 1: Enter Referral Code
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Arkadaş Kodu Kullan",
                            color = Amber,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bir arkadaşının kodunu girerek 50 Seed kazan.",
                            color = CreamDim,
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
                                label = { Text("Kod", color = CreamDim) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Amber,
                                    unfocusedBorderColor = ForestLight,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
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
                                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = ForestDark)
                            ) {
                                if (isReferralLoading || referralState.isLoading) {
                                    CircularProgressIndicator(
                                        color = ForestDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text("Uygula")
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
                    colors = CardDefaults.cardColors(containerColor = ForestDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Gizlilik ve Reklamlar",
                            color = Amber,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reklam rızanızı ve gizlilik seçeneklerinizi dilediğiniz zaman düzenleyebilirsiniz.",
                            color = CreamDim,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Button(
                            onClick = {
                                activity?.let { act ->
                                    viewModel.showPrivacyOptions(act) { /* onDismiss */ }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = ForestDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Gizlilik Tercihlerini Düzenle")
                        }
                    }
                }

                // SECTION 3: Account Deletion (Google Play Compliance)
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Hesap ve Veriler",
                            color = Color(0xFFD56241),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hesabınızı sildiğinizde cihazdaki ve buluttaki tüm verileriniz kalıcı olarak temizlenir.",
                            color = CreamDim,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Button(
                            onClick = { showDeleteConfirmation = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD56241), contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Hesabımı ve Verilerimi Sil")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", color = Amber)
            }
        }
    )

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirmation = false },
            title = { Text("Hesabı Sil?", color = Color(0xFFD56241)) },
            containerColor = ForestDark,
            text = {
                Text(
                    text = "Tüm ilerlemeniz, satın almalarınız ve bahçeniz kalıcı olarak silinecektir. Bu işlem geri alınamaz. Emin misiniz?",
                    color = CreamDim
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        scope.launch {
                            val success = viewModel.deleteAccount()
                            isDeleting = false
                            if (success) {
                                showDeleteConfirmation = false
                                onDismiss()
                                activity?.finishAffinity() // Uygulamayı kapat ve temizle
                            }
                        }
                    },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(color = Color(0xFFD56241), modifier = Modifier.size(20.dp))
                    } else {
                        Text("Evet, Sil", color = Color(0xFFD56241))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    enabled = !isDeleting
                ) {
                    Text("İptal", color = CreamDim)
                }
            }
        )
    }
}

