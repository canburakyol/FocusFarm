package com.focusfarm.app.ui.screens.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.text.TextStyle
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
import com.focusfarm.app.ui.components.labelRes
import com.focusfarm.app.ui.theme.TimerFontFamily
import com.focusfarm.core.ui.R

@Composable
fun SessionScreen(
    sessionId: String,
    plantId: String,
    targetMinutes: Int,
    onBack: () -> Unit,
    onReviewRequested: () -> Unit = {},
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val canRevive by viewModel.canRevive.collectAsStateWithLifecycle()
    var showAbandonDialog by remember { mutableStateOf(false) }
    var gaveUp by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.reviewRequest.collect { onReviewRequested() }
    }

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
                state = state,
                gaveUp = gaveUp,
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

        else -> ActiveSessionScreen(
            state = state,
            isPaused = state.sessionState == SessionState.PAUSED,
            onPause = viewModel::pauseSession,
            onResume = viewModel::resumeSession,
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
                    gaveUp = true
                    // Stay on the withered-plant screen; it persists the failure when the user leaves.
                    viewModel.abandon()
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
                style = MaterialTheme.typography.headlineMedium,
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
private fun ActiveSessionScreen(
    state: SessionUiState,
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAbandon: () -> Unit,
    onCompleteStopwatch: () -> Unit,
    onSelectAmbience: (AmbienceSound) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val progress = sessionProgress(state)
    val milestone = SessionGrowthMilestone.from(progress)
    FocusBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isLandscape) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 24.dp, vertical = if (isLandscape) 12.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = CircleShape,
                color = if (isPaused) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                },
            ) {
                Text(
                    text = if (isPaused) {
                        "${stringResource(state.selectedTag.labelRes())} · ${stringResource(R.string.session_paused)}"
                    } else {
                        stringResource(state.selectedTag.labelRes())
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isPaused) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }

            Spacer(if (isLandscape) Modifier.height(12.dp) else Modifier.weight(1f))
            GrowthRing(
                state = state,
                progress = progress,
                stage = milestone.stage,
                ringSize = if (isLandscape) 200.dp else 288.dp,
            )
            Spacer(Modifier.height(if (isLandscape) 12.dp else 28.dp))
            Text(
                text = formatClock(
                    if (state.sessionType == FocusSessionType.STOPWATCH) state.elapsedSeconds else state.remainingSeconds,
                ),
                fontFamily = TimerFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = if (isLandscape) 44.sp else 60.sp,
                // Fixed-width digits so the countdown does not shift sideways every second.
                style = TextStyle(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(milestone.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(milestone.detailRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(if (isLandscape) Modifier.height(16.dp) else Modifier.weight(1f))

            if (!isPaused) {
                Text(
                    stringResource(R.string.focus_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                AmbienceSelector(selected = state.currentAmbience, onSelect = onSelectAmbience)
                Spacer(Modifier.height(20.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val buttonModifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                when {
                    isPaused -> Button(onClick = onResume, modifier = buttonModifier) {
                        Text(stringResource(R.string.continue_btn), style = MaterialTheme.typography.titleSmall)
                    }
                    state.sessionType == FocusSessionType.STOPWATCH -> Button(
                        onClick = onCompleteStopwatch,
                        modifier = buttonModifier,
                    ) {
                        Text(stringResource(R.string.session_finish), style = MaterialTheme.typography.titleSmall)
                    }
                    !state.isStrict -> FilledTonalButton(onClick = onPause, modifier = buttonModifier) {
                        Text(stringResource(R.string.session_pause), style = MaterialTheme.typography.titleSmall)
                    }
                }
                OutlinedButton(
                    onClick = onAbandon,
                    modifier = buttonModifier,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.give_up), style = MaterialTheme.typography.titleSmall)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmbienceSelector(selected: AmbienceSound, onSelect: (AmbienceSound) -> Unit) {
    val sounds = AmbienceSound.entries
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp),
    ) {
        sounds.forEachIndexed { index, sound ->
            SegmentedButton(
                selected = selected == sound,
                onClick = { onSelect(sound) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = sounds.size),
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveContainerColor = Color.Transparent,
                ),
            ) {
                Text(
                    text = stringResource(sound.labelRes()),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The plant is the hero: it grows inside a thin progress ring instead of under the timer. */
@Composable
private fun GrowthRing(
    state: SessionUiState,
    progress: Float,
    stage: GrowthStage,
    ringSize: Dp,
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ringSize)) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 6.dp,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
        )
        PlantDisplay(
            plant = state.plant,
            stage = stage,
            size = ringSize * 0.66f,
            breathing = state.sessionState == SessionState.RUNNING,
        )
    }
}

private fun sessionProgress(state: SessionUiState): Float {
    val totalSeconds = (state.targetMinutes * 60).coerceAtLeast(1)
    return if (state.sessionType == FocusSessionType.STOPWATCH) {
        state.elapsedSeconds.toFloat() / totalSeconds
    } else {
        1f - state.remainingSeconds.toFloat() / totalSeconds
    }.coerceIn(0f, 1f)
}

private fun formatClock(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
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
    state: SessionUiState,
    gaveUp: Boolean,
    canRevive: Boolean,
    statusMessage: String?,
    onDismissMessage: () -> Unit,
    onRevive: () -> Unit,
    onBack: () -> Unit,
) {
    // A seed-stage plant is only soil; show at least a sprout so there is something to wither.
    val stage = SessionGrowthMilestone.from(sessionProgress(state)).stage
        .let { if (it == GrowthStage.SEED) GrowthStage.SPROUT else it }
    FocusBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
            ) {
                PlantDisplay(
                    plant = state.plant,
                    stage = stage,
                    size = 140.dp,
                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = -10f
                            alpha = 0.75f
                        }
                        // Desaturate only the plant's own pixels by drawing it through a grayscale layer.
                        .drawWithContent {
                            val grayscale = Paint().apply {
                                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                            }
                            drawIntoCanvas { canvas ->
                                canvas.saveLayer(Rect(Offset.Zero, size), grayscale)
                                drawContent()
                                canvas.restore()
                            }
                        },
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.plant_died),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(if (gaveUp) R.string.plant_gave_up_desc else R.string.plant_died_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            if (canRevive) {
                Button(
                    onClick = onRevive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .height(52.dp),
                ) {
                    Text(stringResource(R.string.session_revive_now), style = MaterialTheme.typography.titleSmall)
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
                TextButton(onClick = onDismissMessage) {
                    Text(stringResource(R.string.action_ok), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.go_home), style = MaterialTheme.typography.titleSmall)
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
                style = MaterialTheme.typography.headlineMedium,
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
