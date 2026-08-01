package com.focusfarm.app.data.repository

import android.content.Context
import androidx.core.content.edit
import com.focusfarm.app.time.Clock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

data class LocalCohortDay(
    val dayIndex: Int,
    val completedSessions: Int,
    val completedRecoverySessions: Int,
)

@Singleton
class LocalCohortTracker @Inject constructor(
    @ApplicationContext context: Context,
    private val clock: Clock,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun recordSessionCompleted(wasRecovery: Boolean) {
        val day = currentDayIndex() ?: return
        prefs.edit {
            putInt(sessionKey(day), prefs.getInt(sessionKey(day), 0) + 1)
            if (wasRecovery) {
                putInt(recoveryKey(day), prefs.getInt(recoveryKey(day), 0) + 1)
            }
        }
    }

    fun snapshot(): List<LocalCohortDay> = (0..7).map { day ->
        LocalCohortDay(
            dayIndex = day,
            completedSessions = prefs.getInt(sessionKey(day), 0),
            completedRecoverySessions = prefs.getInt(recoveryKey(day), 0),
        )
    }

    private fun currentDayIndex(): Int? {
        val now = clock.nowMillis()
        val installedAt = prefs.getLong(KEY_INSTALLED_AT, 0L).takeIf { it > 0L } ?: now.also {
            prefs.edit { putLong(KEY_INSTALLED_AT, it) }
        }
        val installed = Instant.ofEpochMilli(installedAt).atZone(clock.zoneId).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(clock.zoneId).toLocalDate()
        return ChronoUnit.DAYS.between(installed, today).toInt().takeIf { it in 0..7 }
    }

    private fun sessionKey(day: Int) = "day_${day}_sessions"
    private fun recoveryKey(day: Int) = "day_${day}_recoveries"

    private companion object {
        const val PREFS = "local_first_week_cohort"
        const val KEY_INSTALLED_AT = "installed_at"
    }
}
