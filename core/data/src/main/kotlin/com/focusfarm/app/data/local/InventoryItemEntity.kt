package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_items",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["itemType", "itemId"], unique = true),
    ],
)
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val itemType: String, // e.g. "PLANT_SEED", "SOIL", "POT", "BACKGROUND", "AMBIENT_SOUND", "SEASONAL_DECORATION"
    val itemId: String,
    val purchasedAt: Long,
    val quantity: Int = 1,
)
