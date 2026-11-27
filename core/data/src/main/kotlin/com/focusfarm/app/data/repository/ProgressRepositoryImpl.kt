package com.focusfarm.app.data.repository

import android.content.Context
import androidx.core.content.edit
import androidx.room.withTransaction
import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.data.local.CollectionRewardClaimEntity
import com.focusfarm.app.data.local.FreezeUsageEntity
import com.focusfarm.app.data.local.ProgressDao
import com.focusfarm.app.data.local.QuestClaimEntity
import com.focusfarm.app.data.local.QuestRerollEntity
import com.focusfarm.app.data.local.UserProgressEntity
import com.focusfarm.app.data.local.WeeklyRewardClaimEntity
import com.focusfarm.app.domain.DailyQuestProgress
import com.focusfarm.app.domain.DailyQuestType
import com.focusfarm.app.data.local.WalletTransactionEntity
import com.focusfarm.app.data.billing.BillingRepository
import com.focusfarm.app.domain.CalculateSessionRewardUseCase
import com.focusfarm.app.domain.TransactionType
import com.focusfarm.app.domain.FocusProgressEngine
import com.focusfarm.app.time.Clock
import com.focusfarm.app.data.local.MissionEntity
import com.focusfarm.app.data.local.MissionProgressEntity
import com.focusfarm.app.domain.Mission
import com.focusfarm.app.domain.MissionProgress
import com.focusfarm.app.domain.MissionGoalType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val database: AppDatabase,
    private val progressDao: ProgressDao,
    private val clock: Clock,
    private val billingRepository: BillingRepository,
) : ProgressRepository {
    private val calculateSessionReward = CalculateSessionRewardUseCase()
    private val adRewardPrefs = context.getSharedPreferences(PREFS_AD_REWARD_STATE, Context.MODE_PRIVATE)
    private val migrationMutex = Mutex()

    override fun observeSeeds(): Flow<Int> =
        progressDao.observeUserProgress().map { it?.seeds ?: 0 }

    override fun observeFreezeTokens(): Flow<Int> =
        progressDao.observeUserProgress().map { it?.streakFreezeTokens ?: DEFAULT_FREEZE_TOKENS }

    override fun observeNextSessionDoubleSeedBoostActive(): Flow<Boolean> =
        progressDao.observeUserProgress().map { it?.nextSessionDoubleSeedBoostActive ?: false }

    override fun observeClaimedQuestTypes(dateKey: String): Flow<Set<DailyQuestType>> =
        progressDao.observeClaimedQuestTypes(dateKey).map { raw ->
            raw.mapNotNull { value -> value.toDailyQuestTypeOrNull() }.toSet()
        }

    override fun observeQuestRerollState(dateKey: String): Flow<QuestRerollState?> =
        progressDao.observeQuestReroll(dateKey).map { entity -> entity?.toDomain() }

    override fun observeFreezeDates(zoneId: ZoneId): Flow<Set<LocalDate>> =
        progressDao.observeFreezeDates().map { raw ->
            raw.mapNotNull { dateKey -> dateKey.toLocalDateOrNull(zoneId) }.toSet()
        }

    override fun observeClaimedWeeklyRewardKeys(): Flow<Set<String>> =
        progressDao.observeWeeklyRewardClaimKeys().map { it.toSet() }

    override fun observeClaimedCollectionRewardPlantIds(): Flow<Set<String>> =
        progressDao.observeCollectionRewardPlantIds().map { it.toSet() }

    override suspend fun ensureInitializedAndGrantWeeklyFreeze(
        nowMillis: Long,
        zoneId: ZoneId,
    ) {
        ensureReady(nowMillis = nowMillis, zoneId = zoneId)
        database.withTransaction {
            ensureUserProgressRowInTransaction(nowMillis = nowMillis, zoneId = zoneId)
            val weekStart = startOfWeekMillis(nowMillis, zoneId)
            progressDao.grantWeeklyFreezeIfNeeded(
                weekStart = weekStart,
                cap = MAX_FREEZE_TOKENS,
            )
        }
    }

    override suspend fun claimQuest(
        dateKey: String,
        quest: DailyQuestProgress,
        nowMillis: Long,
    ): Boolean {
        migrateLegacyRewardStateIfNeeded(nowMillis = nowMillis, zoneId = clock.zoneId)
        return database.withTransaction {
            ensureUserProgressRowInTransaction(nowMillis = nowMillis)
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
            database.walletDao().insertTransaction(
                WalletTransactionEntity(
                    id = "tr-quest-${dateKey}-${quest.type.name}-${nowMillis}",
                    userId = "user_0",
                    type = TransactionType.MISSION_REWARD.name,
                    amount = quest.rewardSeeds,
                    sourceType = "daily_quest",
                    sourceId = quest.type.name,
                    createdAt = nowMillis,
                )
            )

            val claimedCount = progressDao.getClaimCountForDate(dateKey)
            if (claimedCount == QUESTS_PER_DAY) {
                progressDao.addFreezeTokens(amount = 1, cap = MAX_FREEZE_TOKENS)
            }

            true
        }
    }

    override suspend fun useFreezeForDate(
        dateKey: String,
        nowMillis: Long,
    ): Boolean {
        migrateLegacyRewardStateIfNeeded(nowMillis = nowMillis, zoneId = clock.zoneId)
        return database.withTransaction {
            ensureUserProgressRowInTransaction(nowMillis = nowMillis)

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
    }

    override suspend fun claimWeeklyReward(
        weekKey: String,
        rewardSeeds: Int,
        nowMillis: Long,
    ): Boolean {
        migrateLegacyRewardStateIfNeeded(nowMillis = nowMillis, zoneId = clock.zoneId)
        return database.withTransaction {
            ensureUserProgressRowInTransaction(nowMillis = nowMillis)

            val inserted = progressDao.insertWeeklyRewardClaim(
                WeeklyRewardClaimEntity(
                    weekKey = weekKey,
                    rewardSeeds = rewardSeeds,
                    claimedAt = nowMillis,
                )
            )
            if (inserted == -1L) return@withTransaction false

            progressDao.addSeeds(rewardSeeds)
            database.walletDao().insertTransaction(
                WalletTransactionEntity(
                    id = "tr-weekly-${weekKey}-${nowMillis}",
                    userId = "user_0",
                    type = TransactionType.MISSION_REWARD.name,
                    amount = rewardSeeds,
                    sourceType = "weekly_reward",
                    sourceId = weekKey,
                    createdAt = nowMillis,
                )
            )
            true
        }
    }

    override suspend fun claimCollectionReward(
        plantId: String,
        rewardSeeds: Int,
        nowMillis: Long,
    ): Boolean {
        migrateLegacyRewardStateIfNeeded(nowMillis = nowMillis, zoneId = clock.zoneId)
        return database.withTransaction {
            ensureUserProgressRowInTransaction(nowMillis = nowMillis)

            val inserted = progressDao.insertCollectionRewardClaim(
                CollectionRewardClaimEntity(
                    plantId = plantId,
                    rewardSeeds = rewardSeeds,
                    claimedAt = nowMillis,
                )
            )
            if (inserted == -1L) return@withTransaction false

            progressDao.addSeeds(rewardSeeds)
            database.walletDao().insertTransaction(
                WalletTransactionEntity(
                    id = "tr-col-${plantId}-${nowMillis}",
                    userId = "user_0",
                    type = TransactionType.MISSION_REWARD.name,
                    amount = rewardSeeds,
                    sourceType = "collection_reward",
                    sourceId = plantId,
                    createdAt = nowMillis,
                )
            )
            true
        }
    }

    override suspend fun saveQuestRerollState(
        dateKey: String,
        questType: DailyQuestType,
        target: Int,
        rewardSeeds: Int,
        nowMillis: Long,
    ): Boolean {
        migrateLegacyRewardStateIfNeeded(nowMillis = nowMillis, zoneId = clock.zoneId)
        if (target <= 0 || rewardSeeds <= 0) return false

        return database.withTransaction {
            val inserted = progressDao.insertQuestReroll(
                QuestRerollEntity(
                    dateKey = dateKey,
                    questType = questType.name,
                    target = target,
                    rewardSeeds = rewardSeeds,
                    createdAt = nowMillis,
                )
            )
            inserted != -1L
        }
    }

    override suspend fun armNextSessionDoubleSeedBoost(
        nowMillis: Long,
    ): Boolean {
        migrateLegacyRewardStateIfNeeded(nowMillis = nowMillis, zoneId = clock.zoneId)
        return database.withTransaction {
            ensureUserProgressRowInTransaction(nowMillis = nowMillis)
            progressDao.armNextSessionDoubleSeedBoost() > 0
        }
    }

    override suspend fun grantSessionCompletionReward(
        sessionId: Long,
        durationMinutes: Int,
        nowMillis: Long,
    ): SessionRewardGrant {
        migrateLegacyRewardStateIfNeeded(nowMillis = nowMillis, zoneId = clock.zoneId)
        return database.withTransaction {
            grantSessionCompletionRewardInTransaction(
                sessionId = sessionId,
                durationMinutes = durationMinutes,
                nowMillis = nowMillis,
            )
        }
    }

    override suspend fun ensureReady(
        nowMillis: Long,
        zoneId: ZoneId,
    ) {
        migrateLegacyRewardStateIfNeeded(nowMillis = nowMillis, zoneId = zoneId)
    }

    override suspend fun grantSessionCompletionRewardInTransaction(
        sessionId: Long,
        durationMinutes: Int,
        nowMillis: Long,
    ): SessionRewardGrant {
        ensureUserProgressRowInTransaction(nowMillis = nowMillis)

        // 1. Double reward prevention
        val exists = database.walletDao().existsByTypeAndSource(
            TransactionType.SESSION_REWARD.name,
            sessionId.toString()
        )
        if (exists) {
            return SessionRewardGrant(
                grantedSeeds = 0,
                baseSeeds = 0,
                wasBoosted = false
            )
        }

        // 2. Determine first session of day
        val zoneId = clock.zoneId
        val localDate = LocalDate.ofInstant(Instant.ofEpochMilli(nowMillis), zoneId)
        val startOfDay = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val countToday = database.sessionDao().countCompletedBetween(startOfDay, nowMillis + 1000)
        val isFirstOfDay = countToday <= 1

        // 3. Determine streak count
        val todayKey = FocusProgressEngine.todayDateKey(nowMillis, zoneId)
        val yesterdayKey = FocusProgressEngine.todayDateKey(nowMillis - 24 * 60 * 60 * 1000, zoneId)
        val todayStreak = progressDao.getStreakForDate(todayKey)
        val streakCount = if (todayStreak != null) {
            todayStreak
        } else {
            val yesterdayStreak = progressDao.getStreakForDate(yesterdayKey) ?: 0
            yesterdayStreak + 1
        }

        // 4. Determine premium status
        val isPremium = billingRepository.entitlement.value.isPremiumUnlocked

        // 5. Calculate reward
        val rewardResult = calculateSessionReward(
            CalculateSessionRewardUseCase.Params(
                durationMinutes = durationMinutes,
                isFirstSessionOfDay = isFirstOfDay,
                streakCount = streakCount,
                isPremium = isPremium
            )
        )

        // 6. Handle double seed boost (existing feature)
        val wasBoosted = progressDao.consumeNextSessionDoubleSeedBoost() > 0
        val finalSeeds = if (wasBoosted) rewardResult.totalSeeds * 2 else rewardResult.totalSeeds

        // 7. Update progress seeds and record ledger transaction
        progressDao.addSeeds(finalSeeds)
        database.walletDao().insertTransaction(
            WalletTransactionEntity(
                id = "tr-session-$sessionId",
                userId = "user_0",
                type = TransactionType.SESSION_REWARD.name,
                amount = finalSeeds,
                sourceType = "session",
                sourceId = sessionId.toString(),
                createdAt = nowMillis
            )
        )

        return SessionRewardGrant(
            grantedSeeds = finalSeeds,
            baseSeeds = rewardResult.baseSeeds,
            wasBoosted = wasBoosted
        )
    }

    override suspend fun ensureUserProgressRowInTransaction(
        nowMillis: Long,
        zoneId: ZoneId,
    ) {
        val existing = progressDao.getUserProgress()
        if (existing != null) return

        progressDao.upsertUserProgress(
            UserProgressEntity(
                id = UserProgressEntity.SINGLETON_ID,
                seeds = 0,
                streakFreezeTokens = DEFAULT_FREEZE_TOKENS,
                lastFreezeGrantWeekStart = startOfWeekMillis(nowMillis, zoneId),
                nextSessionDoubleSeedBoostActive = false,
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

    private fun calculateSessionSeedReward(durationMinutes: Int): Int {
        val normalizedMinutes = durationMinutes.coerceAtLeast(1)
        return (normalizedMinutes * SESSION_SEED_PER_MINUTE)
            .roundToInt()
            .coerceIn(MIN_SESSION_SEED_REWARD, MAX_SESSION_SEED_REWARD)
    }

    private suspend fun migrateLegacyRewardStateIfNeeded(
        nowMillis: Long,
        zoneId: ZoneId,
    ) {
        migrationMutex.withLock {
            if (adRewardPrefs.getBoolean(KEY_LEGACY_STATE_MIGRATED, false)) return

            val legacyState = LegacyAdRewardState(
                isBoostActive = adRewardPrefs.getBoolean(KEY_NEXT_SESSION_DOUBLE_SEED_ACTIVE, false),
                rerollState = readLegacyQuestRerollState(),
            )

            database.withTransaction {
                ensureUserProgressRowInTransaction(nowMillis = nowMillis, zoneId = zoneId)
                if (legacyState.isBoostActive) {
                    progressDao.armNextSessionDoubleSeedBoost()
                }
                legacyState.rerollState?.let { reroll ->
                    progressDao.insertQuestReroll(
                        QuestRerollEntity(
                            dateKey = reroll.dateKey,
                            questType = reroll.questType.name,
                            target = reroll.target,
                            rewardSeeds = reroll.rewardSeeds,
                            createdAt = nowMillis,
                        )
                    )
                }
            }

            adRewardPrefs.edit {
                remove(KEY_REROLL_DATE_KEY)
                remove(KEY_REROLL_QUEST_TYPE)
                remove(KEY_REROLL_TARGET)
                remove(KEY_REROLL_REWARD_SEEDS)
                remove(KEY_NEXT_SESSION_DOUBLE_SEED_ACTIVE)
                putBoolean(KEY_LEGACY_STATE_MIGRATED, true)
            }
        }
    }

    private fun readLegacyQuestRerollState(): QuestRerollState? {
        val savedDateKey = adRewardPrefs.getString(KEY_REROLL_DATE_KEY, null) ?: return null
        val typeRaw = adRewardPrefs.getString(KEY_REROLL_QUEST_TYPE, null) ?: return null
        val type = typeRaw.toDailyQuestTypeOrNull() ?: return null
        val target = adRewardPrefs.getInt(KEY_REROLL_TARGET, -1)
        val rewardSeeds = adRewardPrefs.getInt(KEY_REROLL_REWARD_SEEDS, -1)
        if (target <= 0 || rewardSeeds <= 0) return null
        return QuestRerollState(
            dateKey = savedDateKey,
            questType = type,
            target = target,
            rewardSeeds = rewardSeeds,
        )
    }

    override fun observeDailyGoalMinutes(): Flow<Int> =
        progressDao.observeUserProgress().map { it?.dailyGoalMinutes ?: 10 }

    override suspend fun updateDailyGoalMinutes(minutes: Int) {
        database.missionDao().updateDailyGoalMinutes(minutes)
    }

    override fun observeActiveMissions(nowMillis: Long): Flow<List<Mission>> =
        database.missionDao().observeActiveMissions(nowMillis).map { list ->
            list.map { entity ->
                Mission(
                    id = entity.id,
                    title = entity.title,
                    description = entity.description,
                    goalType = MissionGoalType.valueOf(entity.goalType),
                    goalValue = entity.goalValue,
                    rewardSeeds = entity.rewardSeeds,
                    startsAt = entity.startsAt,
                    endsAt = entity.endsAt,
                    isPremium = entity.isPremium,
                    sortOrder = entity.sortOrder
                )
            }
        }

    override fun observeMissionProgress(missionIds: List<String>): Flow<List<MissionProgress>> =
        database.missionDao().observeProgressForMissions(missionIds).map { list ->
            list.map { entity ->
                MissionProgress(
                    missionId = entity.missionId,
                    currentValue = entity.currentValue,
                    completedAt = entity.completedAt,
                    claimedAt = entity.claimedAt
                )
            }
        }

    override suspend fun ensureMissionsForDate(dateKey: String, nowMillis: Long) {
        val zoneId = clock.zoneId
        val localDate = LocalDate.parse(dateKey)
        val startsAt = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endsAt = localDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L

        val sampleId = "${dateKey}_COMPLETE_SESSIONS_1"
        val existing = database.missionDao().getMission(sampleId)
        if (existing == null) {
            val missionsToInsert = listOf(
                MissionEntity(
                    id = "${dateKey}_COMPLETE_SESSIONS_1",
                    title = "Bir odaklanma oturumu tamamla",
                    description = "Bugün 1 odaklanma seansını başarıyla tamamla.",
                    goalType = MissionGoalType.COMPLETE_SESSIONS.name,
                    goalValue = 1,
                    rewardSeeds = 10,
                    startsAt = startsAt,
                    endsAt = endsAt,
                    isPremium = false,
                    sortOrder = 1
                ),
                MissionEntity(
                    id = "${dateKey}_FOCUS_MINUTES_25",
                    title = "Toplam 25 dakika odaklan",
                    description = "Bugün odaklanma seanslarında toplam 25 dakikaya ulaş.",
                    goalType = MissionGoalType.FOCUS_MINUTES.name,
                    goalValue = 25,
                    rewardSeeds = 15,
                    startsAt = startsAt,
                    endsAt = endsAt,
                    isPremium = false,
                    sortOrder = 2
                ),
                MissionEntity(
                    id = "${dateKey}_FOCUS_MINUTES_60",
                    title = "Toplam 60 dakika odaklan",
                    description = "Bugün odaklanma seanslarında toplam 60 dakikaya ulaş.",
                    goalType = MissionGoalType.FOCUS_MINUTES.name,
                    goalValue = 60,
                    rewardSeeds = 30,
                    startsAt = startsAt,
                    endsAt = endsAt,
                    isPremium = false,
                    sortOrder = 3
                ),
                MissionEntity(
                    id = "${dateKey}_WORK_TAGS_2",
                    title = "İki farklı etiketle çalış",
                    description = "Bugün en az 2 farklı odaklanma etiketi kullanarak seans tamamla.",
                    goalType = MissionGoalType.WORK_TAGS.name,
                    goalValue = 2,
                    rewardSeeds = 20,
                    startsAt = startsAt,
                    endsAt = endsAt,
                    isPremium = false,
                    sortOrder = 4
                ),
                MissionEntity(
                    id = "${dateKey}_GROW_PLANTS_1",
                    title = "Bir bitki yetiştir",
                    description = "Bugün 1 adet odaklanma oturumu bitirip bitkiyi bahçene ek.",
                    goalType = MissionGoalType.GROW_PLANTS.name,
                    goalValue = 1,
                    rewardSeeds = 15,
                    startsAt = startsAt,
                    endsAt = endsAt,
                    isPremium = false,
                    sortOrder = 5
                ),
                MissionEntity(
                    id = "${dateKey}_COMPLETE_SESSIONS_3",
                    title = "Üç oturum tamamla",
                    description = "Bugün toplam 3 odaklanma seansını başarıyla tamamla.",
                    goalType = MissionGoalType.COMPLETE_SESSIONS.name,
                    goalValue = 3,
                    rewardSeeds = 25,
                    startsAt = startsAt,
                    endsAt = endsAt,
                    isPremium = false,
                    sortOrder = 6
                )
            )
            database.missionDao().insertMissions(missionsToInsert)

            val progressToInsert = missionsToInsert.map {
                MissionProgressEntity(
                    missionId = it.id,
                    currentValue = 0,
                    completedAt = null,
                    claimedAt = null
                )
            }
            database.missionDao().insertProgress(progressToInsert)
        }
    }

    override suspend fun rerollMissionsForDate(dateKey: String, nowMillis: Long) {
        val zoneId = clock.zoneId
        val localDate = LocalDate.parse(dateKey)
        val startsAt = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endsAt = localDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L

        database.withTransaction {
            val missionsToInsert = listOf(
                MissionEntity(
                    id = "${dateKey}_COMPLETE_SESSIONS_1",
                    title = "Bir odaklanma oturumu tamamla",
                    description = "Bugün 1 odaklanma seansını başarıyla tamamla.",
                    goalType = MissionGoalType.COMPLETE_SESSIONS.name,
                    goalValue = 1,
                    rewardSeeds = (10..15).random(),
                    startsAt = startsAt,
                    endsAt = endsAt,
                    isPremium = false,
                    sortOrder = 1
                ),
                MissionEntity(
                    id = "${dateKey}_FOCUS_MINUTES_25",
                    title = "Toplam 25 dakika odaklan",
                    description = "Bugün odaklanma seanslarında toplam 25 dakikaya ulaş.",
                    goalType = MissionGoalType.FOCUS_MINUTES.name,
                    goalValue = 25,
                    rewardSeeds = (15..20).random(),
                    startsAt = startsAt,
                    endsAt = endsAt,
                    isPremium = false,
                    sortOrder = 2
                ),
                MissionEntity(
                    id = "${dateKey}_FOCUS_MINUTES_60",
                    title = "Toplam 60 dakika odaklan",
                    description = "Bugün odaklanma seanslarında toplam 60 dakikaya ulaş.",
                    goalType = MissionGoalType.FOCUS_MINUTES.name,
                    goalValue = 60,
                    rewardSeeds = (30..40).random(),
                    startsAt = startsAt,
                    endsAt = endsAt,
                    isPremium = false,
                    sortOrder = 3
                ),
                MissionEntity(
                    id = "${dateKey}_WORK_TAGS_2",
                    title = "İki farklı etiketle çalış",
                    description = "Bugün en az 2 farklı odaklanma etiketi kullanarak seans tamamla.",
                    goalType = MissionGoalType.WORK_TAGS.name,
                    goalValue = 2,
                    rewardSeeds = (20..30).random(),
                    startsAt = startsAt,
                    endsAt = endsAt,
                    isPremium = false,
                    sortOrder = 4
                ),
                MissionEntity(
                    id = "${dateKey}_GROW_PLANTS_1",
                    title = "Bir bitki yetiştir",
                    description = "Bugün 1 adet odaklanma oturumu bitirip bitkiyi bahçene ek.",
                    goalType = MissionGoalType.GROW_PLANTS.name,
                    goalValue = 1,
                    rewardSeeds = (15..25).random(),
                    startsAt = startsAt,
                    endsAt = endsAt,
                    isPremium = false,
                    sortOrder = 5
                ),
                MissionEntity(
                    id = "${dateKey}_COMPLETE_SESSIONS_3",
                    title = "Üç oturum tamamla",
                    description = "Bugün toplam 3 odaklanma seansını başarıyla tamamla.",
                    goalType = MissionGoalType.COMPLETE_SESSIONS.name,
                    goalValue = 3,
                    rewardSeeds = (25..35).random(),
                    startsAt = startsAt,
                    endsAt = endsAt,
                    isPremium = false,
                    sortOrder = 6
                )
            )

            database.missionDao().insertMissions(missionsToInsert)

            val progressToInsert = missionsToInsert.map {
                MissionProgressEntity(
                    missionId = it.id,
                    currentValue = 0,
                    completedAt = null,
                    claimedAt = null
                )
            }
            database.missionDao().insertProgress(progressToInsert)
        }
    }

    override suspend fun updateMissionProgress(dateKey: String, nowMillis: Long) {
        val zoneId = clock.zoneId
        val localDate = LocalDate.parse(dateKey)
        val startsAt = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endsAt = localDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L

        val todaySessions = database.sessionDao().getSessionsSince(startsAt)
            .first()
            .filter { it.completedAt in startsAt..endsAt }

        val todayGardenEntries = database.gardenEntryDao().observeSinceWithSpecies(startsAt)
            .first()
            .filter { it.entry.plantedAt in startsAt..endsAt && it.entry.state == "BLOOMED" }

        val completedSessionsCount = todaySessions.size
        val totalFocusMinutes = todaySessions.sumOf { it.durationMinutes }
        val uniqueTagsCount = todaySessions.mapNotNull { it.selectedTag }.distinct().size
        val grownPlantsCount = todayGardenEntries.size

        val missionGoals = listOf(
            "${dateKey}_COMPLETE_SESSIONS_1" to completedSessionsCount,
            "${dateKey}_FOCUS_MINUTES_25" to totalFocusMinutes,
            "${dateKey}_FOCUS_MINUTES_60" to totalFocusMinutes,
            "${dateKey}_WORK_TAGS_2" to uniqueTagsCount,
            "${dateKey}_GROW_PLANTS_1" to grownPlantsCount,
            "${dateKey}_COMPLETE_SESSIONS_3" to completedSessionsCount
        )

        database.withTransaction {
            missionGoals.forEach { (missionId, currentValue) ->
                val mission = database.missionDao().getMission(missionId) ?: return@forEach
                val progress = database.missionDao().getProgress(missionId) ?: MissionProgressEntity(
                    missionId = missionId,
                    currentValue = 0,
                    completedAt = null,
                    claimedAt = null
                )

                val wasCompleted = progress.completedAt != null
                val isNowCompleted = currentValue >= mission.goalValue
                val completedAt = if (isNowCompleted && !wasCompleted) nowMillis else progress.completedAt

                database.missionDao().upsertProgressSingle(
                    progress.copy(
                        currentValue = currentValue,
                        completedAt = completedAt
                    )
                )
            }
        }
    }

    override suspend fun claimMissionReward(missionId: String, nowMillis: Long): Int = database.withTransaction {
        val progress = database.missionDao().getProgress(missionId) ?: return@withTransaction 0
        if (progress.completedAt == null || progress.claimedAt != null) return@withTransaction 0

        val mission = database.missionDao().getMission(missionId) ?: return@withTransaction 0

        database.missionDao().upsertProgressSingle(
            progress.copy(claimedAt = nowMillis)
        )

        progressDao.addSeeds(mission.rewardSeeds)

        database.walletDao().insertTransaction(
            WalletTransactionEntity(
                id = "tr-mission-$missionId",
                userId = "user_0",
                type = TransactionType.MISSION_REWARD.name,
                amount = mission.rewardSeeds,
                sourceType = "mission",
                sourceId = missionId,
                createdAt = nowMillis
            )
        )

        mission.rewardSeeds
    }

    private companion object {
        const val PREFS_AD_REWARD_STATE = "ad_reward_state"
        const val KEY_REROLL_DATE_KEY = "quest_reroll_date_key"
        const val KEY_REROLL_QUEST_TYPE = "quest_reroll_type"
        const val KEY_REROLL_TARGET = "quest_reroll_target"
        const val KEY_REROLL_REWARD_SEEDS = "quest_reroll_reward_seeds"
        const val KEY_NEXT_SESSION_DOUBLE_SEED_ACTIVE = "next_session_double_seed_active"
        const val KEY_LEGACY_STATE_MIGRATED = "legacy_ad_reward_state_migrated_v1"

        const val SESSION_SEED_PER_MINUTE = 0.6f
        const val MIN_SESSION_SEED_REWARD = 6
        const val MAX_SESSION_SEED_REWARD = 60

        const val DEFAULT_FREEZE_TOKENS = 1
        const val QUESTS_PER_DAY = 3
        const val MAX_FREEZE_TOKENS = 3
    }
}

private data class LegacyAdRewardState(
    val isBoostActive: Boolean,
    val rerollState: QuestRerollState?,
)

private fun QuestRerollEntity.toDomain(): QuestRerollState? {
    val type = questType.toDailyQuestTypeOrNull() ?: return null
    return QuestRerollState(
        dateKey = dateKey,
        questType = type,
        target = target,
        rewardSeeds = rewardSeeds,
    )
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
