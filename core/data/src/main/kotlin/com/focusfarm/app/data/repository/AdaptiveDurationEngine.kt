package com.focusfarm.app.data.repository

import com.focusfarm.app.data.local.SessionDifficulty
import com.focusfarm.app.data.local.SessionEvaluationEntity
import javax.inject.Inject

data class AdaptiveDurationInput(
    val evaluations: List<SessionEvaluationEntity>,
    val recentFailureCount: Int,
    val recoveryRequired: Boolean,
)

data class DurationRecommendation(val minutes: Int, val reason: RecommendationReason)
enum class RecommendationReason { RECOVERY_REQUIRED, REPEATED_FAILURE, LAST_SESSION_HARD, BALANCED, READY_TO_GROW }

class AdaptiveDurationEngine @Inject constructor() {
    fun recommend(input: AdaptiveDurationInput): DurationRecommendation {
        val recent = input.evaluations.take(3)
        return when {
            input.recoveryRequired -> DurationRecommendation(8, RecommendationReason.RECOVERY_REQUIRED)
            input.recentFailureCount >= 2 -> DurationRecommendation(8, RecommendationReason.REPEATED_FAILURE)
            recent.firstOrNull()?.difficulty == SessionDifficulty.HARD ->
                DurationRecommendation(15, RecommendationReason.LAST_SESSION_HARD)
            recent.count { it.difficulty == SessionDifficulty.EASY } >= 2 ->
                DurationRecommendation(25, RecommendationReason.READY_TO_GROW)
            else -> DurationRecommendation(15, RecommendationReason.BALANCED)
        }
    }
}
