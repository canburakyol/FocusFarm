package com.focusfarm.app.ui.screens.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.R
import com.focusfarm.app.domain.DailyQuestType
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.app.domain.WeeklyChallengeProgress
import com.focusfarm.app.ui.components.FocusBackdrop
import com.focusfarm.app.ui.components.PlantDisplay
import com.focusfarm.app.ui.theme.Amber
import com.focusfarm.app.ui.theme.CreamDim
import com.focusfarm.app.ui.theme.ForestDark
import com.focusfarm.app.ui.theme.ForestLight
import com.focusfarm.app.ui.theme.ForestMid
import com.focusfarm.app.ui.theme.Success
import com.focusfarm.app.ui.theme.TimerFontFamily
import com.focusfarm.app.ui.theme.plantAccentColor
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

private val TIMER_OPTIONS = listOf(5, 10, 15, 25, 30, 45, 60)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onStartSession: (plantId: String, minutes: Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val selectedPlant by viewModel.selectedPlant.collectAsStateWithLifecycle()
    val selectedMinutes by viewModel.selectedMinutes.collectAsStateWithLifecycle()
    val availablePlants by viewModel.availablePlants.collectAsStateWithLifecycle()
    val isPremiumUnlocked by viewModel.isPremiumUnlocked.collectAsStateWithLifecycle()
    val recentSession by viewModel.recentSession.collectAsStateWithLifecycle()
    val streakDays by viewModel.streakDays.collectAsStateWithLifecycle()
    val todayMinutes by viewModel.todayMinutes.collectAsStateWithLifecycle()
    val dailyQuests by viewModel.dailyQuests.collectAsStateWithLifecycle()
    val weeklyChallenge by viewModel.weeklyChallenge.collectAsStateWithLifecycle()
    val canClaimWeeklyReward by viewModel.canClaimWeeklyReward.collectAsStateWithLifecycle()
    val isWeeklyRewardClaimed by viewModel.isWeeklyRewardClaimed.collectAsStateWithLifecycle()
    val seedBalance by viewModel.seedBalance.collectAsStateWithLifecycle()
    val freezeTokens by viewModel.freezeTokens.collectAsStateWithLifecycle()
    val canUseFreeze by viewModel.canUseFreeze.collectAsStateWithLifecycle()
    val canUseQuestRerollAd by viewModel.canUseQuestRerollAd.collectAsStateWithLifecycle()
    val isQuestRerollUsedToday by viewModel.isQuestRerollUsedToday.collectAsStateWithLifecycle()
    val canActivateNextSessionSeedBoost by viewModel.canActivateNextSessionSeedBoost.collectAsStateWithLifecycle()
    val isNextSessionSeedBoostActive by viewModel.isNextSessionSeedBoostActive.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val dailyPlan by viewModel.dailyPlan.collectAsStateWithLifecycle()
    val personalGoalText by viewModel.personalGoalText.collectAsStateWithLifecycle()
    val engagementNudge by viewModel.engagementNudge.collectAsStateWithLifecycle()
    val weeklyReport by viewModel.weeklyReport.collectAsStateWithLifecycle()
    val isWeeklyReportVisible by viewModel.isWeeklyReportVisible.collectAsStateWithLifecycle()

    FocusBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        Text(
            text = stringResource(R.string.greeting),
            style = MaterialTheme.typography.displayMedium,
        )
        Text(
            text = stringResource(R.string.greeting_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = CreamDim,
        )

        statusMessage?.let { message ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestMid),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = CreamDim,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::dismissMessage) {
                        Text(stringResource(R.string.action_ok), color = Amber)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeStatCard(
                value = stringResource(R.string.streak_days_short, streakDays),
                label = stringResource(R.string.current_streak),
                modifier = Modifier.weight(1f),
            )
            HomeStatCard(
                value = stringResource(R.string.total_focus_minutes, todayMinutes),
                label = stringResource(R.string.today_focus),
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeStatCard(
                value = stringResource(R.string.seed_balance_short, seedBalance),
                label = stringResource(R.string.seed_balance),
                modifier = Modifier.weight(1f),
            )
            HomeStatCard(
                value = stringResource(R.string.freeze_tokens_short, freezeTokens),
                label = stringResource(R.string.freeze_tokens),
                modifier = Modifier.weight(1f),
            )
        }

        if (!isPremiumUnlocked && activity != null) {
            RewardedAdBonusCard(
                canUseQuestRerollAd = canUseQuestRerollAd,
                isQuestRerollUsedToday = isQuestRerollUsedToday,
                canActivateNextSessionSeedBoost = canActivateNextSessionSeedBoost,
                isNextSessionSeedBoostActive = isNextSessionSeedBoostActive,
                onQuestReroll = { viewModel.rerollDailyQuest(activity) },
                onNextSessionBoost = { viewModel.activateNextSessionSeedBoost(activity) },
            )
        }

        if (!isPremiumUnlocked) {
            AdBannerCard()
        }

        DailyPlanCard(
            plan = dailyPlan,
            onStartPlan = { minutes ->
                viewModel.applySuggestedMinutes(minutes)
                onStartSession(selectedPlant.id, minutes)
            },
        )

        PersonalGoalCard(goalText = personalGoalText)

        engagementNudge?.let { nudge ->
            EngagementNudgeCard(
                nudge = nudge,
                onAction = {
                    viewModel.applySuggestedMinutes(nudge.actionMinutes)
                    onStartSession(selectedPlant.id, nudge.actionMinutes)
                },
            )
        }

        if (canUseFreeze) {
            Button(
                onClick = viewModel::useStreakFreeze,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestMid,
                    contentColor = CreamDim,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.use_streak_freeze),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        WeeklyChallengeCard(
            challenge = weeklyChallenge,
            canClaimReward = canClaimWeeklyReward,
            isRewardClaimed = isWeeklyRewardClaimed,
            onClaimReward = viewModel::claimWeeklyReward,
        )
        DailyQuestsCard(
            quests = dailyQuests,
            onClaim = viewModel::claimQuest,
        )

        recentSession?.let { session ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestMid),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.last_session),
                        style = MaterialTheme.typography.labelSmall,
                        color = CreamDim,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(session.plantEmoji, fontSize = 32.sp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(session.plantName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.focused_minutes, session.durationMinutes),
                                style = MaterialTheme.typography.bodySmall,
                                color = Amber,
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.select_plant),
            style = MaterialTheme.typography.labelSmall,
            color = CreamDim,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            availablePlants.forEach { plant ->
                val isSelected = selectedPlant.id == plant.id
                val accentColor = plantAccentColor(plant.id)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(96.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) accentColor.copy(0.15f) else ForestMid)
                        .border(
                            1.5.dp,
                            if (isSelected) accentColor else ForestLight,
                            RoundedCornerShape(20.dp),
                        )
                        .clickable { viewModel.selectPlant(plant) }
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                ) {
                    PlantDisplay(plant = plant, stage = GrowthStage.FULL, size = 56.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        plant.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.min_session, plant.minMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = CreamDim,
                        fontSize = 10.sp,
                    )
                }
            }
        }
        if (!isPremiumUnlocked) {
            Text(
                text = stringResource(R.string.premium_locked_hint),
                style = MaterialTheme.typography.bodySmall,
                color = CreamDim.copy(alpha = 0.85f),
            )
        }

        Text(
            text = stringResource(R.string.select_duration),
            style = MaterialTheme.typography.labelSmall,
            color = CreamDim,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TIMER_OPTIONS.forEach { min ->
                val isSelected = selectedMinutes == min
                val isDisabled = min < selectedPlant.minMinutes
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) Amber else ForestMid)
                        .border(1.dp, if (isSelected) Amber else ForestLight, RoundedCornerShape(50))
                        .clickable(enabled = !isDisabled) { viewModel.selectMinutes(min) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.minutes_short, min),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isSelected -> ForestDark
                            isDisabled -> CreamDim.copy(alpha = 0.3f)
                            else -> CreamDim
                        },
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = ForestMid),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            ) {
                PlantDisplay(plant = selectedPlant, stage = GrowthStage.GROWING, breathing = true)
                Spacer(Modifier.height(24.dp))
                Text(selectedPlant.name, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    selectedPlant.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CreamDim,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Button(
            onClick = { onStartSession(selectedPlant.id, selectedMinutes) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = ForestDark),
        ) {
            Text(
                stringResource(R.string.start_focus),
                style = MaterialTheme.typography.labelLarge,
                fontSize = 18.sp,
            )
        }

        Text(
            stringResource(R.string.app_leave_warning),
            style = MaterialTheme.typography.bodySmall,
            color = CreamDim.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

            Spacer(Modifier.height(24.dp))
        }

        if (isWeeklyReportVisible && weeklyReport != null) {
            WeeklyPersonalReportDialog(
                report = weeklyReport!!,
                onDismiss = viewModel::dismissWeeklyReport,
                onStartWeekPlan = { minutes ->
                    viewModel.dismissWeeklyReport()
                    viewModel.applySuggestedMinutes(minutes)
                    onStartSession(selectedPlant.id, minutes)
                },
            )
        }
    }
}

