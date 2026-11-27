package com.focusfarm.app.review

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.focusfarm.app.time.Clock
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class ReviewPromptGateTest {

    private lateinit var context: Context
    private lateinit var clock: TestClock
    private lateinit var gate: ReviewPromptGate

    private class TestClock(
        private var nowMillis: Long,
        override val zoneId: ZoneId = ZoneId.systemDefault(),
    ) : Clock {
        override fun nowMillis(): Long = nowMillis
        override fun currentDateFlow(): Flow<LocalDate> = flowOf(today())

        fun advanceDays(days: Int) {
            nowMillis += days * 24L * 60L * 60L * 1000L
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("focusfarm_review_prompt", Context.MODE_PRIVATE)
            .edit { clear() }
        clock = TestClock(
            LocalDate.of(2026, 9, 27)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
        )
        gate = ReviewPromptGate(context, clock)
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("focusfarm_review_prompt", Context.MODE_PRIVATE)
            .edit { clear() }
    }

    @Test
    fun firstLaunchBlocksPrompt() {
        gate.recordAppLaunch()
        val decision = gate.shouldPrompt(isActiveSession = false, isErrorVisible = false)
        assertThat(decision.shouldPrompt).isFalse()
        assertThat(decision.reason).isEqualTo("first_launch")
    }

    @Test
    fun activeSessionBlocksPrompt() {
        repeat(5) { gate.recordAppLaunch() }
        val decision = gate.shouldPrompt(isActiveSession = true, isErrorVisible = false)
        assertThat(decision.shouldPrompt).isFalse()
        assertThat(decision.reason).isEqualTo("active_session")
    }

    @Test
    fun visibleErrorBlocksPrompt() {
        repeat(5) { gate.recordAppLaunch() }
        val decision = gate.shouldPrompt(isActiveSession = false, isErrorVisible = true)
        assertThat(decision.shouldPrompt).isFalse()
        assertThat(decision.reason).isEqualTo("error_visible")
    }

    @Test
    fun fifthLaunchAllowsPromptWhenRulesPass() {
        repeat(4) { gate.recordAppLaunch() }
        val record = gate.recordAppLaunch()
        assertThat(record.launchCount).isEqualTo(5)
        assertThat(record.isFifthSession).isTrue()

        val decision = gate.shouldPrompt(isActiveSession = false, isErrorVisible = false)
        assertThat(decision.shouldPrompt).isTrue()
        assertThat(decision.reason).isEqualTo("allowed")
    }

    @Test
    fun promptAfterGoalCompletionAllowedWhenRulesPass() {
        repeat(3) { gate.recordAppLaunch() }
        assertThat(
            gate.checkGoalCompletion(
                minutesBeforeSession = 45,
                sessionDurationMinutes = 25,
                dailyGoalMinutes = 60,
            )
        ).isTrue()

        val decision = gate.shouldPrompt(isActiveSession = false, isErrorVisible = false)
        assertThat(decision.shouldPrompt).isTrue()
    }

    @Test
    fun goalNotCompletedWhenAlreadyOverGoal() {
        repeat(3) { gate.recordAppLaunch() }
        assertThat(
            gate.checkGoalCompletion(
                minutesBeforeSession = 70,
                sessionDurationMinutes = 25,
                dailyGoalMinutes = 60,
            )
        ).isFalse()
    }

    @Test
    fun atMostOnePromptPerDay() {
        repeat(5) { gate.recordAppLaunch() }

        gate.markPromptShown()
        val decision = gate.shouldPrompt(isActiveSession = false, isErrorVisible = false)
        assertThat(decision.shouldPrompt).isFalse()
        assertThat(decision.reason).isEqualTo("already_today")
    }

    @Test
    fun atMostTwoPromptsPerThirtyDays() {
        repeat(5) { gate.recordAppLaunch() }

        gate.markPromptShown()
        clock.advanceDays(1)
        gate.markPromptShown()
        clock.advanceDays(1)

        val decision = gate.shouldPrompt(isActiveSession = false, isErrorVisible = false)
        assertThat(decision.shouldPrompt).isFalse()
        assertThat(decision.reason).isEqualTo("max_30_days")
    }

    @Test
    fun secondPromptAllowedAfterThirtyDays() {
        repeat(5) { gate.recordAppLaunch() }

        gate.markPromptShown()
        clock.advanceDays(15)
        gate.markPromptShown()
        clock.advanceDays(16)

        val decision = gate.shouldPrompt(isActiveSession = false, isErrorVisible = false)
        assertThat(decision.shouldPrompt).isTrue()
    }

    @Test
    fun markPromptShownPersistsTimestamps() {
        repeat(5) { gate.recordAppLaunch() }
        gate.markPromptShown()
        val freshGate = ReviewPromptGate(context, clock)
        val decision = freshGate.shouldPrompt(isActiveSession = false, isErrorVisible = false)
        assertThat(decision.shouldPrompt).isFalse()
        assertThat(decision.reason).isEqualTo("already_today")
    }
}
