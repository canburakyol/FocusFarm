package com.focusfarm.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.repository.GardenRepository
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.data.repository.ReflectionRepository
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.domain.FocusProgressEngine
import com.focusfarm.app.domain.WeeklyChallengeProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    repository: GardenRepository,
    progressRepository: ProgressRepository,
    reflectionRepository: ReflectionRepository,
) : ViewModel() {

    val totalMinutes: StateFlow<Int> = repository
        .getTotalMinutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val sessionCount: StateFlow<Int> = repository
        .getSessionCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentSessions: StateFlow<List<FocusSession>> = repository
        .getRecentSessions(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allSessions: StateFlow<List<FocusSession>> = repository
        .getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val freezeDates = progressRepository.observeFreezeDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val streakDays: StateFlow<Int> = combine(allSessions, freezeDates) { sessions, freezes ->
        FocusProgressEngine.currentStreakDays(
            sessions = sessions,
            freezeDates = freezes,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val startOfWeek: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    val weeklySessions: StateFlow<List<FocusSession>> = repository
        .getSessionsSince(startOfWeek)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyChallenge: StateFlow<WeeklyChallengeProgress> = allSessions
        .map { sessions -> FocusProgressEngine.weeklyChallenge(sessions) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            FocusProgressEngine.weeklyChallenge(emptyList()),
        )

    private val claimedWeeklyRewardKeys = progressRepository.observeClaimedWeeklyRewardKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val isWeeklyRewardClaimed: StateFlow<Boolean> = combine(
        weeklyChallenge,
        claimedWeeklyRewardKeys,
    ) { challenge, claimedKeys ->
        claimedKeys.contains(challenge.weekKey)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val reflectionHistory = reflectionRepository.observeRecentHistory(limit = 20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
