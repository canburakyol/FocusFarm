package com.focusfarm.app.ui.screens.home

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.R
import com.focusfarm.app.ads.RewardedAdManager
import com.focusfarm.app.data.billing.PremiumBillingManager
import com.focusfarm.app.data.repository.GardenRepository
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.data.repository.QuestRerollState
import com.focusfarm.app.domain.DailyQuestProgress
import com.focusfarm.app.domain.DailyQuestType
import com.focusfarm.app.domain.FocusProgressEngine
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.domain.Plant
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.domain.WeeklyChallengeProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class DailyQuestUiState(
    val quest: DailyQuestProgress,
    val isClaimed: Boolean,
) {
    val canClaim: Boolean
        get() = quest.isCompleted && !isClaimed
}

data class DailyPlanUiState(
    val targetSessions: Int,
    val targetMinutes: Int,
    val completedSessions: Int,
    val completedMinutes: Int,
    val suggestedSessionMinutes: Int,
) {
    val remainingSessions: Int
        get() = (targetSessions - completedSessions).coerceAtLeast(0)

    val remainingMinutes: Int
        get() = (targetMinutes - completedMinutes).coerceAtLeast(0)

    val progressRatio: Float
        get() {
            val sessionRatio = if (targetSessions == 0) 0f else completedSessions.toFloat() / targetSessions.toFloat()
            val minuteRatio = if (targetMinutes == 0) 0f else completedMinutes.toFloat() / targetMinutes.toFloat()
            return ((sessionRatio + minuteRatio) / 2f).coerceIn(0f, 1f)
        }

    val isCompleted: Boolean
        get() = completedSessions >= targetSessions && completedMinutes >= targetMinutes
}

data class EngagementNudgeUiState(
    val title: String,
    val message: String,
    val actionLabel: String,
    val actionMinutes: Int,
)

