package com.focusfarm.app.data.focus

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import com.focusfarm.app.domain.DeviceUsageEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Apps the user may use during strict focus without their plant withering. */
@Singleton
class AllowedAppsStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _packages = MutableStateFlow(prefs.getStringSet(KEY, emptySet()).orEmpty().toSet())
    val packages: StateFlow<Set<String>> = _packages.asStateFlow()

    fun set(packages: Set<String>) {
        prefs.edit().putStringSet(KEY, packages).apply()
        _packages.value = packages
    }

    companion object {
        const val PREFS = "focus_allowed_apps"
        private const val KEY = "packages"
    }
}

data class LaunchableApp(val packageName: String, val label: String)

/**
 * Reads which app was in front from the system usage history. Needs the user-granted Usage Access
 * special permission; without it [hasAccess] is false and strict focus behaves as before.
 */
@Singleton
class DeviceUsageSource @Inject constructor(@ApplicationContext private val context: Context) {
    val ownPackage: String = context.packageName

    fun hasAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ownPackage)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun events(fromMillis: Long, toMillis: Long): List<DeviceUsageEvent> {
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        // Start a little early so the screen state at the moment of leaving is known.
        val usage = manager.queryEvents(fromMillis - LOOKBACK_MS, toMillis)
        val event = UsageEvents.Event()
        val result = mutableListOf<DeviceUsageEvent>()
        while (usage.hasNextEvent()) {
            usage.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED ->
                    result += DeviceUsageEvent.AppForeground(event.timeStamp, event.packageName)
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> result += DeviceUsageEvent.ScreenOff(event.timeStamp)
                UsageEvents.Event.SCREEN_INTERACTIVE -> result += DeviceUsageEvent.ScreenOn(event.timeStamp)
            }
        }
        return result
    }

    /** Apps with a launcher icon, except FocusFarm itself, sorted by name. */
    fun launchableApps(): List<LaunchableApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { (pkg, _) -> pkg != ownPackage }
            .distinctBy { it.first }
            .map { (pkg, label) -> LaunchableApp(pkg, label) }
            .sortedBy { it.label.lowercase() }
    }

    private companion object {
        const val LOOKBACK_MS = 60_000L
    }
}
