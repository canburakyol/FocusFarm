package com.focusfarm.app.ui.screens.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.repository.GardenRepository
import com.focusfarm.app.domain.FocusSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GardenViewModel @Inject constructor(
    repository: GardenRepository,
) : ViewModel() {

    val sessions: StateFlow<List<FocusSession>> = repository
        .getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMinutes: StateFlow<Int> = repository
        .getTotalMinutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
