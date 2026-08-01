package com.focusfarm.app.ui.screens.home

/** Contextual home-screen prompt based on today's goal and current streak. */
data class HomeRetentionNudge(
    val title: String,
    val detail: String,
    val tone: Tone,
) {
    enum class Tone { WELCOME, PROGRESS, URGENT, CELEBRATE }

    companion object {
        fun from(
            todayFocusMinutes: Int,
            dailyGoalMinutes: Int,
            currentStreak: Int,
        ): HomeRetentionNudge {
            val today = todayFocusMinutes.coerceAtLeast(0)
            val goal = dailyGoalMinutes.coerceAtLeast(1)
            if (today >= goal) {
                return HomeRetentionNudge(
                    title = "Bugünün bahçesi tamam",
                    detail = "Hedefini doldurdun. İstersen sakin bir ek oturumla bahçeni büyüt.",
                    tone = Tone.CELEBRATE,
                )
            }
            if (today == 0 && currentStreak > 0) {
                return HomeRetentionNudge(
                    title = "Serini koru",
                    detail = "$currentStreak günlük serin için bugün kısa bir odak oturumu yeterli.",
                    tone = Tone.URGENT,
                )
            }
            if (today > 0) {
                val remaining = goal - today
                return HomeRetentionNudge(
                    title = "Ritmini sürdür",
                    detail = "Günlük hedefine ulaşmak için $remaining dakika daha odaklan.",
                    tone = Tone.PROGRESS,
                )
            }
            return HomeRetentionNudge(
                title = "Bahçene küçük bir başlangıç yap",
                detail = "Bugün kısa bir odak oturumu seç ve ilk filizini büyüt.",
                tone = Tone.WELCOME,
            )
        }
    }
}
