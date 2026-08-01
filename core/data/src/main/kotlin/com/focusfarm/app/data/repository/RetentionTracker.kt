package com.focusfarm.app.data.repository

import android.content.Context
import androidx.core.content.edit
import com.focusfarm.app.data.local.RecoverySessionDao
import com.focusfarm.app.data.local.SessionDao
import com.focusfarm.app.telemetry.AppTelemetry
import com.focusfarm.app.time.Clock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetentionTracker @Inject constructor(
    @ApplicationContext context: Context,
    private val sessionDao: SessionDao,
    private val recoverySessionDao: RecoverySessionDao,
    private val telemetry: AppTelemetry,
    private val clock: Clock,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    suspend fun trackCurrentDay(force: Boolean = false) {
        val now = clock.nowMillis()
        val installedAt = prefs.getLong(KEY_INSTALLED_AT, 0L).takeIf { it > 0L } ?: now.also {
            prefs.edit { putLong(KEY_INSTALLED_AT, it) }
        }
        val installDate = Instant.ofEpochMilli(installedAt).atZone(clock.zoneId).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(clock.zoneId).toLocalDate()
        val dayIndex = ChronoUnit.DAYS.between(installDate, today).toInt()
        if (dayIndex !in 0..7) return
        val dateKey = today.toString()
        if (!force && prefs.getString(KEY_LAST_LOGGED_DATE, null) == dateKey) return
        val start = today.atStartOfDay(clock.zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(clock.zoneId).toInstant().toEpochMilli()
        telemetry.logEvent(
            "retention_daily_activity",
            mapOf(
                "day_index" to dayIndex.toString(),
                "cohort_day" to when (dayIndex) { 1 -> "D1"; 3 -> "D3"; 7 -> "D7"; else -> "D$dayIndex" },
                "completed_sessions" to sessionDao.countCompletedBetween(start, end).toString(),
                "recovery_completions" to recoverySessionDao.countCompletedBetween(start, end).toString(),
            ),
        )
        prefs.edit { putString(KEY_LAST_LOGGED_DATE, dateKey) }
    }

    private companion object {
        const val PREFS = "retention_cohort"
        const val KEY_INSTALLED_AT = "installed_at"
        const val KEY_LAST_LOGGED_DATE = "last_logged_date"
    }
}
