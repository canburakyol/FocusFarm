package com.focusfarm.app.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlantAvailabilityTest {
    private fun ids(plants: List<Plant>) = plants.map { it.id }.toSet()

    @Test
    fun freeUserSeesOnlyFreePlants() {
        assertThat(ids(PlantCatalog.availableFor(emptySet(), hasPremium = false)))
            .isEqualTo(ids(PlantCatalog.FREE))
    }

    @Test
    fun seedUnlockedPlantBecomesAvailable() {
        val available = ids(PlantCatalog.availableFor(setOf("bonsai"), hasPremium = false))
        assertThat(available).contains("bonsai")
        assertThat(available).doesNotContain("lotus")
    }

    @Test
    fun premiumSubscriberSeesEveryPlant() {
        assertThat(ids(PlantCatalog.availableFor(emptySet(), hasPremium = true)))
            .isEqualTo(ids(PlantCatalog.ALL))
    }

    @Test
    fun shopSellsOnlyPlantsThatAreLockedByDefault() {
        assertThat(PlantCatalog.SEED_SHOP_IDS).isNotEmpty()
        assertThat(PlantCatalog.SEED_SHOP_IDS.intersect(ids(PlantCatalog.FREE))).isEmpty()
    }
}
