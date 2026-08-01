package com.focusfarm.app.ui.screens.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.repository.GardenRepository
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.data.repository.StreakManager
import com.focusfarm.app.data.repository.StreakSnapshot
import com.focusfarm.app.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class DayStatus {
    ACTIVE,
    FROZEN,
    MISSED,
}

data class CalendarDay(
    val date: LocalDate,
    val status: DayStatus,
)

data class StreakUiState(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val todaySessions: Int = 0,
    val todayMinutes: Int = 0,
    val dailyGoalMinutes: Int = 10,
    val seedBalance: Int = 0,
    val freezeTokens: Int = 0,
    val isStreakAtRisk: Boolean = false,
    val freezeCandidateDate: LocalDate? = null,
    val freezeDates: Set<LocalDate> = emptySet(),
    val calendarDays: List<CalendarDay> = emptyList(),
)

sealed interface StreakIntent {
    data class UpdateDailyGoal(val minutes: Int) : StreakIntent
}

@HiltViewModel
class StreakViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val gardenRepository: GardenRepository,
    private val streakManager: StreakManager,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StreakUiState())
    val uiState: StateFlow<StreakUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                gardenRepository.getAllSessions(),
                progressRepository.observeFreezeDates(clock.zoneId),
                progressRepository.observeDailyGoalMinutes(),
                progressRepository.observeSeeds(),
                progressRepository.observeFreezeTokens()
            ) { sessions, freezeDates, dailyGoal, seeds, freezeTokens ->
                val snapshot = streakManager.getStreakSnapshot(
                    sessions = sessions,
                    freezeDates = freezeDates,
                    nowMillis = clock.nowMillis(),
                    zoneId = clock.zoneId
                )

                val today = clock.today()
                val calendarDays = (0..29).map { offset ->
                    val date = today.minusDays(offset.toLong())
                    val daySessions = sessions.filter {
                        Instant.ofEpochMilli(it.completedAt).atZone(clock.zoneId).toLocalDate() == date
                    }
                    val totalMinutes = daySessions.sumOf { it.durationMinutes }
                    val status = when {
                        freezeDates.contains(date) -> DayStatus.FROZEN
                        totalMinutes >= dailyGoal -> DayStatus.ACTIVE
                        else -> DayStatus.MISSED
                    }
                    CalendarDay(date, status)
                }.reversed()

                StreakUiState(
                    currentStreak = snapshot.currentStreak,
                    bestStreak = snapshot.bestStreak,
                    todaySessions = snapshot.todaySessions,
                    todayMinutes = snapshot.todayMinutes,
                    dailyGoalMinutes = dailyGoal,
                    seedBalance = seeds,
                    freezeTokens = freezeTokens,
                    isStreakAtRisk = snapshot.isStreakAtRisk,
                    freezeCandidateDate = snapshot.freezeCandidateDate,
                    freezeDates = freezeDates,
                    calendarDays = calendarDays
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onIntent(intent: StreakIntent) {
        when (intent) {
            is StreakIntent.UpdateDailyGoal -> updateDailyGoal(intent.minutes)
        }
    }

    private fun updateDailyGoal(minutes: Int) {
        viewModelScope.launch {
            progressRepository.updateDailyGoalMinutes(minutes)
        }
    }
}
