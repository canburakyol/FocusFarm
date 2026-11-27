package com.focusfarm.app.analytics

import com.focusfarm.app.telemetry.AppTelemetry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsTracker @Inject constructor(
    private val telemetry: AppTelemetry,
) {

    fun logEvent(name: String, params: Map<String, String> = emptyMap()) {
        telemetry.logEvent(name, params)
    }

    fun trackSessionStart(plantId: String, durationMinutes: Int) {
        telemetry.logEvent(
            AnalyticsEvents.Session.SESSION_START,
            AnalyticsEvents.Session.sessionStartParams(plantId, durationMinutes),
        )
    }

    fun trackSessionComplete(
        plantId: String,
        durationMinutes: Int,
        seedsEarned: Int,
    ) {
        telemetry.logEvent(
            AnalyticsEvents.Session.SESSION_COMPLETE,
            AnalyticsEvents.Session.sessionCompleteParams(plantId, durationMinutes, seedsEarned),
        )
    }

    fun trackSessionAbandon(plantId: String, durationMinutes: Int) {
        telemetry.logEvent(
            AnalyticsEvents.Session.SESSION_ABANDON,
            AnalyticsEvents.Session.sessionAbandonParams(plantId, durationMinutes),
        )
    }

    fun trackPaywallView(source: String) {
        telemetry.logEvent(
            AnalyticsEvents.Paywall.PAYWALL_VIEW,
            AnalyticsEvents.Paywall.paywallViewParams(source),
        )
    }

    fun trackPurchaseStart(planId: String) {
        telemetry.logEvent(
            AnalyticsEvents.Paywall.PURCHASE_START,
            AnalyticsEvents.Paywall.purchaseStartParams(planId),
        )
    }

    fun trackPurchaseSuccess(planId: String, price: String) {
        telemetry.logEvent(
            AnalyticsEvents.Paywall.PURCHASE_SUCCESS,
            AnalyticsEvents.Paywall.purchaseSuccessParams(planId, price),
        )
    }

    fun trackReferralSent() {
        telemetry.logEvent(AnalyticsEvents.Referral.REFERRAL_SENT, emptyMap())
    }

    fun trackReferralReceived(referralCode: String) {
        telemetry.logEvent(
            AnalyticsEvents.Referral.REFERRAL_RECEIVED,
            AnalyticsEvents.Referral.referralReceivedParams(referralCode),
        )
    }

    fun trackShareGarden(plantCount: Int, streakDays: Int) {
        telemetry.logEvent(
            AnalyticsEvents.Share.SHARE_GARDEN,
            AnalyticsEvents.Share.shareGardenParams(plantCount, streakDays),
        )
    }

    fun trackStreakFreezeUsed(currentStreak: Int) {
        telemetry.logEvent(
            AnalyticsEvents.Streak.STREAK_FREEZE_USED,
            AnalyticsEvents.Streak.streakFreezeUsedParams(currentStreak),
        )
    }

    fun trackNotificationTapped(type: String) {
        telemetry.logEvent(
            AnalyticsEvents.Notification.NOTIFICATION_TAPPED,
            AnalyticsEvents.Notification.notificationTappedParams(type),
        )
    }

    fun trackRewardedAdShown(placement: String) {
        telemetry.logEvent(
            AnalyticsEvents.RewardedAd.REWARDED_AD_SHOWN,
            AnalyticsEvents.RewardedAd.rewardedAdShownParams(placement),
        )
    }

    fun trackGardenView(plantCount: Int) {
        telemetry.logEvent(
            AnalyticsEvents.Garden.GARDEN_VIEW,
            AnalyticsEvents.Garden.gardenViewParams(plantCount),
        )
    }

    fun trackQuestClaimed(questType: String, seeds: Int) {
        telemetry.logEvent(
            AnalyticsEvents.Quest.QUEST_CLAIMED,
            AnalyticsEvents.Quest.questClaimedParams(questType, seeds),
        )
    }
}
