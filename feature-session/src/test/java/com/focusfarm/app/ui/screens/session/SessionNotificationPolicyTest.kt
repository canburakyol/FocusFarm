package com.focusfarm.app.ui.screens.session

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionNotificationPolicyTest {
    @Test
    fun runningSessionShowsTheOngoingNotification() {
        assertThat(sessionNotificationAction(SessionState.RUNNING, appInForeground = true))
            .isEqualTo(SessionNotificationAction.SHOW_RUNNING)
    }

    @Test
    fun sessionFinishingInTheBackgroundKeepsTheEndAlert() {
        assertThat(sessionNotificationAction(SessionState.COMPLETED, appInForeground = false))
            .isEqualTo(SessionNotificationAction.KEEP)
        assertThat(sessionNotificationAction(SessionState.FINALIZING, appInForeground = false))
            .isEqualTo(SessionNotificationAction.KEEP)
    }

    @Test
    fun sessionFinishingOnScreenClearsNotifications() {
        assertThat(sessionNotificationAction(SessionState.COMPLETED, appInForeground = true))
            .isEqualTo(SessionNotificationAction.CANCEL)
    }

    @Test
    fun pausedOrWitheredSessionsClearNotifications() {
        assertThat(sessionNotificationAction(SessionState.PAUSED, appInForeground = false))
            .isEqualTo(SessionNotificationAction.CANCEL)
        assertThat(sessionNotificationAction(SessionState.KILLED, appInForeground = false))
            .isEqualTo(SessionNotificationAction.CANCEL)
    }
}
