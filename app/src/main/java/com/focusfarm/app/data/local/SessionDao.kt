package com.focusfarm.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions ORDER BY completedAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY completedAt DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<SessionEntity>>

    @Query("SELECT COUNT(*) FROM sessions")
    fun getSessionCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM sessions")
    fun getTotalMinutes(): Flow<Int>

    @Query("""
        SELECT * FROM sessions 
        WHERE completedAt >= :startOfWeek 
        ORDER BY completedAt ASC
    """)
    fun getSessionsSince(startOfWeek: Long): Flow<List<SessionEntity>>
}
