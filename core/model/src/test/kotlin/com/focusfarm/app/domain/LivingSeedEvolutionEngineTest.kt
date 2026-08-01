package com.focusfarm.app.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LivingSeedEvolutionEngineTest {
    @Test fun `kept promises and recovery increase growth points`() {
        val weak = LivingSeedEvolutionEngine.calculate(
            SeedEvolutionInput(360, 2, 3, 0, 2, 1),
        )
        val strong = LivingSeedEvolutionEngine.calculate(
            SeedEvolutionInput(360, 5, 0, 2, 2, 7),
        )
        assertThat(strong.growthPoints).isGreaterThan(weak.growthPoints)
        assertThat(strong.stage.ordinal).isGreaterThan(weak.stage.ordinal)
        assertThat(strong.evolutionMultiplier).isGreaterThan(weak.evolutionMultiplier)
    }

    @Test fun `rates are bounded when input counts are inconsistent`() {
        val state = LivingSeedEvolutionEngine.calculate(
            SeedEvolutionInput(60, 1, 0, 4, 1, 2),
        )
        assertThat(state.recoveryRate).isEqualTo(1f)
        assertThat(state.promiseKeepingRate).isEqualTo(1f)
    }
}
