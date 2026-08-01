package com.focusfarm.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SessionEvaluationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(evaluation: SessionEvaluationEntity)

    @Query("SELECT * FROM session_evaluations ORDER BY createdAtEpochMs DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<SessionEvaluationEntity>
}