@Composable
private fun HomeStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForestMid),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
        ) {
            Text(
                text = value,
                fontFamily = TimerFontFamily,
                style = MaterialTheme.typography.titleLarge,
                color = Amber,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = CreamDim,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RewardedAdBonusCard(
    canUseQuestRerollAd: Boolean,
    isQuestRerollUsedToday: Boolean,
    canActivateNextSessionSeedBoost: Boolean,
    isNextSessionSeedBoostActive: Boolean,
    onQuestReroll: () -> Unit,
    onNextSessionBoost: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForestMid),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.ad_rewards_title),
                style = MaterialTheme.typography.bodyMedium,
                color = CreamDim,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.ad_rewards_desc),
                style = MaterialTheme.typography.bodySmall,
                color = CreamDim,
            )
            Button(
                onClick = onQuestReroll,
                enabled = canUseQuestRerollAd,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = ForestDark),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (isQuestRerollUsedToday) {
                        stringResource(R.string.ad_reward_quest_reroll_done)
                    } else {
                        stringResource(R.string.ad_reward_quest_reroll_action)
                    },
                )
            }
            Button(
                onClick = onNextSessionBoost,
                enabled = canActivateNextSessionSeedBoost,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ForestLight, contentColor = CreamDim),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (isNextSessionSeedBoostActive) {
                        stringResource(R.string.ad_reward_next_session_boost_ready)
                    } else {
                        stringResource(R.string.ad_reward_next_session_boost_action)
                    },
                )
            }
        }
    }
}

