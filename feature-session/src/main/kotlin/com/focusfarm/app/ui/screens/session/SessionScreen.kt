package com.focusfarm.app.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.app.domain.FocusTag
import com.focusfarm.app.domain.AmbienceSound
import com.focusfarm.app.domain.FocusSessionType
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.data.local.SessionDifficulty
import com.focusfarm.app.ui.components.FocusBackdrop
import com.focusfarm.app.ui.components.PlantDisplay
import com.focusfarm.app.ui.theme.TimerFontFamily
import com.focusfarm.core.ui.R

@Composable
fun SessionScreen(
    sessionId: String,
    plantId: String,
    targetMinutes: Int,
    onBack: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val canRevive by viewModel.canRevive.collectAsStateWithLifecycle()
    var showAbandonDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(viewModel)
        onDispose { lifecycleOwner.lifecycle.removeObserver(viewModel) }
    }

    LaunchedEffect(Unit) {
        if (state.sessionState == SessionState.IDLE) {
            if (targetMinutes > 0) {
                val plant = PlantCatalog.getById(plantId) ?: PlantCatalog.FREE.first()
                viewModel.startSession(sessionId, plant, targetMinutes)
            } else {
                viewModel.recoverSession(sessionId)
            }
        }
    }

    when (state.sessionState) {
        SessionState.KILLED -> {
            val context = LocalContext.current
            val activity = context as? Activity
            KilledScreen(
                canRevive = canRevive,
                statusMessage = statusMessage,
                onDismissMessage = viewModel::dismissStatusMessage,
                onRevive = { viewModel.revive(activity) },
                onBack = {
                    viewModel.leaveKilledSession()
                    onBack()
                },
            )
        }

        SessionState.FINALIZING -> FinalizingScreen(
            state = state,
            onRetry = viewModel::retryPersistCompletedSession,
        )

        SessionState.COMPLETED -> CompletedScreen(
            state = state,
            onSaveReflection = viewModel::saveReflection,
            onSubmitEvaluation = { viewModel.onIntent(SessionIntent.SubmitEvaluation(it)) },
            onBack = onBack,
        )

        SessionState.PAUSED -> PausedScreen(
            state = state,
            onResume = viewModel::resumeSession,
            onAbandon = { showAbandonDialog = true }
        )

        else -> RunningScreen(
            state = state,
            onPause = viewModel::pauseSession,
            onAbandon = { showAbandonDialog = true },
            onCompleteStopwatch = viewModel::completeStopwatch,
            onSelectAmbience = { viewModel.onIntent(SessionIntent.SelectAmbience(it)) },
        )
    }

    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = { Text(stringResource(R.string.abandon_title)) },
            text = { Text(stringResource(R.string.abandon_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showAbandonDialog = false
                    viewModel.abandon()
                    viewModel.leaveKilledSession(onBack)
                }) {
                    Text(stringResource(R.string.abandon_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonDialog = false }) {
                    Text(stringResource(R.string.continue_btn))
                }
            },
        )
    }
}

@Composable
private fun FinalizingScreen(
    state: SessionUiState,
    onRetry: () -> Unit,
) {
    FocusBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PlantDisplay(plant = state.plant, stage = GrowthStage.FULL, size = 160.dp)
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.session_finalizing_title),
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.session_finalizing_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            state.completionErrorMessage?.let { errorMessage ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(stringResource(R.string.session_finalizing_retry))
                }
            }
        }
    }
}

