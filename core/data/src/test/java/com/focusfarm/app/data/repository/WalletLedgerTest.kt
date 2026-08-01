package com.focusfarm.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.data.billing.FakeBillingRepository
import com.focusfarm.app.domain.CalculateSessionRewardUseCase
import com.focusfarm.app.domain.TransactionType
import com.focusfarm.app.telemetry.AppTelemetry
import com.focusfarm.app.time.Clock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.ConscryptMode.Mode.OFF
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@ConscryptMode(OFF)
class WalletLedgerTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var clock: TestClock
    private lateinit var billingRepository: FakeBillingRepository
    private lateinit var progressRepository: ProgressRepositoryImpl
    private lateinit var purchaseProduct: PurchaseProductUseCase

    private class TestClock(private var currentDate: LocalDate) : Clock {
        override val zoneId: ZoneId get() = ZoneId.systemDefault()
        override fun nowMillis(): Long = currentDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        override fun currentDateFlow(): kotlinx.coroutines.flow.Flow<LocalDate> = kotlinx.coroutines.flow.flowOf(currentDate)
    }

    private val dummyTelemetry = object : AppTelemetry {
        override fun recordNonFatal(tag: String, message: String, throwable: Throwable?) {}
        override fun logEvent(name: String, params: Map<String, String>) {}
    }

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        clock = TestClock(LocalDate.of(2026, 3, 15))
        billingRepository = FakeBillingRepository()

        progressRepository = ProgressRepositoryImpl(
            context = context,
            database = database,
            progressDao = database.progressDao(),
            clock = clock,
            billingRepository = billingRepository,
        )

        purchaseProduct = PurchaseProductUseCase(
            database = database,
            clock = clock,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun walletLedgerPreventsDuplicateSessionRewards() = runTest {
        // Complete session 1
        val reward1 = progressRepository.grantSessionCompletionReward(
            sessionId = 12345L,
            durationMinutes = 25,
            nowMillis = clock.nowMillis(),
        )
        assertThat(reward1.grantedSeeds).isGreaterThan(0)

        // Try to claim again for the same sessionId
        val reward2 = progressRepository.grantSessionCompletionReward(
            sessionId = 12345L,
            durationMinutes = 25,
            nowMillis = clock.nowMillis(),
        )
        assertThat(reward2.grantedSeeds).isEqualTo(0)
    }

    @Test
    fun shopPurchaseFailsIfBalanceIsInsufficient() = runTest {
        progressRepository.ensureReady(clock.nowMillis())

        try {
            purchaseProduct(
                itemType = "PLANT_SEED",
                itemId = "carrot",
                price = 30,
            )
            // Should fail
            assertThat(true).isFalse()
        } catch (e: InsufficientSeedsException) {
            assertThat(e.required).isEqualTo(30)
            assertThat(e.available).isEqualTo(0)
        }
    }

    @Test
    fun shopPurchaseSucceedsIfBalanceIsSufficientAndWritesLedger() = runTest {
        database.progressDao().upsertUserProgress(
            com.focusfarm.app.data.local.UserProgressEntity(seeds = 100)
        )

        val result = purchaseProduct(
            itemType = "PLANT_SEED",
            itemId = "carrot",
            price = 30,
        )
        assertThat(result.spentSeeds).isEqualTo(30)
        assertThat(result.remainingSeeds).isEqualTo(70)
        assertThat(result.alreadyUnlocked).isFalse()

        // Verify inventory record exists
        val hasItem = database.inventoryDao().hasItem("PLANT_SEED", "carrot")
        assertThat(hasItem).isTrue()

        // Verify seeds balance matches
        val progress = database.progressDao().getUserProgress()
        assertThat(progress?.seeds).isEqualTo(70)

        val txExists = database.walletDao().existsByTypeAndSource(
            TransactionType.SHOP_PURCHASE.name,
            "carrot",
        )
        assertThat(txExists).isTrue()
    }

    @Test
    fun shopPurchasePreventsDuplicateProductUnlocks() = runTest {
        database.progressDao().upsertUserProgress(
            com.focusfarm.app.data.local.UserProgressEntity(seeds = 100)
        )

        val result1 = purchaseProduct(
            itemType = "PLANT_SEED",
            itemId = "carrot",
            price = 30,
        )
        assertThat(result1.spentSeeds).isEqualTo(30)

        // Try purchasing again
        val result2 = purchaseProduct(
            itemType = "PLANT_SEED",
            itemId = "carrot",
            price = 30,
        )
        assertThat(result2.spentSeeds).isEqualTo(0)
        assertThat(result2.alreadyUnlocked).isTrue()
    }
}
