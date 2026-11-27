package com.focusfarm.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecoverySessionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: RecoverySessionEntity): Long

    @Query("SELECT * FROM recovery_sessions WHERE activeSessionId = :id LIMIT 1")
    suspend fun get(id: String): RecoverySessionEntity?

    @Query("UPDATE recovery_sessions SET status = :status WHERE activeSessionId = :id")
    suspend fun updateStatus(id: String, status: RecoverySessionStatus): Int

    @Query("UPDATE recovery_sessions SET status = 'COMPLETED', completedAtEpochMs = :completedAt WHERE activeSessionId = :id AND status = 'RUNNING'")
    suspend fun markCompleted(id: String, completedAt: Long): Int

    @Query("SELECT COUNT(*) FROM recovery_sessions WHERE status = 'OFFERED' AND createdAtEpochMs >= :sinceEpochMs")
    suspend fun countOfferedSince(sinceEpochMs: Long): Int

    @Query("SELECT completedAtEpochMs FROM recovery_sessions WHERE status = 'COMPLETED' AND completedAtEpochMs >= :sinceEpochMs")
    suspend fun completedTimestampsSince(sinceEpochMs: Long): List<Long>

    @Query("SELECT completedAtEpochMs FROM recovery_sessions WHERE status = 'COMPLETED' AND completedAtEpochMs IS NOT NULL")
    fun observeCompletedTimestamps(): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM recovery_sessions WHERE status = 'COMPLETED' AND completedAtEpochMs >= :start AND completedAtEpochMs < :end")
    suspend fun countCompletedBetween(start: Long, end: Long): Int
}
