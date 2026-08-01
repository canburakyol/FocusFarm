package com.focusfarm.app.ui.screens.session

import com.google.common.truth.Truth.assertThat
import com.focusfarm.app.domain.GrowthStage
import org.junit.Test

class SessionGrowthMilestoneTest {
    @Test
    fun quarterProgressCelebratesTheFirstSprout() {
        val milestone = SessionGrowthMilestone.from(0.25f)

        assertThat(milestone.stage).isEqualTo(GrowthStage.SPROUT)
        assertThat(milestone.title).isEqualTo("İlk filiz çıktı")
        assertThat(milestone.detail).contains("%25")
    }

    @Test
    fun threeQuarterProgressShowsTheMaturingPlant() {
        val milestone = SessionGrowthMilestone.from(0.75f)

        assertThat(milestone.stage).isEqualTo(GrowthStage.MATURING)
        assertThat(milestone.title).isEqualTo("Bitkin güçleniyor")
        assertThat(milestone.detail).contains("%75")
    }
}
