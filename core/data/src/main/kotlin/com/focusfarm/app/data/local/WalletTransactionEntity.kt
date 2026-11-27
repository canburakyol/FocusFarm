package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wallet_transactions",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["type"]),
        Index(value = ["sourceType", "sourceId"]),
    ],
)
data class WalletTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String, // TransactionType.name
    val amount: Int,
    val sourceType: String,
    val sourceId: String,
    val createdAt: Long,
)
