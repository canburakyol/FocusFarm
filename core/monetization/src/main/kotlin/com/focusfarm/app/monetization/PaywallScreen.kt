package com.focusfarm.app.monetization

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.data.billing.PremiumOffer
import com.focusfarm.app.data.billing.PremiumPlan
import com.focusfarm.core.ui.R

@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    // Automatically close or show success state when premium unlocks
    LaunchedEffect(state.isPremiumUnlocked) {
        if (state.isPremiumUnlocked) {
            // Give user some visual confirmation or dismiss
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070F0B),
                        Color(0xFF0F1E16),
                        Color(0xFF070F0B)
                    )
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))

            Text(
                text = "FOCUSFARM PREMIUM",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFD4AF37),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Çiftliğini Geliştir, Odağını Zirveye Taşı",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFFF5EFEB),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Elegant glassmorphic feature container
            PremiumFeaturesContainer()

            Spacer(Modifier.height(32.dp))

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .height(180.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFD4AF37),
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                PlanSelectorList(
                    offers = state.offers,
                    selectedPlanId = state.selectedPlanId,
                    onSelectPlan = viewModel::selectPlan
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        val planId = state.selectedPlanId
                        if (planId != null && activity != null) {
                            viewModel.purchasePlan(activity, planId)
                        }
                    },
                    enabled = state.selectedPlanId != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4AF37),
                        contentColor = Color(0xFF0F1E16),
                        disabledContainerColor = Color(0xFFD4AF37).copy(alpha = 0.5f),
                        disabledContentColor = Color(0xFF0F1E16).copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "Premium'a Yükselt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                TextButton(onClick = viewModel::restorePurchases) {
                    Text(
                        text = stringResource(R.string.paywall_restore),
                        color = Color(0xFFD8C9AF),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            state.errorMessage?.let { error ->
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2C1916).copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFD56241).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF5B5A5),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::dismissError) {
                            Text("Kapat", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Abonelikler Google Play hesabınız üzerinden faturalandırılır ve iptal edilmediği sürece otomatik yenilenir. İstediğiniz zaman Google Play Ayarlarından iptal edebilirsiniz.",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFD8C9AF).copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }

        // Beautiful custom close button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF1F2F26).copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                color = Color(0xFFD8C9AF),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // Premium success overlay animation
        AnimatedVisibility(
            visible = state.isPremiumUnlocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070F0B)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "🎉",
                        fontSize = 72.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Tebrikler!",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color(0xFFD4AF37),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "FocusFarm Premium özellikleri başarıyla aktif edildi. Artık tüm premium bitkilere, temalara ve gelişmiş analizlere sınırsız erişebilirsiniz.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFF6EFDD),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37), contentColor = Color(0xFF0F1E16))
                    ) {
                        Text("Bahçeme Geri Dön", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumFeaturesContainer() {
    val features = listOf(
        "🚫  Reklamsız Deneyim (Kesintisiz odaklanma)",
        "🌿  Özel Premium Bitki Türleri",
        "🎨  Premium Bahçe Temaları ve Renkleri",
        "🔊  Gelişmiş Doğal Ambiyans Sesleri",
        "📊  Detaylı İstatistikler ve Veri Analizi",
        "⚡  Özel Hızlı Odak Presetleri",
        "🍂  Sezonluk Tematik İçerikler",
        "🔒  Strict Focus (Gelişmiş Odak Korumaları)"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF13241C).copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF233D30).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            features.forEach { feature ->
                Text(
                    text = feature,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE4EFDF),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PlanSelectorList(
    offers: List<PremiumOffer>,
    selectedPlanId: String?,
    onSelectPlan: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        offers.forEach { offer ->
            val isSelected = offer.plan.productId == selectedPlanId
            val isYearly = offer.plan == PremiumPlan.YEARLY

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF1E3A2F) else Color(0xFF13241C)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color(0xFFD4AF37) else Color(0xFF233D30)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPlan(offer.plan.productId) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = when (offer.plan) {
                                    PremiumPlan.MONTHLY -> "Aylık Paket"
                                    PremiumPlan.YEARLY -> "Yıllık Paket"
                                    PremiumPlan.LIFETIME -> "Ömür Boyu"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFF6EFDD),
                                fontWeight = FontWeight.Bold
                            )

                            if (isYearly) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFFD4AF37))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "EN POPÜLER",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF0F1E16),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = offer.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD8C9AF),
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Text(
                        text = offer.price,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFD4AF37),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
