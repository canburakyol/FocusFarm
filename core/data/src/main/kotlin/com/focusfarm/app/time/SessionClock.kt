package com.focusfarm.app.time

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

interface SessionClock {
    fun wallTimeMillis(): Long
    fun elapsedRealtimeMillis(): Long
    fun bootMarker(): Long
}

@Singleton
class AndroidSessionClock @Inject constructor() : SessionClock {
    override fun wallTimeMillis(): Long = System.currentTimeMillis()

    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

    override fun bootMarker(): Long =
        (wallTimeMillis() - elapsedRealtimeMillis()).floorTo(BOOT_MARKER_GRANULARITY_MS)

    private fun Long.floorTo(granularity: Long): Long =
        Math.floorDiv(this, granularity) * granularity

    private companion object {
        const val BOOT_MARKER_GRANULARITY_MS = 60_000L
    }
}
