package com.focusfarm.app.ui.screens.referral

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.data.repository.ReferralManager
import com.focusfarm.core.ui.R

@Composable
fun InviteFriendsScreen(
    referralManager: ReferralManager,
    onBack: () -> Unit,
) {
    val state by referralManager.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1712)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.referral_title),
                style = MaterialTheme.typography.displayMedium,
                color = Color(0xFFE4B156),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.referral_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFD8C9AF),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2A22)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.referral_your_code),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD8C9AF),
                    )

                    Spacer(Modifier.height(8.dp))

                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFFE4B156),
                            modifier = Modifier.size(32.dp),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2D4338))
                                .border(1.dp, Color(0xFFE4B156), RoundedCornerShape(12.dp))
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = state.referralCode.ifEmpty { stringResource(R.string.referral_loading) },
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color(0xFFE4B156),
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.referral_friends_joined, state.referredCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF71C37C),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2A22)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.referral_how_it_works),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFE4B156),
                        fontWeight = FontWeight.SemiBold,
                    )

                    ReferralStep(
                        number = "1",
                        title = stringResource(R.string.referral_step_1_title),
                        description = stringResource(R.string.referral_step_1_desc),
                    )

                    ReferralStep(
                        number = "2",
                        title = stringResource(R.string.referral_step_2_title),
                        description = stringResource(R.string.referral_step_2_desc),
                    )

                    ReferralStep(
                        number = "3",
                        title = stringResource(R.string.referral_step_3_title),
                        description = stringResource(R.string.referral_step_3_desc),
                    )
                }
            }

            state.errorMessage?.let { error ->
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFD56241).copy(alpha = 0.2f)
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
                            color = Color(0xFFD56241),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = referralManager::dismissError) {
                            Text(stringResource(R.string.action_ok), color = Color(0xFFE4B156))
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = referralManager::shareReferralLink,
                enabled = state.referralCode.isNotEmpty() && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE4B156),
                    contentColor = Color(0xFF0F1712),
                ),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFF0F1712),
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.referral_share_button),
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 18.sp,
                    )
                }
            }
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.action_close),
                color = Color(0xFFD8C9AF),
            )
        }
    }
}

@Composable
private fun ReferralStep(
    number: String,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFE4B156)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                color = Color(0xFF0F1712),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFF6EFDD),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD8C9AF),
            )
        }
    }
}
