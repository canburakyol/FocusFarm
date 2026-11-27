package com.focusfarm.app.ui.screens.session

import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.core.ui.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionGrowthMilestoneTest {
    @Test
    fun quarterProgressCelebratesTheFirstSprout() {
        val milestone = SessionGrowthMilestone.from(0.25f)

        assertThat(milestone.stage).isEqualTo(GrowthStage.SPROUT)
        assertThat(milestone.titleRes).isEqualTo(R.string.milestone_sprout_title)
        assertThat(milestone.detailRes).isEqualTo(R.string.milestone_sprout_body)
    }

    @Test
    fun threeQuarterProgressShowsTheMaturingPlant() {
        val milestone = SessionGrowthMilestone.from(0.75f)

        assertThat(milestone.stage).isEqualTo(GrowthStage.MATURING)
        assertThat(milestone.titleRes).isEqualTo(R.string.milestone_maturing_title)
        assertThat(milestone.detailRes).isEqualTo(R.string.milestone_maturing_body)
    }
}
