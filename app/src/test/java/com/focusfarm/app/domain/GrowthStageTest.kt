package com.focusfarm.app.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GrowthStageTest {

    @Test
    fun `fromProgress maps ranges correctly`() {
        assertThat(GrowthStage.fromProgress(0f)).isEqualTo(GrowthStage.SEED)
        assertThat(GrowthStage.fromProgress(0.24f)).isEqualTo(GrowthStage.SEED)
        assertThat(GrowthStage.fromProgress(0.25f)).isEqualTo(GrowthStage.SPROUT)
        assertThat(GrowthStage.fromProgress(0.5f)).isEqualTo(GrowthStage.GROWING)
        assertThat(GrowthStage.fromProgress(0.75f)).isEqualTo(GrowthStage.MATURING)
        assertThat(GrowthStage.fromProgress(1f)).isEqualTo(GrowthStage.FULL)
        assertThat(GrowthStage.fromProgress(1.2f)).isEqualTo(GrowthStage.FULL)
    }
}
