package com.focusfarm.app.domain

enum class MissionGoalType {
    COMPLETE_SESSIONS,
    FOCUS_MINUTES,
    WORK_TAGS,
    GROW_PLANTS,
}

data class Mission(
    val id: String,
    val title: String,
    val description: String,
    val goalType: MissionGoalType,
    val goalValue: Int,
    val rewardSeeds: Int,
    val startsAt: Long,
    val endsAt: Long,
    val isPremium: Boolean,
    val sortOrder: Int,
)

data class MissionProgress(
    val missionId: String,
    val currentValue: Int,
    val completedAt: Long?,
    val claimedAt: Long?,
) {
    val isCompleted: Boolean get() = completedAt != null
    val isClaimed: Boolean get() = claimedAt != null
}
