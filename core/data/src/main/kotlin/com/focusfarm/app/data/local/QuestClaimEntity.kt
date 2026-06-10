package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "quest_claims",
    primaryKeys = ["dateKey", "questType"],
    indices = [Index(value = ["dateKey"])],
)
data class QuestClaimEntity(
    val dateKey: String, // yyyy-MM-dd
    val questType: String,
    val rewardSeeds: Int,
    val claimedAt: Long,
)
