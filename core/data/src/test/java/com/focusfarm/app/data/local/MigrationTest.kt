package com.focusfarm.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.ConscryptMode.Mode.OFF

@RunWith(RobolectricTestRunner::class)
@ConscryptMode(OFF)
class MigrationTest {

    private lateinit var context: Context
    private val dbName = "test_db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun testAllMigrations() = runTest {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS sessions (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            plantId TEXT NOT NULL,
                            plantName TEXT NOT NULL,
                            plantEmoji TEXT NOT NULL,
                            durationMinutes INTEGER NOT NULL,
                            completedAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(config)
        val db = helper.writableDatabase

        // Insert version 1 test data
        db.execSQL(
            "INSERT INTO sessions (plantId, plantName, plantEmoji, durationMinutes, completedAt) " +
            "VALUES ('sprout', 'Sprout', '🌱', 25, 1234567890)"
        )

        // 1 -> 2
        AppDatabase.MIGRATION_1_2.migrate(db)
        val indexCursor = db.query("PRAGMA index_list(sessions)")
        var hasIndex = false
        while (indexCursor.moveToNext()) {
            val name = indexCursor.getString(indexCursor.getColumnIndexOrThrow("name"))
            if (name == "index_sessions_completedAt") {
                hasIndex = true
            }
        }
        indexCursor.close()
        assertThat(hasIndex).isTrue()

        // 2 -> 3
        AppDatabase.MIGRATION_2_3.migrate(db)
        val progressCursor = db.query("SELECT * FROM user_progress")
        assertThat(progressCursor.moveToFirst()).isTrue()
        assertThat(progressCursor.getInt(progressCursor.getColumnIndexOrThrow("id"))).isEqualTo(0)
        assertThat(progressCursor.getInt(progressCursor.getColumnIndexOrThrow("seeds"))).isEqualTo(0)
        assertThat(progressCursor.getInt(progressCursor.getColumnIndexOrThrow("streakFreezeTokens"))).isEqualTo(1)
        assertThat(progressCursor.getLong(progressCursor.getColumnIndexOrThrow("lastFreezeGrantWeekStart"))).isEqualTo(0L)
        progressCursor.close()

        val questClaimsCursor = db.query("SELECT * FROM quest_claims")
        assertThat(questClaimsCursor.columnNames.toList()).containsExactly("dateKey", "questType", "rewardSeeds", "claimedAt")
        questClaimsCursor.close()

        val freezeUsagesCursor = db.query("SELECT * FROM freeze_usages")
        assertThat(freezeUsagesCursor.columnNames.toList()).containsExactly("dateKey", "usedAt")
        freezeUsagesCursor.close()

        // 3 -> 4
        AppDatabase.MIGRATION_3_4.migrate(db)
        val reflectionsCursor = db.query("SELECT * FROM session_reflections")
        assertThat(reflectionsCursor.columnNames.toList()).containsExactly("sessionId", "note", "createdAt", "updatedAt")
        reflectionsCursor.close()

        val weeklyCursor = db.query("SELECT * FROM weekly_reward_claims")
        assertThat(weeklyCursor.columnNames.toList()).containsExactly("weekKey", "rewardSeeds", "claimedAt")
        weeklyCursor.close()

        val collectionCursor = db.query("SELECT * FROM collection_reward_claims")
        assertThat(collectionCursor.columnNames.toList()).containsExactly("plantId", "rewardSeeds", "claimedAt")
        collectionCursor.close()

        // 4 -> 5
        AppDatabase.MIGRATION_4_5.migrate(db)
        val infoCursor = db.query("PRAGMA table_info(user_progress)")
        val columns = mutableListOf<String>()
        while (infoCursor.moveToNext()) {
            columns.add(infoCursor.getString(infoCursor.getColumnIndexOrThrow("name")))
        }
        infoCursor.close()
        assertThat(columns).contains("nextSessionDoubleSeedBoostActive")

        val rerollsCursor = db.query("SELECT * FROM quest_rerolls")
        assertThat(rerollsCursor.columnNames.toList()).containsExactly("dateKey", "questType", "target", "rewardSeeds", "createdAt")
        rerollsCursor.close()

        // 5 -> 6
        AppDatabase.MIGRATION_5_6.migrate(db)
        val streakCursor = db.query("SELECT * FROM streak_history")
        assertThat(streakCursor.columnNames.toList()).containsExactly("dateKey", "streakCount", "sessionsCompleted", "minutesFocused", "recordedAt")
        streakCursor.close()

        // 6 -> 7
        AppDatabase.MIGRATION_6_7.migrate(db)
        val activeSessionCursor = db.query("SELECT * FROM active_session")
        assertThat(activeSessionCursor.columnNames.toList()).containsExactly(
            "id",
            "plantId",
            "targetMinutes",
            "startedAtEpochMs",
            "targetEndEpochMs",
            "startedAtElapsedMs",
            "targetEndElapsedMs",
            "bootMarker",
            "status",
            "remainingSecondsAtInterruption",
            "lastHeartbeatEpochMs",
            "createdAtEpochMs",
            "updatedAtEpochMs",
        )
        activeSessionCursor.close()

