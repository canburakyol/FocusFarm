package com.focusfarm.app.data.repository

import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.data.local.WalletTransactionEntity
import com.focusfarm.app.domain.TransactionType
import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface RewardGrantRepository {
    suspend fun hasClaimedRewardToday(placement: String, dateKey: String): Boolean
    fun observeHasClaimedRewardToday(placement: String, dateKey: String): Flow<Boolean>
    suspend fun grantReward(
        transactionId: String,
        placement: String,
        sourceId: String,
        amount: Int,
        nowMillis: Long
    ): Boolean
}

@Singleton
class RewardGrantRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
) : RewardGrantRepository {

    override suspend fun hasClaimedRewardToday(placement: String, dateKey: String): Boolean {
        return database.walletDao().existsByTypeAndSource(
            type = TransactionType.REWARDED_AD.name,
            sourceId = "$placement-$dateKey"
        )
    }

    override fun observeHasClaimedRewardToday(placement: String, dateKey: String): Flow<Boolean> {
        return database.walletDao().observeAllTransactions().map { transactions ->
            transactions.any {
                it.type == TransactionType.REWARDED_AD.name &&
                it.sourceId == "$placement-$dateKey"
            }
        }
    }

    override suspend fun grantReward(
        transactionId: String,
        placement: String,
        sourceId: String,
        amount: Int,
        nowMillis: Long
    ): Boolean {
        return database.withTransaction {
            val alreadyExists = database.walletDao().existsByTypeAndSource(
                type = TransactionType.REWARDED_AD.name,
                sourceId = sourceId
            )
            if (alreadyExists) return@withTransaction false

            if (amount > 0) {
                database.progressDao().addSeeds(amount)
            }

            val result = database.walletDao().insertTransaction(
                WalletTransactionEntity(
                    id = transactionId,
                    userId = "user_0",
                    type = TransactionType.REWARDED_AD.name,
                    amount = amount,
                    sourceType = "ad_placement",
                    sourceId = sourceId,
                    createdAt = nowMillis
                )
            )
            result != -1L
        }
    }
}
