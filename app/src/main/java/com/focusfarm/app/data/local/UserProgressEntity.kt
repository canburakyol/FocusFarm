package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val seeds: Int = 0,
    val streakFreezeTokens: Int = 1,
    val lastFreezeGrantWeekStart: Long = 0L,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
