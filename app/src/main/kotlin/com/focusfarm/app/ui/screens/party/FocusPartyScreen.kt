package com.focusfarm.app.ui.screens.party

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
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
import com.focusfarm.app.party.FocusPartyManager
import com.focusfarm.app.party.PartyMember
import com.focusfarm.core.ui.R

@Composable
fun FocusPartyScreen(
    partyManager: FocusPartyManager,
    onStartSession: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val state by partyManager.state.collectAsStateWithLifecycle()

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
                text = stringResource(R.string.party_title),
                style = MaterialTheme.typography.displayMedium,
                color = Color(0xFFE4B156),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.party_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFD8C9AF),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            if (state.partyId == null) {
                PartyCreationCard(
                    onCreateParty = { partyManager.createParty("user123", "Sen") },
                    onJoinParty = { /* Join flow */ },
                )
            } else {
                PartyLobbyCard(
                    state = state,
                    onStartSession = onStartSession,
                    onLeaveParty = { partyManager.leaveParty() },
                )
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
private fun PartyCreationCard(
    onCreateParty: () -> Unit,
    onJoinParty: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2A22)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.party_card_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFE4B156),
                fontWeight = FontWeight.SemiBold,
            )

            Button(
                onClick = onCreateParty,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE4B156),
                    contentColor = Color(0xFF0F1712),
                ),
            ) {
                Text(stringResource(R.string.party_create_button))
            }

            Button(
                onClick = onJoinParty,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2D4338),
                    contentColor = Color(0xFFF6EFDD),
                ),
            ) {
                Text(stringResource(R.string.party_join_button))
            }
        }
    }
}

@Composable
private fun PartyLobbyCard(
    state: com.focusfarm.app.party.PartyState,
    onStartSession: (Int) -> Unit,
    onLeaveParty: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2A22)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.party_code_label, state.partyId ?: ""),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFE4B156),
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onLeaveParty) {
                    Text(stringResource(R.string.party_leave_button), color = Color(0xFFD56241))
                }
            }

            Text(
                text = stringResource(R.string.party_members_count, state.members.size, state.maxMembers),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD8C9AF),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.members.size) { index ->
                    PartyMemberCard(state.members[index])
                }
            }

            Button(
                onClick = { onStartSession(state.sessionDurationMinutes) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE4B156),
                    contentColor = Color(0xFF0F1712),
                ),
            ) {
                Text(stringResource(R.string.party_start_session_button, state.sessionDurationMinutes))
            }
        }
    }
}

@Composable
private fun PartyMemberCard(member: PartyMember) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (member.isOnline) Color(0xFF2D4338)
                    else Color(0xFF1C2A22)
                )
                .border(
                    2.dp,
                    if (member.isOnline) Color(0xFF71C37C)
                    else Color(0xFF6E5442),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = member.plantEmoji,
                fontSize = 24.sp,
            )
        }
        Text(
            text = member.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFD8C9AF),
            maxLines = 1,
        )
    }
}
