package com.focusfarm.app.data.repository

import com.focusfarm.app.data.local.SessionDifficulty
import com.focusfarm.app.data.local.SessionEvaluationEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdaptiveDurationEngineTest {
    private val engine = AdaptiveDurationEngine()

    @Test fun recoveryAlwaysGetsMicroStart() {
        assertThat(engine.recommend(AdaptiveDurationInput(emptyList(), 0, true)).minutes).isEqualTo(8)
    }

    @Test fun hardLastSessionReducesDuration() {
        val hard = SessionEvaluationEntity(1, SessionDifficulty.HARD, 1)
        assertThat(engine.recommend(AdaptiveDurationInput(listOf(hard), 0, false)).minutes).isEqualTo(15)
    }

    @Test fun repeatedFailuresOverrideEasyRatings() {
        val easy = SessionEvaluationEntity(1, SessionDifficulty.EASY, 1)
        assertThat(engine.recommend(AdaptiveDurationInput(listOf(easy), 2, false)).minutes).isEqualTo(8)
    }

    @Test fun repeatedEasySessionsAllowTwentyFiveMinutes() {
        val input = listOf(
            SessionEvaluationEntity(2, SessionDifficulty.EASY, 2),
            SessionEvaluationEntity(1, SessionDifficulty.EASY, 1),
        )
        assertThat(engine.recommend(AdaptiveDurationInput(input, 0, false)).minutes).isEqualTo(25)
    }
}
