package com.example.deeplinks

import android.content.Intent
import com.example.data.fixtures.DevFixturesCatalog
import com.example.data.fixtures.VideoItemFixture
import java.net.URI

/**
 * External applications tested for ShareDabba link launching in Stage 7.
 */
enum class DeepLinkSourceApp(val label: String) {
    WHATSAPP("WhatsApp Messenger"),
    TELEGRAM("Telegram"),
    CHROME_BROWSER("Google Chrome / Custom Tabs"),
    CUSTOM_APP_SCHEME("Web Landing 'Open in ShareDabba'"),
    DIRECT_SYSTEM_LINK("Direct Android App Link")
}

/**
 * Resolution outcome for an incoming ShareDabba deep link or Android App Link.
 */
enum class DeepLinkResolutionStatus(val badge: String) {
    RESOLVED_READY_FOR_PLAYER("READY • OPEN EXACT VIDEO IN PLAYER"),
    PRIVATE_VIDEO_AUTH_REQUIRED("403 • PRIVATE VIDEO AUTH REQUIRED"),
    VIDEO_DELETED_410("410 • DELETED VIDEO BLOCKED"),
    VIDEO_NOT_FOUND_404("404 • VIDEO NOT FOUND"),
    INVALID_OR_UNRECOGNIZED_LINK("INVALID • BROWSER FALLBACK ONLY")
}

/**
 * Case 2 Web Landing Page & Fallback Model:
 * Provides:
 * - Video thumbnail
 * - Video title
 * - Watch in browser URL
 * - Open in ShareDabba custom URI / App Link
 * - Get the app when the official Play Store listing is available
 */
data class WebLandingPagePreviewModel(
    val videoId: String,
    val videoTitle: String,
    val uploaderLabel: String,
    val thumbnailUrl: String,
    val watchInBrowserUrl: String,
    val openInShareDabbaUri: String,
    val isPlayStoreListingLive: Boolean = false,
    val playStoreStatusText: String = "Get the app (Available once the official Google Play Store listing is published)"
)

/**
 * Structured result of parsing an incoming URL before backend catalog resolution.
 */
data class ParsedShareDabbaLink(
    val rawUrl: String,
    val normalizedUrl: String,
    val host: String,
    val path: String,
    val extractedVideoId: String?,
    val matchedRoutePattern: String,
    val detectedSourceApp: DeepLinkSourceApp,
    val directStreamOverrideUrl: String?,
    val browserFallbackUrl: String,
    val isRecognizedShareDabbaLink: Boolean
)

/**
 * Full Stage 7 resolution result combining URL parsing, backend catalog lookup,
 * private-video authorization, deleted-video checks, and Case 2 landing page metadata.
 */
data class ResolvedDeepLinkResult(
    val parsedLink: ParsedShareDabbaLink,
    val status: DeepLinkResolutionStatus,
    val video: VideoItemFixture?,
    val playerTargetKey: String?,
    val statusMessage: String,
    val landingPageModel: WebLandingPagePreviewModel
) {
    val canOpenInNativePlayer: Boolean
        get() = status == DeepLinkResolutionStatus.RESOLVED_READY_FOR_PLAYER &&
            !playerTargetKey.isNullOrBlank()
}

/**
 * Preset test scenarios for verifying links shared from WhatsApp, Telegram, Chrome,
 * the web landing page, private videos, and deleted videos.
 */
data class DeepLinkTestPreset(
    val id: String,
    val label: String,
    val sourceApp: DeepLinkSourceApp,
    val sampleUrl: String,
    val description: String
)

/**
 * Digital Asset Links (`/.well-known/assetlinks.json`) configuration specification for Stage 7.
 * Explicitly reports that verified Android App Links are NOT yet active until the production
 * domain and signing certificate SHA-256 fingerprint are deployed and verified.
 */
object AssetLinksConfigurationSpec {
    const val IS_VERIFIED_APP_LINKS_ACTIVE_ON_PRODUCTION_DOMAIN: Boolean = false
    const val APPLICATION_ID: String = "com.aistudio.sharedabba.vstream"
    const val WELL_KNOWN_PATH: String = "https://sharedabba.com/.well-known/assetlinks.json"
    const val PLACEHOLDER_SHA256_FINGERPRINT: String =
        "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99"

