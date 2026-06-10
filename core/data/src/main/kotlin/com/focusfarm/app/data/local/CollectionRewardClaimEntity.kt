package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collection_reward_claims")
data class CollectionRewardClaimEntity(
    @PrimaryKey
    val plantId: String,
    val rewardSeeds: Int,
    val claimedAt: Long,
)
