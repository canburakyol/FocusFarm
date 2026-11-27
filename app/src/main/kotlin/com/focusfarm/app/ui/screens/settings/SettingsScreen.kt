package com.focusfarm.app.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfarm.app.R
import com.focusfarm.app.data.auth.AccountDeletionResult
import com.focusfarm.app.ui.components.FocusBackdrop

private const val PRIVACY_POLICY_URL = "https://canburakyol.github.io/focusfarm-privacy/"

@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val isDeleting by viewModel.isDeleting.collectAsStateWithLifecycle()
    val deletionResult by viewModel.deletionResult.collectAsStateWithLifecycle()

    LaunchedEffect(deletionResult) {
        if (deletionResult == AccountDeletionResult.Success) {
            showDeleteDialog = false
            activity?.finishAffinity()
            viewModel.clearDeletionResult()
        }
    }

    FocusBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (onBack != null) {
                    TextButton(onClick = onBack, enabled = !isDeleting) {
                        Text(stringResource(R.string.go_back))
                    }
                }
            }

            SettingsSectionCard(
                title = stringResource(R.string.settings_privacy_title),
                body = stringResource(R.string.settings_privacy_desc),
            ) {
                Button(
                    onClick = {
                        val currentActivity = activity
                        if (currentActivity != null) {
                            viewModel.showPrivacyOptions(currentActivity)
                        }
                    },
                    enabled = activity != null,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(stringResource(R.string.settings_privacy_preferences))
                }

                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
                        )
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_privacy_policy_button))
                }
            }

            SettingsSectionCard(
                title = stringResource(R.string.settings_feedback_title),
                body = stringResource(R.string.settings_feedback_body),
            ) {
                Button(
                    onClick = {
                        val currentActivity = activity
                        if (currentActivity != null) {
                            viewModel.requestReview(currentActivity)
                        }
                    },
                    enabled = activity != null,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(stringResource(R.string.settings_feedback_rate_button))
                }

                OutlinedButton(
                    onClick = {
                        val currentActivity = activity
                        if (currentActivity != null) {
                            viewModel.sendFeedback(currentActivity)
                        }
                    },
                    enabled = activity != null,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_feedback_send_button))
                }
            }

            SettingsSectionCard(
                title = stringResource(R.string.settings_account_data_title),
                body = stringResource(if (currentUserId == null) R.string.settings_clear_local_data_body else R.string.settings_account_delete_body),
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.clearDeletionResult()
                        showDeleteDialog = true
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(if (currentUserId == null) R.string.settings_clear_local_data_button else R.string.settings_delete_account_button))
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
                title = { Text(stringResource(if (currentUserId == null) R.string.settings_clear_local_data_title else R.string.settings_delete_account_title)) },
                text = {
                    Column {
                        Text(stringResource(if (currentUserId == null) R.string.settings_clear_local_data_confirm_body else R.string.settings_delete_account_confirm_body))
                        deletionResult?.takeIf { it != AccountDeletionResult.Success }?.let { result ->
                            Text(
                                text = stringResource(when (result) {
                                    AccountDeletionResult.NoSignedInAccount -> R.string.settings_delete_no_account
                                    AccountDeletionResult.RequiresRecentLogin -> R.string.settings_delete_reauth_required
                                    AccountDeletionResult.RemoteDeletionFailed -> R.string.settings_delete_remote_failed
                                    AccountDeletionResult.LocalCleanupFailed -> R.string.settings_clear_local_failed
                                    AccountDeletionResult.AccountDeletedLocalCleanupFailed -> R.string.settings_delete_local_failed
                                    AccountDeletionResult.Success -> R.string.settings_delete_remote_failed
                                }),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::deleteAccountOrClearLocalData,
                        enabled = !isDeleting,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(stringResource(if (isDeleting) R.string.settings_delete_processing else if (currentUserId == null) R.string.settings_clear_local_data_confirm else R.string.settings_delete_account_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            content()
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
