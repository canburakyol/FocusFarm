package com.focusfarm.app.data.repository

import androidx.room.withTransaction
import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.data.local.InventoryItemEntity
import com.focusfarm.app.data.local.WalletTransactionEntity
import com.focusfarm.app.domain.TransactionType
import com.focusfarm.app.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

class InsufficientSeedsException(
    val required: Int,
    val available: Int,
) : IllegalStateException("Yetersiz tohum: $required gerekli, $available mevcut")

data class PurchaseProductResult(
    val itemId: String,
    val itemType: String,
    val spentSeeds: Int,
    val remainingSeeds: Int,
    val alreadyUnlocked: Boolean,
)

@Singleton
class PurchaseProductUseCase @Inject constructor(
    private val database: AppDatabase,
    private val clock: Clock,
) {
    suspend operator fun invoke(itemType: String, itemId: String, price: Int): PurchaseProductResult {
        require(itemType.isNotBlank()) { "itemType must not be blank" }
        require(itemId.isNotBlank()) { "itemId must not be blank" }
        require(price >= 0) { "price must be non-negative" }

        val now = clock.nowMillis()

        return database.withTransaction {
            val inventoryDao = database.inventoryDao()
            val walletDao = database.walletDao()
            val progressDao = database.progressDao()

            val alreadyHas = inventoryDao.hasItem(itemType, itemId)

            // Get seed balance
            val progress = progressDao.getUserProgress() ?: com.focusfarm.app.data.local.UserProgressEntity()
            val currentBalance = progress.seeds

            if (alreadyHas) {
                return@withTransaction PurchaseProductResult(
                    itemId = itemId,
                    itemType = itemType,
                    spentSeeds = 0,
                    remainingSeeds = currentBalance,
                    alreadyUnlocked = true,
                )
            }

            // Check if sufficient balance
            if (currentBalance < price) {
                throw InsufficientSeedsException(price, currentBalance)
            }

            // Debit seeds
            progressDao.addSeeds(-price)

            // Create inventory item record
            val inventoryId = "inv-$itemType-$itemId"
            val insertedItem = inventoryDao.insertItem(
                InventoryItemEntity(
                    id = inventoryId,
                    userId = "user_0",
                    itemType = itemType,
                    itemId = itemId,
                    purchasedAt = now,
                    quantity = 1,
                )
            )
            check(insertedItem != -1L) { "Inventory item insertion failed" }

            // Create wallet transaction ledger entry
            val transactionId = "tr-buy-$itemType-$itemId-$now"
            val insertedTransaction = walletDao.insertTransaction(
                WalletTransactionEntity(
                    id = transactionId,
                    userId = "user_0",
                    type = TransactionType.SHOP_PURCHASE.name,
                    amount = -price,
                    sourceType = "shop_item",
                    sourceId = itemId,
                    createdAt = now,
                )
            )
            check(insertedTransaction != -1L) { "Wallet transaction ledger insertion failed" }

            PurchaseProductResult(
                itemId = itemId,
                itemType = itemType,
                spentSeeds = price,
                remainingSeeds = currentBalance - price,
                alreadyUnlocked = false,
            )
        }
    }
}
