package com.focusfarm.app.analytics

object AnalyticsEvents {

    object Session {
        const val SESSION_START = "session_start"
        const val SESSION_COMPLETE = "session_complete"
        const val SESSION_ABANDON = "session_abandon"
        const val SESSION_REVIVE = "session_revive"
        const val SESSION_SEED_REWARD_GRANTED = "session_seed_reward_granted"

        fun sessionStartParams(plantId: String, durationMinutes: Int): Map<String, String> = mapOf(
            "plant_id" to plantId,
            "duration_minutes" to durationMinutes.toString(),
        )

        fun sessionCompleteParams(
            plantId: String,
            durationMinutes: Int,
            seedsEarned: Int,
        ): Map<String, String> = mapOf(
            "plant_id" to plantId,
            "duration_minutes" to durationMinutes.toString(),
            "seeds_earned" to seedsEarned.toString(),
        )

        fun sessionAbandonParams(plantId: String, durationMinutes: Int): Map<String, String> = mapOf(
            "plant_id" to plantId,
            "duration_minutes" to durationMinutes.toString(),
        )
    }

    object Paywall {
        const val PAYWALL_VIEW = "paywall_view"
        const val PAYWALL_DISMISS = "paywall_dismiss"
        const val PURCHASE_START = "purchase_start"
        const val PURCHASE_SUCCESS = "purchase_success"
        const val PURCHASE_FAILED = "purchase_failed"
        const val RESTORE_PURCHASES = "restore_purchases"

        fun paywallViewParams(source: String): Map<String, String> = mapOf(
            "source" to source,
        )

        fun purchaseStartParams(planId: String): Map<String, String> = mapOf(
            "plan_id" to planId,
        )

        fun purchaseSuccessParams(planId: String, price: String): Map<String, String> = mapOf(
            "plan_id" to planId,
            "price" to price,
        )

        fun purchaseFailedParams(planId: String, error: String): Map<String, String> = mapOf(
            "plan_id" to planId,
            "error" to error,
        )
    }

    object Referral {
        const val REFERRAL_SENT = "referral_sent"
        const val REFERRAL_RECEIVED = "referral_received"
        const val REFERRAL_REWARD_GRANTED = "referral_reward_granted"

        fun referralReceivedParams(referralCode: String): Map<String, String> = mapOf(
            "referral_code_present" to referralCode.isNotBlank().toString(),
            "referral_code_length" to referralCode.length.toString(),
        )

        fun referralRewardGrantedParams(seeds: Int): Map<String, String> = mapOf(
            "seeds" to seeds.toString(),
        )
    }

    object Share {
        const val SHARE_GARDEN = "share_garden"
        const val SHARE_GARDEN_BOOST_GRANTED = "share_garden_boost_granted"

        fun shareGardenParams(plantCount: Int, streakDays: Int): Map<String, String> = mapOf(
            "plant_count" to plantCount.toString(),
            "streak_days" to streakDays.toString(),
        )
    }

    object Streak {
        const val STREAK_FREEZE_USED = "streak_freeze_used"
        const val STREAK_BROKEN = "streak_broken"
        const val STREAK_RESTORED = "streak_restored"

        fun streakFreezeUsedParams(currentStreak: Int): Map<String, String> = mapOf(
            "current_streak" to currentStreak.toString(),
        )

        fun streakBrokenParams(brokenStreak: Int): Map<String, String> = mapOf(
            "broken_streak" to brokenStreak.toString(),
        )
    }

    object Notification {
        const val NOTIFICATION_TAPPED = "notification_tapped"
        const val NOTIFICATION_DISMISSED = "notification_dismissed"

        fun notificationTappedParams(type: String): Map<String, String> = mapOf(
            "type" to type,
        )
    }

    object RewardedAd {
        const val REWARDED_AD_SHOWN = "rewarded_ad_shown"
        const val REWARDED_AD_REWARD_GRANTED = "rewarded_ad_reward_granted"

        fun rewardedAdShownParams(placement: String): Map<String, String> = mapOf(
            "placement" to placement,
        )

        fun rewardedAdRewardGrantedParams(
            placement: String,
            rewardAmount: Int,
            rewardType: String,
        ): Map<String, String> = mapOf(
            "placement" to placement,
            "reward_amount" to rewardAmount.toString(),
            "reward_type" to rewardType,
        )
    }

    object Garden {
        const val GARDEN_VIEW = "garden_view"
        const val COLLECTION_VIEW = "collection_view"
        const val PLANT_PURCHASE = "plant_purchase"

        fun gardenViewParams(plantCount: Int): Map<String, String> = mapOf(
            "plant_count" to plantCount.toString(),
        )
    }

    object Quest {
        const val QUEST_CLAIMED = "quest_claimed"
        const val QUEST_REROLL_USED = "quest_reroll_used"
        const val WEEKLY_REWARD_CLAIMED = "weekly_reward_claimed"

        fun questClaimedParams(questType: String, seeds: Int): Map<String, String> = mapOf(
            "quest_type" to questType,
            "seeds" to seeds.toString(),
        )
    }
}
