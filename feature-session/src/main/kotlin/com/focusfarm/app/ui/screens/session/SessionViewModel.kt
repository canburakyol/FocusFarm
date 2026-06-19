package com.focusfarm.app.ui.screens.session

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.repository.CompleteSessionParams
import com.focusfarm.app.data.repository.CompleteSessionUseCase
import com.focusfarm.app.data.repository.ReflectionRepository
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.app.domain.Plant
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.monetization.MonetizationRepository
import com.focusfarm.app.notifications.FocusReminderScheduler
import com.focusfarm.app.telemetry.AppTelemetry
import com.focusfarm.app.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.focusfarm.core.ui.R

enum class SessionState {
    RUNNING,
    FINALIZING,
    COMPLETED,
    KILLED,
    IDLE,
}

data class SessionUiState(
    val plant: Plant = PlantCatalog.FREE.first(),
    val targetMinutes: Int = 25,
    val remainingSeconds: Int = 25 * 60,
    val growthStage: GrowthStage = GrowthStage.SEED,
    val sessionState: SessionState = SessionState.IDLE,
    val completedSessionId: Long? = null,
    val reflectionSaved: Boolean = false,
    val rewardSeedsGranted: Int = 0,
    val rewardBoosted: Boolean = false,
    val completionErrorMessage: String? = null,
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val completeSessionUseCase: CompleteSessionUseCase,
    private val reflectionRepository: ReflectionRepository,
    private val monetizationRepository: MonetizationRepository,
    private val telemetry: AppTelemetry,
    private val clock: Clock,
    @ApplicationContext private val appContext: Context,
) : ViewModel(), LifecycleEventObserver {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _canRevive = MutableStateFlow(false)
    val canRevive: StateFlow<Boolean> = _canRevive.asStateFlow()

    val isPremiumUnlocked: StateFlow<Boolean> = monetizationRepository.billingState
        .map { it.isPremiumUnlocked }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var timerJob: Job? = null
    private var backgroundKillJob: Job? = null
    private var persistJob: Job? = null
    private var sessionEndElapsedRealtimeMs: Long = 0L
    private var sessionEndWallClockMs: Long = 0L
    private var completionPersisted = false
    private var reviveUsedForCurrentSession = false
    private var pendingCompletion: CompleteSessionParams? = null

    init {
        restoreActiveSessionIfPresent()
    }

    fun startSession(plant: Plant, minutes: Int) {
        val totalSeconds = minutes * 60
        sessionEndElapsedRealtimeMs = SystemClock.elapsedRealtime() + (totalSeconds * 1000L)
        sessionEndWallClockMs = clock.nowMillis() + (totalSeconds * 1000L)
        completionPersisted = false
        reviveUsedForCurrentSession = false
        pendingCompletion = null
        _canRevive.value = false
        _statusMessage.value = null
        monetizationRepository.preloadRewarded(appContext)

        telemetry.logEvent(
            name = "session_start",
            params = mapOf(
                "plant_id" to plant.id,
                "target_minutes" to minutes.toString(),
            ),
        )

        _uiState.value = SessionUiState(
            plant = plant,
            targetMinutes = minutes,
            remainingSeconds = totalSeconds,
            growthStage = GrowthStage.SEED,
            sessionState = SessionState.RUNNING,
            completedSessionId = null,
            reflectionSaved = false,
            rewardSeedsGranted = 0,
            rewardBoosted = false,
            completionErrorMessage = null,
        )
        saveActiveSessionState()
        startTimerLoop()
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var lastRemaining = Int.MIN_VALUE
            while (_uiState.value.sessionState == SessionState.RUNNING) {
                val remaining = remainingSecondsFromClock()
                if (remaining != lastRemaining) {
                    updateProgress(remaining)
                    saveActiveSessionState()
                    lastRemaining = remaining
                }
                if (remaining <= 0) {
                    completeSession()
                    break
                }
                delay(TIMER_TICK_INTERVAL_MS)
            }
        }
    }

    private fun updateProgress(remainingSeconds: Int) {
        val total = _uiState.value.targetMinutes * 60
        val elapsed = (total - remainingSeconds).coerceAtLeast(0)
        val progress = if (total > 0) elapsed.toFloat() / total else 0f
        _uiState.value = _uiState.value.copy(
            remainingSeconds = remainingSeconds.coerceAtLeast(0),
            growthStage = GrowthStage.fromProgress(progress),
        )
    }

    private fun remainingSecondsFromClock(): Int {
        val millisLeft = (sessionEndElapsedRealtimeMs - SystemClock.elapsedRealtime())
            .coerceAtLeast(0L)
        return ((millisLeft + 999L) / 1000L).toInt()
    }

    private fun completeSession() {
        if (_uiState.value.sessionState != SessionState.RUNNING || completionPersisted) return

        val state = _uiState.value
        _canRevive.value = false
        pendingCompletion = CompleteSessionParams(
            plantId = state.plant.id,
            plantName = state.plant.name,
            plantEmoji = state.plant.emoji,
            durationMinutes = state.targetMinutes,
            completedAt = clock.nowMillis(),
        )

        telemetry.logEvent(
            name = "session_complete",
            params = mapOf(
                "plant_id" to state.plant.id,
                "target_minutes" to state.targetMinutes.toString(),
            ),
        )

        _uiState.value = _uiState.value.copy(
            remainingSeconds = 0,
            growthStage = GrowthStage.FULL,
            sessionState = SessionState.FINALIZING,
            reflectionSaved = false,
            completionErrorMessage = null,
        )
        persistSession()
    }

    fun abandon() {
        val state = _uiState.value
        val elapsedSeconds = (state.targetMinutes * 60 - state.remainingSeconds).coerceAtLeast(0)
        telemetry.logEvent(
            name = "session_abandon",
            params = mapOf(
                "reason" to "manual",
                "plant_id" to state.plant.id,
                "target_minutes" to state.targetMinutes.toString(),
                "elapsed_seconds" to elapsedSeconds.toString(),
            ),
        )

        timerJob?.cancel()
        timerJob = null
        sessionEndElapsedRealtimeMs = 0L
        sessionEndWallClockMs = 0L
        backgroundKillJob?.cancel()
        _canRevive.value = false
        _uiState.value = _uiState.value.copy(sessionState = SessionState.KILLED)
        clearActiveSessionState()
    }

    fun showReviveAd(activity: Activity) {
        val state = _uiState.value
        if (state.sessionState != SessionState.KILLED || !_canRevive.value) {
            _statusMessage.value = appContext.getString(R.string.status_session_revive_unavailable)
            return
        }

        if (isPremiumUnlocked.value) {
            reviveSession(fromAd = false)
            return
        }

        monetizationRepository.showRewarded(
            activity = activity,
            placement = "session_revive",
            onRewardEarned = {
                reviveSession(fromAd = true)
            },
            onNotReady = {
                _statusMessage.value = appContext.getString(R.string.status_ad_not_ready)
            },
        )
    }

    fun reviveWithoutAd() {
        reviveSession(fromAd = false)
    }

    fun dismissStatusMessage() {
        _statusMessage.value = null
    }

    fun retryPersistCompletedSession() {
        if (pendingCompletion == null || _uiState.value.sessionState != SessionState.FINALIZING) return
        _uiState.value = _uiState.value.copy(completionErrorMessage = null)
        persistSession()
    }

    fun saveReflection(note: String) {
        val sessionId = _uiState.value.completedSessionId ?: return
        viewModelScope.launch {
            reflectionRepository.upsertReflection(
                sessionId = sessionId,
                note = note,
            )
            _uiState.value = _uiState.value.copy(
                reflectionSaved = note.trim().isNotEmpty(),
            )
        }
    }

    private fun reviveSession(fromAd: Boolean) {
        val state = _uiState.value
        if (
            state.sessionState != SessionState.KILLED ||
            state.remainingSeconds <= 0 ||
            reviveUsedForCurrentSession
        ) {
            _statusMessage.value = appContext.getString(R.string.status_session_revive_unavailable)
            return
        }

        reviveUsedForCurrentSession = true
        _canRevive.value = false
        backgroundKillJob?.cancel()
        backgroundKillJob = null
        sessionEndElapsedRealtimeMs = SystemClock.elapsedRealtime() + (state.remainingSeconds * 1000L)
        sessionEndWallClockMs = clock.nowMillis() + (state.remainingSeconds * 1000L)
        _uiState.value = state.copy(sessionState = SessionState.RUNNING)
        _statusMessage.value = appContext.getString(R.string.status_session_revived)
        telemetry.logEvent(
            name = "session_revive",
            params = mapOf(
                "from_ad" to fromAd.toString(),
                "plant_id" to state.plant.id,
                "remaining_seconds" to state.remainingSeconds.toString(),
            ),
        )
        saveActiveSessionState()
        startTimerLoop()
    }

    private fun persistSession() {
        val completion = pendingCompletion ?: return
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            try {
                val result = completeSessionUseCase(
                    completion,
                )
                completionPersisted = true
                pendingCompletion = null
                clearActiveSessionState()

                _uiState.value = _uiState.value.copy(
                    sessionState = SessionState.COMPLETED,
                    completedSessionId = result.sessionId,
                    rewardSeedsGranted = result.grantedSeeds,
                    rewardBoosted = result.wasBoosted,
                    completionErrorMessage = null,
                )

                telemetry.logEvent(
                    name = "session_seed_reward_granted",
                    params = mapOf(
                        "granted_seeds" to result.grantedSeeds.toString(),
                        "base_seeds" to result.baseSeeds.toString(),
                        "boosted" to result.wasBoosted.toString(),
                        "reminder_rescheduled" to result.reminderRescheduled.toString(),
                    ),
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                completionPersisted = false
                telemetry.recordNonFatal(
                    tag = "session_persist_failed",
                    message = "Completed session could not be saved",
                    throwable = t,
                )
                _uiState.value = _uiState.value.copy(
                    sessionState = SessionState.FINALIZING,
                    completionErrorMessage = appContext.getString(R.string.session_persist_failed_retry),
                )
                saveActiveSessionState()
            }
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                savedStateHandle.remove<Long>(KEY_BACKGROUND_START_TIME_MS)
                savedStateHandle.remove<Long>(KEY_BACKGROUND_START_TIME_WALL_MS)
                backgroundKillJob?.cancel()
                backgroundKillJob = null
            }

            Lifecycle.Event.ON_STOP -> {
                if (_uiState.value.sessionState == SessionState.RUNNING) {
                    savedStateHandle[KEY_BACKGROUND_START_TIME_MS] = SystemClock.elapsedRealtime()
                    savedStateHandle[KEY_BACKGROUND_START_TIME_WALL_MS] = clock.nowMillis()
                    backgroundKillJob?.cancel()
                    backgroundKillJob = viewModelScope.launch {
                        delay(BACKGROUND_KILL_DELAY_MS)
                        if (_uiState.value.sessionState == SessionState.RUNNING) {
                            val currentState = _uiState.value
                            val elapsedSeconds =
                                (currentState.targetMinutes * 60 - currentState.remainingSeconds)
                                    .coerceAtLeast(0)
                            telemetry.logEvent(
                                name = "session_abandon",
                                params = mapOf(
                                    "reason" to "background_timeout",
                                    "plant_id" to currentState.plant.id,
                                    "target_minutes" to currentState.targetMinutes.toString(),
                                    "elapsed_seconds" to elapsedSeconds.toString(),
                                ),
                            )
                            timerJob?.cancel()
                            timerJob = null
                            sessionEndElapsedRealtimeMs = 0L
                            sessionEndWallClockMs = 0L
                            _statusMessage.value = null
                            _canRevive.value =
                                !reviveUsedForCurrentSession && currentState.remainingSeconds > 0
                            _uiState.value = _uiState.value.copy(sessionState = SessionState.KILLED)
                            saveActiveSessionState()
                        }
                    }
                }
            }

            else -> Unit
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        backgroundKillJob?.cancel()
        persistJob?.cancel()
    }

    private companion object {
        const val TIMER_TICK_INTERVAL_MS = 250L
        const val BACKGROUND_KILL_DELAY_MS = 90_000L
        const val KEY_ACTIVE_SESSION_PRESENT = "active_session_present"
        const val KEY_SESSION_STATE = "session_state"
        const val KEY_PLANT_ID = "session_plant_id"
        const val KEY_TARGET_MINUTES = "session_target_minutes"
        const val KEY_REMAINING_SECONDS = "session_remaining_seconds"
        const val KEY_END_WALL_CLOCK_MS = "session_end_wall_clock_ms"
        const val KEY_END_ELAPSED_REALTIME_MS = "session_end_elapsed_realtime_ms"
        const val KEY_COMPLETED_AT_MS = "session_completed_at_ms"
        const val KEY_REVIVE_USED = "session_revive_used"
        const val KEY_CAN_REVIVE = "session_can_revive"
        const val KEY_BACKGROUND_START_TIME_MS = "session_background_start_time_ms"
        const val KEY_BACKGROUND_START_TIME_WALL_MS = "session_background_start_time_wall_ms"
    }

    private fun restoreActiveSessionIfPresent() {
        if (savedStateHandle.get<Boolean>(KEY_ACTIVE_SESSION_PRESENT) != true) return

        val plantId = savedStateHandle.get<String>(KEY_PLANT_ID) ?: return clearActiveSessionState()
        val plant = PlantCatalog.getById(plantId) ?: PlantCatalog.FREE.first()
        val targetMinutes = savedStateHandle.get<Int>(KEY_TARGET_MINUTES) ?: 25
        val savedRemainingSeconds = savedStateHandle.get<Int>(KEY_REMAINING_SECONDS) ?: (targetMinutes * 60)
        val restoredState = savedStateHandle.get<String>(KEY_SESSION_STATE)
            ?.let { runCatching { SessionState.valueOf(it) }.getOrNull() }
            ?: SessionState.IDLE
        reviveUsedForCurrentSession = savedStateHandle.get<Boolean>(KEY_REVIVE_USED) ?: false
        sessionEndWallClockMs = savedStateHandle.get<Long>(KEY_END_WALL_CLOCK_MS) ?: 0L

        val savedEndElapsedRealtime = savedStateHandle.get<Long>(KEY_END_ELAPSED_REALTIME_MS) ?: 0L
        val currentRealtime = SystemClock.elapsedRealtime()
        val currentWallClock = clock.nowMillis()

        // SOTA: Cihazın yeniden başlatılıp başlatılmadığını kontrol et
        val isRebooted = currentRealtime < (savedEndElapsedRealtime - (targetMinutes * 60 * 1000L))

        when (restoredState) {
            SessionState.RUNNING -> {
                val backgroundStartTimeMs = savedStateHandle.get<Long>(KEY_BACKGROUND_START_TIME_MS) ?: 0L
                val backgroundStartWallMs = savedStateHandle.get<Long>(KEY_BACKGROUND_START_TIME_WALL_MS) ?: 0L

                val isKilledByBackgroundLimit = if (backgroundStartTimeMs > 0L) {
                    if (isRebooted || currentRealtime < backgroundStartTimeMs) {
                        // Eğer cihaz yeniden başlatıldısa veya zaman geriye gittiyse wall clock'a güven
                        val elapsedInBackgroundWall = currentWallClock - backgroundStartWallMs
                        elapsedInBackgroundWall >= BACKGROUND_KILL_DELAY_MS
                    } else {
                        val elapsedInBackground = currentRealtime - backgroundStartTimeMs
                        elapsedInBackground >= BACKGROUND_KILL_DELAY_MS
                    }
                } else {
                    false
                }

                if (isKilledByBackgroundLimit) {
                    val remainingSecondsAtDeath = if (isRebooted || currentRealtime < backgroundStartTimeMs) {
                        val killWallTimeMs = backgroundStartWallMs + BACKGROUND_KILL_DELAY_MS
                        val remainingMs = (sessionEndWallClockMs - killWallTimeMs).coerceAtLeast(0L)
                        ((remainingMs + 999L) / 1000L).toInt()
                    } else {
                        val killElapsedMs = backgroundStartTimeMs + BACKGROUND_KILL_DELAY_MS
                        val remainingMs = (savedEndElapsedRealtime - killElapsedMs).coerceAtLeast(0L)
                        ((remainingMs + 999L) / 1000L).toInt()
                    }
                    val finalRemaining = remainingSecondsAtDeath.coerceAtLeast(0)

                    _canRevive.value = !reviveUsedForCurrentSession && finalRemaining > 0
                    _uiState.value = SessionUiState(
                        plant = plant,
                        targetMinutes = targetMinutes,
                        remainingSeconds = finalRemaining,
                        growthStage = GrowthStage.fromProgress(
                            (targetMinutes * 60 - finalRemaining).coerceAtLeast(0).toFloat() /
                                (targetMinutes * 60).coerceAtLeast(1).toFloat(),
                        ),
                        sessionState = SessionState.KILLED,
                    )

                    val elapsedSeconds = (targetMinutes * 60 - finalRemaining).coerceAtLeast(0)
                    telemetry.logEvent(
                        name = "session_abandon",
                        params = mapOf(
                            "reason" to "background_timeout_post_death",
                            "plant_id" to plant.id,
                            "target_minutes" to targetMinutes.toString(),
                            "elapsed_seconds" to elapsedSeconds.toString(),
                        ),
                    )
                    saveActiveSessionState()
                } else {
                    // Zaman Hilesi Koruması (Clock Tampering)
                    val remainingSeconds = if (isRebooted) {
                        val millisLeft = (sessionEndWallClockMs - currentWallClock).coerceAtLeast(0L)
                        ((millisLeft + 999L) / 1000L).toInt()
                    } else {
                        val millisLeft = (savedEndElapsedRealtime - currentRealtime).coerceAtLeast(0L)

                        // Saat manipülasyonu tespiti
                        val expectedWallLeft = sessionEndWallClockMs - currentWallClock
                        val elapsedLeft = savedEndElapsedRealtime - currentRealtime
                        if (kotlin.math.abs(expectedWallLeft - elapsedLeft) > 5000L) {
                            telemetry.logEvent(
                                "clock_tampering_detected",
                                mapOf("wall_diff" to (expectedWallLeft - elapsedLeft).toString())
                            )
                        }
                        ((millisLeft + 999L) / 1000L).toInt()
                    }

                    if (remainingSeconds <= 0) {
                        _uiState.value = SessionUiState(
                            plant = plant,
                            targetMinutes = targetMinutes,
                            remainingSeconds = 0,
                            growthStage = GrowthStage.FULL,
                            sessionState = SessionState.FINALIZING,
                        )
                        pendingCompletion = CompleteSessionParams(
                            plantId = plant.id,
                            plantName = plant.name,
                            plantEmoji = plant.emoji,
                            durationMinutes = targetMinutes,
                            completedAt = sessionEndWallClockMs.takeIf { it > 0L } ?: currentWallClock,
                        )
                        saveActiveSessionState()
                        persistSession()
                    } else {
                        sessionEndElapsedRealtimeMs = currentRealtime + (remainingSeconds * 1000L)
                        sessionEndWallClockMs = currentWallClock + (remainingSeconds * 1000L)
                        _uiState.value = SessionUiState(
                            plant = plant,
                            targetMinutes = targetMinutes,
                            remainingSeconds = remainingSeconds,
                            growthStage = GrowthStage.fromProgress(
                                (targetMinutes * 60 - remainingSeconds).coerceAtLeast(0).toFloat() /
                                    (targetMinutes * 60).coerceAtLeast(1).toFloat(),
                            ),
                            sessionState = SessionState.RUNNING,
                        )
                        saveActiveSessionState()
                        startTimerLoop()
                    }
                }
            }

            SessionState.KILLED -> {
                _canRevive.value = savedStateHandle.get<Boolean>(KEY_CAN_REVIVE) ?: false
                _uiState.value = SessionUiState(
                    plant = plant,
                    targetMinutes = targetMinutes,
                    remainingSeconds = savedRemainingSeconds.coerceAtLeast(0),
                    growthStage = GrowthStage.fromProgress(
                        (targetMinutes * 60 - savedRemainingSeconds).coerceAtLeast(0).toFloat() /
                            (targetMinutes * 60).coerceAtLeast(1).toFloat(),
                    ),
                    sessionState = SessionState.KILLED,
                )
            }

            SessionState.FINALIZING -> {
                pendingCompletion = CompleteSessionParams(
                    plantId = plant.id,
                    plantName = plant.name,
                    plantEmoji = plant.emoji,
                    durationMinutes = targetMinutes,
                    completedAt = savedStateHandle.get<Long>(KEY_COMPLETED_AT_MS) ?: currentWallClock,
                )
                _uiState.value = SessionUiState(
                    plant = plant,
                    targetMinutes = targetMinutes,
                    remainingSeconds = 0,
                    growthStage = GrowthStage.FULL,
                    sessionState = SessionState.FINALIZING,
                )
                persistSession()
            }

            else -> clearActiveSessionState()
        }
    }

    private fun remainingSecondsFromRestoredWallClock(savedRemainingSeconds: Int): Int {
        if (sessionEndWallClockMs <= 0L) return savedRemainingSeconds
        val millisLeft = (sessionEndWallClockMs - clock.nowMillis()).coerceAtLeast(0L)
        return ((millisLeft + 999L) / 1000L).toInt()
    }

    private fun saveActiveSessionState() {
        val state = _uiState.value
        if (state.sessionState == SessionState.IDLE || state.sessionState == SessionState.COMPLETED) {
            clearActiveSessionState()
            return
        }

        savedStateHandle[KEY_ACTIVE_SESSION_PRESENT] = true
        savedStateHandle[KEY_SESSION_STATE] = state.sessionState.name
        savedStateHandle[KEY_PLANT_ID] = state.plant.id
        savedStateHandle[KEY_TARGET_MINUTES] = state.targetMinutes
        savedStateHandle[KEY_REMAINING_SECONDS] = state.remainingSeconds
        savedStateHandle[KEY_END_WALL_CLOCK_MS] = sessionEndWallClockMs
        savedStateHandle[KEY_END_ELAPSED_REALTIME_MS] = sessionEndElapsedRealtimeMs
        savedStateHandle[KEY_COMPLETED_AT_MS] = pendingCompletion?.completedAt
        savedStateHandle[KEY_REVIVE_USED] = reviveUsedForCurrentSession
        savedStateHandle[KEY_CAN_REVIVE] = _canRevive.value
    }

    private fun clearActiveSessionState() {
        savedStateHandle[KEY_ACTIVE_SESSION_PRESENT] = false
        savedStateHandle.remove<String>(KEY_SESSION_STATE)
        savedStateHandle.remove<String>(KEY_PLANT_ID)
        savedStateHandle.remove<Int>(KEY_TARGET_MINUTES)
        savedStateHandle.remove<Int>(KEY_REMAINING_SECONDS)
        savedStateHandle.remove<Long>(KEY_END_WALL_CLOCK_MS)
        savedStateHandle.remove<Long>(KEY_END_ELAPSED_REALTIME_MS)
        savedStateHandle.remove<Long>(KEY_COMPLETED_AT_MS)
        savedStateHandle.remove<Boolean>(KEY_REVIVE_USED)
        savedStateHandle.remove<Boolean>(KEY_CAN_REVIVE)
        savedStateHandle.remove<Long>(KEY_BACKGROUND_START_TIME_MS)
        savedStateHandle.remove<Long>(KEY_BACKGROUND_START_TIME_WALL_MS)
    }
}
