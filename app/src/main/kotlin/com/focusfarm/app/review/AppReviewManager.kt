package com.focusfarm.app.review

import android.app.Activity
import com.focusfarm.app.review.InAppReviewLauncher
import com.focusfarm.app.review.ReviewPromptGate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-level coordinator for the Google Play In-App Review flow.
 *
 * All entry points (app open, goal completion, settings) go through the same
 * [ReviewPromptGate] limits and mark a successful launch as a shown prompt.
 */
@Singleton
class AppReviewManager @Inject constructor(
    private val gate: ReviewPromptGate,
    private val launcher: InAppReviewLauncher,
) {

    /**
     * Records the app launch and, if this is the 5th launch and there is no
     * active focus session, attempts the review flow.
     */
    suspend fun onAppOpen(activity: Activity, hasActiveSession: Boolean) {
        val record = gate.recordAppLaunch()
        if (hasActiveSession) return
        if (!record.isFifthSession) return
        launchIfAllowed(activity)
    }

    /** Called from the session completion screen when a daily goal was reached. */
    suspend fun onGoalCompleted(activity: Activity) {
        launchIfAllowed(activity)
    }

    /** Called from the settings "Send feedback" row. */
    suspend fun requestReviewFromSettings(activity: Activity) {
        launchIfAllowed(activity)
    }

    private suspend fun launchIfAllowed(activity: Activity) {
        val decision = gate.shouldPrompt(
            isActiveSession = false,
            isErrorVisible = false,
        )
        if (!decision.shouldPrompt) return

        val launched = launcher.launchReview(activity)
        if (launched) {
            gate.markPromptShown()
        }
    }
}