        // 7 -> 8
        AppDatabase.MIGRATION_7_8.migrate(db)
        val evaluationsCursor = db.query("SELECT * FROM session_evaluations")
        assertThat(evaluationsCursor.columnNames.toList()).containsExactly("sessionId", "difficulty", "createdAtEpochMs")
        evaluationsCursor.close()
        val recoveryCursor = db.query("SELECT * FROM recovery_sessions")
        assertThat(recoveryCursor.columnNames.toList()).containsExactly(
            "activeSessionId", "plantId", "suggestedMinutes", "status", "createdAtEpochMs", "completedAtEpochMs"
        )
        recoveryCursor.close()

        // 8 -> 9
        AppDatabase.MIGRATION_8_9.migrate(db)
        val inventoryCursor = db.query("SELECT * FROM user_inventory")
        assertThat(inventoryCursor.moveToFirst()).isTrue()
        assertThat(inventoryCursor.getInt(inventoryCursor.getColumnIndexOrThrow("id"))).isEqualTo(0)
        assertThat(inventoryCursor.getInt(inventoryCursor.getColumnIndexOrThrow("waterDroplets"))).isEqualTo(0)
        assertThat(inventoryCursor.getInt(inventoryCursor.getColumnIndexOrThrow("totalExperience"))).isEqualTo(0)
        inventoryCursor.close()
        val unlockedSeedsCursor = db.query("SELECT * FROM unlocked_seeds")
        assertThat(unlockedSeedsCursor.columnNames.toList()).containsExactly("seedId", "unlockedAtEpochMs")
        unlockedSeedsCursor.close()

        // 9 -> 10
        AppDatabase.MIGRATION_9_10.migrate(db)
        val activeSessionTagCursor = db.query("PRAGMA table_info(active_session)")
        val activeColumns = mutableListOf<String>()
        while (activeSessionTagCursor.moveToNext()) {
            activeColumns += activeSessionTagCursor.getString(activeSessionTagCursor.getColumnIndexOrThrow("name"))
        }
        activeSessionTagCursor.close()
        assertThat(activeColumns).contains("selectedTag")
        val sessionTagCursor = db.query("PRAGMA table_info(sessions)")
        val sessionColumns = mutableListOf<String>()
        while (sessionTagCursor.moveToNext()) {
            sessionColumns += sessionTagCursor.getString(sessionTagCursor.getColumnIndexOrThrow("name"))
        }
        sessionTagCursor.close()
        assertThat(sessionColumns).contains("selectedTag")

        // 10 -> 11
        AppDatabase.MIGRATION_10_11.migrate(db)
        
        // Check sessions table info
        val sessionsInfoCursor = db.query("PRAGMA table_info(sessions)")
        val sessionsCols = mutableListOf<String>()
        while (sessionsInfoCursor.moveToNext()) {
            sessionsCols += sessionsInfoCursor.getString(sessionsInfoCursor.getColumnIndexOrThrow("name"))
        }
        sessionsInfoCursor.close()
        assertThat(sessionsCols).contains("status")
        assertThat(sessionsCols).contains("failReason")

        // Check active_session table info
        val activeSessionInfoCursor = db.query("PRAGMA table_info(active_session)")
        val activeSessionCols = mutableListOf<String>()
        while (activeSessionInfoCursor.moveToNext()) {
            activeSessionCols += activeSessionInfoCursor.getString(activeSessionInfoCursor.getColumnIndexOrThrow("name"))
        }
        activeSessionInfoCursor.close()
        assertThat(activeSessionCols).contains("sessionType")
        assertThat(activeSessionCols).contains("isStrict")
        assertThat(activeSessionCols).contains("ambienceSound")
        assertThat(activeSessionCols).contains("pausedRemainingSeconds")
        assertThat(activeSessionCols).contains("accumulatedElapsedSeconds")

        // 11 -> 12
        AppDatabase.MIGRATION_11_12.migrate(db)

        // Assert plant_species table
        val speciesCursor = db.query("SELECT COUNT(*) FROM plant_species")
        assertThat(speciesCursor.moveToFirst()).isTrue()
        assertThat(speciesCursor.getInt(0)).isEqualTo(10)
        speciesCursor.close()

        // Assert garden_entries table and migrated row
        val gardenCursor = db.query("SELECT * FROM garden_entries")
        assertThat(gardenCursor.moveToFirst()).isTrue()
        assertThat(gardenCursor.getString(gardenCursor.getColumnIndexOrThrow("id"))).isEqualTo("ge-1")
        assertThat(gardenCursor.getLong(gardenCursor.getColumnIndexOrThrow("sessionId"))).isEqualTo(1L)
        assertThat(gardenCursor.getString(gardenCursor.getColumnIndexOrThrow("plantSpeciesId"))).isEqualTo("sprout")
        assertThat(gardenCursor.getString(gardenCursor.getColumnIndexOrThrow("state"))).isEqualTo("BLOOMED")
        assertThat(gardenCursor.getInt(gardenCursor.getColumnIndexOrThrow("focusMinutes"))).isEqualTo(25)
        assertThat(gardenCursor.getInt(gardenCursor.getColumnIndexOrThrow("growthStageReached"))).isEqualTo(3)
        gardenCursor.close()

