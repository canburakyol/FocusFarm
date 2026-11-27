package com.focusfarm.app.ui.screens.garden

import android.graphics.Bitmap
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.R
import com.focusfarm.app.domain.GardenEntry
import com.focusfarm.app.domain.GardenFilter
import com.focusfarm.app.domain.GardenPlantState
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.domain.PlantSpeciesCatalog
import com.focusfarm.app.domain.ShareGardenUseCase
import com.focusfarm.app.ui.components.FocusBackdrop
import com.focusfarm.app.ui.components.PlantDisplay
import com.focusfarm.app.ui.components.PlantVectorRenderer
import com.focusfarm.app.ui.components.ShareGardenDialog
import java.util.Date

/**
 * The garden is a field the user fills over time: plants stand on soft plots, withered ones fade,
 * and details open on tap instead of crowding every tile.
 */
@Composable
fun GardenScreen(
    onOpenCollection: () -> Unit = {},
    onStartFocus: () -> Unit = {},
    viewModel: GardenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val gardenEntries by viewModel.gardenEntries.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val hideWithered by viewModel.hideWithered.collectAsStateWithLifecycle()
    val totalMinutes by viewModel.totalMinutes.collectAsStateWithLifecycle()
    val streakDays by viewModel.streakDays.collectAsStateWithLifecycle()

    var showShareDialog by remember { mutableStateOf(false) }
    var shareBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedEntry by remember { mutableStateOf<GardenEntry?>(null) }
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
        Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            GardenHeader(
                grownCount = gardenEntries.count { it.state == GardenPlantState.BLOOMED },
                totalMinutes = totalMinutes,
                canShare = sessions.isNotEmpty(),
                onOpenCollection = onOpenCollection,
                onShare = { showShareDialog = true },
            )
            Spacer(Modifier.height(16.dp))
            GardenFilters(
                activeFilter = activeFilter,
                hideWithered = hideWithered,
                onFilter = viewModel::setFilter,
                onToggleWithered = { viewModel.setHideWithered(!hideWithered) },
            )
            Spacer(Modifier.height(12.dp))

            if (gardenEntries.isEmpty()) {
                EmptyGarden(onStartFocus = onStartFocus)
            } else {
                GardenField(entries = gardenEntries, onSelect = { selectedEntry = it })
            }
        }
    }

    selectedEntry?.let { entry ->
        PlantDetailSheet(entry = entry, onDismiss = { selectedEntry = null })
    }
}

@Composable
private fun GardenHeader(
    grownCount: Int,
    totalMinutes: Int,
    canShare: Boolean,
    onOpenCollection: () -> Unit,
    onShare: () -> Unit,
) {
    val focusText = if (totalMinutes >= 60) {
        stringResource(R.string.total_focus_hours_minutes, totalMinutes / 60, totalMinutes % 60)
    } else {
        stringResource(R.string.total_focus_minutes, totalMinutes)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.tab_garden), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "${pluralStringResource(R.plurals.garden_plants, grownCount, grownCount)} · $focusText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenCollection) {
            Icon(Icons.Rounded.AutoStories, contentDescription = stringResource(R.string.collection_book))
        }
        IconButton(onClick = onShare, enabled = canShare) {
            Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.action_share))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GardenFilters(
    activeFilter: GardenFilter,
    hideWithered: Boolean,
    onFilter: (GardenFilter) -> Unit,
    onToggleWithered: () -> Unit,
) {
    val filters = GardenFilter.entries
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            filters.forEachIndexed { index, filter ->
                SegmentedButton(
                    selected = activeFilter == filter,
                    onClick = { onFilter(filter) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = filters.size),
                    icon = {},
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        inactiveContainerColor = Color.Transparent,
                    ),
                ) {
                    Text(stringResource(filter.labelRes()), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        FilterChip(
            selected = hideWithered,
            onClick = onToggleWithered,
            label = { Text(stringResource(R.string.garden_hide_withered)) },
            leadingIcon = if (hideWithered) {
                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else {
                null
            },
            shape = CircleShape,
        )
    }
}

@Composable
private fun GardenField(entries: List<GardenEntry>, onSelect: (GardenEntry) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        items(items = entries, key = { it.id }) { entry ->
            PlantPlot(entry = entry, onClick = { onSelect(entry) })
        }
    }
}

@Composable
private fun PlantPlot(entry: GardenEntry, onClick: () -> Unit) {
    val withered = entry.state == GardenPlantState.WITHERED
    val name = PlantSpeciesCatalog.getById(entry.plantSpeciesId)?.displayName.orEmpty()
    val stateLabel = stringResource(if (withered) R.string.garden_withered else R.string.garden_bloomed)
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$name, $stateLabel" },
    ) {
        PlantVectorRenderer(
            plantId = entry.plantSpeciesId,
            stage = entry.growthStage,
            state = entry.state,
            size = 64.dp,
            modifier = Modifier
                .padding(bottom = 6.dp)
                .alpha(if (withered) 0.45f else 1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantDetailSheet(entry: GardenEntry, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val withered = entry.state == GardenPlantState.WITHERED
    val name = PlantSpeciesCatalog.getById(entry.plantSpeciesId)?.displayName.orEmpty()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
        ) {
            PlantVectorRenderer(
                plantId = entry.plantSpeciesId,
                stage = entry.growthStage,
                state = entry.state,
                size = 120.dp,
                modifier = Modifier.alpha(if (withered) 0.5f else 1f),
            )
            Spacer(Modifier.height(12.dp))
            Text(name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (withered) {
                    stringResource(R.string.garden_withered)
                } else {
                    stringResource(R.string.duration_minutes_short, entry.focusMinutes)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (withered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = DateFormat.getMediumDateFormat(context).format(Date(entry.plantedAt)) + "  " +
                    DateFormat.getTimeFormat(context).format(Date(entry.plantedAt)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyGarden(onStartFocus: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PlantDisplay(plant = PlantCatalog.FREE.first(), stage = GrowthStage.SPROUT, size = 120.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.empty_garden_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.empty_garden_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStartFocus, shape = CircleShape) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.start_focus))
        }
    }
}

private fun GardenFilter.labelRes(): Int = when (this) {
    GardenFilter.TODAY -> R.string.today
    GardenFilter.WEEKLY -> R.string.garden_filter_week
    GardenFilter.MONTHLY -> R.string.garden_filter_month
    GardenFilter.ALL_TIME -> R.string.garden_filter_all
}
