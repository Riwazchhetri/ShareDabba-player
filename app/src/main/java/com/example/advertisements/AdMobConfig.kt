package com.example.advertisements

import com.example.BuildConfig

/**
 * AdMob identifiers for ShareDabba.
 *
 * Debug builds ALWAYS use Google's official demo ad units.
 * Release builds use the production units supplied for ShareDabba.
 *
 * Keep ad requests behind the app's consent flow; do not load ads before
 * consent/request flags have been applied where required.
 */
object AdMobConfig {
    const val APP_ID = "ca-app-pub-4643856509495992~3638853837"

    private const val LIVE_BANNER = "ca-app-pub-4643856509495992/6520168046"
    private const val LIVE_INTERSTITIAL = "ca-app-pub-4643856509495992/1559485409"
    private const val LIVE_REWARDED_INTERSTITIAL = "ca-app-pub-4643856509495992/5550489272"
    private const val LIVE_REWARDED = "ca-app-pub-4643856509495992/5929588213"

    // Google-provided GMA Next-Gen test ad units. Never replace these in debug builds.
    const val testBannerUnitId = "/21775744923/example/adaptive-banner"
    const val testInterstitialUnitId = "ca-app-pub-3940256099942544/1033173712"
    const val testRewardedInterstitialUnitId = "ca-app-pub-3940256099942544/5354046379"
    const val testRewardedUnitId = "ca-app-pub-3940256099942544/5224354917"

    val bannerUnitId: String
        get() = if (BuildConfig.DEBUG) testBannerUnitId else LIVE_BANNER

    val interstitialUnitId: String
        get() = if (BuildConfig.DEBUG) testInterstitialUnitId else LIVE_INTERSTITIAL

    val rewardedInterstitialUnitId: String
        get() = if (BuildConfig.DEBUG) testRewardedInterstitialUnitId else LIVE_REWARDED_INTERSTITIAL

    val rewardedUnitId: String
        get() = if (BuildConfig.DEBUG) testRewardedUnitId else LIVE_REWARDED

    val isUsingTestAds: Boolean
        get() = BuildConfig.DEBUG
}
