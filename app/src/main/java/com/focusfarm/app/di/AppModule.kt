package com.focusfarm.app.di

import android.content.Context
import androidx.room.Room
import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.data.local.ProgressDao
import com.focusfarm.app.data.local.ReflectionDao
import com.focusfarm.app.data.local.SessionDao
import com.focusfarm.app.telemetry.AppTelemetry
import com.focusfarm.app.telemetry.FirebaseAppTelemetry
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
            )
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
    fun provideAppTelemetry(
        @ApplicationContext context: Context,
    ): AppTelemetry = FirebaseAppTelemetry(context)
}
