package com.focusfarm.app.ads

import android.app.Activity
import android.content.Context
import com.focusfarm.app.telemetry.AppTelemetry
import com.focusfarm.app.monetization.ConsentManager
import com.focusfarm.core.ui.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardedAdManager @Inject constructor(
    private val telemetry: AppTelemetry,
    private val consentManager: ConsentManager,
) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private var isShowing = false

    object Placements {
        const val DAILY_QUEST_REROLL = "daily_quest_reroll"
        const val NEXT_SESSION_SEED_BOOST = "next_session_seed_boost"
        const val SESSION_REVIVE = "session_revive"
        const val SHARE_GARDEN_BOOST = "share_garden_boost"
        const val DAILY_BONUS_WHEEL = "daily_bonus_wheel"
    }

    fun preload(context: Context) {
        if (!consentManager.canLoadAds()) return
        loadIfNeeded(context.applicationContext)
    }

    fun show(
        activity: Activity,
        placement: String,
        onRewardEarned: () -> Unit,
        onNotReady: () -> Unit,
    ) {
        if (isShowing) return
        if (!consentManager.canLoadAds()) {
            onNotReady()
            return
        }

        val ad = rewardedAd
        if (ad == null) {
            loadIfNeeded(activity.applicationContext)
            onNotReady()
            return
        }

        rewardedAd = null
        isShowing = true

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                telemetry.logEvent(
                    name = "rewarded_ad_shown",
                    params = mapOf("placement" to placement),
                )
            }

            override fun onAdDismissedFullScreenContent() {
                isShowing = false
                loadIfNeeded(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isShowing = false
                telemetry.recordNonFatal(
                    tag = "rewarded_ad_show_failed",
                    message = "placement=$placement code=${adError.code} message=${adError.message}",
                )
                loadIfNeeded(activity.applicationContext)
                onNotReady()
            }
        }

        ad.show(activity) { rewardItem ->
            telemetry.logEvent(
                name = "rewarded_ad_reward_granted",
                params = mapOf(
                    "placement" to placement,
                    "reward_amount" to rewardItem.amount.toString(),
                    "reward_type" to rewardItem.type,
                ),
            )
            onRewardEarned()
        }
    }

    private fun loadIfNeeded(context: Context) {
        if (rewardedAd != null || isLoading) return

        isLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            context.getString(R.string.admob_rewarded_unit_id),
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoading = false
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoading = false
                    rewardedAd = null
                    telemetry.recordNonFatal(
                        tag = "rewarded_ad_load_failed",
                        message = "code=${loadAdError.code} message=${loadAdError.message}",
                    )
                }
            },
        )
    }
}
