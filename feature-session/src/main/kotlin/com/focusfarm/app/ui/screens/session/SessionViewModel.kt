package com.focusfarm.app.ui.screens.session

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
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.data.repository.ReflectionRepository
import com.focusfarm.app.data.repository.RetentionTracker
import com.focusfarm.app.data.repository.RecoverySessionUseCase
import com.focusfarm.app.data.repository.StatsRepository
import com.focusfarm.app.data.local.ActiveSessionDao
import com.focusfarm.app.data.local.ActiveSessionStatus
import com.focusfarm.app.data.local.ActiveSessionConverters
import com.focusfarm.app.data.local.SessionDifficulty
import com.focusfarm.app.data.local.SessionEvaluationDao
import com.focusfarm.app.data.local.SessionEvaluationEntity
import com.focusfarm.app.data.local.RecoverySessionDao
import com.focusfarm.app.data.local.RecoverySessionEntity
import com.focusfarm.app.data.local.RecoverySessionStatus
import com.focusfarm.app.data.repository.AdaptiveDurationEngine
import com.focusfarm.app.data.repository.AdaptiveDurationInput
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.domain.FocusSessionType
import com.focusfarm.app.domain.SessionStatus
import com.focusfarm.app.domain.FocusTag
import com.focusfarm.app.domain.AmbienceSound
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.app.domain.Plant
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.notifications.FocusReminderScheduler
import com.focusfarm.app.review.ReviewPromptGate
import com.focusfarm.app.telemetry.AppTelemetry
import com.focusfarm.app.time.Clock
import com.focusfarm.app.ui.audio.FocusAudioEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.app.Activity
import com.focusfarm.app.data.billing.BillingRepository
import com.focusfarm.app.ads.RewardedAdManager
import com.focusfarm.app.data.repository.ClaimRewardedAdRewardUseCase
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.focusfarm.core.ui.R

enum class SessionState {
    IDLE,
    RUNNING,
    PAUSED,
    FINALIZING,
    COMPLETED,
    KILLED
}

data class SessionUiState(
    val plant: Plant = PlantCatalog.FREE.first(),
    val targetMinutes: Int = 25,
    val remainingSeconds: Int = 25 * 60,
    val elapsedSeconds: Int = 0,
    val growthStage: GrowthStage = GrowthStage.SEED,
    val sessionState: SessionState = SessionState.IDLE,
    val sessionType: FocusSessionType = FocusSessionType.COUNTDOWN,
    val isStrict: Boolean = false,
    val completedSessionId: Long? = null,
    val reflectionSaved: Boolean = false,
    val rewardSeedsGranted: Int = 0,
    val rewardBaseSeeds: Int = 0,
    val rewardBoosted: Boolean = false,
    val completionErrorMessage: String? = null,
    val evaluation: SessionDifficulty? = null,
    val suggestedNextMinutes: Int? = null,
    val selectedTag: FocusTag = FocusTag.OTHER,
    val currentAmbience: AmbienceSound = AmbienceSound.NONE,
)

