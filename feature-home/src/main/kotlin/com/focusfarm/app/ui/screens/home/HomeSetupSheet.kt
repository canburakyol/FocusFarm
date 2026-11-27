package com.focusfarm.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focusfarm.app.domain.AmbienceSound
import com.focusfarm.app.domain.FocusSessionType
import com.focusfarm.app.domain.FocusTag
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.app.ui.components.PlantDisplay
import com.focusfarm.app.ui.components.labelRes
import com.focusfarm.core.ui.R
import com.focusfarm.app.ui.components.plantName

/**
 * Odak kurulum detayları (bitki, mod, süre, etiket, strict, ambiyans).
 * Ana ekranda kalabalığı azaltmak için BottomSheet içinde gösterilir.
 * State/ Intent aynen HomeViewModel'den gelir, yeni logic içermez.
 */
@Composable
fun HomeSetupSheet(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.setup_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        // Central Plant display
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .align(Alignment.CenterHorizontally),
        ) {
            val selectedPlant =
                com.focusfarm.app.domain.PlantCatalog.getById(state.selectedPlantId)
                    ?: com.focusfarm.app.domain.PlantCatalog.FREE.first()
            PlantDisplay(
                plant = selectedPlant,
                stage = GrowthStage.GROWING,
                breathing = true,
                size = 112.dp,
            )
        }

        // Plant Selector (vector, no emoji)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_plant),
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
                    val selectLabel = stringResource(R.string.setup_select_plant, plantName(plant.id, plant.name))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape,
                            )
                            .clickable { onIntent(HomeIntent.SelectPlant(plant.id)) }
                            .semantics {
                                contentDescription = selectLabel
                            },
                    ) {
                        PlantDisplay(
                            plant = plant,
                            stage = GrowthStage.GROWING,
                            breathing = false,
                            size = 48.dp,
                        )
                    }
                }
            }
        }

        // Session Type Selector
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_mode),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(FocusSessionType.entries) { type ->
                    val isSelected = state.sessionType == type
                    val label = stringResource(type.labelRes())
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
                    )
                }
            }
        }

        // Duration Selector
        if (state.sessionType != FocusSessionType.STOPWATCH) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.setup_duration, state.selectedMinutes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = state.selectedMinutes.toFloat(),
                    onValueChange = { onIntent(HomeIntent.SelectDuration(it.toInt())) },
                    valueRange = 5f..180f,
                    steps = 35,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                // Quick presets live on the home screen; the sheet keeps the fine-grained slider.
            }
        }

        // Strict Mode Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_strict_mode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.setup_strict_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.isStrict,
                    onCheckedChange = { onIntent(HomeIntent.SetStrictFocus(it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }

        // Ambience Sound Selector
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_ambience),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AmbienceSound.entries.forEach { sound ->
                    val selected = state.ambienceSound == sound
                    FilterChip(
                        selected = selected,
                        onClick = { onIntent(HomeIntent.SelectAmbience(sound)) },
                        label = { Text(stringResource(sound.labelRes())) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                        shape = RoundedCornerShape(18.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
