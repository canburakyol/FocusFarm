package com.focusfarm.app.di

import android.content.Context
import androidx.room.Room
import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.data.local.ActiveSessionDao
import com.focusfarm.app.data.local.SessionEvaluationDao
import com.focusfarm.app.data.local.RecoverySessionDao
import com.focusfarm.app.data.local.ProgressDao
import com.focusfarm.app.data.local.ReflectionDao
import com.focusfarm.app.data.local.SessionDao
import com.focusfarm.app.data.local.UserInventoryDao
import com.focusfarm.app.data.local.UnlockedSeedDao
import com.focusfarm.app.data.local.PlantSpeciesDao
import com.focusfarm.app.data.local.GardenEntryDao
import com.focusfarm.app.data.local.WalletDao
import com.focusfarm.app.data.local.InventoryDao
import com.focusfarm.app.data.local.MissionDao
import com.focusfarm.app.telemetry.AppTelemetry
import com.focusfarm.app.telemetry.FirebaseAppTelemetry
import com.focusfarm.app.session.AndroidSessionRecoveryNotifier
import com.focusfarm.app.session.SessionRecoveryNotifier
import com.focusfarm.app.time.AndroidSessionClock
import com.focusfarm.app.time.SessionClock
import com.focusfarm.app.ui.audio.FocusAudioEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "focus_farm_db"

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFocusAudioEngine(@ApplicationContext context: Context): FocusAudioEngine =
        FocusAudioEngine(context)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .addCallback(AppDatabase.SEED_BUILT_IN_SPECIES_CALLBACK)
            .build()

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao =
        database.sessionDao()

    @Provides
    fun provideProgressDao(database: AppDatabase): ProgressDao =
        database.progressDao()

    @Provides
    fun provideReflectionDao(database: AppDatabase): ReflectionDao =
        database.reflectionDao()

    @Provides
    @Singleton
    fun provideActiveSessionDao(database: AppDatabase): ActiveSessionDao =
        database.activeSessionDao()

    @Provides fun provideSessionEvaluationDao(database: AppDatabase): SessionEvaluationDao = database.sessionEvaluationDao()
    @Provides fun provideRecoverySessionDao(database: AppDatabase): RecoverySessionDao = database.recoverySessionDao()
    @Provides fun provideUserInventoryDao(database: AppDatabase): UserInventoryDao = database.userInventoryDao()
    @Provides fun provideUnlockedSeedDao(database: AppDatabase): UnlockedSeedDao = database.unlockedSeedDao()
    @Provides fun providePlantSpeciesDao(database: AppDatabase): PlantSpeciesDao = database.plantSpeciesDao()
    @Provides fun provideGardenEntryDao(database: AppDatabase): GardenEntryDao = database.gardenEntryDao()
    @Provides fun provideWalletDao(database: AppDatabase): WalletDao = database.walletDao()
    @Provides fun provideInventoryDao(database: AppDatabase): InventoryDao = database.inventoryDao()
    @Provides fun provideMissionDao(database: AppDatabase): MissionDao = database.missionDao()

    @Provides
    @Singleton
    fun provideSessionClock(implementation: AndroidSessionClock): SessionClock = implementation

    @Provides
    @Singleton
    fun provideAppTelemetry(
        @ApplicationContext context: Context,
    ): AppTelemetry = FirebaseAppTelemetry(context)

    @Provides
    @Singleton
    fun provideSessionRecoveryNotifier(
        implementation: AndroidSessionRecoveryNotifier,
    ): SessionRecoveryNotifier = implementation
}
