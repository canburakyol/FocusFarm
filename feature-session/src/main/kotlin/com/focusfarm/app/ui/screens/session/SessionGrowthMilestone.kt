package com.focusfarm.app.ui.screens.session

import androidx.annotation.StringRes
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.core.ui.R

/** Human-facing growth feedback derived from the current timer progress. */
data class SessionGrowthMilestone(
    val stage: GrowthStage,
    @StringRes val titleRes: Int,
    @StringRes val detailRes: Int,
) {
    companion object {
        fun from(progress: Float): SessionGrowthMilestone =
            when (val stage = GrowthStage.fromProgress(progress.coerceIn(0f, 1f))) {
                GrowthStage.SEED -> SessionGrowthMilestone(stage, R.string.milestone_seed_title, R.string.milestone_seed_body)
                GrowthStage.SPROUT -> SessionGrowthMilestone(stage, R.string.milestone_sprout_title, R.string.milestone_sprout_body)
                GrowthStage.GROWING -> SessionGrowthMilestone(stage, R.string.milestone_growing_title, R.string.milestone_growing_body)
                GrowthStage.MATURING -> SessionGrowthMilestone(stage, R.string.milestone_maturing_title, R.string.milestone_maturing_body)
                GrowthStage.FULL -> SessionGrowthMilestone(stage, R.string.milestone_full_title, R.string.milestone_full_body)
            }
    }
}
