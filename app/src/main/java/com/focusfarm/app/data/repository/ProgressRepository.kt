package com.focusfarm.app.data.repository

import android.content.Context
import androidx.core.content.edit
import androidx.room.withTransaction
import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.data.local.CollectionRewardClaimEntity
import com.focusfarm.app.data.local.FreezeUsageEntity
import com.focusfarm.app.data.local.ProgressDao
import com.focusfarm.app.data.local.QuestClaimEntity
import com.focusfarm.app.data.local.UserProgressEntity
import com.focusfarm.app.data.local.WeeklyRewardClaimEntity
import com.focusfarm.app.domain.DailyQuestProgress
import com.focusfarm.app.domain.DailyQuestType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

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

@Singleton
class ProgressRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val database: AppDatabase,
    private val progressDao: ProgressDao,
) {
    private val adRewardPrefs = context.getSharedPreferences(PREFS_AD_REWARD_STATE, Context.MODE_PRIVATE)
    private val prefsLock = Any()

    fun observeSeeds(): Flow<Int> =
        progressDao.observeUserProgress().map { it?.seeds ?: 0 }

    fun observeFreezeTokens(): Flow<Int> =
        progressDao.observeUserProgress().map { it?.streakFreezeTokens ?: DEFAULT_FREEZE_TOKENS }

    fun observeClaimedQuestTypes(dateKey: String): Flow<Set<DailyQuestType>> =
        progressDao.observeClaimedQuestTypes(dateKey).map { raw ->
            raw.mapNotNull { value -> value.toDailyQuestTypeOrNull() }.toSet()
        }

    fun observeFreezeDates(zoneId: ZoneId = ZoneId.systemDefault()): Flow<Set<LocalDate>> =
        progressDao.observeFreezeDates().map { raw ->
            raw.mapNotNull { dateKey -> dateKey.toLocalDateOrNull(zoneId) }.toSet()
        }

    fun observeClaimedWeeklyRewardKeys(): Flow<Set<String>> =
        progressDao.observeWeeklyRewardClaimKeys().map { it.toSet() }

    fun observeClaimedCollectionRewardPlantIds(): Flow<Set<String>> =
        progressDao.observeCollectionRewardPlantIds().map { it.toSet() }

    suspend fun ensureInitializedAndGrantWeeklyFreeze(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        database.withTransaction {
            ensureUserProgressRow(nowMillis = nowMillis, zoneId = zoneId)
            val weekStart = startOfWeekMillis(nowMillis, zoneId)
            progressDao.grantWeeklyFreezeIfNeeded(
                weekStart = weekStart,
                cap = MAX_FREEZE_TOKENS,
            )
        }
    }

    suspend fun claimQuest(
        dateKey: String,
        quest: DailyQuestProgress,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean = database.withTransaction {
        ensureUserProgressRow(nowMillis = nowMillis)
        if (!quest.isCompleted) return@withTransaction false

        val inserted = progressDao.insertQuestClaim(
            QuestClaimEntity(
                dateKey = dateKey,
                questType = quest.type.name,
                rewardSeeds = quest.rewardSeeds,
                claimedAt = nowMillis,
            )
        )
        if (inserted == -1L) return@withTransaction false

        progressDao.addSeeds(quest.rewardSeeds)

        val claimedCount = progressDao.getClaimCountForDate(dateKey)
        if (claimedCount == QUESTS_PER_DAY) {
            progressDao.addFreezeTokens(amount = 1, cap = MAX_FREEZE_TOKENS)
        }

        true
    }

    suspend fun useFreezeForDate(
        dateKey: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean = database.withTransaction {
        ensureUserProgressRow(nowMillis = nowMillis)

        val alreadyUsed = progressDao.isFreezeUsedForDate(dateKey)
        if (alreadyUsed) return@withTransaction false

        val consumed = progressDao.consumeFreezeToken()
        if (consumed <= 0) return@withTransaction false

        val inserted = progressDao.insertFreezeUsage(
            FreezeUsageEntity(
                dateKey = dateKey,
                usedAt = nowMillis,
            )
        )
        if (inserted == -1L) {
            progressDao.addFreezeTokens(amount = 1, cap = MAX_FREEZE_TOKENS)
            return@withTransaction false
        }
        true
    }

    suspend fun claimWeeklyReward(
        weekKey: String,
        rewardSeeds: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean = database.withTransaction {
        ensureUserProgressRow(nowMillis = nowMillis)

        val inserted = progressDao.insertWeeklyRewardClaim(
            WeeklyRewardClaimEntity(
                weekKey = weekKey,
                rewardSeeds = rewardSeeds,
                claimedAt = nowMillis,
            )
        )
        if (inserted == -1L) return@withTransaction false

        progressDao.addSeeds(rewardSeeds)
        true
    }

    suspend fun claimCollectionReward(
        plantId: String,
        rewardSeeds: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean = database.withTransaction {
        ensureUserProgressRow(nowMillis = nowMillis)

        val inserted = progressDao.insertCollectionRewardClaim(
            CollectionRewardClaimEntity(
                plantId = plantId,
                rewardSeeds = rewardSeeds,
                claimedAt = nowMillis,
            )
        )
        if (inserted == -1L) return@withTransaction false

        progressDao.addSeeds(rewardSeeds)
        true
    }

    fun getQuestRerollState(dateKey: String): QuestRerollState? = synchronized(prefsLock) {
        val savedDateKey = adRewardPrefs.getString(KEY_REROLL_DATE_KEY, null) ?: return@synchronized null
        if (savedDateKey != dateKey) return@synchronized null

        val typeRaw = adRewardPrefs.getString(KEY_REROLL_QUEST_TYPE, null) ?: return@synchronized null
        val type = typeRaw.toDailyQuestTypeOrNull() ?: return@synchronized null
        val target = adRewardPrefs.getInt(KEY_REROLL_TARGET, -1)
        val rewardSeeds = adRewardPrefs.getInt(KEY_REROLL_REWARD_SEEDS, -1)
        if (target <= 0 || rewardSeeds <= 0) return@synchronized null

        QuestRerollState(
            dateKey = savedDateKey,
            questType = type,
            target = target,
            rewardSeeds = rewardSeeds,
        )
    }

    suspend fun saveQuestRerollState(
        dateKey: String,
        questType: DailyQuestType,
        target: Int,
        rewardSeeds: Int,
    ): Boolean = synchronized(prefsLock) {
        val alreadyUsedToday = adRewardPrefs.getString(KEY_REROLL_DATE_KEY, null) == dateKey
        if (alreadyUsedToday) return@synchronized false
        if (target <= 0 || rewardSeeds <= 0) return@synchronized false

        adRewardPrefs.edit {
            putString(KEY_REROLL_DATE_KEY, dateKey)
            putString(KEY_REROLL_QUEST_TYPE, questType.name)
            putInt(KEY_REROLL_TARGET, target)
            putInt(KEY_REROLL_REWARD_SEEDS, rewardSeeds)
        }
        true
    }

    fun isNextSessionDoubleSeedBoostActive(): Boolean = synchronized(prefsLock) {
        adRewardPrefs.getBoolean(KEY_NEXT_SESSION_DOUBLE_SEED_ACTIVE, false)
    }

    suspend fun armNextSessionDoubleSeedBoost(): Boolean = synchronized(prefsLock) {
        val alreadyArmed = adRewardPrefs.getBoolean(KEY_NEXT_SESSION_DOUBLE_SEED_ACTIVE, false)
        if (alreadyArmed) return@synchronized false

        adRewardPrefs.edit {
            putBoolean(KEY_NEXT_SESSION_DOUBLE_SEED_ACTIVE, true)
        }
        true
    }

    suspend fun grantSessionCompletionReward(
        durationMinutes: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): SessionRewardGrant = database.withTransaction {
        ensureUserProgressRow(nowMillis = nowMillis)
        val baseSeeds = calculateSessionSeedReward(durationMinutes)
        val wasBoosted = consumeNextSessionDoubleSeedBoostIfActive()
        val grantedSeeds = if (wasBoosted) baseSeeds * 2 else baseSeeds
        progressDao.addSeeds(grantedSeeds)

        SessionRewardGrant(
            grantedSeeds = grantedSeeds,
            baseSeeds = baseSeeds,
            wasBoosted = wasBoosted,
        )
    }

    private suspend fun ensureUserProgressRow(
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        val existing = progressDao.getUserProgress()
        if (existing != null) return

        progressDao.upsertUserProgress(
            UserProgressEntity(
                id = UserProgressEntity.SINGLETON_ID,
                seeds = 0,
                streakFreezeTokens = DEFAULT_FREEZE_TOKENS,
                lastFreezeGrantWeekStart = startOfWeekMillis(nowMillis, zoneId),
            )
        )
    }

    private fun startOfWeekMillis(
        nowMillis: Long,
        zoneId: ZoneId,
    ): Long {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        return monday.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    private fun consumeNextSessionDoubleSeedBoostIfActive(): Boolean = synchronized(prefsLock) {
        val active = adRewardPrefs.getBoolean(KEY_NEXT_SESSION_DOUBLE_SEED_ACTIVE, false)
        if (active) {
            adRewardPrefs.edit {
                putBoolean(KEY_NEXT_SESSION_DOUBLE_SEED_ACTIVE, false)
            }
        }
        active
    }

    private fun calculateSessionSeedReward(durationMinutes: Int): Int {
        val normalizedMinutes = durationMinutes.coerceAtLeast(1)
        return (normalizedMinutes * SESSION_SEED_PER_MINUTE)
            .roundToInt()
            .coerceIn(MIN_SESSION_SEED_REWARD, MAX_SESSION_SEED_REWARD)
    }

    private companion object {
        const val PREFS_AD_REWARD_STATE = "ad_reward_state"
        const val KEY_REROLL_DATE_KEY = "quest_reroll_date_key"
        const val KEY_REROLL_QUEST_TYPE = "quest_reroll_type"
        const val KEY_REROLL_TARGET = "quest_reroll_target"
        const val KEY_REROLL_REWARD_SEEDS = "quest_reroll_reward_seeds"
        const val KEY_NEXT_SESSION_DOUBLE_SEED_ACTIVE = "next_session_double_seed_active"

        const val SESSION_SEED_PER_MINUTE = 0.6f
        const val MIN_SESSION_SEED_REWARD = 6
        const val MAX_SESSION_SEED_REWARD = 60

        const val DEFAULT_FREEZE_TOKENS = 1
        const val QUESTS_PER_DAY = 3
        const val MAX_FREEZE_TOKENS = 3
    }
}

private fun String.toDailyQuestTypeOrNull(): DailyQuestType? = try {
    DailyQuestType.valueOf(this)
} catch (_: IllegalArgumentException) {
    null
}

private fun String.toLocalDateOrNull(zoneId: ZoneId): LocalDate? = try {
    LocalDate.parse(this)
} catch (_: Exception) {
    try {
        Instant.ofEpochMilli(this.toLong()).atZone(zoneId).toLocalDate()
    } catch (_: Exception) {
        null
    }
}
