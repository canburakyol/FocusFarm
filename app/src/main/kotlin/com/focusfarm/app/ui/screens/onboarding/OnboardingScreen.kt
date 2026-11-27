package com.focusfarm.app.ui.screens.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.R
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.ui.components.FocusBackdrop
import com.focusfarm.app.ui.components.PlantDisplay
import com.focusfarm.app.ui.components.SeedIcon

private const val STEP_COUNT = 4

/**
 * Three short story pages and a goal step. Notification permission is no longer asked here: the
 * home screen asks the first time a session starts, when the reason is obvious.
 */
@Composable
fun OnboardingScreen(
    onCompleteOnboarding: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.completed.collect { onCompleteOnboarding() }
    }

    FocusBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(88.dp)) {
                    if (state.step > 0) {
                        TextButton(onClick = viewModel::back) { Text(stringResource(R.string.cd_back)) }
                    }
                }
                Spacer(Modifier.weight(1f))
                StepDots(current = state.step)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(88.dp))
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                    label = "onboarding_step",
                ) { step ->
                    when (step) {
                        0 -> StoryPage(R.string.onb1_title, R.string.onb1_body) { GrowthIllustration() }
                        1 -> StoryPage(R.string.onb2_title, R.string.onb2_body) { GardenIllustration() }
                        2 -> StoryPage(R.string.onb3_title, R.string.onb3_body) { SeedIllustration() }
                        else -> GoalSetup(
                            state = state,
                            onDurationSelect = viewModel::selectDuration,
                            onDailyGoalSelect = viewModel::selectDailyGoal,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            val isLast = state.step >= STEP_COUNT - 1
            Button(
                onClick = { if (isLast) viewModel.completeOnboarding() else viewModel.nextStep() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = CircleShape,
            ) {
                Text(
                    text = stringResource(if (isLast) R.string.onb_start else R.string.onb_next),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (!isLast) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun StepDots(current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.semantics { contentDescription = "${current + 1} / $STEP_COUNT" },
    ) {
        repeat(STEP_COUNT) { index ->
            Box(
                Modifier
                    .size(width = if (index == current) 20.dp else 6.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == current) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun StoryPage(@StringRes title: Int, @StringRes body: Int, illustration: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        ) {
            illustration()
        }
        Spacer(Modifier.height(36.dp))
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GrowthIllustration() {
    val plant = PlantCatalog.FREE.first()
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        PlantDisplay(plant = plant, stage = GrowthStage.SPROUT, size = 64.dp)
        PlantDisplay(plant = plant, stage = GrowthStage.GROWING, size = 80.dp)
        PlantDisplay(plant = plant, stage = GrowthStage.FULL, size = 96.dp)
    }
}

@Composable
private fun GardenIllustration() {
    val plants = PlantCatalog.FREE.take(6)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        plants.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { PlantDisplay(plant = it, stage = GrowthStage.FULL, size = 64.dp) }
            }
        }
    }
}

@Composable
private fun SeedIllustration() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            SeedIcon(size = 28.dp)
            SeedIcon(size = 40.dp)
            SeedIcon(size = 28.dp)
        }
        Text(
            text = stringResource(R.string.seeds_plus, 10),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun GoalSetup(
    state: OnboardingUiState,
    onDurationSelect: (Int) -> Unit,
    onDailyGoalSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.onb_goal_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onb_goal_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        ChoiceCard(
            title = R.string.onb_default_duration,
            body = R.string.onb_default_duration_body,
            options = listOf(15, 25, 45, 60),
            selected = state.selectedDurationMinutes,
            onSelect = onDurationSelect,
        )
        Spacer(Modifier.height(16.dp))
        ChoiceCard(
            title = R.string.onb_daily_goal,
            body = R.string.onb_daily_goal_body,
            options = listOf(30, 45, 60, 90),
            selected = state.selectedDailyGoalMinutes,
            onSelect = onDailyGoalSelect,
        )
    }
}

@Composable
private fun ChoiceCard(
    @StringRes title: Int,
    @StringRes body: Int,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${stringResource(title)} (${stringResource(R.string.minutes_unit)})",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { minutes ->
                    FilterChip(
                        selected = selected == minutes,
                        onClick = { onSelect(minutes) },
                        // Number only: "45 min" wraps in a quarter-width chip; the unit is in the card text.
                        label = {
                            Text(
                                "$minutes",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
