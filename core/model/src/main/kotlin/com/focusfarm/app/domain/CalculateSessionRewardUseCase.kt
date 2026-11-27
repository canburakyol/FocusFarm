package com.focusfarm.app.domain

import kotlin.math.roundToInt

/**
 * Use case to calculate the seed rewards for a successful focus session.
 */
class CalculateSessionRewardUseCase {

    data class Params(
        val durationMinutes: Int,
        val isFirstSessionOfDay: Boolean,
        val streakCount: Int,
        val isPremium: Boolean,
    )

    data class Result(
        val baseSeeds: Int,
        val durationBonus: Int,
        val firstSessionBonus: Int,
        val streakBonus: Int,
        val premiumBonus: Int,
        val totalSeeds: Int,
    )

    operator fun invoke(params: Params): Result {
        val duration = params.durationMinutes.coerceAtLeast(0)

        // 1. Base reward: completed minutes (1 seed per minute)
        val base = duration

        // 2. Duration bonus:
        //    >= 45 mins: 20% bonus
        //    >= 25 mins: 10% bonus
        val durationBonus = when {
            duration >= 45 -> (base * 0.20f).roundToInt()
            duration >= 25 -> (base * 0.10f).roundToInt()
            else -> 0
        }

        // 3. Daily first session: flat bonus (50 seeds)
        val firstSessionBonus = if (params.isFirstSessionOfDay) 50 else 0

        // 4. Streak bonus: 5 seeds per streak day, capped at 25 seeds
        val streakBonus = (params.streakCount * 5).coerceAtMost(25)

        // Subtotal before premium bonus
        val subtotal = base + durationBonus + firstSessionBonus + streakBonus

        // 5. Premium bonus: 50% bonus
        val premiumBonus = if (params.isPremium) (subtotal * 0.50f).roundToInt() else 0

        val total = subtotal + premiumBonus

        return Result(
            baseSeeds = base,
            durationBonus = durationBonus,
            firstSessionBonus = firstSessionBonus,
            streakBonus = streakBonus,
            premiumBonus = premiumBonus,
            totalSeeds = total,
        )
    }
}
