package com.focusfarm.app.ui.screens.session

/**
 * Keeps the completion screen honest about what was actually credited to the wallet.
 * A boost is shown only when the persisted completion result confirms it.
 */
data class SessionRewardPresentation(
    val baseSeeds: Int,
    val bonusSeeds: Int,
    val totalSeeds: Int,
) {
    val hasBonus: Boolean get() = bonusSeeds > 0

    companion object {
        fun from(
            grantedSeeds: Int,
            baseSeeds: Int,
            wasBoosted: Boolean,
        ): SessionRewardPresentation {
            val safeTotal = grantedSeeds.coerceAtLeast(0)
            val safeBase = baseSeeds.coerceIn(0, safeTotal)
            val bonus = if (wasBoosted) (safeTotal - safeBase).coerceAtLeast(0) else 0
            return SessionRewardPresentation(
                baseSeeds = safeBase,
                bonusSeeds = bonus,
                totalSeeds = safeTotal,
            )
        }
    }
}
