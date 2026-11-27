package com.focusfarm.app.ui.screens.home

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.domain.AmbienceSound
import com.focusfarm.app.domain.FocusSessionType
import com.focusfarm.app.domain.FocusTag
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.app.domain.Plant
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.ui.components.FocusBackdrop
import com.focusfarm.app.ui.components.PlantDisplay
import com.focusfarm.app.ui.components.labelRes
import com.focusfarm.app.ui.theme.SeedGold
import com.focusfarm.app.ui.theme.TimerFontFamily
import com.focusfarm.core.ui.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen(
    onStartSession: (sessionId: String, plantId: String, minutes: Int) -> Unit,
    onRecoverSession: (sessionId: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HomeEffect.OpenSession -> onStartSession(effect.sessionId, effect.plantId, effect.minutes)
                is HomeEffect.ResumeSession -> onRecoverSession(effect.sessionId)
                is HomeEffect.ShowRecovery -> onRecoverSession(effect.sessionId)
                is HomeEffect.FinalizeSession -> onRecoverSession(effect.sessionId)
                is HomeEffect.MissionClaimed -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.mission_reward_toast, effect.rewardSeeds),
                        Toast.LENGTH_LONG,
                    ).show()
                }
                is HomeEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    HomeContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

/**
 * One focal point above the fold: the plant you are about to grow, its duration and a single
 * start action. Progress, missions and the daily bonus live below in one "Today" card.
 */
