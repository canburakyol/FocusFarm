package com.focusfarm.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantSpeciesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(species: List<PlantSpeciesEntity>)

    @Query("SELECT * FROM plant_species ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<PlantSpeciesEntity>>

    @Query("SELECT * FROM plant_species WHERE id = :id")
    suspend fun getById(id: String): PlantSpeciesEntity?
}
