package com.focusfarm.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: WalletTransactionEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM wallet_transactions WHERE type = :type AND sourceId = :sourceId)")
    suspend fun existsByTypeAndSource(type: String, sourceId: String): Boolean

    @Query("SELECT * FROM wallet_transactions ORDER BY createdAt DESC")
    fun observeAllTransactions(): Flow<List<WalletTransactionEntity>>

    @Query("SELECT SUM(amount) FROM wallet_transactions WHERE userId = :userId")
    suspend fun getBalance(userId: String = "user_0"): Int?

    @Query("SELECT SUM(amount) FROM wallet_transactions WHERE userId = :userId")
    fun observeBalance(userId: String = "user_0"): Flow<Int?>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM wallet_transactions WHERE userId = :userId AND amount > 0 AND createdAt >= :start AND createdAt <= :end")
    fun getSeedsEarnedInRange(start: Long, end: Long, userId: String = "user_0"): Flow<Int>
}
