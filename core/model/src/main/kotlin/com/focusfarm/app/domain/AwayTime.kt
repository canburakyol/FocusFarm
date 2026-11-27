package com.focusfarm.app.domain

/** A device usage change, as reported by the system usage history. */
sealed interface DeviceUsageEvent {
    val atMillis: Long

    data class AppForeground(override val atMillis: Long, val packageName: String) : DeviceUsageEvent
    data class ScreenOff(override val atMillis: Long) : DeviceUsageEvent
    data class ScreenOn(override val atMillis: Long) : DeviceUsageEvent
}

/**
 * Strict focus with allowed apps: only time spent with the screen on in an app that is neither
 * FocusFarm nor on the allowed list counts as being away. The home screen counts as away.
 */
object AwayTime {
    /**
     * Wall-clock time at which away time since [fromMillis] first reaches [limitMillis], or null if
     * it has not by [toMillis]. The user is assumed to be away with the screen on at [fromMillis],
     * since that is the moment FocusFarm went to the background with the screen interactive.
     */
    fun limitReachedAt(
        events: List<DeviceUsageEvent>,
        fromMillis: Long,
        toMillis: Long,
        ownPackage: String,
        allowedPackages: Set<String>,
        limitMillis: Long,
    ): Long? {
        var foreground: String? = null
        var screenOn = true
        var away = 0L
        var cursor = fromMillis

        fun isAway() = screenOn && foreground != ownPackage && foreground !in allowedPackages

        fun advanceTo(t: Long): Long? {
            val end = t.coerceAtMost(toMillis)
            if (end > cursor && isAway()) {
                val needed = limitMillis - away
                if (end - cursor >= needed) return cursor + needed
                away += end - cursor
            }
            cursor = maxOf(cursor, end)
            return null
        }

        for (event in events.sortedBy { it.atMillis }) {
            if (event.atMillis < fromMillis) {
                // Events before leaving set the starting state. The system logs the next app as resumed
                // about a second before FocusFarm reports being stopped, so that app is where the user went.
                when (event) {
                    is DeviceUsageEvent.ScreenOff -> screenOn = false
                    is DeviceUsageEvent.ScreenOn -> screenOn = true
                    is DeviceUsageEvent.AppForeground -> foreground = event.packageName
                }
                continue
            }
            if (event.atMillis > toMillis) break
            advanceTo(event.atMillis)?.let { return it }
            when (event) {
                is DeviceUsageEvent.AppForeground -> foreground = event.packageName
                is DeviceUsageEvent.ScreenOff -> screenOn = false
                is DeviceUsageEvent.ScreenOn -> screenOn = true
            }
        }
        return advanceTo(toMillis)
    }
}
