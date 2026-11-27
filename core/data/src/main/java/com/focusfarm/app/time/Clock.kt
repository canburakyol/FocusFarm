package com.focusfarm.app.time

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface Clock {
    val zoneId: ZoneId

    fun nowMillis(): Long

    fun today(): LocalDate = Instant.ofEpochMilli(nowMillis()).atZone(zoneId).toLocalDate()

    fun currentDateFlow(): Flow<LocalDate>
}

class SystemTimeClock(
    override val zoneId: ZoneId = ZoneId.systemDefault(),
) : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun currentDateFlow(): Flow<LocalDate> = flow {
        while (currentCoroutineContext().isActive) {
            val today = today()
            emit(today)
            val nextMidnight = today
                .plusDays(1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
            val delayMillis = (nextMidnight - nowMillis()).coerceAtLeast(1_000L)
            delay(delayMillis)
        }
    }.distinctUntilChanged()
}

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = SystemTimeClock()
}