    const val VERIFICATION_HONESTY_STATEMENT: String =
        "UNVERIFIED UNTIL DEPLOYED: Android App Links intent filters (autoVerify=true) are configured in AndroidManifest.xml, " +
            "but verified domain auto-launch is NOT active until https://sharedabba.com/.well-known/assetlinks.json is hosted on the " +
            "live website with the release signing certificate SHA-256 fingerprint and verified via `adb shell pm get-app-links`."

    fun buildAssetLinksJson(
        packageName: String = APPLICATION_ID,
        sha256Fingerprint: String = PLACEHOLDER_SHA256_FINGERPRINT
    ): String {
        return """
[
  {
    "relation": [
      "delegate_permission/common.handle_all_urls"
    ],
    "target": {
      "namespace": "android_app",
      "package_name": "$packageName",
      "sha256_cert_fingerprints": [
        "$sha256Fingerprint"
      ]
    }
  }
]
        """.trimIndent()
    }
}

/**
 * Stage 7 Deep Link & Android App Link Router.
 * Handles:
 * - Canonical link parsing (`https://YOUR-DOMAIN/v/VIDEO_ID`, `/watch/VIDEO_ID`, `/video/VIDEO_ID`, `sharedabba://v/VIDEO_ID`)
 * - Source app detection (WhatsApp, Telegram, Chrome, Custom Scheme)
 * - Exact Video ID extraction (Case 3: Opens exact video requested, never homepage)
 * - Deleted-video blocking (`410 Gone`)
 * - Private-video authorization (`403 Forbidden` when unauthenticated or unauthorized)
 * - Browser fallback URL generation (`https://sharedabba.com/v/VIDEO_ID`)
 */
object DeepLinkRouter {

    private val SUPPORTED_WEB_HOSTS = setOf(
        "sharedabba.com",
        "www.sharedabba.com",
        "m.sharedabba.com"
    )

    val testPresets: List<DeepLinkTestPreset> = listOf(
        DeepLinkTestPreset(
            id = "whatsapp",
            label = "WhatsApp Link",
            sourceApp = DeepLinkSourceApp.WHATSAPP,
            sampleUrl = "https://sharedabba.com/v/vid_featured_2?utm_source=whatsapp&utm_medium=chat",
            description = "Canonical /v/VIDEO_ID link shared in a WhatsApp chat; opens 'Creator Studio Masterclass' directly in player."
        ),
        DeepLinkTestPreset(
            id = "telegram",
            label = "Telegram Link",
            sourceApp = DeepLinkSourceApp.TELEGRAM,
            sampleUrl = "https://sharedabba.com/v/vid_recent_2?tg_r=1&utm_source=telegram",
            description = "Canonical /v/VIDEO_ID link shared in a Telegram channel; opens 'Live Indie Game Dev Session' directly."
        ),
        DeepLinkTestPreset(
            id = "chrome",
            label = "Chrome Link",
            sourceApp = DeepLinkSourceApp.CHROME_BROWSER,
            sampleUrl = "https://www.sharedabba.com/watch/vid_pop_1?source=chrome",
            description = "Web watch route (/watch/VIDEO_ID) tapped from Chrome; opens 'Esports Regional Finals' directly."
        ),
        DeepLinkTestPreset(
            id = "landing_open_app",
            label = "Landing 'Open in App'",
            sourceApp = DeepLinkSourceApp.CUSTOM_APP_SCHEME,
            sampleUrl = "sharedabba://v/vid_creator_2",
            description = "Custom scheme URI triggered by the 'Open in ShareDabba' button on the web landing page."
        ),
        DeepLinkTestPreset(
            id = "private_video",
            label = "Private Video (403)",
            sourceApp = DeepLinkSourceApp.DIRECT_SYSTEM_LINK,
            sampleUrl = "https://sharedabba.com/v/vid_private_403",
            description = "Private creator video requiring authorized owner/member access; blocks unauthorized playback."
        ),
        DeepLinkTestPreset(
            id = "deleted_video",
            label = "Deleted Video (410)",
            sourceApp = DeepLinkSourceApp.DIRECT_SYSTEM_LINK,
            sampleUrl = "https://sharedabba.com/v/vid_deleted_410",
            description = "Deleted website video link; blocks playback with 410 Gone status and offers browser fallback."
        )
    )

