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

    @Query("SELECT * FROM sessions ORDER BY completedAt DESC LIMIT :limit")
    suspend fun getRecentSessionsSnapshot(limit: Int): List<SessionEntity>

    @Query("SELECT COUNT(*) FROM sessions WHERE status = 'COMPLETED'")
    fun getSessionCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM sessions WHERE status = 'COMPLETED'")
    fun getTotalMinutes(): Flow<Int>

    @Query("""
        SELECT * FROM sessions 
        WHERE status = 'COMPLETED' AND completedAt >= :startOfWeek 
        ORDER BY completedAt ASC
    """)
    fun getSessionsSince(startOfWeek: Long): Flow<List<SessionEntity>>

    @Query("SELECT COUNT(*) FROM sessions WHERE status = 'COMPLETED' AND completedAt >= :start AND completedAt < :end")
    suspend fun countCompletedBetween(start: Long, end: Long): Int

    @Query("""
        SELECT COALESCE(SUM(durationMinutes), 0) 
        FROM sessions 
        WHERE status = 'COMPLETED' AND completedAt >= :start AND completedAt <= :end
    """)
    fun getTotalMinutesInRange(start: Long, end: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) 
        FROM sessions 
        WHERE status = 'COMPLETED' AND completedAt >= :start AND completedAt <= :end
    """)
    fun getSuccessfulSessionCountInRange(start: Long, end: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) 
        FROM sessions 
        WHERE status IN ('FAILED', 'CANCELLED') AND completedAt >= :start AND completedAt <= :end
    """)
    fun getFailedSessionCountInRange(start: Long, end: Long): Flow<Int>

    @Query("""
        SELECT COALESCE(MAX(durationMinutes), 0) 
        FROM sessions 
        WHERE status = 'COMPLETED' AND completedAt >= :start AND completedAt <= :end
    """)
    fun getLongestSessionMinutesInRange(start: Long, end: Long): Flow<Int>

    @Query("""
        SELECT ((completedAt + :timezoneOffsetMillis) / 86400000) as localDay,
               SUM(durationMinutes) as totalMinutes,
               COUNT(*) as completedSessions
        FROM sessions
        WHERE status = 'COMPLETED' AND completedAt >= :start AND completedAt <= :end
        GROUP BY localDay
        ORDER BY localDay ASC
    """)
    fun getDailyFocusTrend(start: Long, end: Long, timezoneOffsetMillis: Long): Flow<List<DailyTrendPointEntity>>

    @Query("""
        SELECT selectedTag, SUM(durationMinutes) as totalMinutes 
        FROM sessions 
        WHERE status = 'COMPLETED' AND completedAt >= :start AND completedAt <= :end
        GROUP BY selectedTag
    """)
    fun getTagDistribution(start: Long, end: Long): Flow<List<TagDurationEntity>>

    @Query("""
        SELECT DISTINCT ((completedAt + :timezoneOffsetMillis) / 86400000) as localDay
        FROM sessions
        WHERE status = 'COMPLETED'
        ORDER BY localDay ASC
    """)
    fun getAllCompletedDays(timezoneOffsetMillis: Long): Flow<List<Long>>

    @Query("""
        SELECT ((completedAt + :timezoneOffsetMillis) / 86400000) as localDay,
               SUM(durationMinutes) as totalMinutes
        FROM sessions
        WHERE status = 'COMPLETED'
        GROUP BY localDay
        ORDER BY localDay ASC
    """)
    fun getAllCompletedDaysWithMinutes(timezoneOffsetMillis: Long): Flow<List<DayMinutesEntity>>
}

data class DailyTrendPointEntity(
    val localDay: Long,
    val totalMinutes: Int,
    val completedSessions: Int
)

data class TagDurationEntity(
    val selectedTag: com.focusfarm.app.domain.FocusTag,
    val totalMinutes: Int
)

data class DayMinutesEntity(
    val localDay: Long,
    val totalMinutes: Int
)
