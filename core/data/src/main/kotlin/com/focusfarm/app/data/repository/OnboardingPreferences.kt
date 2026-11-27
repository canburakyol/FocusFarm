package com.focusfarm.app.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingPreferences @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isCompleted(): Boolean = prefs.getBoolean(KEY_COMPLETED, false)

    fun complete(focusGoal: String, plantId: String, durationMinutes: Int) {
        prefs.edit {
            putBoolean(KEY_COMPLETED, true)
            putString(KEY_GOAL, focusGoal)
            putString(KEY_PLANT, plantId)
            putInt(KEY_DURATION, durationMinutes)
        }
    }

    fun getDefaultDuration(): Int = prefs.getInt(KEY_DURATION, 25)

    private companion object {
        const val PREFS = "product_onboarding"
        const val KEY_COMPLETED = "completed"
        const val KEY_GOAL = "focus_goal"
        const val KEY_PLANT = "first_plant"
        const val KEY_DURATION = "first_duration"
    }
}
