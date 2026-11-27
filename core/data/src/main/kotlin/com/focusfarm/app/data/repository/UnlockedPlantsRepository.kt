package com.focusfarm.app.data.repository

import com.focusfarm.app.data.local.InventoryDao
import com.focusfarm.app.data.local.UnlockedSeedDao
import com.focusfarm.app.domain.ShopCategory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Plants the user owns. Seed purchases were recorded only as shop inventory while the home screen
 * read `unlocked_seeds`, so both sources count; this also returns plants bought in older versions.
 */
@Singleton
class UnlockedPlantsRepository @Inject constructor(
    private val unlockedSeedDao: UnlockedSeedDao,
    private val inventoryDao: InventoryDao,
) {
    fun observeUnlockedPlantIds(): Flow<Set<String>> = combine(
        unlockedSeedDao.observeUnlockedSeedIds(),
        inventoryDao.observeItemsByType(ShopCategory.PLANT_SEED.name),
    ) { unlockedSeeds, purchasedSeeds ->
        unlockedSeeds.toSet() + purchasedSeeds.map { it.itemId }
    }
}
