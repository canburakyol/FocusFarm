package com.focusfarm.app.session

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.focusfarm.app.MainActivity
import com.focusfarm.app.R
import com.focusfarm.app.notifications.SessionProgressNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ongoing notification uses the system chronometer, so nothing in the app ticks while the screen is
 * off. The end alert is an alarm, not a foreground service, so no special Play declaration is needed.
 */
@Singleton
class AndroidSessionProgressNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : SessionProgressNotifier {

    override fun showRunning(plantName: String, startWallClockMs: Long, endWallClockMs: Long?) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannels(context, manager)
        val builder = NotificationCompat.Builder(context, RUNNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome_premium)
            .setContentTitle(context.getString(R.string.session_notif_running_title, plantName))
            .setContentText(context.getString(R.string.session_notif_running_text))
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setContentIntent(openAppIntent(context))
        if (endWallClockMs != null) {
            builder.setWhen(endWallClockMs)
                .setChronometerCountDown(true)
                .setTimeoutAfter((endWallClockMs - System.currentTimeMillis()).coerceAtLeast(1L))
            scheduleEndAlert(plantName, endWallClockMs)
        } else {
            builder.setWhen(startWallClockMs)
            cancelEndAlert()
        }
        manager.notify(RUNNING_NOTIFICATION_ID, builder.build())
    }

    override fun cancel() {
        cancelEndAlert()
        context.getSystemService(NotificationManager::class.java)?.apply {
            cancel(RUNNING_NOTIFICATION_ID)
            cancel(DONE_NOTIFICATION_ID)
        }
    }

    private fun scheduleEndAlert(plantName: String, endWallClockMs: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = endAlertIntent(plantName)
        // Exact alarms need a user-granted permission from Android 12 on; an inexact alarm may arrive a little late.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endWallClockMs, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endWallClockMs, pending)
        }
    }

    private fun cancelEndAlert() {
        context.getSystemService(AlarmManager::class.java)?.cancel(endAlertIntent(plantName = null))
    }

    private fun endAlertIntent(plantName: String?): PendingIntent = PendingIntent.getBroadcast(
        context,
        END_ALERT_REQUEST_CODE,
        Intent(context, SessionEndReceiver::class.java).putExtra(EXTRA_PLANT_NAME, plantName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        internal const val RUNNING_CHANNEL_ID = "focus_session"
        internal const val DONE_CHANNEL_ID = "focus_session_done"
        internal const val RUNNING_NOTIFICATION_ID = 2201
        internal const val DONE_NOTIFICATION_ID = 2202
        internal const val EXTRA_PLANT_NAME = "plant_name"
        private const val END_ALERT_REQUEST_CODE = 2201

        internal fun ensureChannels(context: Context, manager: NotificationManager) {
            manager.createNotificationChannel(
                NotificationChannel(
                    RUNNING_CHANNEL_ID,
                    context.getString(R.string.session_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    DONE_CHANNEL_ID,
                    context.getString(R.string.session_done_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }

        internal fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            RUNNING_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/** Fires when a countdown session reaches its end while the app may not be on screen. */
class SessionEndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        AndroidSessionProgressNotifier.ensureChannels(context, manager)
        manager.cancel(AndroidSessionProgressNotifier.RUNNING_NOTIFICATION_ID)
        val plantName = intent.getStringExtra(AndroidSessionProgressNotifier.EXTRA_PLANT_NAME)
            ?: context.getString(R.string.app_name)
        manager.notify(
            AndroidSessionProgressNotifier.DONE_NOTIFICATION_ID,
            NotificationCompat.Builder(context, AndroidSessionProgressNotifier.DONE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_monochrome_premium)
                .setContentTitle(context.getString(R.string.session_notif_done_title, plantName))
                .setContentText(context.getString(R.string.session_notif_done_text))
                .setAutoCancel(true)
                .setContentIntent(AndroidSessionProgressNotifier.openAppIntent(context))
                .build(),
        )
    }
}