        // 12 -> 13
        AppDatabase.MIGRATION_12_13.migrate(db)

        // Assert plant_species has 12 species now (carrot and lavender added)
        val species13Cursor = db.query("SELECT COUNT(*) FROM plant_species")
        assertThat(species13Cursor.moveToFirst()).isTrue()
        assertThat(species13Cursor.getInt(0)).isEqualTo(12)
        species13Cursor.close()

        // Assert wallet_transactions table is created
        val walletColsCursor = db.query("PRAGMA table_info(wallet_transactions)")
        val walletCols = mutableListOf<String>()
        while (walletColsCursor.moveToNext()) {
            walletCols.add(walletColsCursor.getString(walletColsCursor.getColumnIndexOrThrow("name")))
        }
        walletColsCursor.close()
        assertThat(walletCols).containsAtLeast("id", "userId", "type", "amount", "sourceType", "sourceId", "createdAt")

        // Assert inventory_items table is created
        val invColsCursor = db.query("PRAGMA table_info(inventory_items)")
        val invCols = mutableListOf<String>()
        while (invColsCursor.moveToNext()) {
            invCols.add(invColsCursor.getString(invColsCursor.getColumnIndexOrThrow("name")))
        }
        invColsCursor.close()
        assertThat(invCols).containsAtLeast("id", "userId", "itemType", "itemId", "purchasedAt", "quantity")

        // 13 -> 14
        AppDatabase.MIGRATION_13_14.migrate(db)

        // Assert dailyGoalMinutes column is created in user_progress
        val userProgressInfoCursor = db.query("PRAGMA table_info(user_progress)")
        val userProgressCols = mutableListOf<String>()
        while (userProgressInfoCursor.moveToNext()) {
            userProgressCols.add(userProgressInfoCursor.getString(userProgressInfoCursor.getColumnIndexOrThrow("name")))
        }
        userProgressInfoCursor.close()
        assertThat(userProgressCols).contains("dailyGoalMinutes")

        // Assert missions table is created
        val missionsColsCursor = db.query("PRAGMA table_info(missions)")
        val missionsCols = mutableListOf<String>()
        while (missionsColsCursor.moveToNext()) {
            missionsCols.add(missionsColsCursor.getString(missionsColsCursor.getColumnIndexOrThrow("name")))
        }
        missionsColsCursor.close()
        assertThat(missionsCols).containsAtLeast("id", "title", "description", "goalType", "goalValue", "rewardSeeds", "startsAt", "endsAt", "isPremium", "sortOrder")

        // 14 -> 15
        AppDatabase.MIGRATION_14_15.migrate(db)

        // Assert indices are created
        val sessionsIndexCursor = db.query("PRAGMA index_list(sessions)")
        var hasSessionsStatusCompletedAtIndex = false
        while (sessionsIndexCursor.moveToNext()) {
            val name = sessionsIndexCursor.getString(sessionsIndexCursor.getColumnIndexOrThrow("name"))
            if (name == "index_sessions_status_completedAt") {
                hasSessionsStatusCompletedAtIndex = true
            }
        }
        sessionsIndexCursor.close()
        assertThat(hasSessionsStatusCompletedAtIndex).isTrue()

        val gardenEntriesIndexCursor = db.query("PRAGMA index_list(garden_entries)")
        var hasGardenEntriesStateIndex = false
        while (gardenEntriesIndexCursor.moveToNext()) {
            val name = gardenEntriesIndexCursor.getString(gardenEntriesIndexCursor.getColumnIndexOrThrow("name"))
            if (name == "index_garden_entries_state") {
                hasGardenEntriesStateIndex = true
            }
        }
        gardenEntriesIndexCursor.close()
        assertThat(hasGardenEntriesStateIndex).isTrue()

        db.version = 15
        helper.close()

        // Verify Room loads the migrated database correctly
        val appDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            dbName
        ).addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9,
            AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_10_11,
            AppDatabase.MIGRATION_11_12,
            AppDatabase.MIGRATION_12_13,
            AppDatabase.MIGRATION_13_14,
            AppDatabase.MIGRATION_14_15,
        ).allowMainThreadQueries()
        .build()

        val sessionDao = appDatabase.sessionDao()
        val allSessions = sessionDao.getRecentSessionsSnapshot(10)
        assertThat(allSessions).hasSize(1)
        assertThat(allSessions[0].plantId).isEqualTo("sprout")

        appDatabase.close()
    }
}
