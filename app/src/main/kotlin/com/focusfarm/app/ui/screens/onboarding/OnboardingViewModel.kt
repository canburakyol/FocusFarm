package com.focusfarm.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.repository.OnboardingPreferences
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.domain.PlantCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: Int = 0,
    val selectedDurationMinutes: Int = 25,
    val selectedDailyGoalMinutes: Int = 60,
    val isStarting: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: OnboardingPreferences,
    private val progressRepository: ProgressRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state = _state.asStateFlow()

    private val _completed = Channel<Unit>(Channel.BUFFERED)
    val completed = _completed.receiveAsFlow()

    fun selectDuration(minutes: Int) {
        _state.update { it.copy(selectedDurationMinutes = minutes) }
    }

    fun selectDailyGoal(minutes: Int) {
        _state.update { it.copy(selectedDailyGoalMinutes = minutes) }
    }

    fun nextStep() {
        _state.update {
            if (it.step < 3) {
                it.copy(step = it.step + 1)
            } else {
                it
            }
        }
    }

    fun back() {
        _state.update {
            it.copy(step = (it.step - 1).coerceAtLeast(0))
        }
    }

    fun completeOnboarding() {
        val current = _state.value
        _state.update { it.copy(isStarting = true) }
        viewModelScope.launch {
            try {
                // Save progress daily goal
                progressRepository.updateDailyGoalMinutes(current.selectedDailyGoalMinutes)
                // Complete preferences
                preferences.complete(
                    focusGoal = "GENERAL",
                    plantId = PlantCatalog.FREE.first().id,
                    durationMinutes = current.selectedDurationMinutes
                )
                _completed.send(Unit)
            } finally {
                _state.update { it.copy(isStarting = false) }
            }
        }
    }
}
