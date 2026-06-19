package com.focusfarm.app.monetization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.focusfarm.core.ui.R

@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFF0F1712)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.paywall_title),
                style = MaterialTheme.typography.displayMedium,
                color = androidx.compose.ui.graphics.Color(0xFFE4B156),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.paywall_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color(0xFFD8C9AF),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            PremiumFeaturesList()

            Spacer(Modifier.height(24.dp))

            if (state.isLoading) {
                CircularProgressIndicator(
                    color = androidx.compose.ui.graphics.Color(0xFFE4B156),
                    modifier = Modifier.size(48.dp),
                )
            } else {
                PlanSelector(
                    plans = state.availablePlans,
                    selectedPlanId = state.selectedPlanId,
                    onSelectPlan = viewModel::selectPlan,
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        state.selectedPlanId?.let { viewModel.purchasePlan(it) }
                    },
                    enabled = state.selectedPlanId != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFE4B156),
                        contentColor = androidx.compose.ui.graphics.Color(0xFF0F1712),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.paywall_trial_button),
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 18.sp,
                    )
                }

                Spacer(Modifier.height(12.dp))

                TextButton(onClick = viewModel::restorePurchases) {
                    Text(
                        text = stringResource(R.string.paywall_restore),
                        color = androidx.compose.ui.graphics.Color(0xFFD8C9AF),
                    )
                }
            }

            state.errorMessage?.let { error ->
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF2D4338).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color(0xFFD56241),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::dismissError) {
                            Text("Tamam", color = androidx.compose.ui.graphics.Color(0xFFE4B156))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.paywall_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = androidx.compose.ui.graphics.Color(0xFFD8C9AF).copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.paywall_close),
                color = androidx.compose.ui.graphics.Color(0xFFD8C9AF),
            )
        }
    }
}

@Composable
private fun PremiumFeaturesList() {
    val features = listOf(
        stringResource(R.string.paywall_feature_1),
        stringResource(R.string.paywall_feature_2),
        stringResource(R.string.paywall_feature_3),
        stringResource(R.string.paywall_feature_4),
        stringResource(R.string.paywall_feature_5),
        stringResource(R.string.paywall_feature_6),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        features.forEach { feature ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "✓",
                    color = androidx.compose.ui.graphics.Color(0xFF71C37C),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = feature,
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color(0xFFF6EFDD),
                )
            }
        }
    }
}

@Composable
private fun PlanSelector(
    plans: List<PaywallPlan>,
    selectedPlanId: String?,
    onSelectPlan: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        plans.forEach { plan ->
            val isSelected = plan.id == selectedPlanId
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        androidx.compose.ui.graphics.Color(0xFF2D4338)
                    else
                        androidx.compose.ui.graphics.Color(0xFF1C2A22)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 2.dp,
                    color = if (isSelected)
                        androidx.compose.ui.graphics.Color(0xFFE4B156)
                    else
                        androidx.compose.ui.graphics.Color(0xFF2D4338),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPlan(plan.id) },
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = plan.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = androidx.compose.ui.graphics.Color(0xFFF6EFDD),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (plan.isRecommended) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(androidx.compose.ui.graphics.Color(0xFFE4B156))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.paywall_recommended),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = androidx.compose.ui.graphics.Color(0xFF0F1712),
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = plan.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = androidx.compose.ui.graphics.Color(0xFFD8C9AF),
                            )
                        }
                        Text(
                            text = plan.price,
                            style = MaterialTheme.typography.titleMedium,
                            color = androidx.compose.ui.graphics.Color(0xFFE4B156),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    plan.savings?.let { savings ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = savings,
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color(0xFF71C37C),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
