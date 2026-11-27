package com.focusfarm.app.data.repository

import com.focusfarm.app.data.local.InventoryDao
import com.focusfarm.app.data.local.InventoryItemEntity
import com.focusfarm.app.data.local.UnlockedSeedDao
import com.focusfarm.app.data.local.UnlockedSeedEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UnlockedPlantsRepositoryTest {
    @Test
    fun seedsBoughtInTheShopCountAsUnlockedPlants() = runTest {
        val repository = UnlockedPlantsRepository(
            unlockedSeedDao = FakeUnlockedSeedDao(listOf("cherry")),
            inventoryDao = FakeInventoryDao(listOf(item("PLANT_SEED", "bonsai"))),
        )

        assertThat(repository.observeUnlockedPlantIds().first()).containsExactly("cherry", "bonsai")
    }

    @Test
    fun nonPlantInventoryItemsAreIgnored() = runTest {
        val repository = UnlockedPlantsRepository(
            unlockedSeedDao = FakeUnlockedSeedDao(emptyList()),
            inventoryDao = FakeInventoryDao(listOf(item("POT", "pot_clay"))),
        )

        assertThat(repository.observeUnlockedPlantIds().first()).isEmpty()
    }

    private fun item(type: String, id: String) =
        InventoryItemEntity(id = "inv-$type-$id", userId = "user_0", itemType = type, itemId = id, purchasedAt = 0L, quantity = 1)
}

private class FakeUnlockedSeedDao(private val ids: List<String>) : UnlockedSeedDao {
    override suspend fun isUnlocked(seedId: String) = seedId in ids
    override suspend fun unlock(entity: UnlockedSeedEntity) = 1L
    override fun observeUnlockedSeedIds() = flowOf(ids)
}

private class FakeInventoryDao(private val items: List<InventoryItemEntity>) : InventoryDao {
    override suspend fun insertItem(item: InventoryItemEntity) = 1L
    override suspend fun hasItem(itemType: String, itemId: String) = items.any { it.itemType == itemType && it.itemId == itemId }
    override fun observeAllItems(userId: String) = flowOf(items)
    override fun observeItemsByType(itemType: String, userId: String) = flowOf(items.filter { it.itemType == itemType })
    override suspend fun getAllItemsSnapshot(userId: String) = items
}
