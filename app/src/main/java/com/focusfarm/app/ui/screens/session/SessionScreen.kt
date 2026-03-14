package com.focusfarm.app.ui.screens.session

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.R
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.ui.components.FocusBackdrop
import com.focusfarm.app.ui.components.PlantDisplay
import com.focusfarm.app.ui.components.TimerDisplay
import com.focusfarm.app.ui.theme.Amber
import com.focusfarm.app.ui.theme.CreamDim
import com.focusfarm.app.ui.theme.Danger
import com.focusfarm.app.ui.theme.ForestDark
import com.focusfarm.app.ui.theme.ForestMid

@Composable
fun SessionScreen(
    plantId: String,
    targetMinutes: Int,
    onBack: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val canRevive by viewModel.canRevive.collectAsStateWithLifecycle()
    val isPremiumUnlocked by viewModel.isPremiumUnlocked.collectAsStateWithLifecycle()
    var showAbandonDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(viewModel)
        onDispose { lifecycleOwner.lifecycle.removeObserver(viewModel) }
    }

    LaunchedEffect(Unit) {
        if (state.sessionState == SessionState.IDLE) {
            val plant = PlantCatalog.getById(plantId) ?: PlantCatalog.FREE.first()
            viewModel.startSession(plant, targetMinutes)
        }
    }

    when (state.sessionState) {
        SessionState.KILLED -> KilledScreen(
            canRevive = canRevive && (isPremiumUnlocked || activity != null),
            isPremiumUnlocked = isPremiumUnlocked,
            statusMessage = statusMessage,
            onDismissMessage = viewModel::dismissStatusMessage,
            onRevive = {
                if (isPremiumUnlocked) {
                    viewModel.reviveWithoutAd()
                } else if (activity != null) {
                    viewModel.showReviveAd(activity)
                }
            },
            onBack = onBack,
        )

        SessionState.COMPLETED -> CompletedScreen(
            state = state,
            onSaveReflection = viewModel::saveReflection,
            onBack = onBack,
        )

        else -> RunningScreen(
            state = state,
            onAbandon = { showAbandonDialog = true },
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
                    onBack()
                }) {
                    Text(stringResource(R.string.abandon_confirm), color = Danger)
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
private fun RunningScreen(state: SessionUiState, onAbandon: () -> Unit) {
    FocusBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.focus_session),
                style = MaterialTheme.typography.labelSmall,
                color = CreamDim,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(state.plant.name, style = MaterialTheme.typography.headlineLarge)

            Spacer(Modifier.weight(1f))
            PlantDisplay(
                plant = state.plant,
                stage = state.growthStage,
                size = 200.dp,
                breathing = true,
            )
            Spacer(Modifier.weight(1f))

            TimerDisplay(
                remainingSeconds = state.remainingSeconds,
                totalSeconds = state.targetMinutes * 60,
            )
            Spacer(Modifier.height(32.dp))

            Text(
                stringResource(R.string.focus_tip),
                style = MaterialTheme.typography.bodySmall,
                color = CreamDim,
            )
            Spacer(Modifier.height(24.dp))

            TextButton(
                onClick = onAbandon,
                modifier = Modifier
                    .border(1.dp, Danger.copy(alpha = 0.5f), RoundedCornerShape(50)),
            ) {
                Text(
                    stringResource(R.string.give_up),
                    color = Danger,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun KilledScreen(
    canRevive: Boolean,
    isPremiumUnlocked: Boolean,
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
                color = CreamDim,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            if (canRevive) {
                Button(
                    onClick = onRevive,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = ForestDark),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (isPremiumUnlocked) {
                            stringResource(R.string.session_revive_now)
                        } else {
                            stringResource(R.string.session_revive_with_ad)
                        },
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = CreamDim,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDismissMessage) {
                    Text(stringResource(R.string.action_ok), color = CreamDim)
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onBack,
                modifier = Modifier.border(1.dp, CreamDim, RoundedCornerShape(50)),
            ) {
                Text(
                    stringResource(R.string.go_home),
                    color = CreamDim,
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
                color = CreamDim,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (state.rewardSeedsGranted > 0) {
                    if (state.rewardBoosted) {
                        stringResource(
                            R.string.session_reward_seeds_boosted,
                            state.rewardSeedsGranted,
                            state.rewardSeedsGranted / 2,
                        )
                    } else {
                        stringResource(R.string.session_reward_seeds, state.rewardSeedsGranted)
                    }
                } else {
                    stringResource(R.string.session_reward_loading)
                },
                style = MaterialTheme.typography.bodySmall,
                color = Amber,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))

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
                colors = ButtonDefaults.buttonColors(containerColor = ForestMid, contentColor = CreamDim),
            ) {
                Text(stringResource(R.string.session_reflection_save))
            }

            if (state.reflectionSaved) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.session_reflection_saved),
                    style = MaterialTheme.typography.bodySmall,
                    color = Amber,
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onBack,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = ForestDark),
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
