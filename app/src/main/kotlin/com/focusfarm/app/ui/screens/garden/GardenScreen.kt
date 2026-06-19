package com.focusfarm.app.ui.screens.garden

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.R
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.domain.ShareGardenUseCase
import com.focusfarm.app.ui.components.FocusBackdrop
import com.focusfarm.app.ui.components.ShareGardenDialog
import com.focusfarm.app.ui.theme.TimerFontFamily
import com.focusfarm.app.ui.theme.plantAccentColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GardenScreen(
    onOpenCollection: () -> Unit = {},
    viewModel: GardenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val totalMinutes by viewModel.totalMinutes.collectAsStateWithLifecycle()
    val streakDays by viewModel.streakDays.collectAsStateWithLifecycle()
    val totalHours = totalMinutes / 60
    val remainingMins = totalMinutes % 60

    var showShareDialog by remember { mutableStateOf(false) }
    var shareBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val shareGardenUseCase = remember { ShareGardenUseCase() }

    if (showShareDialog && shareBitmap != null) {
        ShareGardenDialog(
            bitmap = shareBitmap!!,
            onShare = {
                shareGardenUseCase.shareImage(context, shareBitmap!!)
                showShareDialog = false
                shareBitmap = null
            },
            onDismiss = {
                showShareDialog = false
                shareBitmap = null
            },
        )
    }

    LaunchedEffect(showShareDialog, sessions, totalMinutes, streakDays) {
        if (showShareDialog && shareBitmap == null && sessions.isNotEmpty()) {
            shareBitmap = shareGardenUseCase.generateGardenBitmap(
                context = context,
                sessions = sessions,
                totalMinutes = totalMinutes,
                streak = streakDays,
            )
        }
    }

    FocusBackdrop {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 16.dp),
        ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.tab_garden), style = MaterialTheme.typography.displayMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onOpenCollection) {
                        Text(
                            text = stringResource(R.string.collection_book),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    TextButton(onClick = { showShareDialog = true }) {
                        Text(
                            text = stringResource(R.string.action_share),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(value = "${sessions.size}", label = stringResource(R.string.total_plants), modifier = Modifier.weight(1f))
                StatChip(
                    value = if (totalHours > 0) {
                        stringResource(R.string.total_focus_hours_minutes, totalHours, remainingMins)
                    } else {
                        stringResource(R.string.total_focus_minutes, totalMinutes)
                    },
                    label = stringResource(R.string.total_focus),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

            if (sessions.isEmpty()) {
                EmptyGarden()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = sessions,
                        key = { session -> session.id },
                    ) { session ->
                        PlantCard(session)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlantCard(session: FocusSession) {
    val plant = PlantCatalog.getById(session.plantId)
    val accent = plant?.let { plantAccentColor(it.id) } ?: MaterialTheme.colorScheme.primary
    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.5.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                // If it's a mature plant (completed session), show premium scaling
                val isMature = plant?.illustrationName?.isNotEmpty() == true
                Text(
                    text = session.plantEmoji,
                    fontSize = 30.sp,
                    modifier = if (isMature) Modifier.scale(1.2f) else Modifier
                )
            }
            Text(session.plantName, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.duration_minutes_short, session.durationMinutes),
                fontFamily = TimerFontFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(dateFormat.format(Date(session.completedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            Text(value, fontFamily = TimerFontFamily, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyGarden() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🌱", fontSize = 72.sp)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.empty_garden_title), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.empty_garden_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}


