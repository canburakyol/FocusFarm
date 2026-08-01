package com.focusfarm.app.ui.screens.session

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionRewardPresentationTest {

    @Test
    fun boostedRewardShowsBaseAndBonusSeparately() {
        val presentation = SessionRewardPresentation.from(
            grantedSeeds = 166,
            baseSeeds = 83,
            wasBoosted = true,
        )

        assertThat(presentation.baseSeeds).isEqualTo(83)
        assertThat(presentation.bonusSeeds).isEqualTo(83)
        assertThat(presentation.totalSeeds).isEqualTo(166)
        assertThat(presentation.hasBonus).isTrue()
    }

    @Test
    fun standardRewardDoesNotInventABonus() {
        val presentation = SessionRewardPresentation.from(
            grantedSeeds = 28,
            baseSeeds = 28,
            wasBoosted = false,
        )

        assertThat(presentation.baseSeeds).isEqualTo(28)
        assertThat(presentation.bonusSeeds).isEqualTo(0)
        assertThat(presentation.totalSeeds).isEqualTo(28)
        assertThat(presentation.hasBonus).isFalse()
    }
}
