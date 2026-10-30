package com.example.videoplayer

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.core.logging.AppLogger
import com.example.data.repository.StreamCacheRepository
import com.example.domain.usecase.SyncBookmarkUseCase
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.common.ShareDabbaTopBar
import com.example.ui.common.ShareDabbaVideoCard
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaPurple
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * STAGE 4 — SHARE DABBA NATIVE VIDEO PLAYER
 * Complete Android Media3 ExoPlayer implementation supporting all 20 required features:
 * 1. Play and pause
 * 2. Seek bar
 * 3. Current playback time
 * 4. Total duration
 * 5. Fullscreen
 * 6. Portrait and landscape support
 * 7. Playback speed selection
 * 8. Buffering indicator
 * 9. Network error handling
 * 10. Retry & playback recovery
 * 11. Volume control & mute
 * 12. Video title
 * 13. Uploader information
 * 14. Thumbnail poster (prevents unnecessary black screens)
 * 15. Related videos
 * 16. Next video
 * 17. Share button
 * 18. Picture-in-Picture where supported
 * 19. Video quality selection when multiple streams are supplied
 * 20. Proper lifecycle and player resource management
 */
@Composable
fun VideoPlayerScreen(
    initialStreamUrl: String?,
    syncBookmarkUseCase: SyncBookmarkUseCase,
    streamCacheRepository: StreamCacheRepository,
    onOpenRecentlyWatched: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var currentRequestKey by rememberSaveable(initialStreamUrl) {
        mutableStateOf(initialStreamUrl ?: "vid_featured_1")
    }
    var activeScenario by rememberSaveable {
        mutableStateOf(PlayerTestScenario.STANDARD_MP4)
    }

    val payload = remember(currentRequestKey, activeScenario) {
        VideoPlaybackValidator.resolveAuthorizedPayload(currentRequestKey, activeScenario)
    }

    var customUrlInput by rememberSaveable(payload.authorizedStreamUrl) {
        mutableStateOf(payload.authorizedStreamUrl)
    }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var hasRenderedFirstFrame by remember { mutableStateOf(false) }
    var bufferedPercentage by remember { mutableIntStateOf(0) }
    var currentPositionMs by rememberSaveable { mutableLongStateOf(0L) }
    var totalDurationMs by rememberSaveable(payload.durationEstimateMs) {
        mutableLongStateOf(payload.durationEstimateMs)
    }
    var lastSavedRecoveryPositionMs by rememberSaveable { mutableLongStateOf(0L) }
    var runtimeErrorMessage by remember { mutableStateOf<String?>(null) }

    var volume by rememberSaveable { mutableFloatStateOf(1.0f) }
    var isMuted by rememberSaveable { mutableStateOf(false) }
    var playbackSpeed by rememberSaveable { mutableFloatStateOf(1.0f) }
    var selectedQuality by remember(payload.qualityOptions) {
        mutableStateOf(payload.qualityOptions.firstOrNull())
    }

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var isLandscapeRequested by rememberSaveable { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var adReadinessState by remember { mutableStateOf(AdPlacementReadinessState()) }

    val isPipSupported = remember(context) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    // Back handler exits fullscreen or landscape mode first before leaving screen
    BackHandler(enabled = isFullscreen || isLandscapeRequested) {
        isFullscreen = false
        if (isLandscapeRequested) {
            isLandscapeRequested = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // System bars visibility management for Fullscreen mode
    LaunchedEffect(isFullscreen) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (isFullscreen) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    // Attach Player.Listener & LifecycleEventObserver (20. Proper lifecycle & resource management)
    DisposableEffect(exoPlayer, lifecycleOwner) {
        val playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING) ||
                    (activeScenario == PlayerTestScenario.SLOW_INTERNET && playbackState == Player.STATE_READY && !isPlaying)
                bufferedPercentage = exoPlayer.bufferedPercentage
                if (playbackState == Player.STATE_READY) {
                    val dur = exoPlayer.duration
                    if (dur > 0L && dur != C.TIME_UNSET && activeScenario != PlayerTestScenario.LONG_VIDEO) {
                        totalDurationMs = dur
                    }
                    runtimeErrorMessage = null
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    // Record video start WITHOUT counting as an advertising impression
                    adReadinessState =
                        VideoPlaybackValidator.recordVideoStartWithoutFakeAdImpression(adReadinessState)
                }
            }

            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
            }

            override fun onPlayerError(error: PlaybackException) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(currentPositionMs)
                lastSavedRecoveryPositionMs = pos
                runtimeErrorMessage =
                    "Network/Decoder Error (${error.errorCodeName}): ${error.localizedMessage ?: "Stream interrupted"}"
                AppLogger.e("Media3Player", "PlaybackException: ${error.errorCodeName}", error)
            }
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                        activity?.isInPictureInPictureMode == true
                    if (!inPip) {
                        lastSavedRecoveryPositionMs = exoPlayer.currentPosition
                        exoPlayer.pause()
                        scope.launch {
                            if (payload.isPlayable) {
                                streamCacheRepository.cacheStreamPlayback(
                                    streamId = payload.videoId,
                                    title = payload.title,
                                    streamUrl = payload.authorizedStreamUrl,
                                    positionMs = exoPlayer.currentPosition
                                )
                            }
                        }
                        AppLogger.i("Media3Player", "App backgrounded: paused player & cached timestamp")
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                        activity?.isInPictureInPictureMode == true
                    if (!inPip) {
                        exoPlayer.pause()
                    }
                }

                else -> Unit
            }
        }

        exoPlayer.addListener(playerListener)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            exoPlayer.removeListener(playerListener)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            AppLogger.i("Media3Player", "Releasing Media3 ExoPlayer instance")
            exoPlayer.release()
        }
    }

    // Load or block media whenever payload or scenario changes
    LaunchedEffect(payload) {
        runtimeErrorMessage = null
        hasRenderedFirstFrame = false
        totalDurationMs = payload.durationEstimateMs
        selectedQuality = payload.qualityOptions.firstOrNull()

        if (!payload.isPlayable) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            isPlaying = false
            isBuffering = false
            AppLogger.w(
                "Media3Player",
                "Blocked playback for ${payload.videoId}: ${payload.authorizationStatus} - ${payload.errorOrBlockReason}"
            )
        } else {
            isBuffering = activeScenario == PlayerTestScenario.SLOW_INTERNET
            exoPlayer.setMediaItem(MediaItem.fromUri(payload.authorizedStreamUrl))
            exoPlayer.prepare()
            if (lastSavedRecoveryPositionMs > 0L) {
                exoPlayer.seekTo(lastSavedRecoveryPositionMs)
            }
            exoPlayer.playWhenReady = false
            AppLogger.i(
                "Media3Player",
                "Prepared authorized stream (${payload.formatLabel}): ${payload.authorizedStreamUrl}"
            )
        }
    }

    // Live position & buffer ticker
    LaunchedEffect(isPlaying, exoPlayer) {
        while (true) {
            if (payload.isPlayable) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                if (pos > 0L) {
                    currentPositionMs = pos
                }
                bufferedPercentage = exoPlayer.bufferedPercentage
                val dur = exoPlayer.duration
                if (dur > 0L && dur != C.TIME_UNSET && activeScenario != PlayerTestScenario.LONG_VIDEO) {
                    totalDurationMs = dur
                }
            }
            delay(250L)
        }
    }

    // Helper for retry & playback recovery
    val retryAndRecoverPlayback: () -> Unit = {
        runtimeErrorMessage = null
        if (activeScenario == PlayerTestScenario.INTERRUPTED_INTERNET ||
            activeScenario == PlayerTestScenario.INVALID_URL
        ) {
            activeScenario = PlayerTestScenario.STANDARD_MP4
        }
        val targetUrl = payload.authorizedStreamUrl.takeIf {
            VideoPlaybackValidator.isValidStreamUrl(it)
        } ?: "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"

        exoPlayer.setMediaItem(MediaItem.fromUri(targetUrl))
        exoPlayer.prepare()
        if (lastSavedRecoveryPositionMs > 0L) {
            exoPlayer.seekTo(lastSavedRecoveryPositionMs)
        }
        exoPlayer.play()
        statusMessage =
            "Recovered playback at ${VideoPlaybackValidator.formatPlaybackTimeMs(lastSavedRecoveryPositionMs)}"
    }

    // Fullscreen layout vs Standard scrollable layout
    if (isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ShareDabbaBlack)
                .testTag("video_player_fullscreen_container")
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                update = { playerView ->
                    playerView.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize()
            )

            PlayerViewportOverlays(
                payload = payload,
                hasRenderedFirstFrame = hasRenderedFirstFrame,
                isBuffering = isBuffering,
                bufferedPercentage = bufferedPercentage,
                runtimeErrorMessage = runtimeErrorMessage,
                lastSavedPositionMs = lastSavedRecoveryPositionMs,
                onRetryAndRecover = retryAndRecoverPlayback,
                onStartPlayback = {
                    hasRenderedFirstFrame = true
                    exoPlayer.play()
                }
            )

            // Fullscreen Top Header with Title & Exit Fullscreen button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(ShareDabbaBlack.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = payload.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = ShareDabbaWhite,
                        maxLines = 1
                    )
                    Text(
                        text = "${payload.uploaderName} • ${payload.formatLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaCyan
                    )
                }
                IconButton(
                    onClick = { isFullscreen = false },
                    modifier = Modifier.testTag("exit_fullscreen_overlay_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Fullscreen",
                        tint = ShareDabbaCyan
                    )
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .verticalScroll(rememberScrollState())
            .testTag("video_player_screen")
    ) {
        ShareDabbaTopBar(
            title = "ShareDabba Native Player",
            subtitle = "STAGE 4 • Media3 ExoPlayer • ${payload.formatLabel}",
            actions = {
                IconButton(
                    onClick = onOpenRecentlyWatched,
                    modifier = Modifier.testTag("player_open_recently_watched")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Recently Watched",
                        tint = ShareDabbaCyan
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 16:9 Native Media3 Viewport with Aspect-Ratio Preservation & Poster/Buffer/Error Overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ShareDabbaSurface)
                    .border(1.5.dp, ShareDabbaOutline, RoundedCornerShape(18.dp))
                    .testTag("exoplayer_container"),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    update = { playerView ->
                        playerView.player = exoPlayer
                    },
                    modifier = Modifier.fillMaxSize()
                )

                PlayerViewportOverlays(
                    payload = payload,
                    hasRenderedFirstFrame = hasRenderedFirstFrame,
                    isBuffering = isBuffering,
                    bufferedPercentage = bufferedPercentage,
                    runtimeErrorMessage = runtimeErrorMessage,
                    lastSavedPositionMs = lastSavedRecoveryPositionMs,
                    onRetryAndRecover = retryAndRecoverPlayback,
                    onStartPlayback = {
                        if (payload.isPlayable) {
                            hasRenderedFirstFrame = true
                            isBuffering = false
                            exoPlayer.play()
                        }
                    }
                )
            }

            // Native Transport Controls Card (Play/Pause, Seek, Time, Duration, Speed, Quality, Volume, PiP, Fullscreen, Rotation)
            NativeTransportControlsCard(
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                totalDurationMs = totalDurationMs,
                bufferedPercentage = bufferedPercentage,
                volume = volume,
                isMuted = isMuted,
                playbackSpeed = playbackSpeed,
                selectedQuality = selectedQuality,
                qualityOptions = payload.qualityOptions,
                isFullscreen = isFullscreen,
                isLandscapeRequested = isLandscapeRequested,
                isPipSupported = isPipSupported,
                onPlayPauseToggle = {
                    if (!payload.isPlayable) return@NativeTransportControlsCard
                    hasRenderedFirstFrame = true
                    isBuffering = false
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                        isPlaying = false
                    } else {
                        exoPlayer.play()
                        isPlaying = true
                    }
                },
                onSeekToMs = { targetMs ->
                    currentPositionMs = targetMs
                    lastSavedRecoveryPositionMs = targetMs
                    if (payload.isPlayable) {
                        exoPlayer.seekTo(targetMs)
                    }
                },
                onSeekDeltaMs = { deltaMs ->
                    val nextPos = (currentPositionMs + deltaMs).coerceIn(0L, totalDurationMs)
                    currentPositionMs = nextPos
                    lastSavedRecoveryPositionMs = nextPos
                    if (payload.isPlayable) {
                        exoPlayer.seekTo(nextPos)
                    }
                },
                onNextVideoClick = {
                    payload.nextVideo?.let { next ->
                        lastSavedRecoveryPositionMs = 0L
                        currentPositionMs = 0L
                        activeScenario = PlayerTestScenario.STANDARD_MP4
                        currentRequestKey = next.id
                        statusMessage = "Loaded Next Video: ${next.title}"
                    }
                },
                onVolumeChange = { newVolume ->
                    volume = newVolume.coerceIn(0f, 1f)
                    isMuted = volume == 0f
                    exoPlayer.volume = volume
                },
                onMuteToggle = {
                    isMuted = !isMuted
                    exoPlayer.volume = if (isMuted) 0f else volume
                },
                onSpeedSelected = { speed ->
                    playbackSpeed = speed
                    exoPlayer.playbackParameters = PlaybackParameters(speed)
                    statusMessage = "Playback speed set to ${speed}x"
                },
                onQualitySelected = { quality ->
                    selectedQuality = quality
                    val resumeAt = exoPlayer.currentPosition.coerceAtLeast(currentPositionMs)
                    lastSavedRecoveryPositionMs = resumeAt
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setMaxVideoSize(quality.maxWidth, quality.maxHeight)
                        .setMaxVideoBitrate(quality.maxBitrateBps)
                        .build()
                    statusMessage =
                        "Switched quality to ${quality.label} (preserved timestamp ${VideoPlaybackValidator.formatPlaybackTimeMs(resumeAt)})"
                },
                onToggleFullscreen = {
                    isFullscreen = !isFullscreen
                },
                onToggleOrientation = {
                    isLandscapeRequested = !isLandscapeRequested
                    activity?.requestedOrientation = if (isLandscapeRequested) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                    statusMessage = if (isLandscapeRequested) {
                        "Switched to Landscape orientation"
                    } else {
                        "Switched to Portrait orientation"
                    }
                },
                onEnterPip = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null && isPipSupported) {
                        runCatching {
                            val params = PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .build()
                            activity.enterPictureInPictureMode(params)
                        }.onFailure {
                            statusMessage = "Picture-in-Picture unavailable in current window state."
                        }
                    }
                }
            )

            // 12 & 13. Video Title, Uploader Information, Share (17), Cloud Bookmark, & Next Video (16)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(18.dp))
                    .testTag("player_video_metadata_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShareDabbaGradientBadge(text = payload.category)
                        ShareDabbaGradientBadge(text = payload.formatLabel)
                    }

                    Text(
                        text = payload.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = ShareDabbaWhite,
                        modifier = Modifier.testTag("player_video_title")
                    )

                    Text(
                        text = "${payload.viewsLabel} • ${payload.uploadedAgo}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ShareDabbaCyan
                    )

                    // Uploader Information Row (13)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ShareDabbaBlack)
                            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .testTag("player_uploader_info_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(ShareDabbaPurple.copy(alpha = 0.35f))
                                    .border(1.dp, ShareDabbaCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = payload.uploaderName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ShareDabbaWhite
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = payload.uploaderName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ShareDabbaWhite
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified Uploader",
                                        tint = ShareDabbaCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "${payload.uploaderHandle} • ${payload.uploaderFollowers}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ShareDabbaTextSecondary
                                )
                            }
                        }
                        ShareDabbaGradientBadge(text = "UPLOADER")
                    }

                    Text(
                        text = payload.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShareDabbaTextSecondary
                    )

                    // Action Buttons: Share (17), Bookmark, Cache Position, Next Video (16)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, payload.title)
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Watch '${payload.title}' on ShareDabba: ${payload.shareUrl}"
                                    )
                                }
                                val chooser = Intent.createChooser(sendIntent, "Share ShareDabba Video")
                                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooser)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("player_share_stream_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = ShareDabbaCyan
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", color = ShareDabbaWhite)
                        }

                        OutlinedButton(
                            onClick = {
                                if (payload.isPlayable) {
                                    scope.launch {
                                        val result = syncBookmarkUseCase.addBookmarkAndCache(
                                            title = payload.title,
                                            streamUrl = payload.authorizedStreamUrl,
                                            category = payload.category
                                        )
                                        statusMessage = result.fold(
                                            onSuccess = { "Synced bookmark to Firestore & Room cache." },
                                            onFailure = { "Bookmark failed: ${it.localizedMessage}" }
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bookmark_stream_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                tint = ShareDabbaCyan
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bookmark", color = ShareDabbaWhite)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    streamCacheRepository.cacheStreamPlayback(
                                        streamId = payload.videoId,
                                        title = payload.title,
                                        streamUrl = payload.authorizedStreamUrl,
                                        positionMs = currentPositionMs
                                    )
                                    statusMessage =
                                        "Saved timestamp (${VideoPlaybackValidator.formatPlaybackTimeMs(currentPositionMs)}) to Recently Watched."
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_position_cache_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = ShareDabbaCyan
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cache Position", color = ShareDabbaWhite)
                        }

                        if (payload.nextVideo != null) {
                            Button(
                                onClick = {
                                    lastSavedRecoveryPositionMs = 0L
                                    currentPositionMs = 0L
                                    activeScenario = PlayerTestScenario.STANDARD_MP4
                                    currentRequestKey = payload.nextVideo.id
                                    statusMessage = "Playing Next: ${payload.nextVideo.title}"
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("player_up_next_card_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ShareDabbaPurple,
                                    contentColor = ShareDabbaWhite
                                )
                            ) {
                                Icon(imageVector = Icons.Default.SkipNext, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Next Video")
                            }
                        }
                    }

                    if (!statusMessage.isNullOrBlank()) {
                        Text(
                            text = statusMessage.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = ShareDabbaCyan,
                            modifier = Modifier.testTag("player_status_message")
                        )
                    }
                }
            }

            // Custom Stream URL Loader & Validator Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Direct Stream URL & Format Loader",
                        style = MaterialTheme.typography.titleSmall,
                        color = ShareDabbaWhite
                    )
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        label = { Text("Authorized HLS / DASH / MP4 Stream URL") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_url_input")
                    )
                    Button(
                        onClick = {
                            val trimmed = customUrlInput.trim()
                            lastSavedRecoveryPositionMs = 0L
                            currentPositionMs = 0L
                            if (!VideoPlaybackValidator.isValidStreamUrl(trimmed)) {
                                activeScenario = PlayerTestScenario.INVALID_URL
                                currentRequestKey = trimmed
                            } else {
                                activeScenario = PlayerTestScenario.STANDARD_MP4
                                currentRequestKey = trimmed
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("load_stream_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShareDabbaCyan,
                            contentColor = ShareDabbaBlack
                        )
                    ) {
                        Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Validate & Load Stream URL")
                    }
                }
            }

            // Stage 4 Resilience Scenarios & Advertising Policy Compliance
            PlayerResilienceAndAdPolicySection(
                activeScenario = activeScenario,
                adState = adReadinessState,
                onSelectScenario = { scenario ->
                    if (scenario == PlayerTestScenario.INTERRUPTED_INTERNET) {
                        lastSavedRecoveryPositionMs = currentPositionMs.coerceAtLeast(48_000L)
                    }
                    activeScenario = scenario
                }
            )

            // 15. Related Videos Section
            Text(
                text = "Related Videos (${payload.relatedVideos.size})",
                style = MaterialTheme.typography.titleLarge,
                color = ShareDabbaWhite
            )

            payload.relatedVideos.forEach { related ->
                ShareDabbaVideoCard(
                    video = related,
                    onCardClick = {
                        lastSavedRecoveryPositionMs = 0L
                        currentPositionMs = 0L
                        activeScenario = PlayerTestScenario.STANDARD_MP4
                        currentRequestKey = related.id
                    },
                    onWatchClick = {
                        lastSavedRecoveryPositionMs = 0L
                        currentPositionMs = 0L
                        activeScenario = PlayerTestScenario.STANDARD_MP4
                        currentRequestKey = related.id
                    },
                    testTag = "player_related_video_${related.id}"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
