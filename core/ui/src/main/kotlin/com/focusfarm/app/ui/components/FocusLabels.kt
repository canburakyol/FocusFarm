package com.focusfarm.app.ui.components

import androidx.annotation.StringRes
import com.focusfarm.app.domain.AmbienceSound
import com.focusfarm.app.domain.FocusSessionType
import com.focusfarm.app.domain.FocusTag
import com.focusfarm.core.ui.R

/** Localized display names for focus settings, shared by the home, setup and session screens. */
@StringRes
fun FocusTag.labelRes(): Int = when (this) {
    FocusTag.WORK -> R.string.tag_work
    FocusTag.STUDY -> R.string.tag_study
    FocusTag.READING -> R.string.tag_reading
    FocusTag.EXERCISE -> R.string.tag_exercise
    FocusTag.OTHER -> R.string.tag_other
}

@StringRes
fun FocusSessionType.labelRes(): Int = when (this) {
    FocusSessionType.COUNTDOWN -> R.string.mode_countdown
    FocusSessionType.STOPWATCH -> R.string.mode_stopwatch
    FocusSessionType.POMODORO -> R.string.mode_pomodoro
    FocusSessionType.SHORT_BREAK -> R.string.mode_short_break
    FocusSessionType.LONG_BREAK -> R.string.mode_long_break
}

@StringRes
fun AmbienceSound.labelRes(): Int = when (this) {
    AmbienceSound.NONE -> R.string.ambience_off
    AmbienceSound.RAIN -> R.string.ambience_rain
    AmbienceSound.WHITE_NOISE -> R.string.ambience_white_noise
}