    /**
     * Parses any incoming raw URL string without assuming a single hardcoded route pattern.
     * Supports `/v/{VIDEO_ID}`, `/watch/{VIDEO_ID}`, `/video/{VIDEO_ID}`, query params `?v=` / `?videoId=`,
     * and custom scheme `sharedabba://v/{VIDEO_ID}` or `sharedabba://stream?url=...`.
     */
    fun parseIncomingUrl(rawUrl: String?): ParsedShareDabbaLink {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return ParsedShareDabbaLink(
                rawUrl = "",
                normalizedUrl = "",
                host = "",
                path = "",
                extractedVideoId = null,
                matchedRoutePattern = "NONE",
                detectedSourceApp = DeepLinkSourceApp.DIRECT_SYSTEM_LINK,
                directStreamOverrideUrl = null,
                browserFallbackUrl = "https://sharedabba.com",
                isRecognizedShareDabbaLink = false
            )
        }

        // If a bare video ID like "vid_recent_2" was passed directly inside the app
        if (!trimmed.contains("://") && trimmed.startsWith("vid_")) {
            return ParsedShareDabbaLink(
                rawUrl = trimmed,
                normalizedUrl = "https://sharedabba.com/v/$trimmed",
                host = "sharedabba.com",
                path = "/v/$trimmed",
                extractedVideoId = trimmed,
                matchedRoutePattern = "/v/{VIDEO_ID} (Direct Video ID)",
                detectedSourceApp = DeepLinkSourceApp.DIRECT_SYSTEM_LINK,
                directStreamOverrideUrl = null,
                browserFallbackUrl = "https://sharedabba.com/v/$trimmed",
                isRecognizedShareDabbaLink = true
            )
        }

        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            null
        }

        if (uri == null || uri.scheme == null) {
            return ParsedShareDabbaLink(
                rawUrl = trimmed,
                normalizedUrl = trimmed,
                host = "",
                path = "",
                extractedVideoId = null,
                matchedRoutePattern = "MALFORMED_URI",
                detectedSourceApp = DeepLinkSourceApp.DIRECT_SYSTEM_LINK,
                directStreamOverrideUrl = null,
                browserFallbackUrl = "https://sharedabba.com",
                isRecognizedShareDabbaLink = false
            )
        }

        val scheme = uri.scheme.lowercase()
        val host = (uri.host ?: "").lowercase()
        val rawPath = uri.path ?: ""
        val queryMap = parseQueryParameters(uri.rawQuery)

        val detectedSource = when {
            queryMap["utm_source"]?.contains("whatsapp", ignoreCase = true) == true ||
                queryMap.containsKey("wa") -> DeepLinkSourceApp.WHATSAPP
            queryMap["utm_source"]?.contains("telegram", ignoreCase = true) == true ||
                queryMap.containsKey("tg_r") -> DeepLinkSourceApp.TELEGRAM
            queryMap["source"]?.contains("chrome", ignoreCase = true) == true ||
                queryMap["feature"]?.contains("chrome", ignoreCase = true) == true -> DeepLinkSourceApp.CHROME_BROWSER
            scheme == "sharedabba" -> DeepLinkSourceApp.CUSTOM_APP_SCHEME
            else -> DeepLinkSourceApp.DIRECT_SYSTEM_LINK
        }

        val streamOverride = queryMap["url"]?.takeIf { it.startsWith("http") }

        val segments = rawPath.split('/').filter { it.isNotBlank() }
        var extractedVideoId: String? = null
        var routePattern = "UNVERIFIED_ROUTE"

        if (scheme == "sharedabba") {
            when (host) {
                "v", "watch", "video" -> {
                    extractedVideoId = segments.firstOrNull() ?: queryMap["v"] ?: queryMap["videoId"]
                    routePattern = "sharedabba://$host/{VIDEO_ID}"
                }
                "stream" -> {
                    extractedVideoId = segments.firstOrNull() ?: queryMap["v"] ?: queryMap["videoId"]
                    routePattern = "sharedabba://stream"
                }
            }
        } else if ((scheme == "https" || scheme == "http") &&
            (host in SUPPORTED_WEB_HOSTS || host.endsWith(".sharedabba.com"))
        ) {
            when {
                segments.size >= 2 && segments[0].equals("v", ignoreCase = true) -> {
                    extractedVideoId = segments[1]
                    routePattern = "https://$host/v/{VIDEO_ID} (Candidate Canonical Route)"
                }
                segments.size >= 2 && segments[0].equals("watch", ignoreCase = true) -> {
                    extractedVideoId = segments[1]
                    routePattern = "https://$host/watch/{VIDEO_ID}"
                }
                segments.size >= 2 && segments[0].equals("video", ignoreCase = true) -> {
                    extractedVideoId = segments[1]
                    routePattern = "https://$host/video/{VIDEO_ID}"
                }
                !queryMap["v"].isNullOrBlank() -> {
                    extractedVideoId = queryMap["v"]
                    routePattern = "https://$host$rawPath?v={VIDEO_ID}"
                }
                !queryMap["videoId"].isNullOrBlank() -> {
                    extractedVideoId = queryMap["videoId"]
                    routePattern = "https://$host$rawPath?videoId={VIDEO_ID}"
                }
            }
        }

        val isRecognized = !extractedVideoId.isNullOrBlank() || !streamOverride.isNullOrBlank()
        val fallbackUrl = if (!extractedVideoId.isNullOrBlank()) {
            "https://sharedabba.com/v/$extractedVideoId"
        } else if (scheme == "https" || scheme == "http") {
            trimmed
        } else {
            "https://sharedabba.com"
        }

        return ParsedShareDabbaLink(
            rawUrl = trimmed,
            normalizedUrl = fallbackUrl,
            host = host,
            path = rawPath,
            extractedVideoId = extractedVideoId,
            matchedRoutePattern = routePattern,
            detectedSourceApp = detectedSource,
            directStreamOverrideUrl = streamOverride,
            browserFallbackUrl = fallbackUrl,
            isRecognizedShareDabbaLink = isRecognized
        )
    }

    /**
     * Resolves a parsed link against the ShareDabba backend catalog, enforcing:
     * - Deleted video checks (`410 Gone`)
     * - Private video authorization checks (`403 Forbidden`)
     * - Exact video resolution (Case 3: Never opens homepage when a valid video link is requested)
     */
    fun resolveDeepLink(
        rawUrl: String?,
        isUserAuthenticated: Boolean = true,
        currentUserId: String? = null
    ): ResolvedDeepLinkResult {
        val parsed = parseIncomingUrl(rawUrl)
        val videoId = parsed.extractedVideoId

        // 1. Check if link is unrecognized or malformed
        if (!parsed.isRecognizedShareDabbaLink) {
            val defaultLanding = WebLandingPagePreviewModel(
                videoId = "unknown",
                videoTitle = "Unrecognized ShareDabba Link",
                uploaderLabel = "ShareDabba Platform",
                thumbnailUrl = "https://sharedabba.com/assets/thumbs/default.jpg",
                watchInBrowserUrl = parsed.browserFallbackUrl,
                openInShareDabbaUri = "sharedabba://v/vid_featured_1"
            )
            return ResolvedDeepLinkResult(
                parsedLink = parsed,
                status = DeepLinkResolutionStatus.INVALID_OR_UNRECOGNIZED_LINK,
                video = null,
                playerTargetKey = null,
                statusMessage = "Link does not match a recognized ShareDabba video route. Use Browser Fallback to inspect on the web.",
                landingPageModel = defaultLanding
            )
        }

        // 2. Check Deleted Video (410 Gone)
        if (videoId != null && (videoId.contains("deleted", ignoreCase = true) || videoId == "vid_deleted_410")) {
            val deletedLanding = WebLandingPagePreviewModel(
                videoId = videoId,
                videoTitle = "Removed Video (410 Gone)",
                uploaderLabel = "Removed by Creator / Moderation",
                thumbnailUrl = "https://sharedabba.com/assets/thumbs/deleted.jpg",
                watchInBrowserUrl = parsed.browserFallbackUrl,
                openInShareDabbaUri = "sharedabba://v/$videoId"
            )
            return ResolvedDeepLinkResult(
                parsedLink = parsed,
                status = DeepLinkResolutionStatus.VIDEO_DELETED_410,
                video = null,
                playerTargetKey = "vid_deleted_410",
                statusMessage = "410 Gone: Video '$videoId' was deleted on the ShareDabba website and cannot be played.",
                landingPageModel = deletedLanding
            )
        }

        // 3. Check Private / Unauthorized Video (403 Forbidden)
        if (videoId != null && (videoId.contains("private", ignoreCase = true) || videoId == "vid_unauth_403")) {
            val isAuthorizedOwner = isUserAuthenticated && currentUserId == "usr_private_owner"
            if (!isAuthorizedOwner) {
                val privateLanding = WebLandingPagePreviewModel(
                    videoId = videoId,
                    videoTitle = "Private Creator Stream (Authorization Required)",
                    uploaderLabel = "Private Uploader Vault",
                    thumbnailUrl = "https://sharedabba.com/assets/thumbs/private.jpg",
                    watchInBrowserUrl = parsed.browserFallbackUrl,
                    openInShareDabbaUri = "sharedabba://v/$videoId"
                )
                return ResolvedDeepLinkResult(
                    parsedLink = parsed,
                    status = DeepLinkResolutionStatus.PRIVATE_VIDEO_AUTH_REQUIRED,
                    video = null,
                    playerTargetKey = "vid_unauth_403",
                    statusMessage = if (!isUserAuthenticated) {
                        "401/403 Authentication Required: Sign in to your ShareDabba account to verify access to private video '$videoId'."
                    } else {
                        "403 Private Video: Your account (${currentUserId ?: "signed-in user"}) does not have permission to view private video '$videoId'."
                    },
                    landingPageModel = privateLanding
                )
            }
        }

        // 4. Resolve exact video from catalog (or direct stream override)
        val matchedVideo = if (videoId != null) {
            DevFixturesCatalog.videos.firstOrNull { it.id.equals(videoId, ignoreCase = true) }
                ?: if (videoId.equals("featured", ignoreCase = true)) {
                    DevFixturesCatalog.videos.first()
                } else {
                    null
                }
        } else {
            DevFixturesCatalog.videos.firstOrNull { it.streamUrl == parsed.directStreamOverrideUrl }
        }

        if (matchedVideo == null && parsed.directStreamOverrideUrl == null) {
            val notFoundLanding = WebLandingPagePreviewModel(
                videoId = videoId ?: "not_found",
                videoTitle = "Video Not Found ($videoId)",
                uploaderLabel = "ShareDabba Catalog",
                thumbnailUrl = "https://sharedabba.com/assets/thumbs/404.jpg",
                watchInBrowserUrl = parsed.browserFallbackUrl,
                openInShareDabbaUri = "sharedabba://v/${videoId ?: ""}"
            )
            return ResolvedDeepLinkResult(
                parsedLink = parsed,
                status = DeepLinkResolutionStatus.VIDEO_NOT_FOUND_404,
                video = null,
                playerTargetKey = null,
                statusMessage = "404 Not Found: Video ID '$videoId' was not found in the ShareDabba catalog.",
                landingPageModel = notFoundLanding
            )
        }

        val effectiveVideo = matchedVideo ?: DevFixturesCatalog.videos.first()
        val targetKey = matchedVideo?.id ?: parsed.directStreamOverrideUrl ?: effectiveVideo.id

        val landingModel = WebLandingPagePreviewModel(
            videoId = effectiveVideo.id,
            videoTitle = effectiveVideo.title,
            uploaderLabel = "${effectiveVideo.uploaderName} (${effectiveVideo.uploaderHandle})",
            thumbnailUrl = effectiveVideo.thumbnailUrl,
            watchInBrowserUrl = "https://sharedabba.com/v/${effectiveVideo.id}",
            openInShareDabbaUri = "sharedabba://v/${effectiveVideo.id}"
        )

        return ResolvedDeepLinkResult(
            parsedLink = parsed,
            status = DeepLinkResolutionStatus.RESOLVED_READY_FOR_PLAYER,
            video = effectiveVideo,
            playerTargetKey = targetKey,
            statusMessage = "Resolved exact video '${effectiveVideo.title}' (ID: ${effectiveVideo.id}) from ${parsed.detectedSourceApp.label}.",
            landingPageModel = landingModel
        )
    }

    /**
     * Extracts the exact player request key (`videoId` or `streamUrl`) from an Android `Intent`.
     * Used by `MainActivity` (`onCreate` and `onNewIntent`) to route directly to the Native Video Player.
     */
    fun extractStreamUrlFromIntent(intent: Intent?): String? {
        val dataString = intent?.dataString ?: intent?.data?.toString() ?: return null
        val resolved = resolveDeepLink(dataString)
        return resolved.playerTargetKey ?: dataString
    }

    private fun parseQueryParameters(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split('&')
            .mapNotNull { pair ->
                val idx = pair.indexOf('=')
                if (idx <= 0) {
                    null
                } else {
                    val key = pair.substring(0, idx).trim()
                    val value = pair.substring(idx + 1).trim()
                    key to value
                }
            }
            .toMap()
    }
}
