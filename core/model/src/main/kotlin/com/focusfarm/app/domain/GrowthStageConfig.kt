package com.focusfarm.app.domain

/**
 * Configurable growth stage calculation based on focus duration.
 *
 * Growth stages are mapped to [GrowthStage] ordinals (0..4):
 *   0 = SEED, 1 = SPROUT, 2 = GROWING, 3 = MATURING, 4 = FULL
 *
 * The thresholds are defined here so they can be changed without touching UI code.
 */
data class GrowthStageConfig(
    /** Minimum minutes for each stage index (0..4). Index 0 is always 0. */
    val thresholds: List<Int>,
) {
    init {
        require(thresholds.size == 5) { "Exactly 5 thresholds required (one per growth stage)." }
        require(thresholds[0] == 0) { "First threshold must be 0 (SEED)." }
        require(thresholds.zipWithNext().all { (a, b) -> b > a }) {
            "Thresholds must be strictly increasing."
        }
    }

    /**
     * Returns the highest [GrowthStage] that [focusMinutes] qualifies for.
     */
    fun stageFor(focusMinutes: Int): GrowthStage {
        val stageIndex = thresholds.indexOfLast { focusMinutes >= it }
            .coerceIn(0, GrowthStage.entries.lastIndex)
        return GrowthStage.entries[stageIndex]
    }

    companion object {
        /**
         * Default production thresholds:
         *   0–4 min  → Tohum (SEED)
         *   5–14 min → Filiz (SPROUT)
         *  15–24 min → Genç bitki (GROWING)
         *  25–44 min → Olgun bitki (MATURING)
         *  45+ min   → Çiçek açmış (FULL)
         */
        val DEFAULT = GrowthStageConfig(
            thresholds = listOf(0, 5, 15, 25, 45),
        )
    }
}
