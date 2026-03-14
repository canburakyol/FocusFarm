package com.focusfarm.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ReflectionHistoryItem(
    val sessionId: Long,
    val completedAt: Long,
    val plantName: String,
    val plantEmoji: String,
    val durationMinutes: Int,
    val note: String,
)

@Dao
interface ReflectionDao {

    @Query("SELECT * FROM session_reflections WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getReflection(sessionId: Long): SessionReflectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReflection(reflection: SessionReflectionEntity)

    @Query("DELETE FROM session_reflections WHERE sessionId = :sessionId")
    suspend fun deleteReflection(sessionId: Long)

    @Query(
        """
        SELECT
            s.id AS sessionId,
            s.completedAt AS completedAt,
            s.plantName AS plantName,
            s.plantEmoji AS plantEmoji,
            s.durationMinutes AS durationMinutes,
            r.note AS note
        FROM session_reflections r
        INNER JOIN sessions s ON s.id = r.sessionId
        ORDER BY s.completedAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentReflectionHistory(limit: Int): Flow<List<ReflectionHistoryItem>>
}
