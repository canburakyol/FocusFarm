package com.focusfarm.app.ui.screens.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.R
import com.focusfarm.app.ui.components.FocusBackdrop
import com.focusfarm.app.ui.theme.Amber
import com.focusfarm.app.ui.theme.CreamDim
import com.focusfarm.app.ui.theme.ForestDark
import com.focusfarm.app.ui.theme.ForestMid
import com.focusfarm.app.ui.theme.Success
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CollectionScreen(
    onBack: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val plants by viewModel.collectionPlants.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

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
            Text(stringResource(R.string.collection_book), style = MaterialTheme.typography.displayMedium)
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.go_back), color = CreamDim)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.collection_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = CreamDim,
        )
        statusMessage?.let { message ->
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestMid),
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
                        color = CreamDim,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::dismissMessage) {
                        Text(stringResource(R.string.action_ok), color = Amber)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

@Composable
private fun CollectionPlantCard(
    item: CollectionPlantUiState,
    onClaimReward: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.forLanguageTag("tr")) }
    val statusText = when {
        !item.isPremiumAccessUnlocked -> stringResource(R.string.collection_premium_locked)
        item.isDiscovered -> stringResource(R.string.collection_discovered_count, item.discoveredCount)
        else -> stringResource(R.string.collection_not_discovered)
    }
    val statusColor = when {
        !item.isPremiumAccessUnlocked -> CreamDim
        item.isDiscovered -> Success
        else -> CreamDim
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = ForestMid),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (!item.isPremiumAccessUnlocked) "🔒" else item.plant.emoji,
                fontSize = 34.sp,
            )
            Text(
                text = item.plant.name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
                textAlign = TextAlign.Center,
            )

            if (item.isDiscovered && item.lastCompletedAt != null) {
                Text(
                    text = stringResource(
                        R.string.collection_last_grown,
                        dateFormat.format(Date(item.lastCompletedAt)),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Amber,
                    fontWeight = FontWeight.Medium,
                )
            } else {
                Text(
                    text = item.plant.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = CreamDim,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }

            Text(
                text = stringResource(R.string.collection_reward_seeds, item.rewardSeeds),
                style = MaterialTheme.typography.labelSmall,
                color = Amber,
            )

            when {
                item.canClaimReward -> {
                    Button(
                        onClick = onClaimReward,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Amber,
                            contentColor = ForestDark,
                        ),
                    ) {
                        Text(stringResource(R.string.collection_claim_reward))
                    }
                }

                item.isRewardClaimed -> {
                    Text(
                        text = stringResource(R.string.collection_reward_claimed),
                        style = MaterialTheme.typography.labelSmall,
                        color = Success,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}


