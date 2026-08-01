package com.focusfarm.app.data.repository

import com.focusfarm.app.domain.DailyQuestProgress
import com.focusfarm.app.domain.DailyQuestType
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

data class QuestRerollState(
    val dateKey: String,
    val questType: DailyQuestType,
    val target: Int,
    val rewardSeeds: Int,
)

data class SessionRewardGrant(
    val grantedSeeds: Int,
    val baseSeeds: Int,
    val wasBoosted: Boolean,
)

interface ProgressRepository {
    fun observeSeeds(): Flow<Int>
    fun observeFreezeTokens(): Flow<Int>
    fun observeNextSessionDoubleSeedBoostActive(): Flow<Boolean>
    fun observeClaimedQuestTypes(dateKey: String): Flow<Set<DailyQuestType>>
    fun observeQuestRerollState(dateKey: String): Flow<QuestRerollState?>
    fun observeFreezeDates(zoneId: ZoneId = ZoneId.systemDefault()): Flow<Set<LocalDate>>
    fun observeClaimedWeeklyRewardKeys(): Flow<Set<String>>
    fun observeClaimedCollectionRewardPlantIds(): Flow<Set<String>>
    suspend fun ensureInitializedAndGrantWeeklyFreeze(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    )
    suspend fun claimQuest(
        dateKey: String,
        quest: DailyQuestProgress,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean
    suspend fun useFreezeForDate(
        dateKey: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean
    suspend fun claimWeeklyReward(
        weekKey: String,
        rewardSeeds: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean
    suspend fun claimCollectionReward(
        plantId: String,
        rewardSeeds: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean
    suspend fun saveQuestRerollState(
        dateKey: String,
        questType: DailyQuestType,
        target: Int,
        rewardSeeds: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean
    suspend fun armNextSessionDoubleSeedBoost(
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean
    suspend fun grantSessionCompletionReward(
        sessionId: Long,
        durationMinutes: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): SessionRewardGrant
    suspend fun ensureReady(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    )

    // Transaction-specific helpers
    suspend fun ensureUserProgressRowInTransaction(nowMillis: Long, zoneId: ZoneId = ZoneId.systemDefault())
    suspend fun grantSessionCompletionRewardInTransaction(sessionId: Long, durationMinutes: Int, nowMillis: Long): SessionRewardGrant

    // Daily Goal & Missions
    fun observeDailyGoalMinutes(): Flow<Int>
    suspend fun updateDailyGoalMinutes(minutes: Int)
    fun observeActiveMissions(nowMillis: Long = System.currentTimeMillis()): Flow<List<com.focusfarm.app.domain.Mission>>
    fun observeMissionProgress(missionIds: List<String>): Flow<List<com.focusfarm.app.domain.MissionProgress>>
    suspend fun ensureMissionsForDate(dateKey: String, nowMillis: Long = System.currentTimeMillis())
    suspend fun rerollMissionsForDate(dateKey: String, nowMillis: Long = System.currentTimeMillis())
    suspend fun updateMissionProgress(dateKey: String, nowMillis: Long = System.currentTimeMillis())
    suspend fun claimMissionReward(missionId: String, nowMillis: Long = System.currentTimeMillis()): Int
}
