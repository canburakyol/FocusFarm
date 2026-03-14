package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weekly_reward_claims")
data class WeeklyRewardClaimEntity(
    @PrimaryKey
    val weekKey: String, // yyyy-MM-dd monday key
    val rewardSeeds: Int,
    val claimedAt: Long,
)