@Composable
private fun PausedScreen(
    state: SessionUiState,
    onResume: () -> Unit,
    onAbandon: () -> Unit,
) {
    FocusBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = "${state.selectedTag.icon()} ${state.selectedTag.label()} • DURDURULDU",
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )

            Spacer(Modifier.weight(1f))
            SessionProgressDial(state)
            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Devam Et", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 4.dp))
                }

                TextButton(
                    onClick = onAbandon,
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(50)),
                ) {
                    Text(
                        stringResource(R.string.give_up),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RunningScreen(
    state: SessionUiState,
    onPause: () -> Unit,
    onAbandon: () -> Unit,
    onCompleteStopwatch: () -> Unit,
    onSelectAmbience: (AmbienceSound) -> Unit,
) {
    FocusBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = "${state.selectedTag.icon()} ${state.selectedTag.label()}",
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(Modifier.weight(1f))
            SessionProgressDial(state)
            Spacer(Modifier.weight(1f))

            Text(
                stringResource(R.string.focus_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AmbienceSound.entries.forEach { sound ->
                    val selected = state.currentAmbience == sound
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectAmbience(sound) },
                        label = { Text(sound.label()) },
                        leadingIcon = { Text(sound.icon()) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                        shape = RoundedCornerShape(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                if (state.sessionType == FocusSessionType.STOPWATCH) {
                    Button(
                        onClick = onCompleteStopwatch,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tamamla", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (!state.isStrict) {
                    Button(
                        onClick = onPause,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = if (state.sessionType == FocusSessionType.STOPWATCH) Modifier.weight(1f) else Modifier.fillMaxWidth(0.5f)
                    ) {
                        Text("Durdur", style = MaterialTheme.typography.labelLarge)
                    }
                }

                TextButton(
                    onClick = onAbandon,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        stringResource(R.string.give_up),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun AmbienceSound.icon(): String = when (this) {
    AmbienceSound.NONE -> "♪"
    AmbienceSound.RAIN -> "☂"
    AmbienceSound.WHITE_NOISE -> "≈"
}

private fun AmbienceSound.label(): String = when (this) {
    AmbienceSound.NONE -> "Kapalı"
    AmbienceSound.RAIN -> "Yağmur"
    AmbienceSound.WHITE_NOISE -> "Beyaz Gürültü"
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
private fun SessionProgressDial(state: SessionUiState) {
    val totalSeconds = (state.targetMinutes * 60).coerceAtLeast(1)
    val progress = if (state.sessionType == FocusSessionType.STOPWATCH) {
        (state.elapsedSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        (1f - state.remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
    }
    
    val secondsToFormat = if (state.sessionType == FocusSessionType.STOPWATCH) state.elapsedSeconds else state.remainingSeconds
    val hours = secondsToFormat / 3600
    val minutes = (secondsToFormat % 3600) / 60
    val seconds = secondsToFormat % 60

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(292.dp)) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 14.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (hours > 0) {
                    "%02d:%02d:%02d".format(hours, minutes, seconds)
                } else {
                    "%02d:%02d".format(minutes, seconds)
                },
                fontFamily = TimerFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 52.sp,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            PlantDisplay(
                plant = state.plant,
                stage = state.growthStage,
                size = 92.dp,
                breathing = state.sessionState == SessionState.RUNNING,
            )
        }
    }
}

@Composable
private fun HarvestRewardCard(
    grantedSeeds: Int,
    baseSeeds: Int,
    wasBoosted: Boolean,
) {
    val reward = SessionRewardPresentation.from(grantedSeeds, baseSeeds, wasBoosted)
    val isLoading = grantedSeeds <= 0
    val cardColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardColor, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isLoading) "Hasat hazırlanıyor" else "Hasat tamamlandı",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isLoading) "…" else "+${reward.totalSeeds} Seed",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        if (!isLoading) {
            Spacer(Modifier.height(6.dp))
            if (reward.hasBonus) {
                Text(
                    text = "${reward.baseSeeds} odak ödülü + ${reward.bonusSeeds} aktif güçlendirme",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = "Bu hasat bahçene ve Seed bakiyene eklendi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun KilledScreen(
    canRevive: Boolean,
    statusMessage: String?,
    onDismissMessage: () -> Unit,
    onRevive: () -> Unit,
    onBack: () -> Unit,
) {
    FocusBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("🥀", fontSize = 80.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.plant_died),
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.plant_died_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            if (canRevive) {
                Button(
                    onClick = onRevive,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.session_revive_now))
                }
                Spacer(Modifier.height(12.dp))
            }

            statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDismissMessage) {
                    Text(stringResource(R.string.action_ok), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onBack,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50)),
            ) {
                Text(
                    stringResource(R.string.go_home),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun CompletedScreen(
    state: SessionUiState,
    onSaveReflection: (String) -> Unit,
    onSubmitEvaluation: (SessionDifficulty) -> Unit,
    onBack: () -> Unit,
) {
    var reflection by remember { mutableStateOf("") }

    FocusBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PlantDisplay(plant = state.plant, stage = GrowthStage.FULL, size = 160.dp)
            Spacer(Modifier.height(32.dp))
            Text(
                stringResource(R.string.session_complete),
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.focused_minutes, state.targetMinutes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            HarvestRewardCard(
                grantedSeeds = state.rewardSeedsGranted,
                baseSeeds = state.rewardBaseSeeds,
                wasBoosted = state.rewardBoosted,
            )

            Spacer(Modifier.height(20.dp))

            Text(stringResource(R.string.session_evaluation_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SessionDifficulty.entries.forEach { difficulty ->
                    val label = when (difficulty) {
                        SessionDifficulty.EASY -> stringResource(R.string.session_evaluation_easy)
                        SessionDifficulty.MODERATE -> stringResource(R.string.session_evaluation_moderate)
                        SessionDifficulty.HARD -> stringResource(R.string.session_evaluation_hard)
                    }
                    TextButton(onClick = { onSubmitEvaluation(difficulty) }) { Text(label) }
                }
            }
            state.suggestedNextMinutes?.let { minutes ->
                Text(
                    stringResource(R.string.session_next_suggestion, minutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = reflection,
                onValueChange = { reflection = it },
                label = { Text(stringResource(R.string.session_reflection_label)) },
                placeholder = { Text(stringResource(R.string.session_reflection_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
            )

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = { onSaveReflection(reflection) },
                enabled = state.completedSessionId != null,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(stringResource(R.string.session_reflection_save))
            }

            if (state.reflectionSaved) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.session_reflection_saved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onBack,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    stringResource(R.string.go_garden),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}
