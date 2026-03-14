package com.focusfarm.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Query("SELECT * FROM user_progress WHERE id = :id LIMIT 1")
    fun observeUserProgress(id: Int = UserProgressEntity.SINGLETON_ID): Flow<UserProgressEntity?>

    @Query("SELECT * FROM user_progress WHERE id = :id LIMIT 1")
    suspend fun getUserProgress(id: Int = UserProgressEntity.SINGLETON_ID): UserProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProgress(userProgress: UserProgressEntity)

    @Query("UPDATE user_progress SET seeds = seeds + :amount WHERE id = :id")
    suspend fun addSeeds(amount: Int, id: Int = UserProgressEntity.SINGLETON_ID)

    @Query("UPDATE user_progress SET streakFreezeTokens = MIN(streakFreezeTokens + :amount, :cap) WHERE id = :id")
    suspend fun addFreezeTokens(
        amount: Int,
        cap: Int,
        id: Int = UserProgressEntity.SINGLETON_ID,
    )

    @Query("UPDATE user_progress SET streakFreezeTokens = streakFreezeTokens - 1 WHERE id = :id AND streakFreezeTokens > 0")
    suspend fun consumeFreezeToken(id: Int = UserProgressEntity.SINGLETON_ID): Int

    @Query(
        """
        UPDATE user_progress
        SET lastFreezeGrantWeekStart = :weekStart,
            streakFreezeTokens = MIN(streakFreezeTokens + 1, :cap)
        WHERE id = :id AND lastFreezeGrantWeekStart < :weekStart
        """
    )
    suspend fun grantWeeklyFreezeIfNeeded(
        weekStart: Long,
        cap: Int,
        id: Int = UserProgressEntity.SINGLETON_ID,
    ): Int

    @Query("SELECT questType FROM quest_claims WHERE dateKey = :dateKey")
    fun observeClaimedQuestTypes(dateKey: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuestClaim(claim: QuestClaimEntity): Long

    @Query("SELECT COUNT(*) FROM quest_claims WHERE dateKey = :dateKey")
    suspend fun getClaimCountForDate(dateKey: String): Int

    @Query("SELECT dateKey FROM freeze_usages")
    fun observeFreezeDates(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM freeze_usages WHERE dateKey = :dateKey)")
    suspend fun isFreezeUsedForDate(dateKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFreezeUsage(freezeUsage: FreezeUsageEntity): Long

    @Query("SELECT weekKey FROM weekly_reward_claims")
    fun observeWeeklyRewardClaimKeys(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWeeklyRewardClaim(claim: WeeklyRewardClaimEntity): Long

    @Query("SELECT plantId FROM collection_reward_claims")
    fun observeCollectionRewardPlantIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollectionRewardClaim(claim: CollectionRewardClaimEntity): Long
}
