package com.focusfarm.app.ui.screens.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.focus.AllowedAppsStore
import com.focusfarm.app.data.focus.DeviceUsageSource
import com.focusfarm.app.data.focus.LaunchableApp
import com.focusfarm.core.ui.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AllowedAppsViewModel @Inject constructor(
    private val store: AllowedAppsStore,
    private val usage: DeviceUsageSource,
) : ViewModel() {
    val selected: StateFlow<Set<String>> = store.packages

    private val _hasAccess = MutableStateFlow(usage.hasAccess())
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    private val _apps = MutableStateFlow<List<LaunchableApp>?>(null)
    val apps: StateFlow<List<LaunchableApp>?> = _apps.asStateFlow()

    /** Re-checked on resume: the user grants Usage Access in system settings and comes back. */
    fun refresh() {
        _hasAccess.value = usage.hasAccess()
        if (_apps.value == null) {
            viewModelScope.launch(Dispatchers.IO) { _apps.value = usage.launchableApps() }
        }
    }

    fun toggle(packageName: String) {
        val current = store.packages.value
        store.set(if (packageName in current) current - packageName else current + packageName)
    }
}

/** Shown under the strict mode switch: opens the allowed-apps picker. */
@Composable
fun AllowedAppsRow(viewModel: AllowedAppsViewModel = hiltViewModel()) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val hasAccess by viewModel.hasAccess.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { open = true }
            .padding(vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.allowed_apps_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (hasAccess && selected.isNotEmpty()) {
                    stringResource(R.string.allowed_apps_count, selected.size)
                } else {
                    stringResource(R.string.allowed_apps_none)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
    }

    if (open) AllowedAppsSheet(viewModel = viewModel, onDismiss = { open = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllowedAppsSheet(viewModel: AllowedAppsViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val hasAccess by viewModel.hasAccess.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.allowed_apps_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.allowed_apps_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            if (!hasAccess) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.allowed_apps_access_title), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.allowed_apps_access_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }) {
                            Text(stringResource(R.string.allowed_apps_access_button))
                        }
                    }
                }
            } else {
                val list = apps
                if (list == null) {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 480.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(list, key = { it.packageName }) { app ->
                            AppRow(
                                app = app,
                                checked = app.packageName in selected,
                                onToggle = { viewModel.toggle(app.packageName) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: LaunchableApp, checked: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        runCatching { context.packageManager.getApplicationIcon(app.packageName).toBitmap(96, 96).asImageBitmap() }
            .getOrNull()
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .padding(vertical = 6.dp),
    ) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(36.dp))
        } else {
            Spacer(Modifier.size(36.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(app.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = null)
    }
}
