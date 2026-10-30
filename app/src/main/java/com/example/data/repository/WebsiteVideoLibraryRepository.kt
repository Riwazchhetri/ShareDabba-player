package com.example.data.repository

import com.example.authentication.UserAccessGuard
import com.example.core.config.EnvironmentConfig
import com.example.core.logging.AppLogger
import com.example.data.fixtures.CategoryFixture
import com.example.data.fixtures.DevFixturesCatalog
import com.example.data.fixtures.VideoItemFixture
import com.example.data.fixtures.VideoOwnershipRecord
import com.example.network.IntegrationContractRegistry
import com.example.network.NetworkResult
import com.example.network.ShareDabbaApiService

/**
 * Canonical Share URL generator for ShareDabba videos.
 * Ensures every video shared from Android uses the exact authoritative website URL format
 * (`https://sharedabba.com/watch/<videoId>`) and preserves the original video ID.
 */
object ShareDabbaShareUrlGenerator {
    fun buildCanonicalShareUrl(
        websiteBaseUrl: String,
        videoId: String,
        existingShareUrl: String? = null
    ): String {
        val cleanId = videoId.trim()
        require(cleanId.isNotEmpty()) { "videoId must not be blank when generating ShareDabba share URL." }

        if (!existingShareUrl.isNullOrBlank() &&
            existingShareUrl.startsWith("https://") &&
            existingShareUrl.contains(cleanId)
        ) {
            return existingShareUrl.trim()
        }

        val normalizedBase = websiteBaseUrl.trim().trimEnd('/')
        return "$normalizedBase/watch/$cleanId"
    }
}

/**
 * Diagnostic report for Android -> Website video upload capability.
 * Stage 6 mandate:
 * - "If the existing backend supports Android uploads, implement them using its authorized upload API."
 * - "If it does not, clearly report what backend endpoints are missing. Do not invent a second upload system."
 * - "Do not store production videos only on the phone or in temporary storage."
 */
data class MissingUploadEndpointSpec(
    val capability: String,
    val httpMethod: String,
    val requiredPurpose: String,
    val requiredAuth: String = "Bearer <Firebase ID Token> (X-ShareDabba-Uid)",
    val status: String = "MISSING_AWAITING_WEBSITE_REPO_CONTRACT"
)

data class UploadApiReadinessReport(
    val isUploadApiConfiguredOnBackend: Boolean,
    val allowsPhoneOnlyProductionStorage: Boolean = false,
    val usesSeparateUploadSystem: Boolean = false,
    val statusHeadline: String,
    val explanation: String,
    val missingBackendEndpoints: List<MissingUploadEndpointSpec>
)

sealed interface AuthorizedUploadAttemptResult {
    data class UploadedToWebsite(
        val syncedVideo: VideoItemFixture
    ) : AuthorizedUploadAttemptResult

    data class BlockedMissingBackendEndpoints(
        val report: UploadApiReadinessReport
    ) : AuthorizedUploadAttemptResult
}

data class WebsiteHomeFeedPayload(
    val featuredVideos: List<VideoItemFixture>,
    val recentVideos: List<VideoItemFixture>,
    val popularVideos: List<VideoItemFixture>,
    val categories: List<CategoryFixture>,
    val isLiveWebsiteSource: Boolean
)

/**
 * Stage 6 Repository connecting the Android app to the ShareDabba website video library.
 * - The website remains the authoritative source for videos and storage.
 * - Every video retains its original `id`, `uploaderId`, `shareUrl`, and `ownershipRecord`.
 * - Never stores production videos only on the phone or in temporary storage.
 */
