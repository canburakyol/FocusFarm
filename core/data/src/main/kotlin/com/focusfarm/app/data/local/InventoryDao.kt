package com.focusfarm.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: InventoryItemEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM inventory_items WHERE itemType = :itemType AND itemId = :itemId)")
    suspend fun hasItem(itemType: String, itemId: String): Boolean

    @Query("SELECT * FROM inventory_items WHERE userId = :userId")
    fun observeAllItems(userId: String = "user_0"): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE itemType = :itemType AND userId = :userId")
    fun observeItemsByType(itemType: String, userId: String = "user_0"): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE userId = :userId")
    suspend fun getAllItemsSnapshot(userId: String = "user_0"): List<InventoryItemEntity>
}
