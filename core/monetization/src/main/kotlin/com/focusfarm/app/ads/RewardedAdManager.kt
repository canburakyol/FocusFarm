package com.focusfarm.app.ads

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class RewardedAdManager @Inject constructor(
    private val telemetry: AppTelemetry,
    private val consentManager: ConsentManager,
) {
    private var rewardedAd: RewardedAd? = null

    private val _adState = MutableStateFlow<RewardedAdState>(RewardedAdState.Idle)
    val adState: StateFlow<RewardedAdState> = _adState.asStateFlow()

    object Placements {
        const val DAILY_QUEST_REROLL = "daily_quest_reroll"
        const val NEXT_SESSION_SEED_BOOST = "next_session_seed_boost"
        const val SESSION_REVIVE = "session_revive"
        const val SHARE_GARDEN_BOOST = "share_garden_boost"
        const val DAILY_BONUS_WHEEL = "daily_bonus_wheel"
        const val DAILY_SEED_BONUS = "daily_seed_bonus"
        const val SHOP_SEED_PACK = "shop_seed_pack"
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
        if (_adState.value == RewardedAdState.Showing) return
        if (!consentManager.canLoadAds()) {
            _adState.value = RewardedAdState.Error("Reklam izni verilmedi")
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
        _adState.value = RewardedAdState.Showing

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                telemetry.logEvent(
                    name = "rewarded_ad_shown",
                    params = mapOf("placement" to placement),
                )
            }

            override fun onAdDismissedFullScreenContent() {
                _adState.value = RewardedAdState.Idle
                loadIfNeeded(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                _adState.value = RewardedAdState.Error("Reklam gösterilemedi: ${adError.message}")
                telemetry.recordNonFatal(
                    tag = "rewarded_ad_show_failed",
                    message = "placement=$placement code=${adError.code} message=${adError.message}",
                )
                loadIfNeeded(activity.applicationContext)
                onNotReady()
            }
        }

        ad.show(activity) { rewardItem ->
            _adState.value = RewardedAdState.Completed
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
        if (rewardedAd != null || _adState.value == RewardedAdState.Loading) return

        _adState.value = RewardedAdState.Loading
        val adRequest = AdRequest.Builder().build()

        val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val adUnitId = if (isDebug) {
            "ca-app-pub-3940256099942544/5224354917" // Official AdMob Rewarded Test ID
        } else {
            context.getString(R.string.admob_rewarded_unit_id)
        }

        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _adState.value = RewardedAdState.Ready
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    _adState.value = RewardedAdState.Error("Reklam yüklenemedi: ${loadAdError.message}")
                    telemetry.recordNonFatal(
                        tag = "rewarded_ad_load_failed",
                        message = "code=${loadAdError.code} message=${loadAdError.message}",
                    )
                }
            },
        )
    }
}
