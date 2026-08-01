package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plant_species")
data class PlantSpeciesEntity(
    @PrimaryKey val id: String,
    val slug: String,
    val displayName: String,
    val description: String,
    val rarity: String, // PlantRarity.name
    val growthStageCount: Int,
    val minimumFocusMinutes: Int,
    val seedCost: Int,
    val unlockType: String,
    val isPremium: Boolean,
    val isSeasonal: Boolean,
    val assetKey: String,
    val sortOrder: Int,
)
