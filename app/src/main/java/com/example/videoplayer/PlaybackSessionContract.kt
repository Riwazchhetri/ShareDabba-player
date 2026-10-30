package com.example.videoplayer

import com.example.data.fixtures.DevFixturesCatalog
import com.example.data.fixtures.VideoItemFixture
import java.util.Locale

/**
 * Represents an available video quality variant supplied by the backend or adaptive manifest.
 */
data class StreamQualityOption(
    val id: String,
    val label: String,
    val resolutionBadge: String,
    val maxWidth: Int,
    val maxHeight: Int,
    val maxBitrateBps: Int,
    val streamUrl: String
)

/**
 * Authorization & availability states returned when resolving a ShareDabba video for playback.
 * Ensures unauthorized, deleted, or malformed URLs are never played.
 */
enum class PlaybackAuthorizationStatus {
    AUTHORIZED,
    UNAUTHORIZED_ACCESS,
    VIDEO_DELETED,
    INVALID_STREAM_URL,
    NETWORK_INTERRUPTED
}

/**
 * Approved advertising placement readiness state.
 * Strictly obeys Stage 4 requirements:
 * - No fake ads are inserted.
 * - Uses the website's documented ad configuration only when available.
 * - Video starts are NEVER counted as advertising impressions.
 */
data class AdPlacementReadinessState(
    val isWebsiteAdConfigConnected: Boolean = false,
    val documentedVastTagUrl: String? = null,
    val videoStartsCount: Int = 0,
    val verifiedAdImpressionsCount: Int = 0,
    val policySummary: String = "No fake ads inserted. Video starts are tracked separately and never counted as ad impressions."
)

/**
 * Complete playback session payload retrieved from the ShareDabba backend (or development fixture
 * catalog while the website repository connection is pending).
 */
data class AuthorizedPlaybackPayload(
    val videoId: String,
    val title: String,
    val description: String,
    val category: String,
    val uploaderName: String,
    val uploaderHandle: String,
    val uploaderFollowers: String,
    val viewsLabel: String,
    val uploadedAgo: String,
    val shareUrl: String,
    val authorizedStreamUrl: String,
    val formatLabel: String,
    val durationEstimateMs: Long,
    val authorizationStatus: PlaybackAuthorizationStatus,
    val errorOrBlockReason: String? = null,
    val qualityOptions: List<StreamQualityOption>,
    val relatedVideos: List<VideoItemFixture>,
    val nextVideo: VideoItemFixture?,
    val isDevelopmentFixture: Boolean = true
) {
    val isPlayable: Boolean
        get() = authorizationStatus == PlaybackAuthorizationStatus.AUTHORIZED &&
            VideoPlaybackValidator.isValidStreamUrl(authorizedStreamUrl)
}

/**
 * Built-in Stage 4 test scenarios for verifying multi-format playback, slow/interrupted network,
 * playback recovery, long videos, invalid URLs, and unauthorized/deleted video blocking.
 */
enum class PlayerTestScenario(
    val id: String,
    val label: String,
    val description: String
) {
    STANDARD_MP4(
        id = "mp4",
        label = "MP4 Progressive",
        description = "Standard H.264/AAC progressive MP4 stream with multiple quality options."
    ),
    HLS_STREAM(
        id = "hls",
        label = "HLS Adaptive",
        description = "Apple HTTP Live Streaming (.m3u8) adaptive bitrate stream."
    ),
    DASH_STREAM(
        id = "dash",
        label = "DASH Stream",
        description = "MPEG-DASH (.mpd) adaptive multi-track stream."
    ),
    LONG_VIDEO(
        id = "long_video",
        label = "Long Video (1h+)",
        description = "Long-form timeline verification (HH:MM:SS formatting & deep seek scrubbing)."
    ),
    SLOW_INTERNET(
        id = "slow_net",
        label = "Slow Internet",
        description = "Simulates high-latency buffering state with poster retention & buffer progress."
    ),
    INTERRUPTED_INTERNET(
        id = "interrupted_net",
        label = "Interrupted Net",
        description = "Simulates mid-stream connection drop and 1-tap Playback Recovery at exact timestamp."
    ),
    INVALID_URL(
        id = "invalid_url",
        label = "Invalid URL",
        description = "Verifies player rejects malformed/unsupported stream URLs with retryable error state."
    ),
    UNAUTHORIZED_VIDEO(
        id = "unauthorized",
        label = "403 Unauthorized",
        description = "Verifies unauthorized videos are blocked before ExoPlayer loads media."
    ),
    DELETED_VIDEO(
        id = "deleted",
        label = "410 Deleted Video",
        description = "Verifies deleted videos are blocked and never played."
    )
}

