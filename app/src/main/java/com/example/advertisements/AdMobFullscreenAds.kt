package com.example.advertisements

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader

/**
 * Full-screen GMA Next-Gen ad lifecycle.
 *
 * Ads are preloaded only after UMP has allowed requests and the website Admin Panel
 * explicitly enables the Google AdMob provider and the relevant placement.
 * Interstitials are only shown at a user-initiated navigation transition, never
 * over active video playback. Reward callbacks are exposed to the caller; this
 * class does not invent wallet credits or grant client-authoritative money.
 */
object AdMobFullscreenAds {
    private var configuredInterstitialId: String? = null
    private var configuredRewardedId: String? = null
    private var lastFullscreenImpressionAtMs = 0L
    private var interstitialImpressionsThisSession = 0
    private var completedVideosThisSession = 0
    private var isInterstitialShowing = false

    private const val MIN_FULLSCREEN_GAP_MS = 180_000L
    private const val MAX_FULLSCREEN_IMPRESSIONS_PER_SESSION = 4
    private const val VIDEOS_BETWEEN_INTERSTITIALS = 5

    fun startPreloading(interstitialUnitId: String?, rewardedUnitId: String?) {
        if (!AdMobRuntime.isInitialized.value) return

        interstitialUnitId?.takeIf { it.isNotBlank() }?.let { id ->
            if (configuredInterstitialId != id) {
                configuredInterstitialId = id
                InterstitialAdPreloader.start(id, PreloadConfiguration(AdRequest.Builder(id).build()))
            }
        }
        rewardedUnitId?.takeIf { it.isNotBlank() }?.let { id ->
            if (configuredRewardedId != id) {
                configuredRewardedId = id
                RewardedAdPreloader.start(id, PreloadConfiguration(AdRequest.Builder(id).build()))
            }
        }
    }

    fun onVideoStarted() {
        completedVideosThisSession += 1
    }

    /**
     * Returns false and immediately invokes onFinished when no eligible ad is ready.
     * Call only for an explicit transition such as the viewer tapping Back.
     */
    fun showInterstitialAtTransition(activity: Activity, onFinished: () -> Unit): Boolean {
        val id = configuredInterstitialId
        val now = System.currentTimeMillis()
        val eligible = id != null &&
            !isInterstitialShowing &&
            interstitialImpressionsThisSession < MAX_FULLSCREEN_IMPRESSIONS_PER_SESSION &&
            completedVideosThisSession >= VIDEOS_BETWEEN_INTERSTITIALS &&
            (lastFullscreenImpressionAtMs == 0L || now - lastFullscreenImpressionAtMs >= MIN_FULLSCREEN_GAP_MS)

        if (!eligible) {
            onFinished()
            return false
        }

        val ad = InterstitialAdPreloader.pollAd(id!!)
        if (ad == null) {
            onFinished()
            return false
        }

        isInterstitialShowing = true
        var finished = false
        fun finishOnce() {
            if (!finished) {
                finished = true
                isInterstitialShowing = false
                onFinished()
            }
        }

        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() = Unit
            override fun onAdDismissedFullScreenContent() = finishOnce()
            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) = finishOnce()
            override fun onAdImpression() {
                lastFullscreenImpressionAtMs = System.currentTimeMillis()
                interstitialImpressionsThisSession += 1
                completedVideosThisSession = 0
            }
            override fun onAdClicked() = Unit
        }
        try {
            ad.show(activity)
            return true
        } catch (_: Exception) {
            finishOnce()
            return false
        }
    }

    /**
     * Shows a rewarded ad only after an explicit user action. onRewardEarned is called
     * exclusively from Google's earned-reward callback. The caller must connect this
     * to a server-verified entitlement before adding wallet value or creator earnings.
     */
    fun showRewardedAfterOptIn(
        activity: Activity,
        onRewardEarned: (amount: Int, type: String) -> Unit,
        onFinished: () -> Unit = {}
    ): Boolean {
        val id = configuredRewardedId ?: return false
        val ad = RewardedAdPreloader.pollAd(id) ?: return false
        var finished = false
        fun finishOnce() {
            if (!finished) {
                finished = true
                onFinished()
            }
        }
        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdShowedFullScreenContent() = Unit
            override fun onAdDismissedFullScreenContent() = finishOnce()
            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) = finishOnce()
            override fun onAdImpression() {
                lastFullscreenImpressionAtMs = System.currentTimeMillis()
            }
            override fun onAdClicked() = Unit
        }
        return try {
            ad.show(activity) { rewardItem ->
                onRewardEarned(rewardItem.amount, rewardItem.type)
            }
            true
        } catch (_: Exception) {
            finishOnce()
            false
        }
    }

    fun stopPreloading() {
        configuredInterstitialId?.let(InterstitialAdPreloader::destroy)
        configuredRewardedId?.let(RewardedAdPreloader::destroy)
        configuredInterstitialId = null
        configuredRewardedId = null
    }
}
