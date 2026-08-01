package com.focusfarm.app.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CalculateSessionRewardUseCaseTest {

    private val useCase = CalculateSessionRewardUseCase()

    @Test
    fun `session duration reward calculations are correct`() {
        val res1 = useCase(
            CalculateSessionRewardUseCase.Params(
                durationMinutes = 10,
                isFirstSessionOfDay = false,
                streakCount = 0,
                isPremium = false,
            )
        )
        assertThat(res1.baseSeeds).isEqualTo(10)
        assertThat(res1.durationBonus).isEqualTo(0)
        assertThat(res1.firstSessionBonus).isEqualTo(0)
        assertThat(res1.streakBonus).isEqualTo(0)
        assertThat(res1.premiumBonus).isEqualTo(0)
        assertThat(res1.totalSeeds).isEqualTo(10)
    }

    @Test
    fun `session duration 25 mins gets 10 percent bonus`() {
        val res = useCase(
            CalculateSessionRewardUseCase.Params(
                durationMinutes = 25,
                isFirstSessionOfDay = false,
                streakCount = 0,
                isPremium = false,
            )
        )
        assertThat(res.baseSeeds).isEqualTo(25)
        assertThat(res.durationBonus).isEqualTo(3)
        assertThat(res.totalSeeds).isEqualTo(28)
    }

    @Test
    fun `session duration 45 mins gets 20 percent bonus`() {
        val res = useCase(
            CalculateSessionRewardUseCase.Params(
                durationMinutes = 45,
                isFirstSessionOfDay = false,
                streakCount = 0,
                isPremium = false,
            )
        )
        assertThat(res.baseSeeds).isEqualTo(45)
        assertThat(res.durationBonus).isEqualTo(9)
        assertThat(res.totalSeeds).isEqualTo(54)
    }

    @Test
    fun `first session of day gets flat 50 seeds bonus`() {
        val res = useCase(
            CalculateSessionRewardUseCase.Params(
                durationMinutes = 10,
                isFirstSessionOfDay = true,
                streakCount = 0,
                isPremium = false,
            )
        )
        assertThat(res.firstSessionBonus).isEqualTo(50)
        assertThat(res.totalSeeds).isEqualTo(60)
    }

    @Test
    fun `streak bonus is capped at 25 seeds`() {
        val res1 = useCase(
            CalculateSessionRewardUseCase.Params(
                durationMinutes = 10,
                isFirstSessionOfDay = false,
                streakCount = 3,
                isPremium = false,
            )
        )
        assertThat(res1.streakBonus).isEqualTo(15)

        val res2 = useCase(
            CalculateSessionRewardUseCase.Params(
                durationMinutes = 10,
                isFirstSessionOfDay = false,
                streakCount = 10,
                isPremium = false,
            )
        )
        assertThat(res2.streakBonus).isEqualTo(25)
    }

    @Test
    fun `premium users get 50 percent bonus on subtotal`() {
        val res = useCase(
            CalculateSessionRewardUseCase.Params(
                durationMinutes = 20,
                isFirstSessionOfDay = true,
                streakCount = 2,
                isPremium = true,
            )
        )
        assertThat(res.premiumBonus).isEqualTo(40)
        assertThat(res.totalSeeds).isEqualTo(120)
    }
}