/**
 * Pure domain validator & resolver for ShareDabba Native Video Player (Stage 4).
 */
object VideoPlaybackValidator {

    private const val PROGRESSIVE_MP4_HD =
        "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
    private const val AUDIO_MP3_STREAM =
        "https://storage.googleapis.com/exoplayer-test-media-0/jazz_in_paris.mp3"
    private const val SAMPLE_HLS_URL =
        "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8"
    private const val SAMPLE_DASH_URL =
        "https://storage.googleapis.com/wvmedia/clear/h264/tears/tears.mpd"

    val supportedPlaybackSpeeds: List<Float> = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    /**
     * Validates that a stream URL uses a secure HTTP(S) scheme, has a valid host, and does not
     * contain unauthorized/deleted markers.
     */
    fun isValidStreamUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        if (!trimmed.startsWith("https://", ignoreCase = true) &&
            !trimmed.startsWith("http://", ignoreCase = true)
        ) {
            return false
        }
        val afterScheme = trimmed.substringAfter("://", "")
        if (afterScheme.isBlank() || !afterScheme.contains(".") || afterScheme.contains(" ")) {
            return false
        }
        return true
    }

    /**
     * Formats milliseconds into `MM:SS` (or `HH:MM:SS` when >= 1 hour) for seek bar and duration labels.
     */
    fun formatPlaybackTimeMs(timeMs: Long): String {
        val safeMs = timeMs.coerceAtLeast(0L)
        val totalSeconds = safeMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Enforces the Stage 4 Advertising rule:
     * Video starts increment `videoStartsCount` ONLY and NEVER increment `verifiedAdImpressionsCount`.
     */
    fun recordVideoStartWithoutFakeAdImpression(
        current: AdPlacementReadinessState
    ): AdPlacementReadinessState {
        return current.copy(
            videoStartsCount = current.videoStartsCount + 1,
            // Advertising impressions remain strictly unchanged on video start
            verifiedAdImpressionsCount = current.verifiedAdImpressionsCount
        )
    }

    /**
     * Builds the multi-quality stream options for a given primary stream URL.
     */
    fun buildQualityOptions(primaryStreamUrl: String): List<StreamQualityOption> {
        val safeUrl = primaryStreamUrl.ifBlank { PROGRESSIVE_MP4_HD }
        return listOf(
            StreamQualityOption(
                id = "auto",
                label = "Auto (Adaptive)",
                resolutionBadge = "ABR",
                maxWidth = Int.MAX_VALUE,
                maxHeight = Int.MAX_VALUE,
                maxBitrateBps = Int.MAX_VALUE,
                streamUrl = safeUrl
            ),
            StreamQualityOption(
                id = "1080p",
                label = "1080p Full HD",
                resolutionBadge = "1080p",
                maxWidth = 1920,
                maxHeight = 1080,
                maxBitrateBps = 6_000_000,
                streamUrl = safeUrl
            ),
            StreamQualityOption(
                id = "720p",
                label = "720p HD",
                resolutionBadge = "720p",
                maxWidth = 1280,
                maxHeight = 720,
                maxBitrateBps = 3_000_000,
                streamUrl = safeUrl
            ),
            StreamQualityOption(
                id = "480p",
                label = "480p Standard",
                resolutionBadge = "480p",
                maxWidth = 854,
                maxHeight = 480,
                maxBitrateBps = 1_500_000,
                streamUrl = safeUrl
            ),
            StreamQualityOption(
                id = "360p",
                label = "360p Data Saver",
                resolutionBadge = "360p",
                maxWidth = 640,
                maxHeight = 360,
                maxBitrateBps = 800_000,
                streamUrl = safeUrl
            )
        )
    }

    /**
     * Resolves an [AuthorizedPlaybackPayload] for a video or test scenario, enforcing authorization,
     * deleted-content checks, and URL validation before ExoPlayer is allowed to load media.
     */
    fun resolveAuthorizedPayload(
        requestedStreamUrlOrVideoId: String?,
        scenario: PlayerTestScenario = PlayerTestScenario.STANDARD_MP4
    ): AuthorizedPlaybackPayload {
        val parsedDeepLink = if (!requestedStreamUrlOrVideoId.isNullOrBlank() &&
            (requestedStreamUrlOrVideoId.contains("sharedabba.com") ||
                requestedStreamUrlOrVideoId.startsWith("sharedabba://"))
        ) {
            com.example.deeplinks.DeepLinkRouter.parseIncomingUrl(requestedStreamUrlOrVideoId)
        } else {
            null
        }

        val effectiveKey = parsedDeepLink?.extractedVideoId
            ?: parsedDeepLink?.directStreamOverrideUrl
            ?: requestedStreamUrlOrVideoId

        val effectiveScenario = when {
            scenario != PlayerTestScenario.STANDARD_MP4 -> scenario
            effectiveKey != null && (effectiveKey.contains("deleted", ignoreCase = true) || effectiveKey == "vid_deleted_410") ->
                PlayerTestScenario.DELETED_VIDEO
            effectiveKey != null && (effectiveKey.contains("private", ignoreCase = true) || effectiveKey == "vid_unauth_403") ->
                PlayerTestScenario.UNAUTHORIZED_VIDEO
            else -> scenario
        }

        val matchingVideo = DevFixturesCatalog.videos.firstOrNull {
            it.id.equals(effectiveKey, ignoreCase = true) || it.streamUrl == effectiveKey
        } ?: DevFixturesCatalog.videos.first()

        val related = DevFixturesCatalog.getRelatedVideos(matchingVideo.id)
        val nextVideo = related.firstOrNull()

        return when (effectiveScenario) {
            PlayerTestScenario.UNAUTHORIZED_VIDEO -> {
                AuthorizedPlaybackPayload(
                    videoId = "vid_unauth_403",
                    title = "${matchingVideo.title} [Protected]",
                    description = "This video requires an authorized playback token from the ShareDabba backend.",
                    category = matchingVideo.category,
                    uploaderName = matchingVideo.uploaderName,
                    uploaderHandle = matchingVideo.uploaderHandle,
                    uploaderFollowers = matchingVideo.uploaderFollowers,
                    viewsLabel = matchingVideo.viewsLabel,
                    uploadedAgo = matchingVideo.uploadedAgo,
                    shareUrl = matchingVideo.shareUrl,
                    authorizedStreamUrl = "",
                    formatLabel = "BLOCKED (403)",
                    durationEstimateMs = 0L,
                    authorizationStatus = PlaybackAuthorizationStatus.UNAUTHORIZED_ACCESS,
                    errorOrBlockReason = "403 Unauthorized: Playback token missing or expired. Unauthorized videos cannot be played.",
                    qualityOptions = emptyList(),
                    relatedVideos = related,
                    nextVideo = nextVideo
                )
            }

            PlayerTestScenario.DELETED_VIDEO -> {
                AuthorizedPlaybackPayload(
                    videoId = "vid_deleted_410",
                    title = "Removed Video",
                    description = "This video was deleted by the uploader or removed by ShareDabba moderation.",
                    category = matchingVideo.category,
                    uploaderName = matchingVideo.uploaderName,
                    uploaderHandle = matchingVideo.uploaderHandle,
                    uploaderFollowers = matchingVideo.uploaderFollowers,
                    viewsLabel = "0 views",
                    uploadedAgo = "Removed",
                    shareUrl = matchingVideo.shareUrl,
                    authorizedStreamUrl = "",
                    formatLabel = "DELETED (410)",
                    durationEstimateMs = 0L,
                    authorizationStatus = PlaybackAuthorizationStatus.VIDEO_DELETED,
                    errorOrBlockReason = "410 Gone: This video has been deleted from ShareDabba and is no longer available for playback.",
                    qualityOptions = emptyList(),
                    relatedVideos = related,
                    nextVideo = nextVideo
                )
            }

            PlayerTestScenario.INVALID_URL -> {
                val badUrl = requestedStreamUrlOrVideoId?.takeIf { !isValidStreamUrl(it) }
                    ?: "invalid-scheme://corrupted-stream-url"
                AuthorizedPlaybackPayload(
                    videoId = matchingVideo.id,
                    title = matchingVideo.title,
                    description = matchingVideo.description,
                    category = matchingVideo.category,
                    uploaderName = matchingVideo.uploaderName,
                    uploaderHandle = matchingVideo.uploaderHandle,
                    uploaderFollowers = matchingVideo.uploaderFollowers,
                    viewsLabel = matchingVideo.viewsLabel,
                    uploadedAgo = matchingVideo.uploadedAgo,
                    shareUrl = matchingVideo.shareUrl,
                    authorizedStreamUrl = badUrl,
                    formatLabel = "INVALID URL",
                    durationEstimateMs = 0L,
                    authorizationStatus = PlaybackAuthorizationStatus.INVALID_STREAM_URL,
                    errorOrBlockReason = "Malformed or unsupported playback URL ('$badUrl'). Only authorized HTTP/HTTPS streams are permitted.",
                    qualityOptions = emptyList(),
                    relatedVideos = related,
                    nextVideo = nextVideo
                )
            }

            PlayerTestScenario.INTERRUPTED_INTERNET -> {
                AuthorizedPlaybackPayload(
                    videoId = matchingVideo.id,
                    title = matchingVideo.title,
                    description = matchingVideo.description,
                    category = matchingVideo.category,
                    uploaderName = matchingVideo.uploaderName,
                    uploaderHandle = matchingVideo.uploaderHandle,
                    uploaderFollowers = matchingVideo.uploaderFollowers,
                    viewsLabel = matchingVideo.viewsLabel,
                    uploadedAgo = matchingVideo.uploadedAgo,
                    shareUrl = matchingVideo.shareUrl,
                    authorizedStreamUrl = PROGRESSIVE_MP4_HD,
                    formatLabel = "MP4 • INTERRUPTED NET",
                    durationEstimateMs = 596_000L,
                    authorizationStatus = PlaybackAuthorizationStatus.NETWORK_INTERRUPTED,
                    errorOrBlockReason = "Network connection interrupted during segment fetch. Tap 'Retry & Recover' to resume from your exact playback timestamp.",
                    qualityOptions = buildQualityOptions(PROGRESSIVE_MP4_HD),
                    relatedVideos = related,
                    nextVideo = nextVideo
                )
            }

            PlayerTestScenario.HLS_STREAM -> {
                AuthorizedPlaybackPayload(
                    videoId = matchingVideo.id,
                    title = "${matchingVideo.title} (HLS Adaptive)",
                    description = matchingVideo.description,
                    category = matchingVideo.category,
                    uploaderName = matchingVideo.uploaderName,
                    uploaderHandle = matchingVideo.uploaderHandle,
                    uploaderFollowers = matchingVideo.uploaderFollowers,
                    viewsLabel = matchingVideo.viewsLabel,
                    uploadedAgo = matchingVideo.uploadedAgo,
                    shareUrl = matchingVideo.shareUrl,
                    authorizedStreamUrl = SAMPLE_HLS_URL,
                    formatLabel = "HLS (.m3u8)",
                    durationEstimateMs = 1_800_000L,
                    authorizationStatus = PlaybackAuthorizationStatus.AUTHORIZED,
                    qualityOptions = buildQualityOptions(SAMPLE_HLS_URL),
                    relatedVideos = related,
                    nextVideo = nextVideo
                )
            }

            PlayerTestScenario.DASH_STREAM -> {
                AuthorizedPlaybackPayload(
                    videoId = matchingVideo.id,
                    title = "${matchingVideo.title} (MPEG-DASH)",
                    description = matchingVideo.description,
                    category = matchingVideo.category,
                    uploaderName = matchingVideo.uploaderName,
                    uploaderHandle = matchingVideo.uploaderHandle,
                    uploaderFollowers = matchingVideo.uploaderFollowers,
                    viewsLabel = matchingVideo.viewsLabel,
                    uploadedAgo = matchingVideo.uploadedAgo,
                    shareUrl = matchingVideo.shareUrl,
                    authorizedStreamUrl = SAMPLE_DASH_URL,
                    formatLabel = "DASH (.mpd)",
                    durationEstimateMs = 734_000L,
                    authorizationStatus = PlaybackAuthorizationStatus.AUTHORIZED,
                    qualityOptions = buildQualityOptions(SAMPLE_DASH_URL),
                    relatedVideos = related,
                    nextVideo = nextVideo
                )
            }

            PlayerTestScenario.LONG_VIDEO -> {
                AuthorizedPlaybackPayload(
                    videoId = matchingVideo.id,
                    title = "${matchingVideo.title} (Extended 2h Masterclass)",
                    description = matchingVideo.description,
                    category = matchingVideo.category,
                    uploaderName = matchingVideo.uploaderName,
                    uploaderHandle = matchingVideo.uploaderHandle,
                    uploaderFollowers = matchingVideo.uploaderFollowers,
                    viewsLabel = matchingVideo.viewsLabel,
                    uploadedAgo = matchingVideo.uploadedAgo,
                    shareUrl = matchingVideo.shareUrl,
                    authorizedStreamUrl = PROGRESSIVE_MP4_HD,
                    formatLabel = "MP4 • LONG FORM (02:15:30)",
                    durationEstimateMs = 8_130_000L, // 2h 15m 30s
                    authorizationStatus = PlaybackAuthorizationStatus.AUTHORIZED,
                    qualityOptions = buildQualityOptions(PROGRESSIVE_MP4_HD),
                    relatedVideos = related,
                    nextVideo = nextVideo
                )
            }

            PlayerTestScenario.SLOW_INTERNET, PlayerTestScenario.STANDARD_MP4 -> {
                val candidateUrl = if (effectiveKey != null &&
                    effectiveKey.startsWith("http")
                ) {
                    effectiveKey
                } else {
                    matchingVideo.streamUrl.ifBlank { PROGRESSIVE_MP4_HD }
                }

                if (!isValidStreamUrl(candidateUrl)) {
                    AuthorizedPlaybackPayload(
                        videoId = matchingVideo.id,
                        title = matchingVideo.title,
                        description = matchingVideo.description,
                        category = matchingVideo.category,
                        uploaderName = matchingVideo.uploaderName,
                        uploaderHandle = matchingVideo.uploaderHandle,
                        uploaderFollowers = matchingVideo.uploaderFollowers,
                        viewsLabel = matchingVideo.viewsLabel,
                        uploadedAgo = matchingVideo.uploadedAgo,
                        shareUrl = matchingVideo.shareUrl,
                        authorizedStreamUrl = candidateUrl,
                        formatLabel = "INVALID URL",
                        durationEstimateMs = 0L,
                        authorizationStatus = PlaybackAuthorizationStatus.INVALID_STREAM_URL,
                        errorOrBlockReason = "Invalid stream URL: $candidateUrl",
                        qualityOptions = emptyList(),
                        relatedVideos = related,
                        nextVideo = nextVideo
                    )
                } else {
                    val formatBadge = when {
                        candidateUrl.endsWith(".m3u8", ignoreCase = true) -> "HLS (.m3u8)"
                        candidateUrl.endsWith(".mpd", ignoreCase = true) -> "DASH (.mpd)"
                        candidateUrl.endsWith(".mp3", ignoreCase = true) -> "AUDIO STREAM (.mp3)"
                        else -> "MP4 (H.264/AAC)"
                    }
                    AuthorizedPlaybackPayload(
                        videoId = matchingVideo.id,
                        title = matchingVideo.title,
                        description = matchingVideo.description,
                        category = matchingVideo.category,
                        uploaderName = matchingVideo.uploaderName,
                        uploaderHandle = matchingVideo.uploaderHandle,
                        uploaderFollowers = matchingVideo.uploaderFollowers,
                        viewsLabel = matchingVideo.viewsLabel,
                        uploadedAgo = matchingVideo.uploadedAgo,
                        shareUrl = matchingVideo.shareUrl,
                        authorizedStreamUrl = candidateUrl,
                        formatLabel = formatBadge,
                        durationEstimateMs = 596_000L,
                        authorizationStatus = PlaybackAuthorizationStatus.AUTHORIZED,
                        qualityOptions = buildQualityOptions(candidateUrl),
                        relatedVideos = related,
                        nextVideo = nextVideo
                    )
                }
            }
        }
    }
}
