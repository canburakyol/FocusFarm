package com.focusfarm.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.focusfarm.app.data.auth.AuthManager
import com.focusfarm.app.data.repository.ReferralManager
import com.focusfarm.app.monetization.ConsentManager
import com.focusfarm.app.telemetry.AppTelemetry
import com.focusfarm.app.ui.navigation.AppNavigation
import com.focusfarm.app.ui.theme.FocusFarmTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var telemetry: AppTelemetry

    @Inject
    lateinit var consentManager: ConsentManager

    @Inject
    lateinit var referralManager: ReferralManager

    @Inject
    lateinit var authManager: AuthManager

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusFarmTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
        telemetry.logEvent("app_open")
        consentManager.refreshConsent(this)
        maybeRequestNotificationPermission()
        handleDeepLink(intent)

        lifecycleScope.launch(Dispatchers.IO) {
            authManager.signInAnonymouslyIfNeeded()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data == null) return

        when (data.host) {
            "focusfarm.app" -> {
                when (data.path) {
                    "/referral" -> {
                        val code = data.getQueryParameter("code")
                        if (!code.isNullOrEmpty()) {
                            handleReferralLink(code)
                        }
                    }
                }
            }
        }
    }

    private fun handleReferralLink(referralCode: String) {
        lifecycleScope.launch {
            telemetry.logEvent(
                "deep_link_referral_opened",
                mapOf("referral_code" to referralCode),
            )

            val result = referralManager.verifyReferral(referralCode)

            if (result.success) {
                telemetry.logEvent(
                    "referral_success",
                    mapOf("reward_amount" to result.rewardAmount.toString()),
                )
            } else {
                telemetry.logEvent(
                    "referral_failed",
                    mapOf("error" to result.message),
                )
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            return
        }

        val prefs = getSharedPreferences(PREFS_PERMISSIONS, MODE_PRIVATE)
        val askedBefore = prefs.getBoolean(KEY_NOTIFICATIONS_ASKED, false)
        if (askedBefore) return

        prefs.edit { putBoolean(KEY_NOTIFICATIONS_ASKED, true) }
        notificationPermissionLauncher.launch(permission)
    }

    private companion object {
        const val PREFS_PERMISSIONS = "permission_prefs"
        const val KEY_NOTIFICATIONS_ASKED = "notifications_asked"
    }
}
