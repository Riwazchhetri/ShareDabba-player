package com.example.videoplayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaError
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaPurple
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWarning
import com.example.ui.theme.ShareDabbaWhite

/**
 * Poster thumbnail & buffering/error/blocked overlays inside the 16:9 Media3 video viewport.
 * Prevents unnecessary black screens before the first video frame renders or during buffering.
 */
@Composable
fun PlayerViewportOverlays(
    payload: AuthorizedPlaybackPayload,
    hasRenderedFirstFrame: Boolean,
    isBuffering: Boolean,
    bufferedPercentage: Int,
    runtimeErrorMessage: String?,
    lastSavedPositionMs: Long,
    onRetryAndRecover: () -> Unit,
    onStartPlayback: () -> Unit
) {
    val isBlockedOrError = !payload.isPlayable || runtimeErrorMessage != null

    // 14. Thumbnail poster shown before first frame renders so there are no unnecessary black screens
    if (!hasRenderedFirstFrame && !isBlockedOrError) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("player_thumbnail_poster")
        ) {
            Image(
                painter = painterResource(id = R.drawable.bg_hero_streaming),
                contentDescription = "Thumbnail for ${payload.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ShareDabbaBlack.copy(alpha = 0.55f))
            )
            if (!isBuffering) {
                IconButton(
                    onClick = onStartPlayback,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(ShareDabbaCyan)
                        .testTag("poster_play_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play ${payload.title}",
                        tint = ShareDabbaBlack,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
    }

    // 8. Buffering indicator overlay
    if (isBuffering && !isBlockedOrError) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ShareDabbaBlack.copy(alpha = 0.6f))
                .testTag("player_buffering_indicator"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    color = ShareDabbaCyan,
                    modifier = Modifier.size(44.dp),
                    strokeWidth = 3.dp
                )
                Text(
                    text = "Buffering stream... ($bufferedPercentage%)",
                    style = MaterialTheme.typography.labelLarge,
                    color = ShareDabbaWhite
                )
            }
        }
    }

    // 9 & 10. Network Error / Unauthorized / Deleted Video Gate & Retry Recovery Overlay
    if (isBlockedOrError) {
        val isPolicyBlock =
            payload.authorizationStatus == PlaybackAuthorizationStatus.UNAUTHORIZED_ACCESS ||
                payload.authorizationStatus == PlaybackAuthorizationStatus.VIDEO_DELETED

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ShareDabbaBlack.copy(alpha = 0.92f))
                .padding(20.dp)
                .testTag("player_error_or_block_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isPolicyBlock) Icons.Default.Lock else Icons.Default.ErrorOutline,
                    contentDescription = "Playback Blocked or Error",
                    tint = if (isPolicyBlock) ShareDabbaWarning else ShareDabbaError,
                    modifier = Modifier.size(40.dp)
                )
                ShareDabbaGradientBadge(text = payload.formatLabel)
                Text(
                    text = when (payload.authorizationStatus) {
                        PlaybackAuthorizationStatus.UNAUTHORIZED_ACCESS -> "Unauthorized Video Blocked"
                        PlaybackAuthorizationStatus.VIDEO_DELETED -> "Deleted Video Blocked"
                        PlaybackAuthorizationStatus.INVALID_STREAM_URL -> "Invalid Stream URL"
                        PlaybackAuthorizationStatus.NETWORK_INTERRUPTED -> "Network Interrupted"
                        PlaybackAuthorizationStatus.AUTHORIZED -> "Playback Error"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = ShareDabbaWhite
                )
                Text(
                    text = runtimeErrorMessage
                        ?: payload.errorOrBlockReason
                        ?: "An unexpected error occurred during video playback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ShareDabbaTextSecondary,
                    textAlign = TextAlign.Center
                )
                if (!isPolicyBlock) {
                    Button(
                        onClick = onRetryAndRecover,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShareDabbaCyan,
                            contentColor = ShareDabbaBlack
                        ),
                        modifier = Modifier.testTag("player_retry_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lastSavedPositionMs > 0L) {
                                "Retry & Recover (${VideoPlaybackValidator.formatPlaybackTimeMs(lastSavedPositionMs)})"
                            } else {
                                "Retry Playback"
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Complete Native Transport Controls Card covering:
 * 1. Play and pause
 * 2. Seek bar
 * 3. Current playback time
 * 4. Total duration
 * 5. Fullscreen toggle
 * 6. Portrait and landscape orientation toggle
 * 7. Playback speed selection
 * 11. Volume control & mute
 * 16. Next video button
 * 18. Picture-in-Picture trigger
 * 19. Video quality selection
 */
@Composable
fun NativeTransportControlsCard(
    isPlaying: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    bufferedPercentage: Int,
    volume: Float,
    isMuted: Boolean,
    playbackSpeed: Float,
    selectedQuality: StreamQualityOption?,
    qualityOptions: List<StreamQualityOption>,
    isFullscreen: Boolean,
    isLandscapeRequested: Boolean,
    isPipSupported: Boolean,
    onPlayPauseToggle: () -> Unit,
    onSeekToMs: (Long) -> Unit,
    onSeekDeltaMs: (Long) -> Unit,
    onNextVideoClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onQualitySelected: (StreamQualityOption) -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleOrientation: () -> Unit,
    onEnterPip: () -> Unit
) {
    val safeDuration = totalDurationMs.coerceAtLeast(1L)
    val sliderFraction = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(18.dp))
            .testTag("native_transport_controls_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Seek Bar + Current Time (3) & Total Duration (4)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Slider(
                    value = sliderFraction,
                    onValueChange = { fraction ->
                        val targetMs = (fraction * safeDuration).toLong()
                        onSeekToMs(targetMs)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = ShareDabbaCyan,
                        activeTrackColor = ShareDabbaCyan,
                        inactiveTrackColor = ShareDabbaOutline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_seek_bar")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = VideoPlaybackValidator.formatPlaybackTimeMs(currentPositionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = ShareDabbaCyan,
                        modifier = Modifier.testTag("player_current_time_text")
                    )
                    Text(
                        text = "Buffered: $bufferedPercentage%",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaTextSecondary
                    )
                    Text(
                        text = VideoPlaybackValidator.formatPlaybackTimeMs(totalDurationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = ShareDabbaWhite,
                        modifier = Modifier.testTag("player_total_duration_text")
                    )
                }
            }

            // Primary Transport Row: -10s, Play/Pause (1), +10s, Next Video (16), Orientation (6), PiP (18), Fullscreen (5)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSeekDeltaMs(-10_000L) },
                    modifier = Modifier.testTag("player_rewind_10s_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        tint = ShareDabbaWhite
                    )
                }

                Button(
                    onClick = onPlayPauseToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShareDabbaCyan,
                        contentColor = ShareDabbaBlack
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("player_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPlaying) "Pause" else "Play")
                }

                IconButton(
                    onClick = { onSeekDeltaMs(10_000L) },
                    modifier = Modifier.testTag("player_forward_10s_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10 seconds",
                        tint = ShareDabbaWhite
                    )
                }

                IconButton(
                    onClick = onNextVideoClick,
                    modifier = Modifier.testTag("player_next_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Video",
                        tint = ShareDabbaCyan
                    )
                }

                IconButton(
                    onClick = onToggleOrientation,
                    modifier = Modifier.testTag("player_orientation_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = if (isLandscapeRequested) {
                            "Switch to Portrait"
                        } else {
                            "Switch to Landscape"
                        },
                        tint = if (isLandscapeRequested) ShareDabbaCyan else ShareDabbaWhite
                    )
                }

                if (isPipSupported) {
                    IconButton(
                        onClick = onEnterPip,
                        modifier = Modifier.testTag("player_pip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Enter Picture-in-Picture",
                            tint = ShareDabbaPurple
                        )
                    }
                }

                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.testTag("player_fullscreen_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isFullscreen) {
                            Icons.Default.FullscreenExit
                        } else {
                            Icons.Default.Fullscreen
                        },
                        contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                        tint = ShareDabbaCyan
                    )
                }
            }

            // 11. Volume Control Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onMuteToggle,
                    modifier = Modifier.testTag("player_mute_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isMuted || volume == 0f) {
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = ShareDabbaCyan
                    )
                }
                Slider(
                    value = if (isMuted) 0f else volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_volume_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = ShareDabbaPurple,
                        activeTrackColor = ShareDabbaPurple,
                        inactiveTrackColor = ShareDabbaOutline
                    )
                )
                Text(
                    text = "${((if (isMuted) 0f else volume) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = ShareDabbaWhite,
                    modifier = Modifier.width(38.dp)
                )
            }

            // 7. Playback Speed Selection Row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = ShareDabbaCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Playback Speed (${playbackSpeed}x)",
                        style = MaterialTheme.typography.labelMedium,
                        color = ShareDabbaWhite
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(VideoPlaybackValidator.supportedPlaybackSpeeds) { speed ->
                        FilterChip(
                            selected = playbackSpeed == speed,
                            onClick = { onSpeedSelected(speed) },
                            label = { Text("${speed}x") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ShareDabbaCyan,
                                selectedLabelColor = ShareDabbaBlack,
                                containerColor = ShareDabbaBlack,
                                labelColor = ShareDabbaWhite
                            ),
                            modifier = Modifier.testTag("player_speed_chip_${speed}")
                        )
                    }
                }
            }

            // 19. Video Quality Selection Row
            if (qualityOptions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = null,
                            tint = ShareDabbaPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Stream Quality (${selectedQuality?.label ?: "Auto"})",
                            style = MaterialTheme.typography.labelMedium,
                            color = ShareDabbaWhite
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(qualityOptions, key = { it.id }) { option ->
                            FilterChip(
                                selected = selectedQuality?.id == option.id,
                                onClick = { onQualitySelected(option) },
                                label = { Text(option.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ShareDabbaPurple,
                                    selectedLabelColor = ShareDabbaWhite,
                                    containerColor = ShareDabbaBlack,
                                    labelColor = ShareDabbaWhite
                                ),
                                modifier = Modifier.testTag("player_quality_chip_${option.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Stage 4 Resilience & Format Test Scenario Bar + Advertising Policy Telemetry Card.
 */
@Composable
fun PlayerResilienceAndAdPolicySection(
    activeScenario: PlayerTestScenario,
    adState: AdPlacementReadinessState,
    onSelectScenario: (PlayerTestScenario) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
            .testTag("player_resilience_test_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stage 4 Player Test Scenarios",
                    style = MaterialTheme.typography.titleSmall,
                    color = ShareDabbaWhite
                )
                ShareDabbaGradientBadge(text = activeScenario.label)
            }
            Text(
                text = activeScenario.description,
                style = MaterialTheme.typography.bodySmall,
                color = ShareDabbaTextSecondary
            )
            LazyRow(
                contentPadding = PaddingValues(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PlayerTestScenario.entries) { scenario ->
                    FilterChip(
                        selected = activeScenario == scenario,
                        onClick = { onSelectScenario(scenario) },
                        label = { Text(scenario.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ShareDabbaCyan,
                            selectedLabelColor = ShareDabbaBlack,
                            containerColor = ShareDabbaBlack,
                            labelColor = ShareDabbaWhite
                        ),
                        modifier = Modifier.testTag("player_scenario_chip_${scenario.id}")
                    )
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
            .testTag("player_ad_compliance_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = ShareDabbaCyan
                )
                Column {
                    Text(
                        text = "Approved Ad Placement Readiness (Zero Fake Ads)",
                        style = MaterialTheme.typography.labelLarge,
                        color = ShareDabbaWhite
                    )
                    Text(
                        text = "Video Starts: ${adState.videoStartsCount} • Ad Impressions: ${adState.verifiedAdImpressionsCount} (Video starts are never counted as ad impressions)",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShareDabbaTextSecondary
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Verified Ad Policy",
                tint = ShareDabbaCyan
            )
        }
    }
}
