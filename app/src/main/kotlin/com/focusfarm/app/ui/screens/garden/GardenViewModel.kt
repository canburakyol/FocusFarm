package com.focusfarm.app.ui.screens.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.repository.GardenRepository
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.data.local.RecoverySessionDao
import com.focusfarm.app.data.local.UnlockedSeedDao
import com.focusfarm.app.data.repository.GardenPreferences
import com.focusfarm.app.domain.FocusProgressEngine
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.domain.GardenEntry
import com.focusfarm.app.domain.GardenFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GardenViewModel @Inject constructor(
    private val repository: GardenRepository,
    private val progressRepository: ProgressRepository,
    private val recoverySessionDao: RecoverySessionDao,
    private val unlockedSeedDao: UnlockedSeedDao,
    private val gardenPreferences: GardenPreferences,
) : ViewModel() {

    private val _activeFilter = MutableStateFlow(GardenFilter.ALL_TIME)
    val activeFilter: StateFlow<GardenFilter> = _activeFilter.asStateFlow()

    val hideWithered: StateFlow<Boolean> = gardenPreferences.hideWithered

    val seedBalance: StateFlow<Int> = progressRepository.observeSeeds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unlockedPlantIds: StateFlow<Set<String>> = unlockedSeedDao.observeUnlockedSeedIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val gardenEntries: StateFlow<List<GardenEntry>> = combine(
        _activeFilter,
        gardenPreferences.hideWithered
    ) { filter, hideWithered ->
        Pair(filter, hideWithered)
    }.flatMapLatest { (filter, hideWithered) ->
        repository.getGardenEntries(filter, hideWithered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<FocusSession>> = repository
        .getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMinutes: StateFlow<Int> = repository
        .getTotalMinutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val streakDays: StateFlow<Int> = combine(
        repository.getAllSessions(),
        progressRepository.observeFreezeDates(),
        recoverySessionDao.observeCompletedTimestamps(),
        progressRepository.observeDailyGoalMinutes(),
    ) { sessions, freezeDates, completedRecoveries, dailyGoalMinutes ->
        val recoveryDates = FocusProgressEngine.recoveryProtectionDates(
            completedRecoveryTimestamps = completedRecoveries,
            sessions = sessions,
            dailyGoalMinutes = dailyGoalMinutes,
            existingProtectedDates = freezeDates,
        )
        val protectedDates = freezeDates + recoveryDates
        FocusProgressEngine.currentStreakDays(sessions, dailyGoalMinutes, protectedDates)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setFilter(filter: GardenFilter) {
        _activeFilter.value = filter
    }

    fun setHideWithered(hide: Boolean) {
        gardenPreferences.setHideWithered(hide)
    }
}
