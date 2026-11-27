package com.focusfarm.app.ui.screens.session

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StrictKillTimerTest {
    @Test
    fun leavingForAnotherAppDuringStrictSessionStartsTheTimer() {
        assertThat(shouldStartStrictKillTimer(SessionState.RUNNING, isStrict = true, screenInteractive = true)).isTrue()
    }

    @Test
    fun lockingThePhoneDuringStrictSessionDoesNotKillThePlant() {
        assertThat(shouldStartStrictKillTimer(SessionState.RUNNING, isStrict = true, screenInteractive = false)).isFalse()
    }

    @Test
    fun nonStrictOrPausedSessionsNeverStartTheTimer() {
        assertThat(shouldStartStrictKillTimer(SessionState.RUNNING, isStrict = false, screenInteractive = true)).isFalse()
        assertThat(shouldStartStrictKillTimer(SessionState.PAUSED, isStrict = true, screenInteractive = true)).isFalse()
    }
}
