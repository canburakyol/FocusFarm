package com.focusfarm.app.ads

sealed interface RewardedAdState {
    object Idle : RewardedAdState
    object Loading : RewardedAdState
    object Ready : RewardedAdState
    object Showing : RewardedAdState
    object Completed : RewardedAdState
    data class Error(val message: String) : RewardedAdState
}
