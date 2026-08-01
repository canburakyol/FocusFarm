package com.focusfarm.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GardenEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: GardenEntryEntity)

    @Transaction
    @Query("SELECT * FROM garden_entries ORDER BY plantedAt DESC")
    fun observeAllWithSpecies(): Flow<List<GardenEntryWithSpecies>>

    @Transaction
    @Query("SELECT * FROM garden_entries WHERE plantedAt >= :sinceEpochMs ORDER BY plantedAt DESC")
    fun observeSinceWithSpecies(sinceEpochMs: Long): Flow<List<GardenEntryWithSpecies>>

    @Transaction
    @Query(
        """SELECT * FROM garden_entries
           WHERE plantedAt >= :sinceEpochMs AND state != 'WITHERED'
           ORDER BY plantedAt DESC"""
    )
    fun observeSinceExcludingWitheredWithSpecies(sinceEpochMs: Long): Flow<List<GardenEntryWithSpecies>>

    @Transaction
    @Query("SELECT * FROM garden_entries WHERE state != 'WITHERED' ORDER BY plantedAt DESC")
    fun observeAllExcludingWitheredWithSpecies(): Flow<List<GardenEntryWithSpecies>>

    @Query("SELECT COUNT(*) FROM garden_entries WHERE state = 'BLOOMED'")
    fun observeBloomedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM garden_entries")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM garden_entries WHERE state = 'BLOOMED' AND plantedAt >= :start AND plantedAt <= :end")
    fun getGrownPlantsCountInRange(start: Long, end: Long): Flow<Int>
}
