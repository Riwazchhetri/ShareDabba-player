package com.example.guest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.app.Activity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.core.config.EnvironmentConfig
import com.example.advertisements.AdMobConsentManager
import com.example.advertisements.AdMobConfig
import com.example.advertisements.AdMobFullscreenAds
import com.example.advertisements.AdMobRuntime
import com.example.advertisements.ShareDabbaBannerAd
import com.example.app.ShareDabbaApplication
import com.example.remoteconfig.PublishedRemoteConfig
import androidx.compose.runtime.collectAsState
import com.example.network.NetworkClient
import kotlinx.coroutines.launch
import java.net.URI

private const val API_ROOT = "https://sharedabba-pwa-api.onrender.com"

@Composable
fun GuestPlaybackScreen(initialLink: String? = null) {
    val context = LocalContext.current
    val activity = context as? Activity
    val consentManager = remember(activity) { activity?.let { AdMobConsentManager(it) } }
    var privacyOptionsRequired by remember { mutableStateOf(false) }
    LaunchedEffect(consentManager) {
        privacyOptionsRequired = consentManager?.isPrivacyOptionsRequired() == true
    }
    val appContainer = remember(context) {
        (context.applicationContext as? ShareDabbaApplication)?.container
    }
    val remoteConfigState = appContainer?.remoteConfigManager?.activeConfig?.collectAsState()
    val remoteConfig = remoteConfigState?.value ?: PublishedRemoteConfig.SAFE_DEFAULT
    val googleAdsApproved = remoteConfig.advertising.approvedAdProvider == "GOOGLE_ADMOB_APPROVED"
    val bannerAdsEnabled = googleAdsApproved && remoteConfig.advertising.enableBannerPlacements
    val interstitialAdsEnabled = googleAdsApproved && remoteConfig.advertising.enableInterstitialPlacements
    val rewardedAdsEnabled = googleAdsApproved && remoteConfig.advertising.enableRewardedPlacements
    val adsSdkInitialized by AdMobRuntime.isInitialized.collectAsState()
    val interstitialUnitId = remoteConfig.advertising.interstitialAdUnitId
        .ifBlank { AdMobConfig.interstitialUnitId }
    val rewardedUnitId = remoteConfig.advertising.rewardedAdUnitId
        .ifBlank { AdMobConfig.rewardedUnitId }

    LaunchedEffect(adsSdkInitialized, interstitialAdsEnabled, rewardedAdsEnabled, interstitialUnitId, rewardedUnitId) {
        if (adsSdkInitialized && (interstitialAdsEnabled || rewardedAdsEnabled)) {
            AdMobFullscreenAds.startPreloading(
                interstitialUnitId = interstitialUnitId.takeIf { interstitialAdsEnabled },
                rewardedUnitId = rewardedUnitId.takeIf { rewardedAdsEnabled }
            )
        }
    }
    val scope = rememberCoroutineScope()
    val config = remember { EnvironmentConfig() }
    val api = remember { NetworkClient.createApiService(config) }
    var link by remember { mutableStateOf(initialLink.orEmpty()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var playbackUrl by remember { mutableStateOf<String?>(null) }
    var videoTitle by remember { mutableStateOf("") }
    var creator by remember { mutableStateOf("") }

    LaunchedEffect(initialLink) {
        if (!initialLink.isNullOrBlank()) link = initialLink
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF101014))) {
        if (playbackUrl != null) {
            GuestVideoSurface(
                url = playbackUrl!!,
                title = videoTitle,
                onBack = { playbackUrl = null },
                bannerAdsEnabled = bannerAdsEnabled,
                interstitialAdsEnabled = interstitialAdsEnabled,
                rewardedAdsEnabled = rewardedAdsEnabled
            )
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ShareDabbaBannerAd(
                    modifier = Modifier.fillMaxWidth(),
                    slotKey = "home-top",
                    enabled = bannerAdsEnabled
                )
                Spacer(Modifier.height(12.dp))
                Text("ShareDabba Player", color = Color.White, fontSize = 27.sp)
                Spacer(Modifier.height(8.dp))
                Text("Paste a ShareDabba video link to play", color = Color(0xFFB8B8C5), fontSize = 14.sp)
                Spacer(Modifier.height(26.dp))
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ShareDabba link") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val slug = extractShareSlug(link)
                        if (slug == null) {
                            error = "This ShareDabba link is not valid."
                        } else {
                            loading = true
                            error = null
                            scope.launch {
                                try {
                                    val response = api.getGuestPlayback(slug)
                                    videoTitle = response.file.name
                                    creator = response.file.creator.orEmpty()
                                    playbackUrl = API_ROOT + response.playbackUrl
                                } catch (e: retrofit2.HttpException) {
                                    error = when (e.code()) {
                                        404, 410 -> "This video is currently unavailable."
                                        else -> "Could not load this video. Please try again."
                                    }
                                } catch (_: Exception) {
                                    error = "Could not connect. Check your internet and try again."
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    },
                    enabled = !loading && link.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (loading) CircularProgressIndicator() else Text("Play Video")
                }
                ShareDabbaBannerAd(
                    modifier = Modifier.fillMaxWidth(),
                    slotKey = "home-below-play",
                    enabled = bannerAdsEnabled
                )
                Spacer(Modifier.height(8.dp))
                ShareDabbaBannerAd(
                    modifier = Modifier.fillMaxWidth(),
                    slotKey = "home-bottom",
                    enabled = bannerAdsEnabled
                )
                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(error!!, color = Color(0xFFFF8A8A), fontSize = 14.sp)
                }
                if (privacyOptionsRequired) {
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            consentManager?.let { manager ->
                                manager.showPrivacyOptionsForm {
                                    privacyOptionsRequired = manager.isPrivacyOptionsRequired()
                                }
                            }
                        }
                    ) {
                        Text("Privacy settings", color = Color(0xFFB8B8C5))
                    }
                }
                if (creator.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Uploaded by $creator", color = Color(0xFFB8B8C5), fontSize = 12.sp)
                }
            }
        }
    }
}

