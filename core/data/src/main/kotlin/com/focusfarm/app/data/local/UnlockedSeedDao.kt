package com.focusfarm.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockedSeedDao {
    @Query("SELECT EXISTS(SELECT 1 FROM unlocked_seeds WHERE seedId = :seedId)")
    suspend fun isUnlocked(seedId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlock(entity: UnlockedSeedEntity): Long

    @Query("SELECT seedId FROM unlocked_seeds")
    fun observeUnlockedSeedIds(): Flow<List<String>>
}
