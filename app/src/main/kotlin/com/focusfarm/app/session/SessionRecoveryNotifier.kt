package com.focusfarm.app.session

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.focusfarm.app.MainActivity
import com.focusfarm.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface SessionRecoveryNotifier {
    fun showResume(sessionId: String)
    fun showRecovery(sessionId: String, suggestedMinutes: Int)
    fun showFinalization(sessionId: String)
}

@Singleton
class AndroidSessionRecoveryNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : SessionRecoveryNotifier {
    override fun showResume(sessionId: String) = show(
        sessionId, context.getString(R.string.recovery_resume_title),
        context.getString(R.string.recovery_resume_text),
    )

    override fun showRecovery(sessionId: String, suggestedMinutes: Int) = show(
        sessionId, context.getString(R.string.recovery_offer_title),
        context.getString(R.string.recovery_offer_text, suggestedMinutes),
    )

    override fun showFinalization(sessionId: String) = show(
        sessionId, context.getString(R.string.recovery_finalize_title),
        context.getString(R.string.recovery_finalize_text),
    )

    private fun show(sessionId: String, title: String, text: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.recovery_channel_name), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_SESSION_ID, sessionId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_monochrome_premium)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                .build(),
        )
    }

    companion object {
        const val EXTRA_SESSION_ID = "recovery_session_id"
        private const val CHANNEL_ID = "session_recovery"
        private const val NOTIFICATION_ID = 2102
    }
}
