package com.focusfarm.app.data.repository

import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.domain.GardenEntry
import com.focusfarm.app.domain.GardenFilter
import kotlinx.coroutines.flow.Flow

interface GardenRepository {
    fun getAllSessions(): Flow<List<FocusSession>>
    fun getRecentSessions(limit: Int = 5): Flow<List<FocusSession>>
    fun getSessionCount(): Flow<Int>
    fun getTotalMinutes(): Flow<Int>
    fun getSessionsSince(startOfWeek: Long): Flow<List<FocusSession>>
    suspend fun saveSession(session: FocusSession): Long
    suspend fun getRecentSessionsSnapshot(limit: Int = 5): List<FocusSession>

    fun getGardenEntries(filter: GardenFilter, hideWithered: Boolean): Flow<List<GardenEntry>>
    suspend fun insertGardenEntry(entry: GardenEntry)
    fun getBloomedCount(): Flow<Int>
    fun getTotalGardenCount(): Flow<Int>
}
