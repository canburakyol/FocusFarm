package com.focusfarm.app.notifications

/**
 * Keeps the user informed about a running focus session while the app is not on screen:
 * an ongoing countdown notification and a "your plant has grown" alert at the end.
 */
interface SessionProgressNotifier {
    /**
     * Shows the ongoing notification. [endWallClockMs] is the countdown target; null means an
     * open-ended stopwatch session that counts up from [startWallClockMs] and has no end alert.
     */
    fun showRunning(plantName: String, startWallClockMs: Long, endWallClockMs: Long?)

    /** Removes the ongoing notification and the pending end alert. */
    fun cancel()
}
