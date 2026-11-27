package com.focusfarm.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.focusfarm.app.domain.PlantSpeciesCatalog
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.ConscryptMode.Mode.OFF

@RunWith(RobolectricTestRunner::class)
@ConscryptMode(OFF)
class AppDatabaseSeedTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private val repairDatabaseName = "empty_species_repair_test"

    @Before
    fun setUp(): Unit {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(repairDatabaseName)
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(AppDatabase.SEED_BUILT_IN_SPECIES_CALLBACK)
            .build()

        // Force Room to create/open the database so the callback has run.
        database.openHelper.writableDatabase
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(repairDatabaseName)
    }

    @Test
    fun freshDatabaseSeedsEveryBuiltInPlantSpecies(): Unit = runBlocking {
        val seededIds = PlantSpeciesCatalog.ALL.mapNotNull { species ->
            database.plantSpeciesDao().getById(species.id)?.id
        }

        assertThat(seededIds).containsExactlyElementsIn(PlantSpeciesCatalog.ALL.map { it.id })
        Unit
    }

    @Test
    fun localDataClearRestoresBuiltInPlantSpecies(): Unit = runBlocking {
        database.clearAllTables()
        AppDatabase.seedBuiltInPlantSpecies(database.openHelper.writableDatabase)

        val seededIds = PlantSpeciesCatalog.ALL.mapNotNull { species ->
            database.plantSpeciesDao().getById(species.id)?.id
        }
        assertThat(seededIds).containsExactlyElementsIn(PlantSpeciesCatalog.ALL.map { it.id })
        Unit
    }

    @Test
    fun openingExistingDatabaseRepairsMissingPlantSpecies() = runBlocking {
        database.close()
        database = Room.databaseBuilder(context, AppDatabase::class.java, repairDatabaseName)
            .allowMainThreadQueries()
            .build()
        database.openHelper.writableDatabase
        assertThat(database.plantSpeciesDao().getById("sprout")).isNull()
        database.close()

        database = Room.databaseBuilder(context, AppDatabase::class.java, repairDatabaseName)
            .allowMainThreadQueries()
            .addCallback(AppDatabase.SEED_BUILT_IN_SPECIES_CALLBACK)
            .build()
        database.openHelper.writableDatabase

        assertThat(database.plantSpeciesDao().getById("sprout")).isNotNull()
    }
}
