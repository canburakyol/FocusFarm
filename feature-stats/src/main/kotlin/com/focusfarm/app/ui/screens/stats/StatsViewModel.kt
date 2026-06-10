package com.focusfarm.app.ui.screens.stats

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.auth.AuthManager
import com.focusfarm.app.data.repository.ReferralManager
import com.focusfarm.app.data.repository.ReferralResult
import com.focusfarm.app.data.repository.ReferralState
import com.focusfarm.app.data.repository.GardenRepository
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.data.repository.ReflectionRepository
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.domain.FocusProgressEngine
import com.focusfarm.app.domain.WeeklyChallengeProgress
import com.focusfarm.app.monetization.ConsentManager
import com.focusfarm.app.time.Clock
import dagger.Binds
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface StatsSettingsActions {
    val referralState: StateFlow<ReferralState>

    suspend fun verifyReferral(referralCode: String): ReferralResult

    fun showPrivacyOptions(activity: Activity, onDismissed: (errorMessage: String?) -> Unit)

    suspend fun deleteAccount(): Boolean
}

@Singleton
class DefaultStatsSettingsActions @Inject constructor(
    private val authManager: AuthManager,
    private val consentManager: ConsentManager,
    private val referralManager: ReferralManager,
) : StatsSettingsActions {
    override val referralState: StateFlow<ReferralState> = referralManager.state

    override suspend fun verifyReferral(referralCode: String): ReferralResult =
        referralManager.verifyReferral(referralCode)

    override fun showPrivacyOptions(activity: Activity, onDismissed: (errorMessage: String?) -> Unit) {
        consentManager.showPrivacyOptions(activity, onDismissed)
    }

    override suspend fun deleteAccount(): Boolean = authManager.deleteAccount()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class StatsSettingsBindings {
    @Binds
    abstract fun bindStatsSettingsActions(impl: DefaultStatsSettingsActions): StatsSettingsActions
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    repository: GardenRepository,
    progressRepository: ProgressRepository,
    reflectionRepository: ReflectionRepository,
    private val clock: Clock,
    private val settingsActions: StatsSettingsActions,
) : ViewModel() {

    val totalMinutes: StateFlow<Int> = repository
        .getTotalMinutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val sessionCount: StateFlow<Int> = repository
        .getSessionCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentSessions: StateFlow<List<FocusSession>> = repository
        .getRecentSessions(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allSessions: StateFlow<List<FocusSession>> = repository
        .getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val freezeDates = progressRepository.observeFreezeDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val currentDate = clock.currentDateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), clock.today())

    private val currentDateAnchorMillis = currentDate
        .map { today -> today.atStartOfDay(clock.zoneId).toInstant().toEpochMilli() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            clock.today().atStartOfDay(clock.zoneId).toInstant().toEpochMilli(),
        )

    val streakDays: StateFlow<Int> = combine(allSessions, freezeDates, currentDateAnchorMillis) { sessions, freezes, nowMillis ->
        FocusProgressEngine.currentStreakDays(
            sessions = sessions,
            freezeDates = freezes,
            nowMillis = nowMillis,
            zoneId = clock.zoneId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val currentWeekStartMillis = currentDateAnchorMillis
        .map { nowMillis -> FocusProgressEngine.startOfWeekMillis(nowMillis, clock.zoneId) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            FocusProgressEngine.startOfWeekMillis(clock.nowMillis(), clock.zoneId),
        )

    val weeklySessions: StateFlow<List<FocusSession>> = combine(
        allSessions,
        currentWeekStartMillis,
    ) { sessions, weekStart ->
        sessions
            .filter { it.completedAt >= weekStart }
            .sortedBy { it.completedAt }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyChallenge: StateFlow<WeeklyChallengeProgress> = combine(
        allSessions,
        currentDateAnchorMillis,
    ) { sessions, nowMillis ->
        FocusProgressEngine.weeklyChallenge(
            sessions = sessions,
            nowMillis = nowMillis,
            zoneId = clock.zoneId,
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            FocusProgressEngine.weeklyChallenge(emptyList(), clock.nowMillis(), clock.zoneId),
        )

    private val claimedWeeklyRewardKeys = progressRepository.observeClaimedWeeklyRewardKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val isWeeklyRewardClaimed: StateFlow<Boolean> = combine(
        weeklyChallenge,
        claimedWeeklyRewardKeys,
    ) { challenge, claimedKeys ->
        claimedKeys.contains(challenge.weekKey)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val reflectionHistory = reflectionRepository.observeRecentHistory(limit = 20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val referralState: StateFlow<ReferralState> = settingsActions.referralState

    suspend fun verifyReferral(referralCode: String): ReferralResult =
        settingsActions.verifyReferral(referralCode)

    fun showPrivacyOptions(activity: Activity, onDismissed: (errorMessage: String?) -> Unit = {}) {
        settingsActions.showPrivacyOptions(activity, onDismissed)
    }

    suspend fun deleteAccount(): Boolean = settingsActions.deleteAccount()

    init {
        viewModelScope.launch {
            progressRepository.ensureInitializedAndGrantWeeklyFreeze()
        }
    }
}
