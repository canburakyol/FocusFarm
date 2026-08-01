package com.focusfarm.app.ui.screens.home

import com.focusfarm.app.domain.FocusSessionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeFocusPlanTest {

    @Test
    fun countdownPlanMakesTheNextFocusActionExplicit() {
        val plan = HomeFocusPlan.from(
            plantName = "Filiz",
            minutes = 25,
            sessionType = FocusSessionType.COUNTDOWN,
        )

        assertThat(plan.headline).isEqualTo("25 dakikalık odak planı")
        assertThat(plan.detail).isEqualTo("Filiz büyümeye hazır")
        assertThat(plan.startLabel).isEqualTo("25 dakika odaklan")
    }

    @Test
    fun stopwatchPlanDoesNotPromiseADuration() {
        val plan = HomeFocusPlan.from(
            plantName = "Filiz",
            minutes = 25,
            sessionType = FocusSessionType.STOPWATCH,
        )

        assertThat(plan.headline).isEqualTo("Serbest odak planı")
        assertThat(plan.startLabel).isEqualTo("Kronometreyi başlat")
    }
}
