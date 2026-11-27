package com.focusfarm.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.focusfarm.app.data.repository.OnboardingPreferences
import com.focusfarm.app.data.repository.RetentionTracker
import com.focusfarm.app.data.repository.ReferralManager
import com.focusfarm.app.data.repository.SessionRecoveryManager
import com.focusfarm.app.data.repository.RecoveryDecision
import com.focusfarm.app.monetization.ConsentManager
import com.focusfarm.app.review.AppReviewManager
import com.focusfarm.app.telemetry.AppTelemetry
import com.focusfarm.app.ui.navigation.AppNavigation
import com.focusfarm.app.ui.navigation.ReleaseFeatureScope
import com.focusfarm.app.ui.theme.FocusFarmTheme
import com.focusfarm.app.widget.FocusWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var telemetry: AppTelemetry

    @Inject
    lateinit var consentManager: ConsentManager

    @Inject
    lateinit var referralManager: ReferralManager

    @Inject
    lateinit var onboardingPreferences: OnboardingPreferences

    @Inject
    lateinit var retentionTracker: RetentionTracker

    @Inject
    lateinit var recoveryManager: SessionRecoveryManager

    @Inject
    lateinit var appReviewManager: AppReviewManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusFarmTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        onboardingCompleted = onboardingPreferences.isCompleted(),
                        appReviewManager = appReviewManager,
                    )
                }
            }
        }
        telemetry.logEvent("app_open")
        handleDeepLink(intent)

        lifecycleScope.launch {
            val decision = withContext(Dispatchers.IO) {
                retentionTracker.trackCurrentDay()
                recoveryManager.reconcile()
            }
            val hasActiveSession = decision != RecoveryDecision.None
            // Never drop the consent form on top of a focus session; only refresh the stored status then.
            consentManager.refreshConsent(if (hasActiveSession) null else this@MainActivity)
            appReviewManager.onAppOpen(
                activity = this@MainActivity,
                hasActiveSession = hasActiveSession,
            )
        }
    }

    override fun onStop() {
        super.onStop()
        // Leaving the app is when today's focus may have changed; refresh the home-screen widget.
        FocusWidgetProvider.requestUpdate(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        if (!ReleaseFeatureScope.REFERRAL_ENABLED) return
        val data: Uri? = intent?.data
        if (data == null || !data.isHierarchical) return

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
                mapOf(
                    "referral_code_present" to "true",
                    "referral_code_length" to referralCode.length.toString(),
                ),
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

}