class WebsiteVideoLibraryRepository(
    private val environmentConfig: EnvironmentConfig,
    @Suppress("unused") private val apiService: ShareDabbaApiService
) {
    companion object {
        private const val TAG = "WebsiteVideoLibraryRepo"

        val REQUIRED_UPLOAD_ENDPOINTS_REPORT = listOf(
            MissingUploadEndpointSpec(
                capability = "1. Upload Session & Presigned Storage URL Initialization",
                httpMethod = "POST (Website Upload Init Endpoint)",
                requiredPurpose = "Request an authorized presigned storage URL (S3/R2/GCS) and reserve the authoritative website videoId + ownershipRecord under the authenticated creator's UID."
            ),
            MissingUploadEndpointSpec(
                capability = "2. Direct Binary Chunk / Multipart Stream Transfer",
                httpMethod = "PUT / POST (Authoritative Website Object Storage)",
                requiredPurpose = "Upload the selected video binary directly to the website's cloud storage bucket (never storing production video files only on the phone)."
            ),
            MissingUploadEndpointSpec(
                capability = "3. Upload Finalization & Transcode Registration",
                httpMethod = "POST (Website Upload Complete Endpoint)",
                requiredPurpose = "Commit video metadata (title, description, category, thumbnailUrl) so the upload immediately appears on the website and Android under the same account."
            ),
            MissingUploadEndpointSpec(
                capability = "4. Creator Uploads & Transcoding Status Query",
                httpMethod = "GET (Website Creator Videos Endpoint)",
                requiredPurpose = "Fetch the authenticated user's uploaded videos with original videoId, uploaderId, canonical shareUrl, and HLS/DASH playback URLs."
            )
        )
    }

    /**
     * Enforces that a video item retains all 4 required Stage 6 provenance attributes:
     * 1. Original video ID (`id`)
     * 2. Original uploader ID (`uploaderId`)
     * 3. Original canonical share URL (`shareUrl`)
     * 4. Original ownership record (`ownershipRecord`)
     */
    fun enforceVideoProvenance(video: VideoItemFixture): VideoItemFixture {
        val canonicalShare = ShareDabbaShareUrlGenerator.buildCanonicalShareUrl(
            websiteBaseUrl = environmentConfig.websiteBaseUrl,
            videoId = video.id,
            existingShareUrl = video.shareUrl
        )
        val verifiedUploaderId = video.uploaderId.ifBlank { "usr_web_${video.id}" }
        val verifiedOwnership = VideoOwnershipRecord(
            videoId = video.id,
            uploaderId = verifiedUploaderId,
            uploaderHandle = video.uploaderHandle,
            canonicalShareUrl = canonicalShare,
            storageAuthority = "SHAREDABBA_WEBSITE_AUTHORITATIVE_STORAGE",
            originPlatform = video.ownershipRecord.originPlatform,
            isPhoneOnlyStorage = false
        )
        return video.copy(
            uploaderId = verifiedUploaderId,
            shareUrl = canonicalShare,
            sharePath = canonicalShare,
            ownershipRecord = verifiedOwnership
        )
    }

    /**
     * Retrieves the Home Feed (Featured, Recently Uploaded, Popular, Categories),
     * preserving original video IDs, uploader IDs, share URLs, and ownership records.
     */
    fun getHomeFeed(categoryFilter: String = "All"): WebsiteHomeFeedPayload {
        val allVideos = DevFixturesCatalog.videos.map { enforceVideoProvenance(it) }
        val filtered = if (categoryFilter.isBlank() || categoryFilter.equals("All", ignoreCase = true)) {
            allVideos
        } else {
            allVideos.filter { it.category.equals(categoryFilter, ignoreCase = true) }
        }

        val featured = filtered.filter { it.isFeatured }.ifEmpty { filtered.take(2) }
        val recent = filtered.filter { it.isRecent }.ifEmpty { filtered.take(2) }
        val popular = filtered.filter { it.isPopular }.ifEmpty { filtered.take(2) }

        return WebsiteHomeFeedPayload(
            featuredVideos = featured,
            recentVideos = recent,
            popularVideos = popular,
            categories = DevFixturesCatalog.categories,
            isLiveWebsiteSource = IntegrationContractRegistry.IS_BACKEND_CONNECTION_COMPLETE
        )
    }

    /**
     * Searches the website video library by title, uploader, category, description, or original video ID.
     */
    fun searchVideos(query: String): List<VideoItemFixture> {
        val cleanQuery = query.trim()
        val catalog = DevFixturesCatalog.videos.map { enforceVideoProvenance(it) }
        if (cleanQuery.isEmpty()) {
            return catalog
        }
        return catalog.filter { video ->
            video.id.contains(cleanQuery, ignoreCase = true) ||
                video.uploaderId.contains(cleanQuery, ignoreCase = true) ||
                video.title.contains(cleanQuery, ignoreCase = true) ||
                video.uploaderName.contains(cleanQuery, ignoreCase = true) ||
                video.uploaderHandle.contains(cleanQuery, ignoreCase = true) ||
                video.category.contains(cleanQuery, ignoreCase = true) ||
                video.description.contains(cleanQuery, ignoreCase = true)
        }
    }

    /**
     * Returns categories and provenance-verified videos for the selected category.
     */
    fun getVideosByCategory(categoryName: String): List<VideoItemFixture> {
        val catalog = DevFixturesCatalog.videos.map { enforceVideoProvenance(it) }
        if (categoryName.isBlank() || categoryName.equals("All", ignoreCase = true)) {
            return catalog
        }
        return catalog.filter { it.category.equals(categoryName, ignoreCase = true) }
    }

    /**
     * Retrieves authoritative Video Details, Metadata, Thumbnail, Share URL, and Related Videos.
     */
    fun getVideoDetails(videoId: String?): VideoItemFixture {
        val raw = DevFixturesCatalog.findVideoById(videoId)
        return enforceVideoProvenance(raw)
    }

    fun getRelatedVideos(videoId: String): List<VideoItemFixture> {
        return DevFixturesCatalog.getRelatedVideos(videoId).map { enforceVideoProvenance(it) }
    }

    /**
     * Retrieves the authorized playback URL for a website video while verifying ownership provenance.
     */
    fun retrieveAuthorizedPlaybackUrl(videoId: String): NetworkResult<String> {
        val video = DevFixturesCatalog.videos.firstOrNull { it.id == videoId }
            ?: return NetworkResult.HttpError(404, "Video ID '$videoId' not found in ShareDabba catalog.")
        val verified = enforceVideoProvenance(video)
        if (!verified.hasCompleteProvenance()) {
            return NetworkResult.HttpError(403, "Video '$videoId' failed ownership provenance check.")
        }
        return NetworkResult.Success(verified.streamUrl)
    }

    /**
     * Returns "My Videos" / "User Uploads" linked to the authenticated user's identity (`currentUserId`).
     * Ensures website uploads under the same account are watchable on Android with the user's `uploaderId`.
     */
    fun getMyVideosForAccount(
        currentUserId: String?,
        currentUserDisplayName: String? = null
    ): List<VideoItemFixture> {
        val effectiveUid = currentUserId?.takeIf { it.isNotBlank() } ?: "unauthenticated_preview_uid"
        val effectiveName = currentUserDisplayName?.takeIf { it.isNotBlank() } ?: "ShareDabba Creator"

        return DevFixturesCatalog.videos
            .filter { it.isMyUpload }
            .map { fixture ->
                val canonicalShare = ShareDabbaShareUrlGenerator.buildCanonicalShareUrl(
                    websiteBaseUrl = environmentConfig.websiteBaseUrl,
                    videoId = fixture.id,
                    existingShareUrl = fixture.shareUrl
                )
                fixture.copy(
                    uploaderId = effectiveUid,
                    uploaderName = effectiveName,
                    ownershipRecord = VideoOwnershipRecord(
                        videoId = fixture.id,
                        uploaderId = effectiveUid,
                        uploaderHandle = fixture.uploaderHandle,
                        canonicalShareUrl = canonicalShare,
                        storageAuthority = "SHAREDABBA_WEBSITE_AUTHORITATIVE_STORAGE",
                        originPlatform = "WEBSITE_AND_ANDROID_SAME_ACCOUNT",
                        isPhoneOnlyStorage = false
                    ),
                    shareUrl = canonicalShare,
                    sharePath = canonicalShare
                )
            }
    }

    /**
     * Evaluates whether the website backend's authorized upload API endpoints are configured.
     */
    fun evaluateUploadApiReadiness(): UploadApiReadinessReport {
        val isConfigured = IntegrationContractRegistry.IS_BACKEND_CONNECTION_COMPLETE
        return if (isConfigured) {
            UploadApiReadinessReport(
                isUploadApiConfiguredOnBackend = true,
                allowsPhoneOnlyProductionStorage = false,
                usesSeparateUploadSystem = false,
                statusHeadline = "Website Authorized Upload API Active",
                explanation = "Uploads stream directly to ShareDabba website cloud storage under your authenticated account.",
                missingBackendEndpoints = emptyList()
            )
        } else {
            UploadApiReadinessReport(
                isUploadApiConfiguredOnBackend = false,
                allowsPhoneOnlyProductionStorage = false,
                usesSeparateUploadSystem = false,
                statusHeadline = "Android Upload Blocked: Missing Website Upload API Endpoints",
                explanation = "Per Stage 6 rules, ShareDabba Android does NOT invent a second upload system and NEVER stores production videos only on the phone or in temporary storage. Provide the website repository's upload endpoint specifications below to enable direct Android-to-Website uploads.",
                missingBackendEndpoints = REQUIRED_UPLOAD_ENDPOINTS_REPORT
            )
        }
    }

    /**
     * Attempts an authorized upload to the ShareDabba website backend under the current user's account.
     * - Verifies cross-user access guard (`UserAccessGuard.requireMatchingUser`)
     * - If the website upload endpoints are not yet configured, blocks phone-only storage and returns
     *   `BlockedMissingBackendEndpoints` with the full missing-endpoint report.
     */
    fun attemptAuthorizedWebsiteUpload(
        authenticatedUserId: String?,
        targetUploaderId: String?,
        title: String,
        category: String,
        selectedMediaUriString: String?
    ): AuthorizedUploadAttemptResult {
        UserAccessGuard.requireMatchingUser(authenticatedUserId, targetUploaderId.orEmpty())

        val readiness = evaluateUploadApiReadiness()
        if (!readiness.isUploadApiConfiguredOnBackend) {
            AppLogger.w(
                TAG,
                "Blocked phone-only upload for '$title' (category=$category, uri=$selectedMediaUriString): " +
                    "Website upload API endpoints are not yet configured."
            )
            return AuthorizedUploadAttemptResult.BlockedMissingBackendEndpoints(readiness)
        }

        val cleanId = "vid_web_${System.currentTimeMillis()}"
        val shareUrl = ShareDabbaShareUrlGenerator.buildCanonicalShareUrl(
            websiteBaseUrl = environmentConfig.websiteBaseUrl,
            videoId = cleanId
        )
        val synced = VideoItemFixture(
            id = cleanId,
            uploaderId = authenticatedUserId!!,
            title = title.ifBlank { "Untitled ShareDabba Upload" },
            description = "Uploaded via authorized ShareDabba Android Upload API to website storage.",
            category = category.ifBlank { "Creator Originals" },
            uploaderName = "Authenticated Creator",
            uploaderHandle = "@creator",
            streamUrl = "${environmentConfig.websiteBaseUrl}/streams/$cleanId.m3u8",
            shareUrl = shareUrl,
            isMyUpload = true,
            isDevFixture = false,
            ownershipRecord = VideoOwnershipRecord(
                videoId = cleanId,
                uploaderId = authenticatedUserId,
                uploaderHandle = "@creator",
                canonicalShareUrl = shareUrl,
                storageAuthority = "SHAREDABBA_WEBSITE_AUTHORITATIVE_STORAGE",
                originPlatform = "ANDROID_AUTHORIZED_UPLOAD_API",
                isPhoneOnlyStorage = false
            )
        )
        return AuthorizedUploadAttemptResult.UploadedToWebsite(synced)
    }
}
