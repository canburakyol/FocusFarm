package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "freeze_usages")
data class FreezeUsageEntity(
    @PrimaryKey
    val dateKey: String, // yyyy-MM-dd
    val usedAt: Long,
)
