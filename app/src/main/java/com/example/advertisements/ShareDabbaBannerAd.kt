package com.example.advertisements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

/**
 * A lifecycle-aware GMA Next-Gen banner slot.
 * It never requests an ad until UMP has permitted requests and GMA is initialized.
 */
@Composable
fun ShareDabbaBannerAd(
    modifier: Modifier = Modifier,
    slotKey: String
) {
    val isInitialized by AdMobRuntime.isInitialized.collectAsState()
    val isPreview = LocalInspectionMode.current
    if (!isInitialized || isPreview) return

    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val adWidthDp = (screenWidthDp - 24).coerceAtLeast(1)
    val adView = remember(slotKey) { AdView(context) }
    val adSize = remember(adWidthDp) {
        AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, adWidthDp)
    }

    AndroidView(
        modifier = modifier,
        factory = { adView }
    )

    LaunchedEffect(adView, adSize, slotKey) {
        val request = BannerAdRequest.Builder(AdMobConfig.bannerUnitId, adSize).build()
        adView.loadAd(
            request,
            object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) = Unit
                override fun onAdFailedToLoad(adError: LoadAdError) = Unit
            }
        )
    }

    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }
}
