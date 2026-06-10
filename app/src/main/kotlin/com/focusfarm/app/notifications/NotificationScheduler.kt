package com.focusfarm.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.focusfarm.app.MainActivity
import com.focusfarm.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    init {
        createNotificationChannel()
    }

    fun scheduleSessionReminder(hour: Int = 10, minute: Int = 0) {
        val workRequest = PeriodicWorkRequestBuilder<SessionReminderWorker>(
            1, TimeUnit.DAYS,
        ).setInitialDelay(
            calculateInitialDelay(hour, minute),
            TimeUnit.MILLISECONDS,
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_SESSION_REMINDER,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest,
        )
    }

    fun scheduleStreakWarning(hour: Int = 18, minute: Int = 0) {
        val workRequest = PeriodicWorkRequestBuilder<StreakReminderWorker>(
            1, TimeUnit.DAYS,
        ).setInitialDelay(
            calculateInitialDelay(hour, minute),
            TimeUnit.MILLISECONDS,
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_STREAK_WARNING,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest,
        )
    }

    fun scheduleComebackPrompt(hour: Int = 12, minute: Int = 0) {
        val workRequest = PeriodicWorkRequestBuilder<ComebackReminderWorker>(
            1, TimeUnit.DAYS,
        ).setInitialDelay(
            calculateInitialDelay(hour, minute),
            TimeUnit.MILLISECONDS,
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_COMEBACK_PROMPT,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest,
        )
    }

    fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWork()
    }

    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= now) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        return calendar.timeInMillis - now
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_FOCUS,
                "FocusFarm Bildirimleri",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Odak oturumu hatirlaticilari ve seri uyariarlari"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        title: String,
        message: String,
        notificationId: Int = DEFAULT_NOTIFICATION_ID,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FOCUS)
            .setSmallIcon(R.drawable.ic_launcher_monochrome_premium)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, notification)
    }

    private companion object {
        const val CHANNEL_ID_FOCUS = "focusfarm_focus_channel"
        const val WORK_NAME_SESSION_REMINDER = "session_reminder"
        const val WORK_NAME_STREAK_WARNING = "streak_warning"
        const val WORK_NAME_COMEBACK_PROMPT = "comeback_prompt"
        const val DEFAULT_NOTIFICATION_ID = 1001
    }
}
