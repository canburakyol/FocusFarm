package com.focusfarm.app.review

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.focusfarm.app.time.Clock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Testable gating logic for the in-app review prompt.
 *
 * The rules are:
 *  - never prompt during an active focus session,
 *  - never prompt while an error state is visible,
 *  - never prompt on the very first app launch,
 *  - at most one prompt per calendar day,
 *  - at most two prompts per rolling 30-day window,
 *  - the 5th app session is a valid trigger if all other rules pass,
 *  - a session that pushes the user over their daily focus goal is a valid
 *    trigger if all other rules pass.
 */
@Singleton
class ReviewPromptGate @Inject constructor(
    @ApplicationContext context: Context,
    private val clock: Clock,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns the total number of recorded app launches. */
    fun getLaunchCount(): Int = prefs.getInt(KEY_LAUNCH_COUNT, 0)

    /**
     * Records a new app launch and returns the updated launch count together
     * with a flag that is `true` exactly on the 5th launch.
     */
    fun recordAppLaunch(): AppLaunchRecord {
        val count = getLaunchCount() + 1
        prefs.edit { putInt(KEY_LAUNCH_COUNT, count) }
        return AppLaunchRecord(
            launchCount = count,
            isFifthSession = count == FIFTH_SESSION,
        )
    }

    /**
     * Returns `true` when the just-completed session is the one that pushed
     * the user over their daily focus goal.
     */
    fun checkGoalCompletion(
        minutesBeforeSession: Int,
        sessionDurationMinutes: Int,
        dailyGoalMinutes: Int,
    ): Boolean {
        if (dailyGoalMinutes <= 0) return false
        val minutesAfter = minutesBeforeSession + sessionDurationMinutes
        return minutesBeforeSession < dailyGoalMinutes && minutesAfter >= dailyGoalMinutes
    }

    /**
     * Checks whether a review prompt is allowed right now.
     *
     * @param isActiveSession `true` if a focus session is currently running.
     * @param isErrorVisible `true` if the screen is showing an error state.
     */
    fun shouldPrompt(
        isActiveSession: Boolean,
        isErrorVisible: Boolean,
        nowMillis: Long = clock.nowMillis(),
    ): ReviewPromptDecision {
        if (isActiveSession) {
            return ReviewPromptDecision(shouldPrompt = false, reason = "active_session")
        }
        if (isErrorVisible) {
            return ReviewPromptDecision(shouldPrompt = false, reason = "error_visible")
        }
        if (getLaunchCount() <= 1) {
            return ReviewPromptDecision(shouldPrompt = false, reason = "first_launch")
        }
        if (hasPromptToday(nowMillis)) {
            return ReviewPromptDecision(shouldPrompt = false, reason = "already_today")
        }
        if (promptCountInLast30Days(nowMillis) >= MAX_PROMPTS_PER_30_DAYS) {
            return ReviewPromptDecision(shouldPrompt = false, reason = "max_30_days")
        }
        return ReviewPromptDecision(shouldPrompt = true, reason = "allowed")
    }

    /** Stores the current timestamp as a shown prompt. */
    fun markPromptShown(nowMillis: Long = clock.nowMillis()) {
        prefs.edit {
            val cleaned = cleanOldTimestamps(readTimestamps(), nowMillis)
            putString(
                KEY_PROMPT_TIMESTAMPS,
                (cleaned + nowMillis).joinToString(",") { it.toString() },
            )
        }
    }

    private fun hasPromptToday(nowMillis: Long): Boolean {
        val today = LocalDate.ofInstant(Instant.ofEpochMilli(nowMillis), clock.zoneId)
        return readTimestamps().any { millis ->
            LocalDate.ofInstant(Instant.ofEpochMilli(millis), clock.zoneId) == today
        }
    }

    private fun promptCountInLast30Days(nowMillis: Long): Int {
        val cutoff = nowMillis - THIRTY_DAYS_MILLIS
        return readTimestamps().count { it > cutoff }
    }

    private fun readTimestamps(): List<Long> {
        val raw = prefs.getString(KEY_PROMPT_TIMESTAMPS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.toLongOrNull() }
    }

    private fun cleanOldTimestamps(timestamps: List<Long>, nowMillis: Long): List<Long> {
        val cutoff = nowMillis - THIRTY_DAYS_MILLIS
        return timestamps.filter { it > cutoff }
    }

    data class AppLaunchRecord(
        val launchCount: Int,
        val isFifthSession: Boolean,
    )

    data class ReviewPromptDecision(
        val shouldPrompt: Boolean,
        val reason: String,
    )

    private companion object {
        const val PREFS_NAME = "focusfarm_review_prompt"
        const val KEY_LAUNCH_COUNT = "app_launch_count"
        const val KEY_PROMPT_TIMESTAMPS = "prompt_timestamps"
        const val FIFTH_SESSION = 5
        const val MAX_PROMPTS_PER_30_DAYS = 2
        const val THIRTY_DAYS_MILLIS = 30L * 24L * 60L * 60L * 1000L
    }
}
