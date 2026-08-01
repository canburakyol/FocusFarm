package com.focusfarm.app.data.repository

import com.focusfarm.app.domain.DailyQuestType
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import javax.inject.Inject

class ClaimRewardedAdRewardUseCase @Inject constructor(
    private val rewardGrantRepository: RewardGrantRepository,
    private val progressRepository: ProgressRepository,
) {
    suspend operator fun invoke(
        placement: String,
        sourceId: String,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        return when (placement) {
            "daily_seed_bonus" -> {
                val dateKey = LocalDate.ofInstant(
                    Instant.ofEpochMilli(nowMillis),
                    zoneId
                ).toString()
                val uniqueSourceId = "daily_seed_bonus-$dateKey"
                rewardGrantRepository.grantReward(
                    transactionId = "ad-reward-$uniqueSourceId",
                    placement = placement,
                    sourceId = uniqueSourceId,
                    amount = 50,
                    nowMillis = nowMillis
                )
            }
            "shop_seed_pack" -> {
                rewardGrantRepository.grantReward(
                    transactionId = "ad-reward-shop-$sourceId",
                    placement = placement,
                    sourceId = "shop-$sourceId",
                    amount = 25,
                    nowMillis = nowMillis
                )
            }
            "session_recovery" -> {
                rewardGrantRepository.grantReward(
                    transactionId = "ad-reward-revive-$sourceId",
                    placement = placement,
                    sourceId = "revive-$sourceId",
                    amount = 0,
                    nowMillis = nowMillis
                )
            }
            "quest_reroll" -> {
                val dateKey = LocalDate.ofInstant(
                    Instant.ofEpochMilli(nowMillis),
                    zoneId
                ).toString()
                val uniqueSourceId = "quest-reroll-$dateKey"
                val success = rewardGrantRepository.grantReward(
                    transactionId = "ad-reward-$uniqueSourceId",
                    placement = placement,
                    sourceId = uniqueSourceId,
                    amount = 0,
                    nowMillis = nowMillis
                )
                if (success) {
                    val randomQuestType = DailyQuestType.values().random()
                    val randomTarget = when (randomQuestType) {
                        DailyQuestType.COMPLETE_SESSIONS -> (2..4).random()
                        DailyQuestType.FOCUS_MINUTES -> listOf(30, 45, 60, 90).random()
                        DailyQuestType.DEEP_SESSION -> listOf(25, 30, 45, 60).random()
                    }
                    val randomReward = randomTarget * 2
                    progressRepository.saveQuestRerollState(
                        dateKey = dateKey,
                        questType = randomQuestType,
                        target = randomTarget,
                        rewardSeeds = randomReward,
                        nowMillis = nowMillis
                    )
                    progressRepository.rerollMissionsForDate(dateKey, nowMillis)
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }
}
