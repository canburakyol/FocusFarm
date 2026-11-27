package com.focusfarm.app.domain

enum class TransactionType {
    SESSION_REWARD,
    MISSION_REWARD,
    SHOP_PURCHASE,
    REWARDED_AD,
    PREMIUM_BONUS,
    ADMIN_GRANT,
    REFUND,
}

data class WalletTransaction(
    val id: String,
    val userId: String,
    val type: TransactionType,
    val amount: Int,
    val sourceType: String,
    val sourceId: String,
    val createdAt: Long,
)
