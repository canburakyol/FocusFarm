package com.focusfarm.app.ui.screens.session

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.R
import com.focusfarm.app.ads.RewardedAdManager
import com.focusfarm.app.data.billing.PremiumBillingManager
import com.focusfarm.app.data.repository.GardenRepository
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.data.repository.ReflectionRepository
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.app.domain.Plant
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.notifications.FocusReminderScheduler
import com.focusfarm.app.telemetry.AppTelemetry
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

enum class SessionState {
    RUNNING,
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
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repository: GardenRepository,
    private val reflectionRepository: ReflectionRepository,
    private val progressRepository: ProgressRepository,
    private val rewardedAdManager: RewardedAdManager,
    premiumBillingManager: PremiumBillingManager,
    private val telemetry: AppTelemetry,
    @ApplicationContext private val appContext: Context,
) : ViewModel(), LifecycleEventObserver {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _canRevive = MutableStateFlow(false)
    val canRevive: StateFlow<Boolean> = _canRevive.asStateFlow()

    val isPremiumUnlocked: StateFlow<Boolean> = premiumBillingManager.state
        .map { it.isPremiumUnlocked }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var timerJob: Job? = null
    private var backgroundKillJob: Job? = null
    private var sessionEndElapsedRealtimeMs: Long = 0L
    private var completionPersisted = false
    private var reviveUsedForCurrentSession = false

    fun startSession(plant: Plant, minutes: Int) {
        val totalSeconds = minutes * 60
        sessionEndElapsedRealtimeMs = SystemClock.elapsedRealtime() + (totalSeconds * 1000L)
        completionPersisted = false
        reviveUsedForCurrentSession = false
        _canRevive.value = false
        _statusMessage.value = null
        rewardedAdManager.preload(appContext)

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
        )
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

        completionPersisted = true
        val state = _uiState.value
        _canRevive.value = false

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
            sessionState = SessionState.COMPLETED,
            reflectionSaved = false,
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
        backgroundKillJob?.cancel()
        _canRevive.value = false
        _uiState.value = _uiState.value.copy(sessionState = SessionState.KILLED)
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

        rewardedAdManager.show(
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
        startTimerLoop()
    }

    private fun persistSession() {
        viewModelScope.launch {
            val state = _uiState.value
            val completedAt = System.currentTimeMillis()
            try {
                val sessionId = repository.saveSession(
                    FocusSession(
                        plantId = state.plant.id,
                        plantName = state.plant.name,
                        plantEmoji = state.plant.emoji,
                        durationMinutes = state.targetMinutes,
                        completedAt = completedAt,
                    )
                )

                val rewardGrant = progressRepository.grantSessionCompletionReward(
                    durationMinutes = state.targetMinutes,
                    nowMillis = completedAt,
                )

                _uiState.value = _uiState.value.copy(
                    completedSessionId = sessionId,
                    rewardSeedsGranted = rewardGrant.grantedSeeds,
                    rewardBoosted = rewardGrant.wasBoosted,
                )

                telemetry.logEvent(
                    name = "session_seed_reward_granted",
                    params = mapOf(
                        "granted_seeds" to rewardGrant.grantedSeeds.toString(),
                        "base_seeds" to rewardGrant.baseSeeds.toString(),
                        "boosted" to rewardGrant.wasBoosted.toString(),
                    ),
                )

                val recentSessions = repository.getRecentSessions(limit = 100).first()
                FocusReminderScheduler.scheduleFromSessions(
                    context = appContext,
                    sessions = recentSessions,
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                telemetry.recordNonFatal(
                    tag = "session_persist_failed",
                    message = "Completed session could not be saved",
                    throwable = t,
                )
            }
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                backgroundKillJob?.cancel()
                backgroundKillJob = null
            }

            Lifecycle.Event.ON_STOP -> {
                if (_uiState.value.sessionState == SessionState.RUNNING) {
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
                            _statusMessage.value = null
                            _canRevive.value =
                                !reviveUsedForCurrentSession && currentState.remainingSeconds > 0
                            _uiState.value = _uiState.value.copy(sessionState = SessionState.KILLED)
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
    }

    private companion object {
        const val TIMER_TICK_INTERVAL_MS = 250L
        const val BACKGROUND_KILL_DELAY_MS = 90_000L
    }
}
