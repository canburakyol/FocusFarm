package com.focusfarm.app.ui.screens.home

import com.focusfarm.app.domain.FocusSessionType

/** The concise commitment shown above the fold before a focus session starts. */
data class HomeFocusPlan(
    val headline: String,
    val detail: String,
    val startLabel: String,
) {
    companion object {
        fun from(
            plantName: String,
            minutes: Int,
            sessionType: FocusSessionType,
        ): HomeFocusPlan = when (sessionType) {
            FocusSessionType.STOPWATCH -> HomeFocusPlan(
                headline = "Serbest odak planı",
                detail = "$plantName büyümeye hazır",
                startLabel = "Kronometreyi başlat",
            )
            else -> HomeFocusPlan(
                headline = "${minutes.coerceAtLeast(1)} dakikalık odak planı",
                detail = "$plantName büyümeye hazır",
                startLabel = "${minutes.coerceAtLeast(1)} dakika odaklan",
            )
        }
    }
}
