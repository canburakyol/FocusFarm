package com.focusfarm.app.data.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    @Singleton
    fun bindGardenRepository(impl: GardenRepositoryImpl): GardenRepository

    @Binds
    @Singleton
    fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds
    @Singleton
    fun bindReflectionRepository(impl: ReflectionRepositoryImpl): ReflectionRepository

    @Binds
    @Singleton
    fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository

    @Binds
    @Singleton
    fun bindBillingRepository(impl: com.focusfarm.app.data.billing.DefaultBillingRepository): com.focusfarm.app.data.billing.BillingRepository

    @Binds
    @Singleton
    fun bindRewardGrantRepository(impl: RewardGrantRepositoryImpl): RewardGrantRepository
}
