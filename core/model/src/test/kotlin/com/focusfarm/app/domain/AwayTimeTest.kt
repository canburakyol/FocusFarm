package com.focusfarm.app.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AwayTimeTest {
    private val own = "com.focusfarm.app"
    private val music = "com.music"
    private val social = "com.social"
    private val limit = 90_000L

    private fun reached(vararg events: DeviceUsageEvent, to: Long, allowed: Set<String> = setOf(music)) =
        AwayTime.limitReachedAt(events.toList(), fromMillis = 0L, toMillis = to, ownPackage = own, allowedPackages = allowed, limitMillis = limit)

    @Test
    fun stayingInAnAllowedAppNeverWithers() {
        assertThat(reached(DeviceUsageEvent.AppForeground(1_000, music), to = 600_000)).isNull()
    }

    @Test
    fun aDisallowedAppWithersAfterTheLimit() {
        assertThat(reached(DeviceUsageEvent.AppForeground(1_000, social), to = 600_000)).isEqualTo(90_000L)
    }

    @Test
    fun awayTimeAddsUpAcrossVisitsAndSkipsAllowedTime() {
        val at = reached(
            DeviceUsageEvent.AppForeground(0, social), // 60 s away
            DeviceUsageEvent.AppForeground(60_000, music), // 5 min allowed
            DeviceUsageEvent.AppForeground(360_000, social), // 30 s more reaches the limit
            to = 1_000_000,
        )
        assertThat(at).isEqualTo(390_000L)
    }

    @Test
    fun screenOffTimeIsNotAway() {
        val at = reached(
            DeviceUsageEvent.AppForeground(0, social),
            DeviceUsageEvent.ScreenOff(30_000),
            DeviceUsageEvent.ScreenOn(600_000),
            to = 1_000_000,
        )
        assertThat(at).isEqualTo(660_000L)
    }

    @Test
    fun theHomeScreenRightAfterLeavingCountsAsAway() {
        assertThat(reached(to = 120_000)).isEqualTo(90_000L)
        assertThat(reached(to = 60_000)).isNull()
    }

    @Test
    fun theAppOpenedJustBeforeLeavingIsWhereTheUserWent() {
        // Real devices log the next app as resumed ~1.5 s before FocusFarm's own stop.
        val events = listOf(
            DeviceUsageEvent.AppForeground(-10_000, own),
            DeviceUsageEvent.AppForeground(-1_500, music),
        )
        val stayed = AwayTime.limitReachedAt(events, 0L, 600_000, own, setOf(music), limit)
        assertThat(stayed).isNull()

        val distracted = AwayTime.limitReachedAt(events, 0L, 600_000, own, setOf("com.other"), limit)
        assertThat(distracted).isEqualTo(90_000L)
    }

    @Test
    fun returningToFocusFarmStopsTheCount() {
        val at = reached(DeviceUsageEvent.AppForeground(60_000, own), to = 600_000)
        assertThat(at).isNull()
    }
}
