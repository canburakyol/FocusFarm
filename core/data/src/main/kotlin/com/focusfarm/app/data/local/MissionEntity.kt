package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val goalType: String,
    val goalValue: Int,
    val rewardSeeds: Int,
    val startsAt: Long,
    val endsAt: Long,
    val isPremium: Boolean,
    val sortOrder: Int,
)
