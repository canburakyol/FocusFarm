package com.focusfarm.app.ui.screens.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.auth.AuthManager
import com.focusfarm.app.monetization.ConsentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val consentManager: ConsentManager,
    private val authManager: AuthManager,
) : ViewModel() {
    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    fun showPrivacyOptions(activity: Activity) {
        consentManager.showPrivacyOptions(activity) { /* UMP owns user-facing errors. */ }
    }

    fun deleteAccount() {
        if (_isDeleting.value) return
        viewModelScope.launch {
            _isDeleting.update { true }
            authManager.deleteAccount()
            _isDeleting.update { false }
        }
    }
}
