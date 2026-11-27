package com.focusfarm.app.data.local

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relation that joins a [GardenEntryEntity] with its [PlantSpeciesEntity].
 */
data class GardenEntryWithSpecies(
    @Embedded val entry: GardenEntryEntity,
    @Relation(
        parentColumn = "plantSpeciesId",
        entityColumn = "id",
    )
    val species: PlantSpeciesEntity,
)