@Composable
private fun AdBannerCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForestMid),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.ad_banner_label),
                style = MaterialTheme.typography.labelSmall,
                color = CreamDim.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            AdMobBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            )
        }
    }
}

@Composable
private fun AdMobBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = {
            AdView(it).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = context.getString(R.string.admob_banner_unit_id)
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}

@Composable
private fun WeeklyPersonalReportDialog(
    report: WeeklyReportUiState,
    onDismiss: () -> Unit,
    onStartWeekPlan: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForestMid,
        title = {
            Text(
                text = stringResource(R.string.weekly_report_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.weekly_report_subtitle, report.weekLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = CreamDim,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportMetric(
                        label = stringResource(R.string.weekly_report_stat_sessions),
                        value = report.totalSessions.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    ReportMetric(
                        label = stringResource(R.string.weekly_report_stat_minutes),
                        value = "${report.totalMinutes} dk",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportMetric(
                        label = stringResource(R.string.weekly_report_stat_average),
                        value = "${report.averageSessionMinutes} dk",
                        modifier = Modifier.weight(1f),
                    )
                    ReportMetric(
                        label = stringResource(R.string.weekly_report_stat_longest),
                        value = "${report.longestSessionMinutes} dk",
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = stringResource(
                        R.string.weekly_report_best_day,
                        report.bestDayLabel,
                        report.bestDayMinutes,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = CreamDim,
                )
                Text(
                    text = stringResource(R.string.weekly_report_insight_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = Amber,
                )
                Text(
                    text = report.insight,
                    style = MaterialTheme.typography.bodySmall,
                    color = CreamDim,
                )
                Text(
                    text = stringResource(R.string.weekly_report_next_goal, report.nextGoalMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = Amber,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onStartWeekPlan(report.suggestedSessionMinutes) },
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = ForestDark),
                shape = RoundedCornerShape(50),
            ) {
                Text(stringResource(R.string.weekly_report_action_start, report.suggestedSessionMinutes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.weekly_report_action_later),
                    color = CreamDim,
                )
            }
        },
    )
}

@Composable
private fun ReportMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForestLight.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Amber,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = CreamDim,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WeeklyChallengeCard(
    challenge: WeeklyChallengeProgress,
    canClaimReward: Boolean,
    isRewardClaimed: Boolean,
    onClaimReward: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForestMid),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = challenge.seasonTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = CreamDim,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = challenge.seasonSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = CreamDim,
            )
            ProgressTrack(challenge.overallRatio)
            Text(
                text = stringResource(
                    R.string.weekly_challenge_progress,
                    challenge.currentSessions,
                    challenge.targetSessions,
                    challenge.currentMinutes,
                    challenge.targetMinutes,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (challenge.isCompleted) Success else CreamDim,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.weekly_reward_seeds, challenge.rewardSeeds),
                style = MaterialTheme.typography.labelSmall,
                color = Amber,
            )
            when {
                canClaimReward -> {
                    TextButton(onClick = onClaimReward) {
                        Text(stringResource(R.string.weekly_reward_claim), color = Amber)
                    }
                }

                isRewardClaimed -> {
                    Text(
                        text = stringResource(R.string.weekly_reward_claimed),
                        style = MaterialTheme.typography.labelSmall,
                        color = Success,
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyPlanCard(
    plan: DailyPlanUiState,
    onStartPlan: (Int) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForestMid),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.daily_plan_title),
                style = MaterialTheme.typography.bodyMedium,
                color = CreamDim,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.daily_plan_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = CreamDim,
            )
            ProgressTrack(plan.progressRatio)
            Text(
                text = stringResource(
                    R.string.daily_plan_progress,
                    plan.completedSessions,
                    plan.targetSessions,
                    plan.completedMinutes,
                    plan.targetMinutes,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (plan.isCompleted) Success else CreamDim,
                fontWeight = FontWeight.Medium,
            )
            if (!plan.isCompleted) {
                Button(
                    onClick = { onStartPlan(plan.suggestedSessionMinutes) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = ForestDark),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.daily_plan_action_start, plan.suggestedSessionMinutes))
                }
            }
        }
    }
}

