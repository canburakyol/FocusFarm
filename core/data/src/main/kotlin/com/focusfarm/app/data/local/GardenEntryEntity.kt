package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "garden_entries",
    foreignKeys = [
        ForeignKey(
            entity = PlantSpeciesEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantSpeciesId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["plantSpeciesId"]),
        Index(value = ["plantedAt"]),
        Index(value = ["sessionId"]),
        Index(value = ["state"]),
    ],
)
data class GardenEntryEntity(
    @PrimaryKey val id: String,
    val sessionId: Long,
    val plantSpeciesId: String,
    val state: String, // GardenPlantState.name
    val plantedAt: Long, // epoch millis
    val focusMinutes: Int,
    val seedsEarned: Int,
    val tagId: String,
    val growthStageReached: Int, // ordinal 0..4
)
