package com.focusfarm.app.monetization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class BannerAdViewModel @Inject constructor(
    monetizationRepository: MonetizationRepository,
) : ViewModel() {
    val canShowAds: StateFlow<Boolean> = monetizationRepository.canShowAds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )
}
