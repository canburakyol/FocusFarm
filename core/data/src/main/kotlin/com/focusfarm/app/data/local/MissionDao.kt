package com.focusfarm.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissions(missions: List<MissionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: List<MissionProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgressSingle(progress: MissionProgressEntity)

    @Query("SELECT * FROM missions WHERE id = :id LIMIT 1")
    suspend fun getMission(id: String): MissionEntity?

    @Query("SELECT * FROM mission_progress WHERE missionId = :id LIMIT 1")
    suspend fun getProgress(id: String): MissionProgressEntity?

    @Query("SELECT * FROM missions WHERE startsAt <= :now AND endsAt >= :now ORDER BY sortOrder ASC")
    fun observeActiveMissions(now: Long): Flow<List<MissionEntity>>

    @Query("SELECT * FROM mission_progress WHERE missionId IN (:ids)")
    fun observeProgressForMissions(ids: List<String>): Flow<List<MissionProgressEntity>>

    @Query("UPDATE mission_progress SET claimedAt = :claimedAt WHERE missionId = :id")
    suspend fun markClaimed(id: String, claimedAt: Long): Int

    @Query("UPDATE user_progress SET dailyGoalMinutes = :minutes WHERE id = :id")
    suspend fun updateDailyGoalMinutes(minutes: Int, id: Int = UserProgressEntity.SINGLETON_ID): Int
}
