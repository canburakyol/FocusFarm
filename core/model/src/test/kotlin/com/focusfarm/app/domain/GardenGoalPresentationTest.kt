package com.focusfarm.app.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GardenGoalPresentationTest {
    @Test
    fun emptyGardenGuidesTheFirstFocusSessionInsteadOfShowingADeadEnd() {
        val goal = GardenGoalPresentation.from(
            hasPlants = false,
            seedBalance = 0,
            unlockedPlantIds = emptySet(),
        )

        assertThat(goal.title).isEqualTo("İlk bitkini yetiştir")
        assertThat(goal.detail).contains("odak seansı")
        assertThat(goal.progress).isEqualTo(0f)
    }

    @Test
    fun establishedGardenShowsTheNextAffordablePlantTarget() {
        val goal = GardenGoalPresentation.from(
            hasPlants = true,
            seedBalance = 15,
            unlockedPlantIds = emptySet(),
        )

        assertThat(goal.title).isEqualTo("Sonraki keşif: Havuç")
        assertThat(goal.detail).isEqualTo("15 Seed daha kazan ve bahçene ekle.")
        assertThat(goal.progress).isEqualTo(0.5f)
    }
}