sealed interface SessionIntent {
    data class SubmitEvaluation(val difficulty: SessionDifficulty) : SessionIntent
    data class SelectAmbience(val sound: AmbienceSound) : SessionIntent
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val completeSessionUseCase: CompleteSessionUseCase,
    private val reflectionRepository: ReflectionRepository,
    private val activeSessionDao: ActiveSessionDao,
    private val evaluationDao: SessionEvaluationDao,
    private val recoverySessionDao: RecoverySessionDao,
    private val adaptiveDurationEngine: AdaptiveDurationEngine,
    private val recoverySessionUseCase: RecoverySessionUseCase,
    private val retentionTracker: RetentionTracker,
    private val telemetry: AppTelemetry,
    private val clock: Clock,
    private val focusAudioEngine: FocusAudioEngine,
    private val billingRepository: BillingRepository,
    private val rewardedAdManager: RewardedAdManager,
    private val claimRewardedAdReward: ClaimRewardedAdRewardUseCase,
    private val progressRepository: ProgressRepository,
    private val statsRepository: StatsRepository,
    private val reviewPromptGate: ReviewPromptGate,
    @ApplicationContext private val appContext: Context,
) : ViewModel(), LifecycleEventObserver {

    fun onIntent(intent: SessionIntent) {
        when (intent) {
            is SessionIntent.SubmitEvaluation -> submitEvaluation(intent.difficulty)
            is SessionIntent.SelectAmbience -> selectAmbience(intent.sound)
        }
    }

    private fun selectAmbience(sound: AmbienceSound) {
        if (_uiState.value.sessionState != SessionState.RUNNING) return
        val succeeded = runCatching {
            if (sound == AmbienceSound.NONE) focusAudioEngine.pauseAmbience()
            else focusAudioEngine.playAmbience(sound)
        }.isSuccess
        if (succeeded) _uiState.value = _uiState.value.copy(currentAmbience = sound)
    }

    private fun submitEvaluation(difficulty: SessionDifficulty) {
        val sessionId = _uiState.value.completedSessionId ?: return
        viewModelScope.launch {
            evaluationDao.upsert(SessionEvaluationEntity(sessionId, difficulty, clock.nowMillis()))
            val recentFailureCount = recoverySessionDao.countOfferedSince(
                (clock.nowMillis() - RECENT_FAILURE_WINDOW_MS).coerceAtLeast(0L),
            )
            val recommendation = adaptiveDurationEngine.recommend(
                AdaptiveDurationInput(
                    evaluations = evaluationDao.recent(3),
                    recentFailureCount = recentFailureCount,
                    recoveryRequired = activeSessionDao.getActive()?.status == ActiveSessionStatus.RECOVERY_REQUIRED,
                ),
            )
            _uiState.value = _uiState.value.copy(
                evaluation = difficulty,
                suggestedNextMinutes = recommendation.minutes,
            )
        }
    }

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _canRevive = MutableStateFlow(false)
    val canRevive: StateFlow<Boolean> = _canRevive.asStateFlow()

    private val _reviewRequest = Channel<Unit>(Channel.CONFLATED)
    val reviewRequest = _reviewRequest.receiveAsFlow()

    private var timerJob: Job? = null
    private var backgroundKillJob: Job? = null
    private var persistJob: Job? = null
    private var sessionEndElapsedRealtimeMs: Long = 0L
    private var sessionEndWallClockMs: Long = 0L
    private var completionPersisted = false
    private var reviveUsedForCurrentSession = false
    private var pendingCompletion: CompleteSessionParams? = null
    private var activeSessionId: String? = null
    private var lastSessionDurationMinutes: Int = 0

    init {
        restoreActiveSessionIfPresent()
    }

    fun startSession(sessionId: String, plant: Plant, minutes: Int) {
        viewModelScope.launch {
            val active = activeSessionDao.getActive()?.takeIf { it.id == sessionId }
            val tag = active?.selectedTag ?: FocusTag.OTHER
            val sessionType = active?.sessionType ?: FocusSessionType.COUNTDOWN
            val isStrict = active?.isStrict ?: false
            val ambienceSound = active?.ambienceSound ?: AmbienceSound.NONE
            startSessionInternal(sessionId, plant, minutes, tag, sessionType, isStrict, ambienceSound)
        }
    }

    private fun startSessionInternal(
        sessionId: String,
        plant: Plant,
        minutes: Int,
        tag: FocusTag,
        sessionType: FocusSessionType,
        isStrict: Boolean,
        ambienceSound: AmbienceSound,
    ) {
        activeSessionId = sessionId
        val totalSeconds = minutes * 60
        completionPersisted = false
        reviveUsedForCurrentSession = false
        pendingCompletion = null
        _canRevive.value = false
        _statusMessage.value = null

        if (sessionType == FocusSessionType.STOPWATCH) {
            sessionEndElapsedRealtimeMs = SystemClock.elapsedRealtime()
            sessionEndWallClockMs = clock.nowMillis()
        } else {
            sessionEndElapsedRealtimeMs = SystemClock.elapsedRealtime() + (totalSeconds * 1000L)
            sessionEndWallClockMs = clock.nowMillis() + (totalSeconds * 1000L)
        }

        telemetry.logEvent(
            name = "session_start",
            params = mapOf(
                "plant_id" to plant.id,
                "target_minutes" to minutes.toString(),
                "session_type" to sessionType.name,
                "is_strict" to isStrict.toString(),
            ),
        )

        _uiState.value = SessionUiState(
            plant = plant,
            targetMinutes = minutes,
            remainingSeconds = if (sessionType == FocusSessionType.STOPWATCH) 0 else totalSeconds,
            elapsedSeconds = 0,
            growthStage = GrowthStage.SEED,
            sessionState = SessionState.RUNNING,
            sessionType = sessionType,
            isStrict = isStrict,
            completedSessionId = null,
            reflectionSaved = false,
            rewardSeedsGranted = 0,
            rewardBoosted = false,
            completionErrorMessage = null,
            selectedTag = tag,
            currentAmbience = ambienceSound,
        )

        if (ambienceSound != AmbienceSound.NONE) {
            runCatching { focusAudioEngine.playAmbience(ambienceSound) }
        }

        saveActiveSessionState()
        startTimerLoop()
    }

    fun recoverSession(sessionId: String) {
        if (_uiState.value.sessionState != SessionState.IDLE) return
        viewModelScope.launch {
            val active = activeSessionDao.getActive()?.takeIf { it.id == sessionId } ?: return@launch
            activeSessionId = active.id
            val plant = PlantCatalog.getById(active.plantId) ?: PlantCatalog.FREE.first()
            when (active.status) {
                ActiveSessionStatus.PAUSED -> {
                    val remaining = active.pausedRemainingSeconds ?: (active.targetMinutes * 60)
                    val elapsed = active.accumulatedElapsedSeconds ?: 0
                    _uiState.value = SessionUiState(
                        plant = plant,
                        targetMinutes = active.targetMinutes,
                        remainingSeconds = remaining,
                        elapsedSeconds = elapsed,
                        growthStage = GrowthStage.fromProgress(
                            if (active.sessionType == FocusSessionType.STOPWATCH) {
                                elapsed.toFloat() / (active.targetMinutes * 60).coerceAtLeast(1).toFloat()
                            } else {
                                (active.targetMinutes * 60 - remaining).toFloat() / (active.targetMinutes * 60).coerceAtLeast(1).toFloat()
                            }
                        ),
                        sessionState = SessionState.PAUSED,
                        sessionType = active.sessionType,
                        isStrict = active.isStrict,
                        selectedTag = active.selectedTag,
                        currentAmbience = active.ambienceSound,
                    )
                }
                ActiveSessionStatus.RUNNING -> {
                    if (active.sessionType == FocusSessionType.STOPWATCH) {
                        val currentRealtime = SystemClock.elapsedRealtime()
                        val currentWall = clock.nowMillis()
                        val isRebooted = currentRealtime < active.startedAtElapsedMs
                        val elapsed = if (isRebooted) {
                            ((currentWall - active.startedAtEpochMs) / 1000L).toInt()
                        } else {
                            ((currentRealtime - active.startedAtElapsedMs) / 1000L).toInt()
                        }

                        if (elapsed >= 180 * 60) {
                            pendingCompletion = CompleteSessionParams(
                                activeSessionId = active.id,
                                plantId = plant.id,
                                plantName = plant.name,
                                plantEmoji = plant.emoji,
                                durationMinutes = 180,
                                completedAt = currentWall,
                                selectedTag = active.selectedTag,
                            )
                            _uiState.value = SessionUiState(
                                plant = plant,
                                targetMinutes = active.targetMinutes,
                                remainingSeconds = 180 * 60,
                                elapsedSeconds = 180 * 60,
                                growthStage = GrowthStage.FULL,
                                sessionState = SessionState.FINALIZING,
                                sessionType = active.sessionType,
                                isStrict = active.isStrict,
                                selectedTag = active.selectedTag,
                            )
                            persistSession()
                        } else {
                            sessionEndElapsedRealtimeMs = currentRealtime - elapsed * 1000L
                            sessionEndWallClockMs = currentWall - elapsed * 1000L
                            _uiState.value = SessionUiState(
                                plant = plant,
                                targetMinutes = active.targetMinutes,
                                remainingSeconds = elapsed,
                                elapsedSeconds = elapsed,
                                growthStage = GrowthStage.fromProgress(
                                    elapsed.toFloat() / (active.targetMinutes * 60).coerceAtLeast(1).toFloat()
                                ),
                                sessionState = SessionState.RUNNING,
                                sessionType = active.sessionType,
                                isStrict = active.isStrict,
                                selectedTag = active.selectedTag,
                                currentAmbience = active.ambienceSound,
                            )
                            startTimerLoop()
                        }
                    } else {
                        val currentRealtime = SystemClock.elapsedRealtime()
                        val currentWall = clock.nowMillis()
                        val isRebooted = currentRealtime < (active.targetEndElapsedMs - (active.targetMinutes * 60 * 1000L))
                        val remaining = if (isRebooted) {
                            ((active.targetEndEpochMs - currentWall).coerceAtLeast(0L) / 1000L).toInt()
                        } else {
                            ((active.targetEndElapsedMs - currentRealtime).coerceAtLeast(0L) / 1000L).toInt()
                        }

                        if (remaining <= 0) {
                            pendingCompletion = CompleteSessionParams(
                                activeSessionId = active.id,
                                plantId = plant.id,
                                plantName = plant.name,
                                plantEmoji = plant.emoji,
                                durationMinutes = active.targetMinutes,
                                completedAt = active.targetEndEpochMs,
                                selectedTag = active.selectedTag,
                            )
                            _uiState.value = SessionUiState(
                                plant = plant,
                                targetMinutes = active.targetMinutes,
                                remainingSeconds = 0,
                                growthStage = GrowthStage.FULL,
                                sessionState = SessionState.FINALIZING,
                                sessionType = active.sessionType,
                                isStrict = active.isStrict,
                                selectedTag = active.selectedTag,
                            )
                            persistSession()
                        } else {
                            sessionEndWallClockMs = active.targetEndEpochMs
                            sessionEndElapsedRealtimeMs = currentRealtime + remaining * 1000L
                            _uiState.value = SessionUiState(
                                plant = plant,
                                targetMinutes = active.targetMinutes,
                                remainingSeconds = remaining,
                                growthStage = GrowthStage.fromProgress(
                                    (active.targetMinutes * 60 - remaining).toFloat() / (active.targetMinutes * 60).coerceAtLeast(1).toFloat()
                                ),
                                sessionState = SessionState.RUNNING,
                                sessionType = active.sessionType,
                                isStrict = active.isStrict,
                                selectedTag = active.selectedTag,
                                currentAmbience = active.ambienceSound,
                            )
                            startTimerLoop()
                        }
                    }
                }
                ActiveSessionStatus.RECOVERY_REQUIRED -> {
                    val recoverySeconds = (active.targetMinutes / 3).coerceIn(3, 8) * 60
                    _canRevive.value = true
                    _uiState.value = SessionUiState(
                        plant = plant,
                        targetMinutes = active.targetMinutes,
                        remainingSeconds = recoverySeconds,
                        sessionState = SessionState.KILLED,
                        sessionType = active.sessionType,
                        isStrict = active.isStrict,
                        selectedTag = active.selectedTag,
                    )
                }
                ActiveSessionStatus.COMPLETION_PENDING,
                ActiveSessionStatus.FINALIZING,
                -> {
                    pendingCompletion = CompleteSessionParams(
                        activeSessionId = active.id,
                        plantId = plant.id,
                        plantName = plant.name,
                        plantEmoji = plant.emoji,
                        durationMinutes = active.targetMinutes,
                        completedAt = active.targetEndEpochMs,
                        selectedTag = active.selectedTag,
                    )
                    _uiState.value = SessionUiState(
                        plant = plant,
                        targetMinutes = active.targetMinutes,
                        remainingSeconds = 0,
                        growthStage = GrowthStage.FULL,
                        sessionState = SessionState.FINALIZING,
                        sessionType = active.sessionType,
                        isStrict = active.isStrict,
                        selectedTag = active.selectedTag,
                    )
                    persistSession()
                }
            }
        }
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var lastRemaining = Int.MIN_VALUE
            var lastElapsed = Int.MIN_VALUE
            while (_uiState.value.sessionState == SessionState.RUNNING) {
                val state = _uiState.value
                if (state.sessionType == FocusSessionType.STOPWATCH) {
                    val elapsed = elapsedSecondsFromClock()
                    if (elapsed != lastElapsed) {
                        updateProgressStopwatch(elapsed)
                        lastElapsed = elapsed
                    }
                    if (elapsed >= 180 * 60) {
                        completeSession()
                        break
                    }
                } else {
                    val remaining = remainingSecondsFromClock()
                    if (remaining != lastRemaining) {
                        updateProgress(remaining)
                        lastRemaining = remaining
                    }
                    if (remaining <= 0) {
                        completeSession()
                        break
                    }
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
            elapsedSeconds = elapsed,
            growthStage = GrowthStage.fromProgress(progress),
        )
    }

    private fun updateProgressStopwatch(elapsedSeconds: Int) {
        val total = (_uiState.value.targetMinutes * 60).coerceAtLeast(60)
        val progress = elapsedSeconds.toFloat() / total.toFloat()
        _uiState.value = _uiState.value.copy(
            remainingSeconds = elapsedSeconds,
            elapsedSeconds = elapsedSeconds,
            growthStage = GrowthStage.fromProgress(progress),
        )
    }

    private fun remainingSecondsFromClock(): Int {
        val millisLeft = (sessionEndElapsedRealtimeMs - SystemClock.elapsedRealtime())
            .coerceAtLeast(0L)
        return ((millisLeft + 999L) / 1000L).toInt()
    }

    private fun elapsedSecondsFromClock(): Int {
        val millisElapsed = (SystemClock.elapsedRealtime() - sessionEndElapsedRealtimeMs)
            .coerceAtLeast(0L)
        return (millisElapsed / 1000L).toInt()
    }

    fun pauseSession() {
        val state = _uiState.value
        if (state.sessionState != SessionState.RUNNING || state.isStrict) return

        timerJob?.cancel()
        timerJob = null
        focusAudioEngine.pauseAmbience()

        val remaining = state.remainingSeconds
        val elapsed = state.elapsedSeconds

        _uiState.value = state.copy(
            sessionState = SessionState.PAUSED,
            remainingSeconds = remaining,
            elapsedSeconds = elapsed
        )

        viewModelScope.launch {
            activeSessionDao.getActive()?.let { active ->
                val updated = active.copy(
                    status = ActiveSessionStatus.PAUSED,
                    pausedRemainingSeconds = remaining,
                    accumulatedElapsedSeconds = elapsed,
                    updatedAtEpochMs = clock.nowMillis()
                )
                activeSessionDao.upsert(updated)
            }
        }
        saveActiveSessionState()
    }

    fun resumeSession() {
        val state = _uiState.value
        if (state.sessionState != SessionState.PAUSED) return

        val remaining = state.remainingSeconds
        val elapsed = state.elapsedSeconds

        val currentRealtime = SystemClock.elapsedRealtime()
        val currentWall = clock.nowMillis()

        if (state.sessionType == FocusSessionType.STOPWATCH) {
            sessionEndElapsedRealtimeMs = currentRealtime - elapsed * 1000L
            sessionEndWallClockMs = currentWall - elapsed * 1000L
        } else {
            sessionEndElapsedRealtimeMs = currentRealtime + remaining * 1000L
            sessionEndWallClockMs = currentWall + remaining * 1000L
        }

        _uiState.value = state.copy(sessionState = SessionState.RUNNING)

        if (state.currentAmbience != AmbienceSound.NONE) {
            runCatching { focusAudioEngine.playAmbience(state.currentAmbience) }
        }

        viewModelScope.launch {
            activeSessionDao.getActive()?.let { active ->
                val updated = active.copy(
                    status = ActiveSessionStatus.RUNNING,
                    startedAtEpochMs = if (state.sessionType == FocusSessionType.STOPWATCH) sessionEndWallClockMs else active.startedAtEpochMs,
                    targetEndEpochMs = if (state.sessionType == FocusSessionType.STOPWATCH) active.targetEndEpochMs else sessionEndWallClockMs,
                    startedAtElapsedMs = if (state.sessionType == FocusSessionType.STOPWATCH) sessionEndElapsedRealtimeMs else active.startedAtElapsedMs,
                    targetEndElapsedMs = if (state.sessionType == FocusSessionType.STOPWATCH) active.targetEndElapsedMs else sessionEndElapsedRealtimeMs,
                    pausedRemainingSeconds = null,
                    accumulatedElapsedSeconds = null,
                    updatedAtEpochMs = clock.nowMillis()
                )
                activeSessionDao.upsert(updated)
            }
        }
        saveActiveSessionState()
        startTimerLoop()
    }

    fun completeStopwatch() {
        val state = _uiState.value
        if (state.sessionType == FocusSessionType.STOPWATCH && state.sessionState == SessionState.RUNNING) {
            completeSession()
        }
    }

    private fun completeSession() {
        if (_uiState.value.sessionState != SessionState.RUNNING || completionPersisted) return

        val state = _uiState.value
        focusAudioEngine.stopAndRelease()
        _canRevive.value = false
        val duration = if (state.sessionType == FocusSessionType.STOPWATCH) state.elapsedSeconds / 60 else state.targetMinutes
        lastSessionDurationMinutes = duration
        pendingCompletion = CompleteSessionParams(
            activeSessionId = activeSessionId,
            plantId = state.plant.id,
            plantName = state.plant.name,
            plantEmoji = state.plant.emoji,
            durationMinutes = duration,
            completedAt = clock.nowMillis(),
            selectedTag = state.selectedTag,
            status = SessionStatus.COMPLETED,
        )

        telemetry.logEvent(
            name = "session_complete",
            params = mapOf(
                "plant_id" to state.plant.id,
                "target_minutes" to duration.toString(),
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
        focusAudioEngine.stopAndRelease()
        val elapsedSeconds = if (state.sessionType == FocusSessionType.STOPWATCH) state.elapsedSeconds else (state.targetMinutes * 60 - state.remainingSeconds).coerceAtLeast(0)
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
        
        if (state.sessionType == FocusSessionType.SHORT_BREAK || state.sessionType == FocusSessionType.LONG_BREAK) {
            _canRevive.value = false
            _uiState.value = _uiState.value.copy(sessionState = SessionState.KILLED)
        } else {
            _canRevive.value = !reviveUsedForCurrentSession && state.remainingSeconds > 0
            _uiState.value = _uiState.value.copy(sessionState = SessionState.KILLED)
            saveActiveSessionState()
        }
    }

    fun leaveKilledSession(onComplete: () -> Unit = {}) {
        if (_uiState.value.sessionState == SessionState.KILLED && !completionPersisted) {
            viewModelScope.launch {
                try {
                    withContext(NonCancellable) {
                        val reason = if (
                            _uiState.value.sessionType == FocusSessionType.SHORT_BREAK ||
                            _uiState.value.sessionType == FocusSessionType.LONG_BREAK
                        ) "cancelled_break" else "abandoned"
                        persistFailure(_uiState.value, reason)
                    }
                } finally {
                    onComplete()
                }
            }
        } else {
            onComplete()
        }
    }

    fun revive(activity: Activity? = null) {
        val state = _uiState.value
        if (state.sessionState != SessionState.KILLED || !_canRevive.value) {
            _statusMessage.value = appContext.getString(R.string.status_session_revive_unavailable)
            return
        }
        val sessionId = activeSessionId ?: return
        val suggestedMinutes = (state.remainingSeconds / 60).coerceIn(3, 8)
        
        viewModelScope.launch {
            val isPremium = billingRepository.entitlement.value.isPremiumUnlocked
            if (isPremium) {
                recoverySessionUseCase(sessionId, suggestedMinutes)
                reviveSession()
            } else {
                if (activity == null) {
                    _statusMessage.value = "Reklam yüklenemedi: Ekran hazır değil."
                    return@launch
                }
                rewardedAdManager.show(
                    activity = activity,
                    placement = RewardedAdManager.Placements.SESSION_REVIVE,
                    onRewardEarned = {
                        viewModelScope.launch {
                            val success = claimRewardedAdReward(
                                placement = "session_recovery",
                                sourceId = sessionId,
                                nowMillis = clock.nowMillis()
                            )
                            if (success) {
                                recoverySessionUseCase(sessionId, suggestedMinutes)
                                reviveSession()
                            } else {
                                _statusMessage.value = "Ödül doğrulanamadı."
                            }
                        }
                    },
                    onNotReady = {
                        _statusMessage.value = "Reklam hazır değil. Lütfen daha sonra tekrar deneyin."
                    }
                )
            }
        }
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

    private fun reviveSession() {
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
                runCatching { retentionTracker.trackCurrentDay(force = true) }

                _uiState.value = _uiState.value.copy(
                    sessionState = SessionState.COMPLETED,
                    completedSessionId = result.sessionId,
                    rewardSeedsGranted = result.grantedSeeds,
                    rewardBaseSeeds = result.baseSeeds,
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

                maybeRequestReviewAfterGoal()
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

    private suspend fun maybeRequestReviewAfterGoal() {
        val sessionDuration = lastSessionDurationMinutes
        if (sessionDuration <= 0) return

        val dailyGoal = progressRepository.observeDailyGoalMinutes().first()
        if (dailyGoal <= 0) return

        val zoneId = clock.zoneId
        val today = clock.today()
        val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val todayTotalMinutes = statsRepository.getTotalMinutesInRange(startOfDay, endOfDay).first()
        val minutesBefore = (todayTotalMinutes - sessionDuration).coerceAtLeast(0)

        val completesGoal = reviewPromptGate.checkGoalCompletion(
            minutesBeforeSession = minutesBefore,
            sessionDurationMinutes = sessionDuration,
            dailyGoalMinutes = dailyGoal,
        )
        if (!completesGoal) return

        val decision = reviewPromptGate.shouldPrompt(
            isActiveSession = false,
            isErrorVisible = false,
        )
        if (decision.shouldPrompt) {
            _reviewRequest.send(Unit)
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
                val state = _uiState.value
                if (state.sessionState == SessionState.RUNNING && state.isStrict) {
                    savedStateHandle[KEY_BACKGROUND_START_TIME_MS] = SystemClock.elapsedRealtime()
                    savedStateHandle[KEY_BACKGROUND_START_TIME_WALL_MS] = clock.nowMillis()
                    backgroundKillJob?.cancel()
                    backgroundKillJob = viewModelScope.launch {
                        delay(BACKGROUND_KILL_DELAY_MS)
                        if (_uiState.value.sessionState == SessionState.RUNNING) {
                            val currentState = _uiState.value
                            val elapsedSeconds = (currentState.targetMinutes * 60 - currentState.remainingSeconds).coerceAtLeast(0)
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
                            focusAudioEngine.stopAndRelease()
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
        focusAudioEngine.stopAndRelease()
        if (_uiState.value.sessionState == SessionState.KILLED && !completionPersisted) {
            viewModelScope.launch {
                persistFailure(_uiState.value, "abandoned")
            }
        }
    }

    private companion object {
        const val TIMER_TICK_INTERVAL_MS = 250L
        const val BACKGROUND_KILL_DELAY_MS = 90_000L
        const val RECENT_FAILURE_WINDOW_MS = 7 * 24 * 60 * 60 * 1_000L
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
        const val KEY_FOCUS_TAG = "session_focus_tag"
    }

    private fun restoreActiveSessionIfPresent() {
        if (savedStateHandle.get<Boolean>(KEY_ACTIVE_SESSION_PRESENT) != true) return

        val plantId = savedStateHandle.get<String>(KEY_PLANT_ID) ?: return clearActiveSessionState()
        val plant = PlantCatalog.getById(plantId) ?: PlantCatalog.FREE.first()
        val targetMinutes = savedStateHandle.get<Int>(KEY_TARGET_MINUTES) ?: 25
        val selectedTag = savedStateHandle.get<String>(KEY_FOCUS_TAG)
            ?.let { runCatching { FocusTag.valueOf(it) }.getOrNull() }
            ?: FocusTag.OTHER
        val savedRemainingSeconds = savedStateHandle.get<Int>(KEY_REMAINING_SECONDS) ?: (targetMinutes * 60)
        val restoredState = savedStateHandle.get<String>(KEY_SESSION_STATE)
            ?.let { runCatching { SessionState.valueOf(it) }.getOrNull() }
            ?: SessionState.IDLE
        reviveUsedForCurrentSession = savedStateHandle.get<Boolean>(KEY_REVIVE_USED) ?: false
        sessionEndWallClockMs = savedStateHandle.get<Long>(KEY_END_WALL_CLOCK_MS) ?: 0L

        val savedEndElapsedRealtime = savedStateHandle.get<Long>(KEY_END_ELAPSED_REALTIME_MS) ?: 0L
        val currentRealtime = SystemClock.elapsedRealtime()
        val currentWallClock = clock.nowMillis()

        val isRebooted = currentRealtime < (savedEndElapsedRealtime - (targetMinutes * 60 * 1000L))

        val typeName = savedStateHandle.get<String>("session_type") ?: FocusSessionType.COUNTDOWN.name
        val sessionType = runCatching { FocusSessionType.valueOf(typeName) }.getOrDefault(FocusSessionType.COUNTDOWN)
        val isStrict = savedStateHandle.get<Boolean>("is_strict") ?: false
        val ambienceName = savedStateHandle.get<String>("ambience_sound") ?: AmbienceSound.NONE.name
        val ambienceSound = runCatching { AmbienceSound.valueOf(ambienceName) }.getOrDefault(AmbienceSound.NONE)

        when (restoredState) {
            SessionState.PAUSED -> {
                _uiState.value = SessionUiState(
                    plant = plant,
                    targetMinutes = targetMinutes,
                    remainingSeconds = savedRemainingSeconds,
                    elapsedSeconds = if (sessionType == FocusSessionType.STOPWATCH) savedRemainingSeconds else (targetMinutes * 60 - savedRemainingSeconds).coerceAtLeast(0),
                    growthStage = GrowthStage.fromProgress(
                        if (sessionType == FocusSessionType.STOPWATCH) {
                            savedRemainingSeconds.toFloat() / (targetMinutes * 60).coerceAtLeast(1).toFloat()
                        } else {
                            (targetMinutes * 60 - savedRemainingSeconds).coerceAtLeast(0).toFloat() / (targetMinutes * 60).coerceAtLeast(1).toFloat()
                        }
                    ),
                    sessionState = SessionState.PAUSED,
                    sessionType = sessionType,
                    isStrict = isStrict,
                    selectedTag = selectedTag,
                    currentAmbience = ambienceSound,
                )
            }
            SessionState.RUNNING -> {
                val backgroundStartTimeMs = savedStateHandle.get<Long>(KEY_BACKGROUND_START_TIME_MS) ?: 0L
                val backgroundStartWallMs = savedStateHandle.get<Long>(KEY_BACKGROUND_START_TIME_WALL_MS) ?: 0L

                val isKilledByBackgroundLimit = if (isStrict && backgroundStartTimeMs > 0L) {
                    if (isRebooted || currentRealtime < backgroundStartTimeMs) {
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
                        sessionType = sessionType,
                        isStrict = isStrict,
                        selectedTag = selectedTag,
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
                    viewModelScope.launch { persistFailure(_uiState.value, "background_timeout") }
                    saveActiveSessionState()
                } else {
                    if (sessionType == FocusSessionType.STOPWATCH) {
                        val elapsedSeconds = if (isRebooted) {
                            ((currentWallClock - sessionEndWallClockMs) / 1000L).toInt()
                        } else {
                            ((currentRealtime - savedEndElapsedRealtime) / 1000L).toInt()
                        }

                        if (elapsedSeconds >= 180 * 60) {
                            _uiState.value = SessionUiState(
                                plant = plant,
                                targetMinutes = targetMinutes,
                                remainingSeconds = 180 * 60,
                                elapsedSeconds = 180 * 60,
                                growthStage = GrowthStage.FULL,
                                sessionState = SessionState.FINALIZING,
                                sessionType = sessionType,
                                isStrict = isStrict,
                                selectedTag = selectedTag,
                            )
                            pendingCompletion = CompleteSessionParams(
                                plantId = plant.id,
                                plantName = plant.name,
                                plantEmoji = plant.emoji,
                                durationMinutes = 180,
                                completedAt = currentWallClock,
                                selectedTag = selectedTag,
                            )
                            saveActiveSessionState()
                            persistSession()
                        } else {
                            sessionEndElapsedRealtimeMs = currentRealtime - elapsedSeconds * 1000L
                            sessionEndWallClockMs = currentWallClock - elapsedSeconds * 1000L
                            _uiState.value = SessionUiState(
                                plant = plant,
                                targetMinutes = targetMinutes,
                                remainingSeconds = elapsedSeconds,
                                elapsedSeconds = elapsedSeconds,
                                growthStage = GrowthStage.fromProgress(
                                    elapsedSeconds.toFloat() / (targetMinutes * 60).coerceAtLeast(1).toFloat()
                                ),
                                sessionState = SessionState.RUNNING,
                                sessionType = sessionType,
                                isStrict = isStrict,
                                selectedTag = selectedTag,
                                currentAmbience = ambienceSound,
                            )
                            saveActiveSessionState()
                            startTimerLoop()
                        }
                    } else {
                        val remainingSeconds = if (isRebooted) {
                            val millisLeft = (sessionEndWallClockMs - currentWallClock).coerceAtLeast(0L)
                            ((millisLeft + 999L) / 1000L).toInt()
                        } else {
                            val millisLeft = (savedEndElapsedRealtime - currentRealtime).coerceAtLeast(0L)
                            ((millisLeft + 999L) / 1000L).toInt()
                        }

                        if (remainingSeconds <= 0) {
                            _uiState.value = SessionUiState(
                                plant = plant,
                                targetMinutes = targetMinutes,
                                remainingSeconds = 0,
                                growthStage = GrowthStage.FULL,
                                sessionState = SessionState.FINALIZING,
                                sessionType = sessionType,
                                isStrict = isStrict,
                                selectedTag = selectedTag,
                            )
                            pendingCompletion = CompleteSessionParams(
                                plantId = plant.id,
                                plantName = plant.name,
                                plantEmoji = plant.emoji,
                                durationMinutes = targetMinutes,
                                completedAt = sessionEndWallClockMs.takeIf { it > 0L } ?: currentWallClock,
                                selectedTag = selectedTag,
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
                                sessionType = sessionType,
                                isStrict = isStrict,
                                selectedTag = selectedTag,
                                currentAmbience = ambienceSound,
                            )
                            saveActiveSessionState()
                            startTimerLoop()
                        }
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
                    sessionType = sessionType,
                    isStrict = isStrict,
                    selectedTag = selectedTag,
                )
            }

            SessionState.FINALIZING -> {
                pendingCompletion = CompleteSessionParams(
                    plantId = plant.id,
                    plantName = plant.name,
                    plantEmoji = plant.emoji,
                    durationMinutes = targetMinutes,
                    completedAt = savedStateHandle.get<Long>(KEY_COMPLETED_AT_MS) ?: currentWallClock,
                    selectedTag = selectedTag,
                )
                _uiState.value = SessionUiState(
                    plant = plant,
                    targetMinutes = targetMinutes,
                    remainingSeconds = 0,
                    growthStage = GrowthStage.FULL,
                    sessionState = SessionState.FINALIZING,
                    sessionType = sessionType,
                    isStrict = isStrict,
                    selectedTag = selectedTag,
                )
                persistSession()
            }

            else -> clearActiveSessionState()
        }
    }

    private suspend fun persistFailure(state: SessionUiState, reason: String) {
        val active = activeSessionDao.getActive() ?: return
        val now = clock.nowMillis()
        if (state.sessionType == FocusSessionType.SHORT_BREAK || state.sessionType == FocusSessionType.LONG_BREAK) {
            completeSessionUseCase(
                CompleteSessionParams(
                    activeSessionId = active.id,
                    plantId = state.plant.id,
                    plantName = "İptal Edilmiş Mola",
                    plantEmoji = "⏹️",
                    durationMinutes = 0,
                    completedAt = now,
                    status = SessionStatus.CANCELLED,
                    failReason = reason,
                )
            )
            clearActiveSessionState()
            return
        }

        completeSessionUseCase(
            CompleteSessionParams(
                activeSessionId = active.id,
                plantId = state.plant.id,
                plantName = "Solmuş Bitki",
                plantEmoji = "🥀",
                durationMinutes = if (state.sessionType == FocusSessionType.STOPWATCH) state.elapsedSeconds / 60 else state.targetMinutes,
                completedAt = now,
                status = SessionStatus.FAILED,
                failReason = reason,
            )
        )
        clearActiveSessionState()
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
        savedStateHandle[KEY_FOCUS_TAG] = state.selectedTag.name
        savedStateHandle["session_type"] = state.sessionType.name
        savedStateHandle["is_strict"] = state.isStrict
        savedStateHandle["ambience_sound"] = state.currentAmbience.name
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
        savedStateHandle.remove<String>(KEY_FOCUS_TAG)
        savedStateHandle.remove<Long>(KEY_BACKGROUND_START_TIME_MS)
        savedStateHandle.remove<Long>(KEY_BACKGROUND_START_TIME_WALL_MS)
        savedStateHandle.remove<String>("session_type")
        savedStateHandle.remove<Boolean>("is_strict")
        savedStateHandle.remove<String>("ambience_sound")
    }
}
