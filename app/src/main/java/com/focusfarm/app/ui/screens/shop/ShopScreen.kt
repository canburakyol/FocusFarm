package com.focusfarm.app.ui.screens.shop

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.R
import com.focusfarm.app.data.billing.PremiumOffer
import com.focusfarm.app.data.billing.PremiumPlan
import com.focusfarm.app.domain.Plant
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.ui.components.FocusBackdrop
import com.focusfarm.app.ui.theme.Amber
import com.focusfarm.app.ui.theme.CreamDim
import com.focusfarm.app.ui.theme.ForestDark
import com.focusfarm.app.ui.theme.ForestMid
import com.focusfarm.app.ui.theme.Success
import com.focusfarm.app.ui.theme.TimerFontFamily
import com.focusfarm.app.ui.theme.plantAccentColor

@Composable
fun ShopScreen(viewModel: ShopViewModel = hiltViewModel()) {
    val billingState by viewModel.billingState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    FocusBackdrop {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(stringResource(R.string.tab_shop), style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.shop_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = CreamDim,
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Amber.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.premium_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = ForestDark,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.premium_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestDark.copy(alpha = 0.8f),
                    )
                }
                Text(
                    text = if (billingState.isPremiumUnlocked) {
                        stringResource(R.string.shop_current_plan)
                    } else {
                        billingState.offers.minByOrNull { it.plan.ordinal }?.price ?: "-"
                    },
                    fontFamily = TimerFontFamily,
                    fontSize = 16.sp,
                    color = ForestDark,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (billingState.isPremiumUnlocked) {
            Text(
                text = stringResource(R.string.shop_premium_active),
                style = MaterialTheme.typography.bodySmall,
                color = Success,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = stringResource(R.string.shop_plans_title),
                style = MaterialTheme.typography.labelSmall,
                color = CreamDim,
            )
        }

        when {
            billingState.offers.isNotEmpty() -> {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    billingState.offers.forEach { offer ->
                        OfferCard(
                            offer = offer,
                            isPremiumUnlocked = billingState.isPremiumUnlocked,
                            onBuy = {
                                if (activity != null) {
                                    viewModel.buy(activity, offer.plan)
                                }
                            },
                        )
                    }
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text(stringResource(R.string.shop_restore), color = CreamDim)
                    }
                }
            }

            billingState.isLoading -> {
                Text(
                    text = stringResource(R.string.shop_loading_products),
                    style = MaterialTheme.typography.bodySmall,
                    color = CreamDim,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }

            else -> {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Text(
                        text = stringResource(R.string.shop_billing_disconnected),
                        style = MaterialTheme.typography.bodySmall,
                        color = CreamDim,
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text(stringResource(R.string.shop_retry), color = Amber)
                    }
                }
            }
        }

        billingState.message?.let { message ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestMid),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = CreamDim,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.clearMessage() }) {
                        Text(stringResource(R.string.action_ok), color = Amber)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(PlantCatalog.ALL, key = { it.id }) { plant ->
                    ShopPlantCard(
                        plant = plant,
                        isPremiumUnlocked = billingState.isPremiumUnlocked,
                    )
                }
            }
        }
    }
}

@Composable
private fun OfferCard(
    offer: PremiumOffer,
    isPremiumUnlocked: Boolean,
    onBuy: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForestMid),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = offer.plan.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = offer.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = CreamDim,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = offer.price,
                    style = MaterialTheme.typography.titleMedium,
                    color = Amber,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onBuy,
                    enabled = !isPremiumUnlocked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber,
                        contentColor = ForestDark,
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (isPremiumUnlocked) {
                            stringResource(R.string.shop_current_plan)
                        } else {
                            stringResource(R.string.shop_buy_now)
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopPlantCard(
    plant: Plant,
    isPremiumUnlocked: Boolean,
) {
    val accentColor = plantAccentColor(plant.id)
    Card(
        colors = CardDefaults.cardColors(containerColor = ForestMid),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(plant.emoji, fontSize = 36.sp)
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    plant.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    plant.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = CreamDim,
                )
                Text(
                    stringResource(R.string.min_session, plant.minMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = Amber,
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            plant.isPremium && isPremiumUnlocked -> Success.copy(alpha = 0.15f)
                            plant.isPremium -> accentColor.copy(alpha = 0.15f)
                            else -> Success.copy(alpha = 0.15f)
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = when {
                        plant.isPremium && isPremiumUnlocked -> stringResource(R.string.unlocked_badge)
                        plant.isPremium -> stringResource(R.string.premium_badge)
                        else -> stringResource(R.string.free_badge)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        plant.isPremium && isPremiumUnlocked -> Success
                        plant.isPremium -> accentColor
                        else -> Success
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun PremiumPlan.displayName(): String = when (this) {
    PremiumPlan.MONTHLY -> stringResource(R.string.plan_monthly)
    PremiumPlan.YEARLY -> stringResource(R.string.plan_yearly)
    PremiumPlan.LIFETIME -> stringResource(R.string.plan_lifetime)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

