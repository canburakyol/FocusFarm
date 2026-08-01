package com.focusfarm.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserInventoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureExists(entity: UserInventoryEntity = UserInventoryEntity()): Long

    @Query("SELECT * FROM user_inventory WHERE id = 0 LIMIT 1")
    suspend fun get(): UserInventoryEntity?

    @Query("SELECT * FROM user_inventory WHERE id = 0 LIMIT 1")
    fun observe(): Flow<UserInventoryEntity?>

    @Query("UPDATE user_inventory SET waterDroplets = waterDroplets + :water, totalExperience = totalExperience + :experience WHERE id = 0")
    suspend fun grant(water: Int, experience: Int): Int

    @Query("UPDATE user_inventory SET waterDroplets = waterDroplets - :amount WHERE id = 0 AND waterDroplets >= :amount")
    suspend fun debitIfSufficient(amount: Int): Int
}
