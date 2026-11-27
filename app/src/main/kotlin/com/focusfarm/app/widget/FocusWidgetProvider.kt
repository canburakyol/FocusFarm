package com.focusfarm.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.focusfarm.app.MainActivity
import com.focusfarm.app.R
import com.focusfarm.app.data.repository.GardenRepository
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.domain.FocusWidgetSummary
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Home-screen widget: today's focus against the daily goal, the streak, and a button that opens the
 * app. Refreshed every 30 minutes by the system and whenever the app goes to the background.
 */
class FocusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val summary = runCatching { loadSummary(context) }.getOrNull()
                appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, render(context, summary)) }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun loadSummary(context: Context): FocusWidgetSummary {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, FocusWidgetEntryPoint::class.java)
        val zone = ZoneId.systemDefault()
        return FocusWidgetSummary.from(
            sessions = entryPoint.gardenRepository().getAllSessions().first(),
            goalMinutes = entryPoint.progressRepository().observeDailyGoalMinutes().first(),
            freezeDates = entryPoint.progressRepository().observeFreezeDates(zone).first(),
            nowMillis = System.currentTimeMillis(),
            zoneId = zone,
        )
    }

    private fun render(context: Context, summary: FocusWidgetSummary?): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_focus).apply {
            if (summary != null) {
                setTextViewText(
                    R.id.widget_today,
                    context.getString(R.string.widget_today, summary.todayMinutes, summary.goalMinutes),
                )
                setTextViewText(
                    R.id.widget_streak,
                    context.resources.getQuantityString(R.plurals.streak_days, summary.streakDays, summary.streakDays),
                )
                setProgressBar(R.id.widget_progress, 100, summary.progressPercent, false)
            } else {
                setTextViewText(R.id.widget_today, context.getString(R.string.app_name))
            }
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            setOnClickPendingIntent(R.id.widget_root, open)
            setOnClickPendingIntent(R.id.widget_start, open)
        }

    companion object {
        /** Re-renders every placed widget; cheap no-op when none are placed. */
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FocusWidgetProvider::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, FocusWidgetProvider::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
            )
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FocusWidgetEntryPoint {
    fun gardenRepository(): GardenRepository
    fun progressRepository(): ProgressRepository
}
