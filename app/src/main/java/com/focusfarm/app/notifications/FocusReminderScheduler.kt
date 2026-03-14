package com.focusfarm.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import com.focusfarm.app.domain.FocusSession
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object FocusReminderScheduler {
    private const val PREFS_NAME = "focus_reminders"
    private const val PREF_REMINDER_HOUR = "reminder_hour"
    private const val REMINDER_MINUTE = 0
    private const val REQUEST_CODE_REMINDER = 4201

    const val CHANNEL_ID = "focus_reminder_channel"

    fun scheduleFromSessions(
        context: Context,
        sessions: List<FocusSession>,
    ) {
        val bestHour = bestHourFromSessions(sessions)
        scheduleDailyReminder(context, bestHour)
    }

    fun restoreSchedule(context: Context) {
        val hour = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(PREF_REMINDER_HOUR, -1)
        if (hour in 0..23) {
            scheduleDailyReminder(context, hour, persistSelection = false)
        }
    }

    fun scheduleDailyReminder(
        context: Context,
        hourOfDay: Int,
        persistSelection: Boolean = true,
    ) {
        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderPendingIntent(context)
        alarmManager.cancel(pendingIntent)

        val triggerAtMillis = nextTriggerMillis(hourOfDay, REMINDER_MINUTE)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent,
        )

        if (persistSelection) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putInt(PREF_REMINDER_HOUR, hourOfDay)
            }
        }
    }

    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Focus Hatirlatici",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Odak seansi hatirlatmalari"
        }
        manager.createNotificationChannel(channel)
    }

    fun reminderPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, FocusReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun bestHourFromSessions(sessions: List<FocusSession>): Int {
        if (sessions.isEmpty()) return 20

        val hourScores = IntArray(24)
        sessions.take(100).forEach { session ->
            val hour = Instant.ofEpochMilli(session.completedAt)
                .atZone(ZoneId.systemDefault())
                .hour
            hourScores[hour] += session.durationMinutes.coerceAtLeast(1)
        }
        val bestHour = hourScores.indices.maxByOrNull { idx -> hourScores[idx] } ?: 20
        return bestHour.coerceIn(7, 23)
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val zoneId = ZoneId.systemDefault()
        val now = LocalDateTime.now(zoneId)
        var trigger = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)
        if (!trigger.isAfter(now)) {
            trigger = trigger.plusDays(1)
        }
        return trigger.atZone(zoneId).toInstant().toEpochMilli()
    }
}
