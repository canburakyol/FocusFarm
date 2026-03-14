package com.focusfarm.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SessionEntity::class,
        UserProgressEntity::class,
        QuestClaimEntity::class,
        FreezeUsageEntity::class,
        SessionReflectionEntity::class,
        WeeklyRewardClaimEntity::class,
        CollectionRewardClaimEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun progressDao(): ProgressDao
    abstract fun reflectionDao(): ReflectionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sessions_completedAt ON sessions(completedAt)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_progress (
                        id INTEGER NOT NULL PRIMARY KEY,
                        seeds INTEGER NOT NULL,
                        streakFreezeTokens INTEGER NOT NULL,
                        lastFreezeGrantWeekStart INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO user_progress (id, seeds, streakFreezeTokens, lastFreezeGrantWeekStart)
                    VALUES (0, 0, 1, 0)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS quest_claims (
                        dateKey TEXT NOT NULL,
                        questType TEXT NOT NULL,
                        rewardSeeds INTEGER NOT NULL,
                        claimedAt INTEGER NOT NULL,
                        PRIMARY KEY(dateKey, questType)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_quest_claims_dateKey ON quest_claims(dateKey)"
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS freeze_usages (
                        dateKey TEXT NOT NULL PRIMARY KEY,
                        usedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS session_reflections (
                        sessionId INTEGER NOT NULL PRIMARY KEY,
                        note TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weekly_reward_claims (
                        weekKey TEXT NOT NULL PRIMARY KEY,
                        rewardSeeds INTEGER NOT NULL,
                        claimedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS collection_reward_claims (
                        plantId TEXT NOT NULL PRIMARY KEY,
                        rewardSeeds INTEGER NOT NULL,
                        claimedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
