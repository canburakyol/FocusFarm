package com.focusfarm.app.ui.screens.session

/** What the session notification should do after a session state change. */
internal enum class SessionNotificationAction { SHOW_RUNNING, CANCEL, KEEP }

/**
 * A session that finishes while the app is in the background keeps its end alert, so the user
 * learns the plant has grown. Every other way a session stops removes the notifications.
 */
internal fun sessionNotificationAction(state: SessionState, appInForeground: Boolean): SessionNotificationAction =
    when (state) {
        SessionState.RUNNING -> SessionNotificationAction.SHOW_RUNNING
        SessionState.FINALIZING, SessionState.COMPLETED ->
            if (appInForeground) SessionNotificationAction.CANCEL else SessionNotificationAction.KEEP
        else -> SessionNotificationAction.CANCEL
    }