@Composable
private fun PersonalGoalCard(goalText: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForestMid),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.personal_goal_title),
                style = MaterialTheme.typography.bodyMedium,
                color = CreamDim,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = goalText,
                style = MaterialTheme.typography.bodySmall,
                color = CreamDim,
            )
        }
    }
}

@Composable
private fun EngagementNudgeCard(
    nudge: EngagementNudgeUiState,
    onAction: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForestMid.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Amber.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.engagement_nudge_title),
                style = MaterialTheme.typography.labelSmall,
                color = Amber,
            )
            Text(
                text = nudge.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = nudge.message,
                style = MaterialTheme.typography.bodySmall,
                color = CreamDim,
            )
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = ForestDark),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(nudge.actionLabel)
            }
        }
    }
}

@Composable
private fun DailyQuestsCard(
    quests: List<DailyQuestUiState>,
    onClaim: (DailyQuestType) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForestMid),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.daily_quests_title),
                style = MaterialTheme.typography.bodyMedium,
                color = CreamDim,
                fontWeight = FontWeight.SemiBold,
            )
            quests.forEach { quest ->
                QuestRow(
                    quest = quest,
                    onClaim = onClaim,
                )
            }
        }
    }
}

@Composable
private fun QuestRow(
    quest: DailyQuestUiState,
    onClaim: (DailyQuestType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (quest.quest.type) {
                        DailyQuestType.COMPLETE_SESSIONS ->
                            stringResource(R.string.quest_complete_sessions, quest.quest.target)
                        DailyQuestType.FOCUS_MINUTES ->
                            stringResource(R.string.quest_focus_minutes, quest.quest.target)
                        DailyQuestType.DEEP_SESSION ->
                            stringResource(R.string.quest_deep_session, quest.quest.target)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = stringResource(R.string.quest_reward_seeds, quest.quest.rewardSeeds),
                    style = MaterialTheme.typography.labelSmall,
                    color = Amber,
                )
            }

            when {
                quest.isClaimed -> {
                    Text(
                        text = stringResource(R.string.quest_claimed),
                        style = MaterialTheme.typography.labelSmall,
                        color = Success,
                    )
                }

                quest.canClaim -> {
                    TextButton(onClick = { onClaim(quest.quest.type) }) {
                        Text(stringResource(R.string.quest_claim), color = Amber)
                    }
                }

                else -> {
                    Text(
                        text = "${quest.quest.progress}/${quest.quest.target}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CreamDim,
                    )
                }
            }
        }
        ProgressTrack(quest.quest.progressRatio)
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

