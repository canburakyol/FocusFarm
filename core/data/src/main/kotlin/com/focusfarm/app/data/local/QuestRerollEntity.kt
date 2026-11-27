package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quest_rerolls",
    indices = [Index(value = ["dateKey"], unique = true)],
)
data class QuestRerollEntity(
    @PrimaryKey
    val dateKey: String,
    val questType: String,
    val target: Int,
    val rewardSeeds: Int,
    val createdAt: Long,
)
