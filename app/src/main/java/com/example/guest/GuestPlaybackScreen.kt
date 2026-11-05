package com.example.guest

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.core.config.EnvironmentConfig
import com.example.network.NetworkClient
import android.os.Handler
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import kotlinx.coroutines.launch
import java.net.URI

private const val API_ROOT = "https://sharedabba-pwa-api.onrender.com"
private const val TEST_BANNER_ID = "/21775744923/example/adaptive-banner"
private const val TEST_REWARDED_ID = "/6499/example/rewarded"
private val PageBackground = Color(0xFF090A12)
private val CardBackground = Color(0xFF151827)
private val Accent = Color(0xFF9B7BFF)
private val MutedText = Color(0xFFADB3C8)

@Composable
fun GuestPlaybackScreen(initialLink: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config = remember { EnvironmentConfig() }
    val api = remember { NetworkClient.createApiService(config) }
    var link by remember { mutableStateOf(initialLink.orEmpty()) }
    var loading by remember { mutableStateOf(false) }
    var adLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingPlaybackUrl by remember { mutableStateOf<String?>(null) }
    var playbackUrl by remember { mutableStateOf<String?>(null) }
    var videoTitle by remember { mutableStateOf("") }
    var creator by remember { mutableStateOf("") }
    var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) }
    var rewardReady by remember { mutableStateOf(false) }
    var showUnlockChoice by remember { mutableStateOf(false) }

    fun preloadRewarded() {
        if (rewardedAd != null || adLoading) return
        adLoading = true
        RewardedAd.load(
            AdRequest.Builder(TEST_REWARDED_ID).build(),
            object : AdLoadCallback<RewardedAd> {
                override fun onAdLoaded(ad: RewardedAd) {
                    Handler(Looper.getMainLooper()).post {
                        rewardedAd = ad
                        rewardReady = true
                        adLoading = false
                    }
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Handler(Looper.getMainLooper()).post {
                        rewardedAd = null
                        rewardReady = false
                        adLoading = false
                    }
                }
            }
        )
    }

    LaunchedEffect(Unit) { preloadRewarded() }
    LaunchedEffect(initialLink) { if (!initialLink.isNullOrBlank()) link = initialLink }

    fun resolveLink() {
        val slug = extractShareSlug(link)
        if (slug == null) {
            error = "Please enter a valid ShareDabba video link."
            return
        }
        loading = true
        error = null
        scope.launch {
            try {
                val response = api.getGuestPlayback(slug)
                videoTitle = response.file.name
                creator = response.file.creator.orEmpty()
                pendingPlaybackUrl = if (response.playbackUrl.startsWith("http")) response.playbackUrl else API_ROOT + response.playbackUrl
                showUnlockChoice = true
            } catch (e: retrofit2.HttpException) {
                error = when (e.code()) {
                    404, 410 -> "This video is unavailable or has expired."
                    else -> "We couldn't load this video. Please try again."
                }
            } catch (_: Exception) {
                error = "Connection problem. Check your internet and try again."
            } finally { loading = false }
        }
    }

    fun playWithoutReward() {
        playbackUrl = pendingPlaybackUrl
        pendingPlaybackUrl = null
        showUnlockChoice = false
    }

    fun showRewardedThenPlay() {
        val activity = context.findActivity()
        val ad = rewardedAd
        if (activity == null || ad == null) {
            error = "Rewarded ad is not ready. You can continue without the ad."
            return
        }
        rewardReady = false
        rewardedAd = null
        var earned = false
        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                Handler(Looper.getMainLooper()).post {
                    if (earned) playWithoutReward()
                    preloadRewarded()
                }
            }
            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                Handler(Looper.getMainLooper()).post {
                    rewardedAd = null
                    rewardReady = false
                    adLoading = false
                    preloadRewarded()
                    // The user can still choose "Continue without ad".
                    showUnlockChoice = true
                }
            }
            override fun onAdShowedFullScreenContent() {}
            override fun onAdImpression() {}
            override fun onAdClicked() {}
        }
        ad.show(activity, object : OnUserEarnedRewardListener {
            override fun onUserEarnedReward(rewardItem: com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem) {
                earned = true
            }
        })
    }

    Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
        if (playbackUrl != null) {
            GuestVideoSurface(url = playbackUrl!!, title = videoTitle, creator = creator, onBack = { playbackUrl = null })
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Accent) {
                        Text("SD", Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("ShareDabba", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("PLAYER", color = MutedText, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("●", color = Color(0xFF64DDAA))
                }
                AdBanner(Modifier.fillMaxWidth())
                Spacer(Modifier.weight(1f))
                Text("Your videos,\none tap away.", color = Color.White, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text("Paste a ShareDabba link below to start watching.", color = MutedText, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text("OPEN A VIDEO", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = link, onValueChange = { link = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Paste video link") }, placeholder = { Text("https://sharedabba.com/v/…") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = { resolveLink() }, enabled = !loading && link.isNotBlank(), modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                            if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("Continue  ›", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        if (!error.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(error!!, color = Color(0xFFFF8585), fontSize = 13.sp)
                        }
                        if (creator.isNotBlank()) Text("Uploaded by $creator", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                AdBanner(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("No account needed • ShareDabba Player", color = MutedText, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
            }
        }
    }

    if (showUnlockChoice) {
        AlertDialog(
            onDismissRequest = { showUnlockChoice = false },
            title = { Text("Unlock this video?") },
            text = { Text("Watch a short rewarded ad to unlock playback, or continue without the ad.") },
            confirmButton = {
                TextButton(onClick = { showRewardedThenPlay() }, enabled = rewardReady) {
                    Text(if (rewardReady) "Watch ad & play" else if (adLoading) "Loading ad…" else "Ad unavailable")
                }
            },
            dismissButton = { TextButton(onClick = { playWithoutReward() }) { Text("Continue without ad") } }
        )
    }
}

@Composable
private fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context.findActivity() ?: return
    var adView by remember { mutableStateOf<AdView?>(null) }
    var loaded by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier.wrapContentHeight(),
        factory = { viewContext ->
            AdView(viewContext).also { view ->
                adView = view
                val widthDp = (context.resources.displayMetrics.widthPixels / context.resources.displayMetrics.density).toInt().coerceAtLeast(320)
                val size = AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, widthDp)
                view.loadAd(
                    BannerAdRequest.Builder(TEST_BANNER_ID, size).build(),
                    object : AdLoadCallback<BannerAd> {
                        override fun onAdLoaded(ad: BannerAd) {
                            Handler(Looper.getMainLooper()).post { loaded = true }
                            ad.adEventCallback = object : BannerAdEventCallback {}
                        }
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Handler(Looper.getMainLooper()).post { loaded = false }
                        }
                    }
                )
            }
        },
        update = { }
    )
    DisposableEffect(adView) {
        onDispose {
            adView?.destroy()
            adView = null
        }
    }
}
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun extractShareSlug(raw: String): String? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    if (!value.contains("://") && value.matches(Regex("[A-Za-z0-9_-]{3,128}"))) return value
    val uri = try { URI(value) } catch (_: Exception) { return null }
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
        uri.scheme == "sharedabba" && host in setOf("v", "watch", "video") -> segments.firstOrNull()
        else -> return null
    }
    return slug?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{3,128}")) }
}

@Composable
private fun GuestVideoSurface(url: String, title: String, creator: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹ Back", color = Color.White, modifier = Modifier.clickable(onClick = onBack).padding(10.dp))
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(title, color = Color.White, maxLines = 1)
                if (creator.isNotBlank()) Text("By $creator", color = MutedText, fontSize = 11.sp)
            }
        }
        AdBanner(Modifier.fillMaxWidth())
        AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } }, modifier = Modifier.fillMaxWidth().weight(1f))
        AdBanner(Modifier.fillMaxWidth())
    }
}
