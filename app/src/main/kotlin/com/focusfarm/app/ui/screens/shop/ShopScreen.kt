package com.focusfarm.app.ui.screens.shop

import android.app.Activity
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.R
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.ui.components.FocusBackdrop
import com.focusfarm.app.ui.components.PlantDisplay
import com.focusfarm.app.ui.components.SeedIcon
import kotlinx.coroutines.flow.collectLatest
import com.focusfarm.app.ui.components.plantName

/** Seed market: the one place seeds are spent, on plants that are locked by default. */
@Composable
fun ShopScreen(
    onBack: () -> Unit = {},
    viewModel: ShopViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is StoreEffect.ShowMessage ->
                    snackbarHostState.showSnackbar(context.getString(effect.messageRes, *effect.args))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        FocusBackdrop(modifier = Modifier.padding(padding)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ShopHeader(state = state, onBack = onBack, onIntent = viewModel::onIntent)
                }
                items(state.items, key = { it.product.id }) { item ->
                    PlantOffer(item = item, onBuy = { viewModel.onIntent(StoreIntent.PurchaseProduct(item.product.id)) })
                }
            }
        }
    }
}

@Composable
private fun ShopHeader(state: StoreUiState, onBack: () -> Unit, onIntent: (StoreIntent) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Text(
                text = stringResource(R.string.shop_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SeedIcon(size = 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("${state.seedBalance}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            text = stringResource(if (state.isPremium) R.string.shop_premium_note else R.string.shop_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp),
        )
        if (!state.isPremium) {
            val activity = LocalContext.current as? Activity
            Spacer(Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.SmartDisplay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.shop_free_pack),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            stringResource(R.string.shop_free_pack_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                    FilledTonalButton(onClick = { onIntent(StoreIntent.ClaimRewardedAdSeedPack(activity)) }) {
                        Text("+25")
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PlantOffer(item: ShopItem, onBuy: () -> Unit) {
    val plant = PlantCatalog.getById(item.product.id)
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                if (plant != null) {
                    PlantDisplay(plant = plant, stage = GrowthStage.FULL, size = 96.dp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = plantName(item.product.id, plant?.name ?: item.product.displayName),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            if (item.isUnlocked) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(40.dp)) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.shop_owned), color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Button(
                    onClick = onBuy,
                    enabled = !item.isPurchasing,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    SeedIcon(size = 10.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("${item.product.price}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
