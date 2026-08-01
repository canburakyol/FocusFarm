package com.focusfarm.app.domain

import kotlin.math.roundToInt

enum class SeedEvolutionStage { SEED, SPROUT, GROWING, BLOOMING, THRIVING }

data class SeedEvolutionInput(
    val focusedMinutes: Int,
    val completedSessions: Int,
    val abandonedSessions: Int,
    val completedRecoverySessions: Int,
    val offeredRecoverySessions: Int,
    val consecutiveDays: Int,
)

data class SeedEvolutionState(
    val stage: SeedEvolutionStage,
    val growthPoints: Int,
    val promiseKeepingRate: Float,
    val recoveryRate: Float,
    val consistencyMultiplier: Float,
    val evolutionMultiplier: Float,
    val progressToNextStage: Float,
)

object LivingSeedEvolutionEngine {
    private val thresholds = intArrayOf(0, 60, 180, 420, 900)

    fun calculate(input: SeedEvolutionInput): SeedEvolutionState {
        val attempts = input.completedSessions + input.abandonedSessions
        val promiseRate = if (attempts == 0) 0f else input.completedSessions.toFloat() / attempts
        val recoveryRate = if (input.offeredRecoverySessions == 0) 0f
        else input.completedRecoverySessions.toFloat() / input.offeredRecoverySessions
        val consistency = when {
            input.consecutiveDays >= 7 -> 1.35f
            input.consecutiveDays >= 3 -> 1.18f
            input.consecutiveDays >= 2 -> 1.08f
            else -> 1f
        }
        val behaviorMultiplier = 0.65f + promiseRate.coerceIn(0f, 1f) * 0.25f +
            recoveryRate.coerceIn(0f, 1f) * 0.10f
        val points = (input.focusedMinutes.coerceAtLeast(0) * behaviorMultiplier * consistency)
            .roundToInt()
        val stageIndex = thresholds.indexOfLast { points >= it }.coerceAtLeast(0)
        val nextThreshold = thresholds.getOrNull(stageIndex + 1)
        val progress = if (nextThreshold == null) 1f else {
            val current = thresholds[stageIndex]
            ((points - current).toFloat() / (nextThreshold - current)).coerceIn(0f, 1f)
        }
        return SeedEvolutionState(
            stage = SeedEvolutionStage.entries[stageIndex],
            growthPoints = points,
            promiseKeepingRate = promiseRate.coerceIn(0f, 1f),
            recoveryRate = recoveryRate.coerceIn(0f, 1f),
            consistencyMultiplier = consistency,
            evolutionMultiplier = behaviorMultiplier * consistency,
            progressToNextStage = progress,
        )
    }
}
