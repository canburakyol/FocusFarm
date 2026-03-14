package com.focusfarm.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.focusfarm.app.telemetry.AppTelemetry
import com.focusfarm.app.ui.navigation.AppNavigation
import com.focusfarm.app.ui.theme.FocusFarmTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var telemetry: AppTelemetry

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
        maybeRecordTestNonFatal()
        maybeRequestNotificationPermission()
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

    private fun maybeRecordTestNonFatal() {
        val shouldRecord = intent?.getBooleanExtra(EXTRA_TEST_NON_FATAL, false) ?: false
        if (!shouldRecord) return

        telemetry.recordNonFatal(
            tag = "manual_non_fatal_test",
            message = "Triggered via adb intent extra",
        )
    }

    private companion object {
        const val PREFS_PERMISSIONS = "permission_prefs"
        const val KEY_NOTIFICATIONS_ASKED = "notifications_asked"
        const val EXTRA_TEST_NON_FATAL = "test_non_fatal"
    }
}
