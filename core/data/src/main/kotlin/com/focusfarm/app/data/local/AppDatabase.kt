package com.focusfarm.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.focusfarm.app.domain.PlantSpeciesCatalog

@Database(
    entities = [
        SessionEntity::class,
        UserProgressEntity::class,
        QuestClaimEntity::class,
        QuestRerollEntity::class,
        FreezeUsageEntity::class,
        SessionReflectionEntity::class,
        WeeklyRewardClaimEntity::class,
        CollectionRewardClaimEntity::class,
        StreakHistoryEntity::class,
        ActiveSessionEntity::class,
        SessionEvaluationEntity::class,
        RecoverySessionEntity::class,
        UserInventoryEntity::class,
        UnlockedSeedEntity::class,
        PlantSpeciesEntity::class,
        GardenEntryEntity::class,
        WalletTransactionEntity::class,
        InventoryItemEntity::class,
        MissionEntity::class,
        MissionProgressEntity::class,
    ],
    version = 15,
    exportSchema = true,
)
@TypeConverters(ActiveSessionConverters::class, SessionEvaluationConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun progressDao(): ProgressDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun activeSessionDao(): ActiveSessionDao
    abstract fun sessionEvaluationDao(): SessionEvaluationDao
    abstract fun recoverySessionDao(): RecoverySessionDao
    abstract fun userInventoryDao(): UserInventoryDao
    abstract fun unlockedSeedDao(): UnlockedSeedDao
    abstract fun plantSpeciesDao(): PlantSpeciesDao
    abstract fun gardenEntryDao(): GardenEntryDao
    abstract fun walletDao(): WalletDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun missionDao(): MissionDao

    companion object {
        val SEED_BUILT_IN_SPECIES_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                seedBuiltInPlantSpecies(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                // Repairs installs created directly on a version where migrations never seeded the catalog.
                seedBuiltInPlantSpecies(db)
            }
        }

        internal fun seedBuiltInPlantSpecies(database: SupportSQLiteDatabase) {
            PlantSpeciesCatalog.ALL.forEach { species ->
                database.execSQL(
                    """INSERT OR IGNORE INTO plant_species
                       (id, slug, displayName, description, rarity, growthStageCount, minimumFocusMinutes, seedCost, unlockType, isPremium, isSeasonal, assetKey, sortOrder)
                       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    arrayOf<Any>(
                        species.id,
                        species.slug,
                        species.displayName,
                        species.description,
                        species.rarity.name,
                        species.growthStageCount,
                        species.minimumFocusMinutes,
                        species.seedCost,
                        species.unlockType,
                        if (species.isPremium) 1 else 0,
                        if (species.isSeasonal) 1 else 0,
                        species.assetKey,
                        species.sortOrder,
                    ),
                )
            }
        }

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE user_progress
                    ADD COLUMN nextSessionDoubleSeedBoostActive INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS quest_rerolls (
                        dateKey TEXT NOT NULL PRIMARY KEY,
                        questType TEXT NOT NULL,
                        target INTEGER NOT NULL,
                        rewardSeeds INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_quest_rerolls_dateKey
                    ON quest_rerolls(dateKey)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS streak_history (
                        dateKey TEXT NOT NULL PRIMARY KEY,
                        streakCount INTEGER NOT NULL,
                        sessionsCompleted INTEGER NOT NULL,
                        minutesFocused INTEGER NOT NULL,
                        recordedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_streak_history_dateKey
                    ON streak_history(dateKey)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS active_session (
                        id TEXT NOT NULL PRIMARY KEY,
                        plantId TEXT NOT NULL,
                        targetMinutes INTEGER NOT NULL,
                        startedAtEpochMs INTEGER NOT NULL,
                        targetEndEpochMs INTEGER NOT NULL,
                        startedAtElapsedMs INTEGER NOT NULL,
                        targetEndElapsedMs INTEGER NOT NULL,
                        bootMarker INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        remainingSecondsAtInterruption INTEGER,
                        lastHeartbeatEpochMs INTEGER NOT NULL,
                        createdAtEpochMs INTEGER NOT NULL,
                        updatedAtEpochMs INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS session_evaluations (
                        sessionId INTEGER NOT NULL PRIMARY KEY,
                        difficulty TEXT NOT NULL,
                        createdAtEpochMs INTEGER NOT NULL,
                        FOREIGN KEY(sessionId) REFERENCES sessions(id) ON DELETE CASCADE
                    )""",
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS recovery_sessions (
                        activeSessionId TEXT NOT NULL PRIMARY KEY,
                        plantId TEXT NOT NULL,
                        suggestedMinutes INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        createdAtEpochMs INTEGER NOT NULL,
                        completedAtEpochMs INTEGER
                    )""",
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS user_inventory (
                        id INTEGER NOT NULL PRIMARY KEY,
                        waterDroplets INTEGER NOT NULL,
                        totalExperience INTEGER NOT NULL
                    )""",
                )
                database.execSQL(
                    """INSERT OR IGNORE INTO user_inventory (id, waterDroplets, totalExperience)
                        VALUES (0, 0, 0)""",
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS unlocked_seeds (
                        seedId TEXT NOT NULL PRIMARY KEY,
                        unlockedAtEpochMs INTEGER NOT NULL
                    )""",
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE active_session ADD COLUMN selectedTag TEXT NOT NULL DEFAULT 'OTHER'",
                )
                database.execSQL(
                    "ALTER TABLE sessions ADD COLUMN selectedTag TEXT NOT NULL DEFAULT 'OTHER'",
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE sessions ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'")
                database.execSQL("ALTER TABLE sessions ADD COLUMN failReason TEXT")
                database.execSQL("ALTER TABLE active_session ADD COLUMN sessionType TEXT NOT NULL DEFAULT 'COUNTDOWN'")
                database.execSQL("ALTER TABLE active_session ADD COLUMN isStrict INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE active_session ADD COLUMN ambienceSound TEXT NOT NULL DEFAULT 'NONE'")
                database.execSQL("ALTER TABLE active_session ADD COLUMN pausedRemainingSeconds INTEGER")
                database.execSQL("ALTER TABLE active_session ADD COLUMN accumulatedElapsedSeconds INTEGER")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Create plant_species table
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS plant_species (
                        id TEXT NOT NULL PRIMARY KEY,
                        slug TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        description TEXT NOT NULL,
                        rarity TEXT NOT NULL,
                        growthStageCount INTEGER NOT NULL,
                        minimumFocusMinutes INTEGER NOT NULL,
                        seedCost INTEGER NOT NULL,
                        unlockType TEXT NOT NULL,
                        isPremium INTEGER NOT NULL,
                        isSeasonal INTEGER NOT NULL,
                        assetKey TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )""",
                )

                // 2. Seed built-in plant species
                val species = listOf(
                    "('sprout','sprout','Yeşil Filiz','Her odak seansı sağlam bir başlangıçtır.','COMMON',5,5,0,'free',0,0,'sprout',0)",
                    "('sunflower','sunflower','Ayçiçeği','Dikkatini ışığa çevir, istikrarını artır.','COMMON',5,10,0,'free',0,0,'sunflower',1)",
                    "('cactus','cactus','Kaktüs','Dikkat dağıtıcılar arasında güçlü kal.','COMMON',5,15,0,'free',0,0,'cactus',2)",
                    "('mushroom','mushroom','Mantar','Sessiz, dengeli ve odaklı ilerleme.','UNCOMMON',5,20,0,'free',0,0,'mushroom',3)",
                    "('tree','tree','Meşe','Uzun seansların karşılığı: güçlü kökler.','UNCOMMON',5,25,0,'free',0,0,'tree',4)",
                    "('cherry','cherry','Kiraz Çiçeği','Kısa anların değerini hatırlatır.','RARE',5,10,50,'premium',1,0,'cherry',5)",
                    "('bamboo','bamboo','Bambu','Esnek kal, ritmi bırakma.','RARE',5,15,75,'premium',1,0,'bamboo',6)",
                    "('lotus','lotus','Nilüfer','Zor anlardan bile temiz bir odak çıkar.','EPIC',5,20,100,'premium',1,0,'lotus',7)",
                    "('clover','clover','Dört Yapraklı Yonca','Nadir bulunan düzeni temsil eder.','EPIC',5,30,150,'premium',1,0,'clover',8)",
                    "('bonsai','bonsai','Bonsai','Sabırla büyüyen ustalığın simgesi.','LEGENDARY',5,45,200,'premium',1,0,'bonsai',9)",
                )
                for (row in species) {
                    database.execSQL(
                        """INSERT OR IGNORE INTO plant_species
                           (id,slug,displayName,description,rarity,growthStageCount,minimumFocusMinutes,seedCost,unlockType,isPremium,isSeasonal,assetKey,sortOrder)
                           VALUES $row""",
                    )
                }

                // 3. Create garden_entries table
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS garden_entries (
                        id TEXT NOT NULL PRIMARY KEY,
                        sessionId INTEGER NOT NULL,
                        plantSpeciesId TEXT NOT NULL,
                        state TEXT NOT NULL,
                        plantedAt INTEGER NOT NULL,
                        focusMinutes INTEGER NOT NULL,
                        seedsEarned INTEGER NOT NULL,
                        tagId TEXT NOT NULL,
                        growthStageReached INTEGER NOT NULL,
                        FOREIGN KEY(plantSpeciesId) REFERENCES plant_species(id) ON DELETE CASCADE
                    )""",
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_garden_entries_plantSpeciesId ON garden_entries(plantSpeciesId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_garden_entries_plantedAt ON garden_entries(plantedAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_garden_entries_sessionId ON garden_entries(sessionId)")

                // 4. Migrate existing sessions into garden_entries
                database.execSQL(
                    """INSERT OR IGNORE INTO garden_entries (id, sessionId, plantSpeciesId, state, plantedAt, focusMinutes, seedsEarned, tagId, growthStageReached)
                       SELECT
                           ('ge-' || id),
                           id,
                           plantId,
                           CASE WHEN status = 'COMPLETED' THEN 'BLOOMED' ELSE 'WITHERED' END,
                           completedAt,
                           durationMinutes,
                           0,
                           selectedTag,
                           CASE
                               WHEN durationMinutes >= 45 THEN 4
                               WHEN durationMinutes >= 25 THEN 3
                               WHEN durationMinutes >= 15 THEN 2
                               WHEN durationMinutes >= 5 THEN 1
                               ELSE 0
                           END
                       FROM sessions""",
                )
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Create wallet_transactions table
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS wallet_transactions (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        sourceType TEXT NOT NULL,
                        sourceId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )"""
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_wallet_transactions_userId ON wallet_transactions(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_wallet_transactions_type ON wallet_transactions(type)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_wallet_transactions_sourceType_sourceId ON wallet_transactions(sourceType, sourceId)")

                // 2. Create inventory_items table
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS inventory_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        itemType TEXT NOT NULL,
                        itemId TEXT NOT NULL,
                        purchasedAt INTEGER NOT NULL,
                        quantity INTEGER NOT NULL
                    )"""
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_items_userId ON inventory_items(userId)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_inventory_items_itemType_itemId ON inventory_items(itemType, itemId)")

                // 3. Seed carrot and lavender to plant_species
                database.execSQL(
                    """INSERT OR IGNORE INTO plant_species
                       (id, slug, displayName, description, rarity, growthStageCount, minimumFocusMinutes, seedCost, unlockType, isPremium, isSeasonal, assetKey, sortOrder)
                       VALUES
                       ('carrot', 'carrot', 'Havuç', 'Toprağın altında sabırla büyüyen turuncu tat.', 'COMMON', 5, 5, 30, 'shop', 0, 0, 'carrot', 10),
                       ('lavender', 'lavender', 'Lavanta', 'Sakinleştirici kokusuyla zihnini dinlendirir.', 'UNCOMMON', 5, 20, 45, 'shop', 0, 0, 'lavender', 11)"""
                )
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_progress ADD COLUMN dailyGoalMinutes INTEGER NOT NULL DEFAULT 10")
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS missions (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        goalType TEXT NOT NULL,
                        goalValue INTEGER NOT NULL,
                        rewardSeeds INTEGER NOT NULL,
                        startsAt INTEGER NOT NULL,
                        endsAt INTEGER NOT NULL,
                        isPremium INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )"""
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS mission_progress (
                        missionId TEXT NOT NULL PRIMARY KEY,
                        currentValue INTEGER NOT NULL,
                        completedAt INTEGER,
                        claimedAt INTEGER
                    )"""
                )
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sessions_status_completedAt ON sessions(status, completedAt)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_garden_entries_state ON garden_entries(state)"
                )
            }
        }
    }
}
