package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlocked_seeds")
data class UnlockedSeedEntity(
    @PrimaryKey val seedId: String,
    val unlockedAtEpochMs: Long,
)
