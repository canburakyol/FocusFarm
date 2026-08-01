package com.focusfarm.app.domain

/**
 * A single entry in the user's garden, created when a focus session ends.
 */
data class GardenEntry(
    val id: String,
    val sessionId: Long,
    val plantSpeciesId: String,
    val state: GardenPlantState,
    val plantedAt: Long, // epoch millis
    val focusMinutes: Int,
    val seedsEarned: Int,
    val tagId: String,
    val growthStageReached: Int, // ordinal of GrowthStage (0..4)
) {
    /** Convenience accessor for the resolved [GrowthStage]. */
    val growthStage: GrowthStage
        get() = GrowthStage.entries.getOrElse(growthStageReached) { GrowthStage.SEED }
}
