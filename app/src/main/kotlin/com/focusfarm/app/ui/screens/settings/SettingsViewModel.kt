package com.focusfarm.app.ui.screens.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.R
import com.focusfarm.app.data.auth.AuthManager
import com.focusfarm.app.data.auth.AccountDeletionResult
import com.focusfarm.app.monetization.ConsentManager
import com.focusfarm.app.review.AppReviewManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val consentManager: ConsentManager,
    private val authManager: AuthManager,
    private val appReviewManager: AppReviewManager,
) : ViewModel() {
    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()
    val currentUserId: StateFlow<String?> = authManager.currentUserId
    private val _deletionResult = MutableStateFlow<AccountDeletionResult?>(null)
    val deletionResult: StateFlow<AccountDeletionResult?> = _deletionResult.asStateFlow()

    fun showPrivacyOptions(activity: Activity) {
        consentManager.showPrivacyOptions(activity) { /* UMP owns user-facing errors. */ }
    }

    fun deleteAccountOrClearLocalData() {
        if (_isDeleting.value) return
        viewModelScope.launch {
            _isDeleting.value = true
            _deletionResult.value = null
            try {
                _deletionResult.value = if (authManager.currentUserId.value == null) {
                    authManager.clearLocalData()
                } else {
                    authManager.deleteAccount()
                }
            } finally {
                _isDeleting.value = false
            }
        }
    }

    fun clearDeletionResult() { _deletionResult.value = null }

    fun requestReview(activity: Activity) {
        viewModelScope.launch {
            appReviewManager.requestReviewFromSettings(activity)
        }
    }

    fun sendFeedback(activity: Activity) {
        val email = activity.getString(R.string.feedback_email)
        val subject = activity.getString(R.string.feedback_subject)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        try {
            activity.startActivity(
                Intent.createChooser(intent, activity.getString(R.string.feedback_chooser_title))
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                activity,
                activity.getString(R.string.feedback_no_email_app),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}
