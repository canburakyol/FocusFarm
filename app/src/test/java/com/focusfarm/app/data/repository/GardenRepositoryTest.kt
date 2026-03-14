package com.focusfarm.app.data.repository

import com.focusfarm.app.data.local.SessionDao
import com.focusfarm.app.data.local.SessionEntity
import com.focusfarm.app.domain.FocusSession
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GardenRepositoryTest {

    @Test
    fun `saveSession persists and maps back to domain`() = runTest {
        val dao = FakeSessionDao()
        val repository = GardenRepository(dao)

        val session = FocusSession(
            plantId = "sprout",
            plantName = "Yeşil Filiz",
            plantEmoji = "🌿",
            durationMinutes = 25,
            completedAt = 1_700_000_000_000,
        )

        repository.saveSession(session)

        val recent = repository.getRecentSessions(1).first()
        assertThat(recent).hasSize(1)
        assertThat(recent.first().plantId).isEqualTo("sprout")
        assertThat(recent.first().durationMinutes).isEqualTo(25)
    }
}

private class FakeSessionDao : SessionDao {
    private val state = MutableStateFlow<List<SessionEntity>>(emptyList())

    override suspend fun insert(session: SessionEntity): Long {
        val nextId = (state.value.maxOfOrNull { it.id } ?: 0L) + 1L
        state.value = state.value + session.copy(id = nextId)
        return nextId
    }

    override fun getAllSessions(): Flow<List<SessionEntity>> =
        state.map { it.sortedByDescending { item -> item.completedAt } }

    override fun getRecentSessions(limit: Int): Flow<List<SessionEntity>> =
        state.map { it.sortedByDescending { item -> item.completedAt }.take(limit) }

    override fun getSessionCount(): Flow<Int> =
        state.map { it.size }

    override fun getTotalMinutes(): Flow<Int> =
        state.map { list -> list.sumOf { it.durationMinutes } }

    override fun getSessionsSince(startOfWeek: Long): Flow<List<SessionEntity>> =
        state.map { list ->
            list.filter { it.completedAt >= startOfWeek }
                .sortedBy { it.completedAt }
        }
}
