package com.focusfarm.app.ui.screens.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.R
import com.focusfarm.app.domain.PlantRarity
import com.focusfarm.app.ui.components.FocusBackdrop
import com.focusfarm.app.ui.components.PlantDisplay
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.app.domain.PlantCatalog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.focusfarm.app.ui.components.SeedIcon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.Icons
import com.focusfarm.app.ui.components.plantName

@Composable
fun CollectionScreen(
    onBack: (() -> Unit)? = null,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val plants by viewModel.collectionPlants.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val selectedRarity by viewModel.selectedRarity.collectAsStateWithLifecycle()

    FocusBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.collection_book), style = MaterialTheme.typography.headlineMedium)
                if (onBack != null) {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.go_back), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.collection_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            statusMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::dismissMessage) {
                            Text(stringResource(R.string.action_ok), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Rarity filters row
            val rarityOptions = listOf(
                null to stringResource(R.string.rarity_all),
                PlantRarity.COMMON to stringResource(R.string.rarity_common),
                PlantRarity.UNCOMMON to stringResource(R.string.rarity_uncommon),
                PlantRarity.RARE to stringResource(R.string.rarity_rare),
                PlantRarity.EPIC to stringResource(R.string.rarity_epic),
                PlantRarity.LEGENDARY to stringResource(R.string.rarity_legendary)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(rarityOptions) { (rarity, title) ->
                    val isSelected = selectedRarity == rarity
                    val chipColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    }
                    val textColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(chipColor)
                            .clickable { viewModel.filterRarity(rarity) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (plants.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.collection_empty_rarity),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(plants, key = { it.plant.id }) { item ->
                        CollectionPlantCard(
                            item = item,
                            onClaimReward = { viewModel.claimReward(item.plant.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionPlantCard(
    item: CollectionPlantUiState,
    onClaimReward: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val plantObj = remember(item.plant.id) { PlantCatalog.getById(item.plant.id) }

    val statusText = when {
        !item.isUnlocked -> stringResource(R.string.collection_locked)
        item.isDiscovered -> stringResource(R.string.collection_grown, item.grownCount)
        else -> stringResource(R.string.collection_undiscovered)
    }

    val statusColor = when {
        !item.isUnlocked -> MaterialTheme.colorScheme.onSurfaceVariant
        item.isDiscovered -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.isDiscovered) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Rarity tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = stringResource(item.plant.rarity.labelRes()),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Plant graphics or Lock
            Box(
                modifier = Modifier
                    .height(84.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (!item.isUnlocked) {
                    Icon(Icons.Rounded.Lock, contentDescription = stringResource(R.string.collection_locked), modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (plantObj != null) {
                    PlantDisplay(
                        plant = plantObj,
                        stage = if (item.isDiscovered) GrowthStage.FULL else GrowthStage.SEED,
                        size = 76.dp
                    )
                } else {
                    SeedIcon(size = 20.dp)
                }
            }

            // Name
            Text(
                text = plantName(item.plant.id, item.plant.displayName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            // Status label
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            // Unlock conditions or discovery dates
            if (!item.isUnlocked) {
                val condition = when {
                    item.plant.isPremium -> stringResource(R.string.collection_requires_premium)
                    item.plant.unlockType == "shop" -> stringResource(R.string.collection_shop_locked)
                    else -> stringResource(R.string.collection_unlock_minutes, item.plant.minimumFocusMinutes)
                }
                Text(
                    text = condition,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.heightIn(min = 28.dp)
                )
            } else if (item.isDiscovered && item.firstDiscoveredAt != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.heightIn(min = 28.dp)
                ) {
                    Text(
                        text = stringResource(R.string.collection_first, dateFormat.format(Date(item.firstDiscoveredAt))),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                    item.lastCompletedAt?.let { last ->
                        Text(
                            text = stringResource(R.string.collection_last, dateFormat.format(Date(last))),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.collection_unlocked_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.heightIn(min = 28.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            // Seed Claim/Reward section
            Text(
                text = stringResource(R.string.collection_gift, item.rewardSeeds),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            when {
                item.canClaimReward -> {
                    Button(
                        onClick = onClaimReward,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.home_mission_claim), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                item.isRewardClaimed -> {
                    Text(
                        text = stringResource(R.string.missions_claimed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                else -> {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun PlantRarity.labelRes(): Int = when (this) {
    PlantRarity.COMMON -> R.string.rarity_common
    PlantRarity.UNCOMMON -> R.string.rarity_uncommon
    PlantRarity.RARE -> R.string.rarity_rare
    PlantRarity.EPIC -> R.string.rarity_epic
    PlantRarity.LEGENDARY -> R.string.rarity_legendary
}