private fun extractShareSlug(raw: String): String? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    if (!value.contains("://") && value.matches(Regex("[A-Za-z0-9_-]{3,128}"))) {
        return value
    }

    val uri = try {
        URI(value)
    } catch (_: Exception) {
        return null
    }
    if (uri.userInfo != null || uri.port != -1) return null

    val host = uri.host?.lowercase() ?: return null
    val segments = uri.path.orEmpty().split('/').filter { it.isNotBlank() }
    val slug = when {
        uri.scheme == "https" && host in setOf("sharedabba.com", "www.sharedabba.com") -> {
            if (segments.size < 2 || segments.first().lowercase() !in setOf("v", "watch", "video")) return null
            segments[1]
        }
        uri.scheme == "https" && host == "sharedabba-pwa-api.onrender.com" -> {
            if (segments.size != 2 || segments.first().lowercase() != "s") return null
            segments[1]
        }
        uri.scheme == "sharedabba" && host in setOf("v", "watch", "video") -> {
            segments.firstOrNull()
        }
        else -> return null
    }
    return slug?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{3,128}")) }
}

@Composable
private fun GuestVideoSurface(
    url: String,
    title: String,
    onBack: () -> Unit,
    bannerAdsEnabled: Boolean,
    interstitialAdsEnabled: Boolean,
    rewardedAdsEnabled: Boolean
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var skipNextInterstitial by remember(url) { mutableStateOf(false) }
    var rewardMessage by remember(url) { mutableStateOf<String?>(null) }
    LaunchedEffect(url) { AdMobFullscreenAds.onVideoStarted() }
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {}
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Text(
            "‹  Back",
            color = Color.White,
            modifier = Modifier.padding(16.dp).clickable {
                if (skipNextInterstitial || !interstitialAdsEnabled || activity == null) {
                    skipNextInterstitial = false
                    onBack()
                } else {
                    AdMobFullscreenAds.showInterstitialAtTransition(activity, onFinished = onBack)
                }
            }
        )
        Text(title, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (rewardedAdsEnabled && activity != null) {
            TextButton(
                onClick = {
                    val shown = AdMobFullscreenAds.showRewardedAfterOptIn(
                        activity = activity,
                        onRewardEarned = { _, _ ->
                            skipNextInterstitial = true
                            rewardMessage = "Reward complete: your next full-screen ad is skipped."
                        }
                    )
                    if (!shown) rewardMessage = "Reward ad is not ready yet. You can keep watching."
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text("Watch ad to skip next full-screen ad", color = Color(0xFFB9A5FF))
            }
            rewardMessage?.let { message ->
                Text(message, color = Color(0xFFB8B8C5), modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
        ShareDabbaBannerAd(
            modifier = Modifier.fillMaxWidth(),
            slotKey = "player-top",
            enabled = bannerAdsEnabled
        )
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        ShareDabbaBannerAd(
            modifier = Modifier.fillMaxWidth(),
            slotKey = "player-bottom",
            enabled = bannerAdsEnabled
        )
    }
}
