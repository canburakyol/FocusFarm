package com.focusfarm.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveSessionDao {
    @Query("SELECT * FROM active_session LIMIT 1")
    fun observeActive(): Flow<ActiveSessionEntity?>

    @Query("SELECT * FROM active_session LIMIT 1")
    suspend fun getActive(): ActiveSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ActiveSessionEntity)

    @Query(
        """
        UPDATE active_session
        SET status = :status,
            remainingSecondsAtInterruption = :remaining,
            updatedAtEpochMs = :updatedAt
        WHERE id = :sessionId
        """,
    )
    suspend fun updateRecoveryState(
        sessionId: String,
        status: ActiveSessionStatus,
        remaining: Int?,
        updatedAt: Long,
    ): Int

    @Query("DELETE FROM active_session WHERE id = :sessionId")
    suspend fun delete(sessionId: String): Int
}
