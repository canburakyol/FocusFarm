package com.focusfarm.app.data.repository

import com.focusfarm.app.data.local.ActiveSessionDao
import com.focusfarm.app.data.local.ActiveSessionEntity
import com.focusfarm.app.data.local.ActiveSessionStatus
import com.focusfarm.app.data.local.RecoverySessionDao
import com.focusfarm.app.data.local.RecoverySessionEntity
import com.focusfarm.app.data.local.RecoverySessionStatus
import com.focusfarm.app.time.SessionClock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SessionRecoveryManagerTest {
    @Test
    fun noActiveSessionReturnsNone() = runTest {
        val manager = manager(session = null)

        assertThat(manager.reconcile()).isEqualTo(RecoveryDecision.None)
    }

    @Test
    fun sameBootRunningSessionUsesElapsedClock() = runTest {
        val session = activeSession(
            targetEndEpochMs = 99_999L,
            targetEndElapsedMs = 15_500L,
        )
        val manager = manager(session, wall = 50_000L, elapsed = 10_000L, boot = 1_000L)

        assertThat(manager.reconcile()).isEqualTo(
            RecoveryDecision.Resume(session.id, remainingSeconds = 6),
        )
    }

    @Test
    fun sameBootExpiredSessionRequiresCompletionConfirmation() = runTest {
        val session = activeSession(targetEndElapsedMs = 9_000L)
        val manager = manager(session, elapsed = 10_000L, boot = 1_000L)

        assertThat(manager.reconcile()).isEqualTo(RecoveryDecision.ConfirmCompletion(session.id))
    }

    @Test
    fun rebootedSessionStillRunningUsesWallClock() = runTest {
        val session = activeSession(targetEndEpochMs = 65_001L)
        val manager = manager(session, wall = 60_000L, elapsed = 500L, boot = 90_000L)

        assertThat(manager.reconcile()).isEqualTo(
            RecoveryDecision.Resume(session.id, remainingSeconds = 6),
        )
    }

    @Test
    fun rebootedExpiredSessionBecomesRecoveryRequired() = runTest {
        val dao = FakeActiveSessionDao(activeSession(targetEndEpochMs = 50_000L))
        val manager = SessionRecoveryManager(
            activeSessionDao = dao,
            recoverySessionDao = FakeRecoverySessionDao(),
            clock = FakeSessionClock(wall = 60_000L, elapsed = 500L, boot = 90_000L),
        )

        assertThat(manager.reconcile()).isEqualTo(
            RecoveryDecision.OfferRecovery("session-1", suggestedMinutes = 8),
        )
        assertThat(dao.getActive()?.status).isEqualTo(ActiveSessionStatus.RECOVERY_REQUIRED)
    }

    @Test
    fun wallClockRollbackNeverAutoCompletesSession() = runTest {
        val dao = FakeActiveSessionDao(activeSession(startedAtEpochMs = 50_000L))
        val manager = SessionRecoveryManager(
            activeSessionDao = dao,
            recoverySessionDao = FakeRecoverySessionDao(),
            clock = FakeSessionClock(wall = 40_000L, elapsed = 500L, boot = Long.MAX_VALUE),
        )

        assertThat(manager.reconcile()).isEqualTo(
            RecoveryDecision.OfferRecovery("session-1", suggestedMinutes = 8),
        )
    }

    @Test
    fun pendingCompletionRequiresFinalization() = runTest {
        val session = activeSession(status = ActiveSessionStatus.COMPLETION_PENDING)

        assertThat(manager(session).reconcile()).isEqualTo(
            RecoveryDecision.FinalizationRequired(session.id),
        )
    }

    private fun manager(
        session: ActiveSessionEntity?,
        wall: Long = 10_000L,
        elapsed: Long = 10_000L,
        boot: Long = 1_000L,
    ) = SessionRecoveryManager(
        activeSessionDao = FakeActiveSessionDao(session),
        recoverySessionDao = FakeRecoverySessionDao(),
        clock = FakeSessionClock(wall, elapsed, boot),
    )

    private fun activeSession(
        startedAtEpochMs: Long = 1_000L,
        targetEndEpochMs: Long = 20_000L,
        startedAtElapsedMs: Long = 1_000L,
        targetEndElapsedMs: Long = 20_000L,
        status: ActiveSessionStatus = ActiveSessionStatus.RUNNING,
    ) = ActiveSessionEntity(
        id = "session-1",
        plantId = "sprout",
        targetMinutes = 25,
        startedAtEpochMs = startedAtEpochMs,
        targetEndEpochMs = targetEndEpochMs,
        startedAtElapsedMs = startedAtElapsedMs,
        targetEndElapsedMs = targetEndElapsedMs,
        bootMarker = 1_000L,
        status = status,
        remainingSecondsAtInterruption = null,
        lastHeartbeatEpochMs = startedAtEpochMs,
        createdAtEpochMs = startedAtEpochMs,
        updatedAtEpochMs = startedAtEpochMs,
    )
}