data class WeeklyReportUiState(
    val reportWeekKey: String,
    val weekLabel: String,
    val totalSessions: Int,
    val totalMinutes: Int,
    val averageSessionMinutes: Int,
    val longestSessionMinutes: Int,
    val bestDayLabel: String,
    val bestDayMinutes: Int,
    val insight: String,
    val nextGoalMinutes: Int,
    val suggestedSessionMinutes: Int,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: GardenRepository,
    premiumBillingManager: PremiumBillingManager,
    private val rewardedAdManager: RewardedAdManager,
    private val progressRepository: ProgressRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val todayDateKey = FocusProgressEngine.todayDateKey()
    private val weeklyReportPrefs = context.getSharedPreferences(PREFS_WEEKLY_REPORT, Context.MODE_PRIVATE)
    private var lastSeenWeeklyReportWeekKey: String? =
        weeklyReportPrefs.getString(KEY_LAST_SEEN_WEEK_KEY, null)

    private val _selectedPlant = MutableStateFlow(PlantCatalog.FREE.first())
    val selectedPlant: StateFlow<Plant> = _selectedPlant.asStateFlow()

    private val _selectedMinutes = MutableStateFlow(25)
    val selectedMinutes: StateFlow<Int> = _selectedMinutes.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isWeeklyReportVisible = MutableStateFlow(false)
    val isWeeklyReportVisible: StateFlow<Boolean> = _isWeeklyReportVisible.asStateFlow()

    private val _questRerollState = MutableStateFlow(progressRepository.getQuestRerollState(todayDateKey))
    private val _isNextSessionSeedBoostActive =
        MutableStateFlow(progressRepository.isNextSessionDoubleSeedBoostActive())

    val isPremiumUnlocked: StateFlow<Boolean> = premiumBillingManager.state
        .map { it.isPremiumUnlocked }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val availablePlants: StateFlow<List<Plant>> = isPremiumUnlocked
        .map { unlocked -> if (unlocked) PlantCatalog.ALL else PlantCatalog.FREE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlantCatalog.FREE)

    val recentSession: StateFlow<FocusSession?> = repository
        .getRecentSessions(1)
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val allSessions: StateFlow<List<FocusSession>> = repository
        .getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val freezeDates = progressRepository.observeFreezeDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val claimedQuestTypes = progressRepository.observeClaimedQuestTypes(todayDateKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val claimedWeeklyRewardKeys = progressRepository.observeClaimedWeeklyRewardKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val seedBalance: StateFlow<Int> = progressRepository.observeSeeds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val freezeTokens: StateFlow<Int> = progressRepository.observeFreezeTokens()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val isQuestRerollUsedToday: StateFlow<Boolean> = _questRerollState
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isNextSessionSeedBoostActive: StateFlow<Boolean> = _isNextSessionSeedBoostActive.asStateFlow()

    val canUseQuestRerollAd: StateFlow<Boolean> = combine(
        isPremiumUnlocked,
        isQuestRerollUsedToday,
    ) { premiumUnlocked, rerollUsedToday ->
        !premiumUnlocked && !rerollUsedToday
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val canActivateNextSessionSeedBoost: StateFlow<Boolean> = combine(
        isPremiumUnlocked,
        isNextSessionSeedBoostActive,
    ) { premiumUnlocked, boostActive ->
        !premiumUnlocked && !boostActive
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val streakDays: StateFlow<Int> = combine(allSessions, freezeDates) { sessions, freezes ->
        FocusProgressEngine.currentStreakDays(
            sessions = sessions,
            freezeDates = freezes,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayMinutes: StateFlow<Int> = allSessions
        .map { sessions -> FocusProgressEngine.todayMinutes(sessions) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val dailyQuestsRaw: StateFlow<List<DailyQuestProgress>> = combine(
        allSessions,
        _questRerollState,
    ) { sessions, reroll ->
        val base = FocusProgressEngine.dailyQuests(sessions)
        applyRerolledQuest(base, reroll)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyQuests: StateFlow<List<DailyQuestUiState>> = combine(
        dailyQuestsRaw,
        claimedQuestTypes,
    ) { quests, claimed ->
        quests.map { quest ->
            DailyQuestUiState(
                quest = quest,
                isClaimed = claimed.contains(quest.type),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyChallenge: StateFlow<WeeklyChallengeProgress> = allSessions
        .map { sessions -> FocusProgressEngine.weeklyChallenge(sessions) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            FocusProgressEngine.weeklyChallenge(emptyList()),
        )

    val isWeeklyRewardClaimed: StateFlow<Boolean> = combine(
        weeklyChallenge,
        claimedWeeklyRewardKeys,
    ) { challenge, claimedKeys ->
        claimedKeys.contains(challenge.weekKey)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val canClaimWeeklyReward: StateFlow<Boolean> = combine(
        weeklyChallenge,
        isWeeklyRewardClaimed,
    ) { challenge, claimed ->
        challenge.isCompleted && !claimed
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val freezeCandidateDateKey: StateFlow<String?> = combine(
        allSessions,
        freezeDates,
    ) { sessions, freezes ->
        FocusProgressEngine.freezeCandidateDate(
            sessions = sessions,
            freezeDates = freezes,
        )?.toString()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val canUseFreeze: StateFlow<Boolean> = combine(
        freezeCandidateDateKey,
        freezeTokens,
    ) { dateKey, tokens ->
        dateKey != null && tokens > 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val todaySessions: StateFlow<List<FocusSession>> = allSessions
        .map { sessions -> sessions.filter { FocusProgressEngine.isToday(it.completedAt) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyReport: StateFlow<WeeklyReportUiState?> = allSessions
        .map { sessions -> buildWeeklyReport(sessions) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dailyPlan: StateFlow<DailyPlanUiState> = combine(
        todaySessions,
        streakDays,
        selectedPlant,
    ) { sessions, streak, plant ->
        val completedSessions = sessions.size
        val completedMinutes = sessions.sumOf { it.durationMinutes }
        val targetSessions = when {
            streak >= 14 -> 4
            streak >= 7 -> 3
            else -> 2
        }
        val targetMinutes = when {
            streak >= 14 -> 110
            streak >= 7 -> 85
            else -> 60
        }

        val remainingSessions = (targetSessions - completedSessions).coerceAtLeast(0)
        val remainingMinutes = (targetMinutes - completedMinutes).coerceAtLeast(0)
        val baseSuggestion = when {
            remainingSessions > 1 -> {
                (remainingMinutes.toFloat() / remainingSessions.toFloat())
                    .roundToInt()
                    .coerceIn(15, 40)
            }
            remainingMinutes > 0 -> remainingMinutes.coerceIn(10, 45)
            else -> 20
        }

        DailyPlanUiState(
            targetSessions = targetSessions,
            targetMinutes = targetMinutes,
            completedSessions = completedSessions,
            completedMinutes = completedMinutes,
            suggestedSessionMinutes = baseSuggestion.coerceAtLeast(plant.minMinutes),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DailyPlanUiState(
            targetSessions = 2,
            targetMinutes = 60,
            completedSessions = 0,
            completedMinutes = 0,
            suggestedSessionMinutes = 25,
        ),
    )

    val personalGoalText: StateFlow<String> = combine(
        dailyPlan,
        weeklyChallenge,
        isWeeklyRewardClaimed,
    ) { plan, challenge, rewardClaimed ->
        when {
            challenge.isCompleted && !rewardClaimed ->
                context.getString(R.string.personal_goal_weekly_reward)

            challenge.isCompleted ->
                context.getString(R.string.personal_goal_weekly_done)

            plan.isCompleted ->
                context.getString(R.string.personal_goal_complete)

            plan.remainingSessions > 0 || plan.remainingMinutes > 0 ->
                context.getString(
                    R.string.personal_goal_push,
                    plan.remainingSessions.coerceAtLeast(1),
                    plan.remainingMinutes.coerceAtLeast(plan.suggestedSessionMinutes),
                )

            else ->
                context.getString(R.string.personal_goal_default)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        context.getString(R.string.personal_goal_default),
    )

    val engagementNudge: StateFlow<EngagementNudgeUiState?> = combine(
        todayMinutes,
        dailyPlan,
        weeklyChallenge,
        canUseFreeze,
        selectedPlant,
    ) { todayFocusMinutes, plan, challenge, freezeAvailable, plant ->
        when {
            todayFocusMinutes == 0 -> {
                val actionMinutes = plan.suggestedSessionMinutes.coerceAtLeast(plant.minMinutes)
                EngagementNudgeUiState(
                    title = context.getString(R.string.nudge_first_focus_title),
                    message = context.getString(R.string.nudge_first_focus_body),
                    actionLabel = context.getString(R.string.nudge_first_focus_action, actionMinutes),
                    actionMinutes = actionMinutes,
                )
            }

            freezeAvailable -> {
                val actionMinutes = maxOf(15, plant.minMinutes)
                EngagementNudgeUiState(
                    title = context.getString(R.string.nudge_streak_risk_title),
                    message = context.getString(R.string.nudge_streak_risk_body),
                    actionLabel = context.getString(R.string.nudge_streak_risk_action, actionMinutes),
                    actionMinutes = actionMinutes,
                )
            }

            !challenge.isCompleted && challenge.overallRatio < 0.5f -> {
                val actionMinutes = maxOf(plan.suggestedSessionMinutes, 20, plant.minMinutes)
                EngagementNudgeUiState(
                    title = context.getString(R.string.nudge_weekly_push_title),
                    message = context.getString(R.string.nudge_weekly_push_body),
                    actionLabel = context.getString(R.string.nudge_weekly_push_action, actionMinutes),
                    actionMinutes = actionMinutes,
                )
            }

            !plan.isCompleted -> {
                val actionMinutes = plan.suggestedSessionMinutes.coerceAtLeast(plant.minMinutes)
                EngagementNudgeUiState(
                    title = context.getString(R.string.nudge_plan_finish_title),
                    message = context.getString(R.string.nudge_plan_finish_body),
                    actionLabel = context.getString(R.string.nudge_plan_finish_action, actionMinutes),
                    actionMinutes = actionMinutes,
                )
            }

            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            progressRepository.ensureInitializedAndGrantWeeklyFreeze()
        }
        rewardedAdManager.preload(context)
        viewModelScope.launch {
            availablePlants.collect { plants ->
                if (plants.none { it.id == _selectedPlant.value.id }) {
                    _selectedPlant.value = plants.first()
                }
            }
        }
        viewModelScope.launch {
            weeklyReport.collect { report ->
                if (
                    report != null &&
                    report.reportWeekKey != lastSeenWeeklyReportWeekKey
                ) {
                    _isWeeklyReportVisible.value = true
                }
            }
        }
        viewModelScope.launch {
            recentSession.collect {
                _isNextSessionSeedBoostActive.value = progressRepository.isNextSessionDoubleSeedBoostActive()
            }
        }
    }

    fun selectPlant(plant: Plant) {
        _selectedPlant.value = plant
        if (_selectedMinutes.value < plant.minMinutes) {
            _selectedMinutes.value = plant.minMinutes
        }
    }

    fun selectMinutes(minutes: Int) {
        _selectedMinutes.value = minutes
    }

    fun applySuggestedMinutes(minutes: Int) {
        _selectedMinutes.value = minutes.coerceAtLeast(_selectedPlant.value.minMinutes)
    }

    fun dismissWeeklyReport() {
        _isWeeklyReportVisible.value = false
        val weekKey = weeklyReport.value?.reportWeekKey ?: return
        lastSeenWeeklyReportWeekKey = weekKey
        weeklyReportPrefs.edit {
            putString(KEY_LAST_SEEN_WEEK_KEY, weekKey)
        }
    }

    fun claimQuest(questType: DailyQuestType) {
        viewModelScope.launch {
            val quest = dailyQuestsRaw.value.firstOrNull { it.type == questType } ?: return@launch
            val success = progressRepository.claimQuest(
                dateKey = todayDateKey,
                quest = quest,
            )
            _statusMessage.value = if (success) {
                context.getString(R.string.status_quest_claimed, quest.rewardSeeds)
            } else {
                context.getString(R.string.status_quest_claim_failed)
            }
        }
    }

    fun useStreakFreeze() {
        viewModelScope.launch {
            val dateKey = freezeCandidateDateKey.value ?: return@launch
            val success = progressRepository.useFreezeForDate(dateKey = dateKey)
            _statusMessage.value = if (success) {
                context.getString(R.string.status_freeze_used)
            } else {
                context.getString(R.string.status_freeze_failed)
            }
        }
    }

    fun claimWeeklyReward() {
        viewModelScope.launch {
            val challenge = weeklyChallenge.value
            val success = progressRepository.claimWeeklyReward(
                weekKey = challenge.weekKey,
                rewardSeeds = challenge.rewardSeeds,
            )
            _statusMessage.value = if (success) {
                context.getString(R.string.status_weekly_reward_claimed, challenge.rewardSeeds)
            } else {
                context.getString(R.string.status_weekly_reward_already_claimed)
            }
        }
    }

    fun rerollDailyQuest(activity: Activity) {
        if (_questRerollState.value != null) {
            _statusMessage.value = context.getString(R.string.status_quest_reroll_already_used)
            return
        }

        val selectedQuest = pickQuestForReroll()
        if (selectedQuest == null) {
            _statusMessage.value = context.getString(R.string.status_quest_reroll_unavailable)
            return
        }

        val rerolledQuest = buildRerolledQuest(selectedQuest)
        rewardedAdManager.show(
            activity = activity,
            placement = "daily_quest_reroll",
            onRewardEarned = {
                viewModelScope.launch {
                    val saved = progressRepository.saveQuestRerollState(
                        dateKey = todayDateKey,
                        questType = rerolledQuest.type,
                        target = rerolledQuest.target,
                        rewardSeeds = rerolledQuest.rewardSeeds,
                    )
                    if (saved) {
                        _questRerollState.value = QuestRerollState(
                            dateKey = todayDateKey,
                            questType = rerolledQuest.type,
                            target = rerolledQuest.target,
                            rewardSeeds = rerolledQuest.rewardSeeds,
                        )
                        _statusMessage.value = context.getString(
                            R.string.status_quest_rerolled,
                            rerolledQuest.rewardSeeds,
                        )
                    } else {
                        _statusMessage.value = context.getString(R.string.status_quest_reroll_already_used)
                    }
                }
            },
            onNotReady = {
                _statusMessage.value = context.getString(R.string.status_ad_not_ready)
            },
        )
    }

    fun activateNextSessionSeedBoost(activity: Activity) {
        if (_isNextSessionSeedBoostActive.value) {
            _statusMessage.value = context.getString(R.string.status_next_session_boost_already_active)
            return
        }

        rewardedAdManager.show(
            activity = activity,
            placement = "next_session_seed_boost",
            onRewardEarned = {
                viewModelScope.launch {
                    val armed = progressRepository.armNextSessionDoubleSeedBoost()
                    if (armed) {
                        _isNextSessionSeedBoostActive.value = true
                        _statusMessage.value = context.getString(R.string.status_next_session_boost_armed)
                    } else {
                        _statusMessage.value =
                            context.getString(R.string.status_next_session_boost_already_active)
                    }
                }
            },
            onNotReady = {
                _statusMessage.value = context.getString(R.string.status_ad_not_ready)
            },
        )
    }

    private fun pickQuestForReroll(): DailyQuestProgress? {
        val claimed = claimedQuestTypes.value
        val unclaimed = dailyQuestsRaw.value.filterNot { quest -> claimed.contains(quest.type) }
        if (unclaimed.isEmpty()) return null

        val incomplete = unclaimed.filterNot { it.isCompleted }
        val pool = if (incomplete.isNotEmpty()) incomplete else unclaimed
        return pool.maxByOrNull { quest -> (quest.target - quest.progress).coerceAtLeast(0) }
    }

    private fun buildRerolledQuest(quest: DailyQuestProgress): DailyQuestProgress {
        val reducedTarget = when (quest.type) {
            DailyQuestType.COMPLETE_SESSIONS ->
                (quest.target - REROLL_SESSION_TARGET_REDUCTION).coerceAtLeast(1)

            DailyQuestType.FOCUS_MINUTES ->
                (quest.target - REROLL_MINUTES_TARGET_REDUCTION).coerceAtLeast(20)

            DailyQuestType.DEEP_SESSION ->
                (quest.target - REROLL_DEEP_TARGET_REDUCTION).coerceAtLeast(15)
        }
        val target = reducedTarget.coerceAtLeast(quest.progress + 1)
        val rewardSeeds = maxOf(
            quest.rewardSeeds + REROLL_MIN_REWARD_BONUS,
            (quest.rewardSeeds * REROLL_REWARD_MULTIPLIER).roundToInt(),
        )
        return quest.copy(
            target = target,
            rewardSeeds = rewardSeeds,
        )
    }

    private fun applyRerolledQuest(
        quests: List<DailyQuestProgress>,
        reroll: QuestRerollState?,
    ): List<DailyQuestProgress> {
        if (reroll == null || reroll.dateKey != todayDateKey) return quests

        return quests.map { quest ->
            if (quest.type == reroll.questType) {
                quest.copy(
                    target = reroll.target.coerceAtLeast(1),
                    rewardSeeds = reroll.rewardSeeds,
                )
            } else {
                quest
            }
        }
    }

    private fun buildWeeklyReport(sessions: List<FocusSession>): WeeklyReportUiState? {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val currentWeekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val reportWeekStart = currentWeekStart.minusWeeks(1)
        val reportWeekEndExclusive = currentWeekStart

        val weekStartMillis = reportWeekStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val weekEndMillis = reportWeekEndExclusive.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val weekSessions = sessions.filter { session ->
            session.completedAt >= weekStartMillis && session.completedAt < weekEndMillis
        }
        if (weekSessions.isEmpty()) return null

        val totalSessions = weekSessions.size
        val totalMinutes = weekSessions.sumOf { it.durationMinutes }
        val averageSessionMinutes = (totalMinutes.toFloat() / totalSessions.toFloat()).roundToInt().coerceAtLeast(1)
        val longestSessionMinutes = weekSessions.maxOf { it.durationMinutes }
        val suggestedSessionMinutes = when {
            averageSessionMinutes >= 35 -> 35
            averageSessionMinutes >= 25 -> 25
            else -> 20
        }

        val bestDay = weekSessions
            .groupBy { session ->
                Instant.ofEpochMilli(session.completedAt)
                    .atZone(zoneId)
                    .toLocalDate()
            }
            .mapValues { (_, daySessions) -> daySessions.sumOf { it.durationMinutes } }
            .maxByOrNull { (_, dayMinutes) -> dayMinutes }

        val bestDayLabel = bestDay?.key?.let(::dayLabel) ?: dayLabel(reportWeekStart)
        val bestDayMinutes = bestDay?.value ?: 0
        val insight = when {
            totalMinutes >= 320 -> context.getString(R.string.weekly_report_insight_great)
            totalMinutes >= 180 -> context.getString(R.string.weekly_report_insight_good)
            else -> context.getString(R.string.weekly_report_insight_start)
        }
        val nextGoalMinutes = when {
            totalMinutes >= 320 -> 360
            totalMinutes >= 180 -> 240
            else -> 180
        }

        return WeeklyReportUiState(
            reportWeekKey = reportWeekStart.toString(),
            weekLabel = weekLabel(reportWeekStart, reportWeekEndExclusive.minusDays(1)),
            totalSessions = totalSessions,
            totalMinutes = totalMinutes,
            averageSessionMinutes = averageSessionMinutes,
            longestSessionMinutes = longestSessionMinutes,
            bestDayLabel = bestDayLabel,
            bestDayMinutes = bestDayMinutes,
            insight = insight,
            nextGoalMinutes = nextGoalMinutes,
            suggestedSessionMinutes = suggestedSessionMinutes.coerceAtLeast(_selectedPlant.value.minMinutes),
        )
    }

    private fun dayLabel(date: LocalDate): String =
        when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> context.getString(R.string.day_mon_short)
            DayOfWeek.TUESDAY -> context.getString(R.string.day_tue_short)
            DayOfWeek.WEDNESDAY -> context.getString(R.string.day_wed_short)
            DayOfWeek.THURSDAY -> context.getString(R.string.day_thu_short)
            DayOfWeek.FRIDAY -> context.getString(R.string.day_fri_short)
            DayOfWeek.SATURDAY -> context.getString(R.string.day_sat_short)
            DayOfWeek.SUNDAY -> context.getString(R.string.day_sun_short)
        }

    private fun weekLabel(startDate: LocalDate, endDate: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("tr"))
        return "${startDate.format(formatter)} - ${endDate.format(formatter)}"
    }

    fun dismissMessage() {
        _statusMessage.value = null
    }

    private companion object {
        const val PREFS_WEEKLY_REPORT = "weekly_personal_report"
        const val KEY_LAST_SEEN_WEEK_KEY = "last_seen_week_key"
        const val REROLL_SESSION_TARGET_REDUCTION = 1
        const val REROLL_MINUTES_TARGET_REDUCTION = 15
        const val REROLL_DEEP_TARGET_REDUCTION = 10
        const val REROLL_REWARD_MULTIPLIER = 1.3f
        const val REROLL_MIN_REWARD_BONUS = 3
    }
}
