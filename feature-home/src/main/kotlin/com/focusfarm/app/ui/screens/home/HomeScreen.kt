package com.focusfarm.app.ui.screens.home

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.app.domain.FocusTag
import com.focusfarm.app.domain.FocusSessionType
import com.focusfarm.app.domain.AmbienceSound
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.domain.Plant
import com.focusfarm.app.ui.components.FocusBackdrop
import com.focusfarm.app.ui.components.PlantDisplay
import com.focusfarm.core.ui.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen(
    onStartSession: (sessionId: String, plantId: String, minutes: Int) -> Unit,
    onRecoverSession: (sessionId: String) -> Unit,
    onOpenSettings: () -> Unit,
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
                    Toast.makeText(context, "Görev ödülü alındı: +${effect.rewardSeeds} Seed! 🪙", Toast.LENGTH_LONG).show()
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
        onOpenSettings = onOpenSettings,
    )
}

@Composable
internal fun HomeContent(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedPlant = PlantCatalog.getById(state.selectedPlantId) ?: PlantCatalog.FREE.first()
    val focusPlan = HomeFocusPlan.from(
        plantName = selectedPlant.name,
        minutes = state.selectedMinutes,
        sessionType = state.sessionType,
    )
    val scrollState = rememberScrollState()

    FocusBackdrop {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Top Title & Settings Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Focus Farm",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Tohumunu büyüt, bahçeni yeşert.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("⚙️", fontSize = 24.sp)
                    }
                }

                FocusPlanHero(
                    plan = focusPlan,
                    plant = selectedPlant,
                    selectedTag = state.selectedTag,
                )

                // 1. Dashboard Widget (Goal Progress, Seed Balance & Streak)
                DashboardWidget(
                    todayFocusMinutes = state.todayFocusMinutes,
                    dailyGoalMinutes = state.dailyGoalMinutes,
                    seedBalance = state.seedBalance,
                    currentStreak = state.currentStreak
                )

                // 2. Today's Grown Plants
                TodayGrownPlantsWidget(state.todayGrownPlants)

                // 3. Active Missions Widget
                ActiveMissionsWidget(
                    missions = state.activeMissions,
                    onClaim = { onIntent(HomeIntent.ClaimMission(it)) }
                )

                DailySeedBonusWidget(state = state, onIntent = onIntent)

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // 4. Timer/Focus Setup Section
                Text(
                    text = "Odaklanmayı Başlat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )

                // Central Plant display
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    PlantDisplay(
                        plant = selectedPlant,
                        stage = GrowthStage.GROWING,
                        breathing = true,
                        size = 130.dp,
                    )
                }

                // Scrollable Plant Selector
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Yetiştirilecek Bitki",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.availablePlants) { plant ->
                            val isSelected = plant.id == state.selectedPlantId
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { onIntent(HomeIntent.SelectPlant(plant.id)) }
                                    .semantics {
                                        contentDescription = "${plant.name} seç"
                                    }
                            ) {
                                Text(plant.emoji, fontSize = 26.sp)
                            }
                        }
                    }
                }

                // Session Type Selector
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Oturum Modu",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(FocusSessionType.entries) { type ->
                            val isSelected = state.sessionType == type
                            val label = when (type) {
                                FocusSessionType.COUNTDOWN -> "Geri Sayım"
                                FocusSessionType.STOPWATCH -> "Kronometre"
                                FocusSessionType.POMODORO -> "Pomodoro"
                                FocusSessionType.SHORT_BREAK -> "Kısa Mola"
                                FocusSessionType.LONG_BREAK -> "Uzun Mola"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { onIntent(HomeIntent.SelectSessionType(type)) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                        }
                    }
                }

                // Duration Selector
                if (state.sessionType != FocusSessionType.STOPWATCH) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Süre Seçimi: ${state.selectedMinutes} dakika",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = state.selectedMinutes.toFloat(),
                            onValueChange = { onIntent(HomeIntent.SelectDuration(it.toInt())) },
                            valueRange = 5f..180f,
                            steps = 35,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.quickDurations.forEach { minutes ->
                                val selected = state.selectedMinutes == minutes
                                FilterChip(
                                    selected = selected,
                                    onClick = { onIntent(HomeIntent.SelectDuration(minutes)) },
                                    label = {
                                        Text(
                                            text = stringResource(R.string.minutes_short, minutes),
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                    ),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    ) {
                        Text(
                            text = "⏱️ Kronometre modunda oturum süresi sınırı yoktur.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Tags Selector
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Oturum Etiketi",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.availableTags) { tag ->
                            val selected = state.selectedTag == tag
                            FilterChip(
                                selected = selected,
                                onClick = { onIntent(HomeIntent.SelectTag(tag)) },
                                label = { Text("${tag.icon()} ${tag.label()}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                        }
                    }
                }

                // Strict Mode Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Derin Odaklanma (Strict Focus)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Uygulamadan ayrılırsan bitkin solar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isStrict,
                            onCheckedChange = { onIntent(HomeIntent.SetStrictFocus(it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }

                // Ambience Sound Selector
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Ambiyans Sesi",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AmbienceSound.entries.forEach { sound ->
                            val selected = state.ambienceSound == sound
                            FilterChip(
                                selected = selected,
                                onClick = { onIntent(HomeIntent.SelectAmbience(sound)) },
                                label = { Text(sound.label()) },
                                leadingIcon = { Text(sound.icon()) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                        }
                    }
                }
            }

            // Fixed Start Focus Button at the bottom
            Button(
                onClick = { onIntent(HomeIntent.StartTodaySession) },
                enabled = !state.isStartingSession,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(56.dp)
                    .widthIn(max = 480.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = focusPlan.startLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun FocusPlanHero(
    plan: HomeFocusPlan,
    plant: Plant,
    selectedTag: FocusTag,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                PlantDisplay(
                    plant = plant,
                    stage = GrowthStage.GROWING,
                    breathing = true,
                    size = 66.dp,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BUGÜNÜN ODAĞI",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = plan.headline,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${selectedTag.icon()} ${selectedTag.label()} • ${plan.detail}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DashboardWidget(
    todayFocusMinutes: Int,
    dailyGoalMinutes: Int,
    seedBalance: Int,
    currentStreak: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: Daily Goal Progress Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                val progress = if (dailyGoalMinutes > 0) todayFocusMinutes.toFloat() / dailyGoalMinutes else 0f
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$todayFocusMinutes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "/ $dailyGoalMinutes dk",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: Wallet & Streak
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Wallet Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.semantics { contentDescription = "$seedBalance tohum var" }
                ) {
                    Text("🪙", fontSize = 20.sp)
                    Column {
                        Text(
                            text = "$seedBalance Seed",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Seed Cüzdanı",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Streak Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.semantics { contentDescription = "Aktif seri $currentStreak gün" }
                ) {
                    Text("🔥", fontSize = 20.sp)
                    Column {
                        Text(
                            text = "$currentStreak Günlük Seri",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ritmini koruyorsun",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayGrownPlantsWidget(plants: List<TodayGrownPlant>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Bugün Yetiştirilen Bitkiler",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (plants.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    text = "Bugün henüz yetiştirilmiş bitki yok. İlk odağını ver ve bir tohum büyüt! 🌱",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(plants) { plant ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.width(130.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(plant.emoji, fontSize = 28.sp)
                            Text(
                                text = plant.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${plant.durationMinutes} dk",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveMissionsWidget(
    missions: List<HomeMissionItem>,
    onClaim: (String) -> Unit
) {
    if (missions.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Günlük Görevler",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        missions.take(2).forEach { item ->
            val mission = item.mission
            val progress = item.progress
            val currentProgressValue = progress?.currentValue ?: 0
            val targetValue = mission.goalValue
            val completed = currentProgressValue >= targetValue
            val isClaimed = progress?.isClaimed == true

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = getMissionTitle(mission),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (currentProgressValue.toFloat() / targetValue.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "$currentProgressValue / $targetValue • +${mission.rewardSeeds} Seed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (completed && !isClaimed) {
                        Button(
                            onClick = { onClaim(mission.id) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Ödülü Al", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isClaimed) {
                        Text(
                            text = "✓ Alındı",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun getMissionTitle(mission: com.focusfarm.app.domain.Mission): String = when (mission.goalType.name) {
    "COMPLETE_SESSIONS" -> "${mission.goalValue} odak seansı tamamla"
    "FOCUS_MINUTES" -> "${mission.goalValue} dakika odaklan"
    "DEEP_SESSION" -> "Tek seferde ${mission.goalValue} dakika odaklan"
    else -> "Günlük görevi tamamla"
}

private fun AmbienceSound.icon(): String = when (this) {
    AmbienceSound.NONE -> "♪"
    AmbienceSound.RAIN -> "☂"
    AmbienceSound.WHITE_NOISE -> "≈"
}

private fun AmbienceSound.label(): String = when (this) {
    AmbienceSound.NONE -> "Kapalı"
    AmbienceSound.RAIN -> "Yağmur"
    AmbienceSound.WHITE_NOISE -> "Gürültü"
}

private fun FocusTag.icon(): String = when (this) {
    FocusTag.WORK -> "💼"
    FocusTag.STUDY -> "✏️"
    FocusTag.READING -> "📖"
    FocusTag.EXERCISE -> "🏃"
    FocusTag.OTHER -> "●"
}

private fun FocusTag.label(): String = when (this) {
    FocusTag.WORK -> "Çalışma"
    FocusTag.STUDY -> "Ders"
    FocusTag.READING -> "Okuma"
    FocusTag.EXERCISE -> "Egzersiz"
    FocusTag.OTHER -> "Diğer"
}

@Composable
private fun DailySeedBonusWidget(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit
) {
    if (state.dailySeedBonusClaimed) return

    val context = LocalContext.current
    val activity = context as? Activity

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Günlük Seed Bonusu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (state.isPremium) {
                        "Premium üye olduğunuz için 50 Seed ödülünüzü doğrudan alabilirsiniz."
                    } else {
                        "50 Seed kazanmak için kısa bir reklam izleyin."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = { onIntent(HomeIntent.ClaimDailySeedBonus(activity)) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text(
                    text = if (state.isPremium) "Al (+50)" else "İzle (+50)",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
