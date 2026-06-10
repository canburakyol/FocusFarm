package com.focusfarm.app.bootstrap

import android.os.Debug
import java.io.File

/**
 * Lightweight runtime security checks.
 * Detects rooted devices and attached debuggers to protect
 * local data integrity (Room DB, SharedPreferences).
 */
object SecurityChecker {

    private val KNOWN_ROOT_PATHS = listOf(
        "/system/app/Superuser.apk",
        "/system/xbin/su",
        "/sbin/su",
        "/system/bin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su",
    )

    fun isDeviceCompromised(): Boolean = isRooted() || isDebuggerAttached()

    private fun isRooted(): Boolean = KNOWN_ROOT_PATHS.any { File(it).exists() }

    private fun isDebuggerAttached(): Boolean = Debug.isDebuggerConnected()
}
