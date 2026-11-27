package com.focusfarm.app.monetization

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.focusfarm.core.ui.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun BannerAdSlot(
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!enabled) return

    val context = LocalContext.current
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val adWidth = maxWidth.value.toInt().coerceAtLeast(1)
        val adView = remember(context, adWidth) {
            val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            AdView(context).apply {
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
                )
                adUnitId = if (isDebug) {
                    "ca-app-pub-3940256099942544/9214589741"
                } else {
                    context.getString(R.string.admob_banner_unit_id)
                }
                loadAd(AdRequest.Builder().build())
            }
        }

        DisposableEffect(adView) {
            onDispose { adView.destroy() }
        }

        AndroidView(factory = { adView })
    }
}
