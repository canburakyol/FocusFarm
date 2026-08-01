package com.focusfarm.app.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GardenPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _hideWithered = MutableStateFlow(prefs.getBoolean(KEY_HIDE_WITHERED, false))

    val hideWithered: StateFlow<Boolean> = _hideWithered.asStateFlow()

    fun setHideWithered(hide: Boolean) {
        prefs.edit {
            putBoolean(KEY_HIDE_WITHERED, hide)
        }
        _hideWithered.value = hide
    }

    private companion object {
        const val PREFS = "garden_preferences"
        const val KEY_HIDE_WITHERED = "hide_withered"
    }
}
