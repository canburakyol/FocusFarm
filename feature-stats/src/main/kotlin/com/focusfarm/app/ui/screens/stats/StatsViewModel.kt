package com.focusfarm.app.ui.screens.stats

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.auth.AuthManager
import com.focusfarm.app.data.auth.AccountDeletionResult
import com.focusfarm.app.data.repository.ReferralManager
import com.focusfarm.app.data.repository.ReferralResult
import com.focusfarm.app.data.repository.ReferralState
import com.focusfarm.app.data.repository.GardenRepository
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.data.repository.ReflectionRepository
import com.focusfarm.app.data.repository.TimeFilter
import com.focusfarm.app.data.repository.StatsSummary
import com.focusfarm.app.data.repository.DailyTrendPoint
import com.focusfarm.app.data.repository.TagDuration
import com.focusfarm.app.data.repository.GetStatsSummaryUseCase
import com.focusfarm.app.data.repository.GetDailyFocusTrendUseCase
import com.focusfarm.app.data.repository.GetTagDistributionUseCase
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton

interface StatsSettingsActions {
    val referralState: StateFlow<ReferralState>
    val currentUserId: StateFlow<String?>
    suspend fun verifyReferral(referralCode: String): ReferralResult
    fun showPrivacyOptions(activity: Activity, onDismissed: (errorMessage: String?) -> Unit)
    suspend fun deleteAccount(): AccountDeletionResult
    suspend fun clearLocalData(): AccountDeletionResult
}

@Singleton
class DefaultStatsSettingsActions @Inject constructor(
    private val authManager: AuthManager,
    private val consentManager: ConsentManager,
    private val referralManager: ReferralManager,
) : StatsSettingsActions {
    override val referralState: StateFlow<ReferralState> = referralManager.state
    override val currentUserId: StateFlow<String?> = authManager.currentUserId
    override suspend fun verifyReferral(referralCode: String): ReferralResult =
        referralManager.verifyReferral(referralCode)
    override fun showPrivacyOptions(activity: Activity, onDismissed: (errorMessage: String?) -> Unit) {
        consentManager.showPrivacyOptions(activity, onDismissed)
    }
    override suspend fun deleteAccount(): AccountDeletionResult = authManager.deleteAccount()
    override suspend fun clearLocalData(): AccountDeletionResult = authManager.clearLocalData()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class StatsSettingsBindings {
    @Binds
    abstract fun bindStatsSettingsActions(impl: DefaultStatsSettingsActions): StatsSettingsActions
}

sealed interface StatsUiState {
    object Loading : StatsUiState
    data class Success(
        val summary: StatsSummary,
        val trend: List<DailyTrendPoint>,
        val tagDistribution: List<TagDuration>
    ) : StatsUiState
    object Empty : StatsUiState
    data class Error(val message: String) : StatsUiState
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    repository: GardenRepository,
    private val progressRepository: ProgressRepository,
    reflectionRepository: ReflectionRepository,
    private val clock: Clock,
    private val settingsActions: StatsSettingsActions,
    private val getStatsSummary: GetStatsSummaryUseCase,
    private val getDailyFocusTrend: GetDailyFocusTrendUseCase,
    private val getTagDistribution: GetTagDistributionUseCase,
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(TimeFilter.LAST_7_DAYS)
    val selectedFilter: StateFlow<TimeFilter> = _selectedFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> = _selectedFilter.flatMapLatest { filter ->
        combine(
            getStatsSummary(filter),
            getDailyFocusTrend(filter),
            getTagDistribution(filter)
        ) { summary, trend, tagDistribution ->
            if (summary.successfulSessionCount == 0 && summary.failedSessionCount == 0) {
                StatsUiState.Empty
            } else {
                StatsUiState.Success(
                    summary = summary,
                    trend = trend,
                    tagDistribution = tagDistribution
                )
            }
        }.catch { e ->
            emit(StatsUiState.Error(e.message ?: "Bilinmeyen bir hata oluştu"))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState.Loading)

    fun onFilterSelected(filter: TimeFilter) {
        _selectedFilter.value = filter
    }

    val recentSessions: StateFlow<List<FocusSession>> = repository
        .getRecentSessions(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val dailyGoalMinutes = progressRepository.observeDailyGoalMinutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    private val currentDate = clock.currentDateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), clock.today())

    private val currentDateAnchorMillis = currentDate
        .map { today -> today.atStartOfDay(clock.zoneId).toInstant().toEpochMilli() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            clock.today().atStartOfDay(clock.zoneId).toInstant().toEpochMilli(),
        )

    private val startOfWeekMillisFlow = currentDateAnchorMillis
        .map { FocusProgressEngine.startOfWeekMillis(it, clock.zoneId) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val weeklySessions: StateFlow<List<FocusSession>> = startOfWeekMillisFlow
        .flatMapLatest { startOfWeek ->
            repository.getSessionsSince(startOfWeek)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val weeklyChallenge: StateFlow<WeeklyChallengeProgress> = startOfWeekMillisFlow
        .flatMapLatest { startOfWeek ->
            repository.getSessionsSince(startOfWeek).map { sessions ->
                FocusProgressEngine.weeklyChallenge(
                    sessions = sessions,
                    nowMillis = clock.nowMillis(),
                    zoneId = clock.zoneId
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            FocusProgressEngine.weeklyChallenge(emptyList(), clock.nowMillis(), clock.zoneId)
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
    val currentUserId: StateFlow<String?> = settingsActions.currentUserId

    suspend fun verifyReferral(referralCode: String): ReferralResult =
        settingsActions.verifyReferral(referralCode)

    fun showPrivacyOptions(activity: Activity, onDismissed: (errorMessage: String?) -> Unit = {}) {
        settingsActions.showPrivacyOptions(activity, onDismissed)
    }

    suspend fun deleteAccountOrClearLocalData(): AccountDeletionResult =
        if (currentUserId.value == null) settingsActions.clearLocalData()
        else settingsActions.deleteAccount()

    init {
        viewModelScope.launch {
            progressRepository.ensureInitializedAndGrantWeeklyFreeze()
        }
    }
}