private class FakeRecoverySessionDao : RecoverySessionDao {
    private val entries = linkedMapOf<String, RecoverySessionEntity>()

    override suspend fun insert(entity: RecoverySessionEntity): Long {
        if (entries.containsKey(entity.activeSessionId)) return -1L
        entries[entity.activeSessionId] = entity
        return 1L
    }
    override suspend fun get(id: String) = entries[id]
    override suspend fun updateStatus(id: String, status: RecoverySessionStatus): Int {
        val entity = entries[id] ?: return 0
        entries[id] = entity.copy(status = status)
        return 1
    }
    override suspend fun markCompleted(id: String, completedAt: Long): Int {
        val entity = entries[id]?.takeIf { it.status == RecoverySessionStatus.RUNNING } ?: return 0
        entries[id] = entity.copy(status = RecoverySessionStatus.COMPLETED, completedAtEpochMs = completedAt)
        return 1
    }
    override suspend fun countOfferedSince(sinceEpochMs: Long) = entries.values.count {
        it.status == RecoverySessionStatus.OFFERED && it.createdAtEpochMs >= sinceEpochMs
    }
    override suspend fun completedTimestampsSince(sinceEpochMs: Long) = entries.values.mapNotNull {
        it.completedAtEpochMs?.takeIf { time -> it.status == RecoverySessionStatus.COMPLETED && time >= sinceEpochMs }
    }
    override fun observeCompletedTimestamps(): Flow<List<Long>> = MutableStateFlow(
        entries.values.mapNotNull { it.completedAtEpochMs },
    )
    override suspend fun countCompletedBetween(start: Long, end: Long) = entries.values.count {
        it.status == RecoverySessionStatus.COMPLETED && it.completedAtEpochMs?.let { time -> time in start until end } == true
    }
}

private class FakeSessionClock(
    private val wall: Long,
    private val elapsed: Long,
    private val boot: Long,
) : SessionClock {
    override fun wallTimeMillis(): Long = wall
    override fun elapsedRealtimeMillis(): Long = elapsed
    override fun bootMarker(): Long = boot
}

private class FakeActiveSessionDao(initial: ActiveSessionEntity?) : ActiveSessionDao {
    private val state = MutableStateFlow(initial)

    override fun observeActive(): Flow<ActiveSessionEntity?> = state
    override suspend fun getActive(): ActiveSessionEntity? = state.value
    override suspend fun upsert(session: ActiveSessionEntity) { state.value = session }

    override suspend fun updateRecoveryState(
        sessionId: String,
        status: ActiveSessionStatus,
        remaining: Int?,
        updatedAt: Long,
    ): Int {
        val current = state.value?.takeIf { it.id == sessionId } ?: return 0
        state.value = current.copy(
            status = status,
            remainingSecondsAtInterruption = remaining,
            updatedAtEpochMs = updatedAt,
        )
        return 1
    }

    override suspend fun delete(sessionId: String): Int {
        if (state.value?.id != sessionId) return 0
        state.value = null
        return 1
    }
}