@Composable
internal fun HomeContent(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedPlant = PlantCatalog.getById(state.selectedPlantId) ?: PlantCatalog.FREE.first()
    var showSetupSheet by remember { mutableStateOf(false) }
    val openSetup = { showSetupSheet = true }

    FocusBackdrop {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeTopBar(seedBalance = state.seedBalance, currentStreak = state.currentStreak)
            Spacer(Modifier.height(28.dp))
            NudgeLine(state.retentionNudge)
            Spacer(Modifier.height(24.dp))
            PlantHero(plant = selectedPlant, onClick = openSetup)
            Spacer(Modifier.height(20.dp))
            DurationDisplay(state = state, onClick = openSetup)
            if (state.sessionType != FocusSessionType.STOPWATCH) {
                Spacer(Modifier.height(12.dp))
                QuickDurationRow(state = state, onIntent = onIntent)
            }
            Spacer(Modifier.height(8.dp))
            SetupSummaryButton(state = state, onClick = openSetup)
            Spacer(Modifier.height(16.dp))
            val context = LocalContext.current
            // The session's progress and end alerts need notification permission; ask in context, start either way.
            val notificationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { onIntent(HomeIntent.StartTodaySession) }
            Button(
                onClick = {
                    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    if (needsPermission) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onIntent(HomeIntent.StartTodaySession)
                    }
                },
                enabled = !state.isStartingSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .height(60.dp),
                shape = CircleShape,
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.start_focus),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(36.dp))
            TodayCard(state = state, onIntent = onIntent)
            Spacer(Modifier.height(16.dp))
        }

        @OptIn(ExperimentalMaterial3Api::class)
        if (showSetupSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSetupSheet = false },
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                HomeSetupSheet(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier.padding(bottom = 32.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(seedBalance: Int, currentStreak: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        val seedLabel = stringResource(R.string.seed_balance_short, seedBalance)
        StatPill(text = "$seedBalance", description = seedLabel) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(SeedGold),
            )
        }
        Spacer(Modifier.width(8.dp))
        val streakLabel = stringResource(R.string.streak_days_short, currentStreak)
        StatPill(text = "$currentStreak", description = streakLabel) {
            Icon(
                Icons.Rounded.DateRange,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun StatPill(text: String, description: String, leading: @Composable () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun NudgeLine(nudge: HomeRetentionNudge) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(nudge.titleRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (nudge.tone == HomeRetentionNudge.Tone.URGENT) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = nudge.detailArg?.let { stringResource(nudge.detailRes, it) } ?: stringResource(nudge.detailRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PlantHero(plant: Plant, onClick: () -> Unit) {
    val changeLabel = stringResource(R.string.home_change_plant)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(232.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                .clickable(onClickLabel = changeLabel, onClick = onClick),
        ) {
            PlantDisplay(
                plant = plant,
                stage = GrowthStage.FULL,
                breathing = true,
                size = 184.dp,
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onClick) {
            Text(
                text = plant.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = changeLabel,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DurationDisplay(state: HomeUiState, onClick: () -> Unit) {
    val isStopwatch = state.sessionType == FocusSessionType.STOPWATCH
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = if (isStopwatch) "00:00" else "%02d:00".format(state.selectedMinutes),
            fontFamily = TimerFontFamily,
            fontSize = 64.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (isStopwatch) {
            Text(
                text = stringResource(R.string.home_stopwatch_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickDurationRow(state: HomeUiState, onIntent: (HomeIntent) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.quickDurations.forEach { minutes ->
            val label = stringResource(R.string.minutes_short, minutes)
            FilterChip(
                selected = state.selectedMinutes == minutes,
                onClick = { onIntent(HomeIntent.SelectDuration(minutes)) },
                label = { Text("$minutes", fontWeight = FontWeight.SemiBold) },
                shape = CircleShape,
                modifier = Modifier.semantics { contentDescription = label },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun SetupSummaryButton(state: HomeUiState, onClick: () -> Unit) {
    val parts = buildList {
        add(stringResource(state.selectedTag.labelRes()))
        add(stringResource(state.sessionType.labelRes()))
        if (state.isStrict) add(stringResource(R.string.home_strict_mode))
        if (state.ambienceSound != AmbienceSound.NONE) add(stringResource(state.ambienceSound.labelRes()))
    }
    TextButton(onClick = onClick) {
        Text(
            text = parts.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Rounded.Edit,
            contentDescription = stringResource(R.string.home_edit_setup),
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TodayCard(state: HomeUiState, onIntent: (HomeIntent) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DailyGoalSection(
                todayFocusMinutes = state.todayFocusMinutes,
                dailyGoalMinutes = state.dailyGoalMinutes,
                plants = state.todayGrownPlants,
            )
            if (state.activeMissions.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MissionsSection(
                    missions = state.activeMissions,
                    onClaim = { onIntent(HomeIntent.ClaimMission(it)) },
                )
            }
            if (!state.dailySeedBonusClaimed) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                DailyBonusRow(state = state, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun DailyGoalSection(
    todayFocusMinutes: Int,
    dailyGoalMinutes: Int,
    plants: List<TodayGrownPlant>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.today),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.home_goal_progress, todayFocusMinutes, dailyGoalMinutes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val progress = if (dailyGoalMinutes > 0) todayFocusMinutes.toFloat() / dailyGoalMinutes else 0f
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        if (plants.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(plants) { grown ->
                    PlantDisplay(
                        plant = PlantCatalog.getById(grown.plantId) ?: PlantCatalog.FREE.first(),
                        stage = GrowthStage.FULL,
                        size = 40.dp,
                        modifier = Modifier.semantics { contentDescription = grown.name },
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionsSection(missions: List<HomeMissionItem>, onClaim: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.daily_quests_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        missions.take(2).forEach { item ->
            val mission = item.mission
            val current = item.progress?.currentValue ?: 0
            val completed = current >= mission.goalValue
            val claimed = item.progress?.isClaimed == true
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = missionTitle(mission),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            R.string.home_mission_progress,
                            current.coerceAtMost(mission.goalValue),
                            mission.goalValue,
                            mission.rewardSeeds,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when {
                    claimed -> Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    completed -> FilledTonalButton(onClick = { onClaim(mission.id) }) {
                        Text(stringResource(R.string.home_mission_claim))
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyBonusRow(state: HomeUiState, onIntent: (HomeIntent) -> Unit) {
    val activity = LocalContext.current as? Activity
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_daily_bonus_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    if (state.isPremium) R.string.home_daily_bonus_premium else R.string.home_daily_bonus_ad,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        OutlinedButton(onClick = { onIntent(HomeIntent.ClaimDailySeedBonus(activity)) }) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(SeedGold),
            )
            Spacer(Modifier.width(6.dp))
            Text("+50", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun missionTitle(mission: com.focusfarm.app.domain.Mission): String = when (mission.goalType.name) {
    "COMPLETE_SESSIONS" -> pluralStringResource(R.plurals.mission_complete_sessions, mission.goalValue, mission.goalValue)
    "FOCUS_MINUTES" -> stringResource(R.string.mission_focus_minutes, mission.goalValue)
    "DEEP_SESSION" -> stringResource(R.string.mission_deep_session, mission.goalValue)
    else -> stringResource(R.string.mission_generic)
}
