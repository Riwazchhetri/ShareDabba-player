package com.example

import com.example.authentication.SessionTokenCoordinator
import com.example.authentication.UserAccessGuard
import com.example.core.config.EnvironmentConfig
import com.example.core.logging.AppLogger
import com.example.data.fixtures.DevFixturesCatalog
import com.example.data.repository.AuthorizedUploadAttemptResult
import com.example.data.repository.ShareDabbaShareUrlGenerator
import com.example.data.repository.WebsiteVideoLibraryRepository
import com.example.deeplinks.AssetLinksConfigurationSpec
import com.example.deeplinks.DeepLinkResolutionStatus
import com.example.deeplinks.DeepLinkRouter
import com.example.deeplinks.DeepLinkSourceApp
import com.example.network.AuthHeaderInterceptor
import com.example.network.NetworkClient
import com.example.network.NetworkResult
import com.example.network.TokenRefreshAuthenticator
import com.example.ui.navigation.ShareDabbaDestination
import com.example.videoplayer.AdPlacementReadinessState
import com.example.videoplayer.PlaybackAuthorizationStatus
import com.example.videoplayer.PlayerTestScenario
import com.example.videoplayer.VideoPlaybackValidator
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun environmentConfig_hasExpectedDefaults() {
        val config = EnvironmentConfig()
        assertEquals("com.aistudio.sharedabba.vstream", config.applicationId)
        assertEquals("gemini-3.1-flash-lite", config.lowLatencyAiModel)
        assertEquals("gemini-4-argon", config.highThinkingAiModel)
        assertTrue(config.websiteBaseUrl.startsWith("https://"))
    }

    @Test
    fun appLogger_recordsDiagnosticEntries() {
        AppLogger.i("UnitTest", "Stage 1 test log entry")
        assertTrue(AppLogger.recentLogs.value.any { it.tag == "UnitTest" })
    }

    @Test
    fun stage3_fixturesAndNavigationRoutes_areComplete() {
        assertTrue(DevFixturesCatalog.videos.any { it.isFeatured })
        assertTrue(DevFixturesCatalog.videos.any { it.isRecent })
        assertTrue(DevFixturesCatalog.videos.any { it.isPopular })
        assertTrue(DevFixturesCatalog.videos.any { it.isMyUpload })
        assertTrue(DevFixturesCatalog.videos.all { it.isDevFixture })
        assertTrue(DevFixturesCatalog.categories.isNotEmpty())
        assertTrue(DevFixturesCatalog.earningsBreakdown.isNotEmpty())
        assertTrue(DevFixturesCatalog.withdrawalHistory.isNotEmpty())
        assertTrue(DevFixturesCatalog.initialNotifications.isNotEmpty())
        assertTrue(DevFixturesCatalog.supportFaqs.isNotEmpty())

        assertEquals(
            "video_details/vid_featured_1",
            ShareDabbaDestination.VideoDetails.createRoute("vid_featured_1")
        )
        assertEquals("recently_watched", ShareDabbaDestination.RecentlyWatched.route)
        assertEquals("user_dashboard", ShareDabbaDestination.UserDashboard.route)
        assertEquals("withdrawal_history", ShareDabbaDestination.WithdrawalHistory.route)
        assertEquals("notifications", ShareDabbaDestination.Notifications.route)
        assertEquals("help_support", ShareDabbaDestination.HelpSupport.route)
    }

    @Test
    fun stage4_differentVideoFormats_resolveProperly() {
        val mp4Payload = VideoPlaybackValidator.resolveAuthorizedPayload(
            "vid_featured_1",
            PlayerTestScenario.STANDARD_MP4
        )
        assertTrue(mp4Payload.isPlayable)
        assertTrue(mp4Payload.formatLabel.contains("MP4"))
        assertEquals(5, mp4Payload.qualityOptions.size)
        assertNotNull(mp4Payload.nextVideo)
        assertTrue(mp4Payload.relatedVideos.isNotEmpty())

        val hlsPayload = VideoPlaybackValidator.resolveAuthorizedPayload(
            "vid_featured_1",
            PlayerTestScenario.HLS_STREAM
        )
        assertTrue(hlsPayload.isPlayable)
        assertTrue(hlsPayload.authorizedStreamUrl.endsWith(".m3u8"))
        assertTrue(hlsPayload.formatLabel.contains("HLS"))

        val dashPayload = VideoPlaybackValidator.resolveAuthorizedPayload(
            "vid_featured_1",
            PlayerTestScenario.DASH_STREAM
        )
        assertTrue(dashPayload.isPlayable)
        assertTrue(dashPayload.authorizedStreamUrl.endsWith(".mpd"))
        assertTrue(dashPayload.formatLabel.contains("DASH"))
    }

    @Test
    fun stage4_invalidUnauthorizedAndDeletedVideos_areBlockedFromPlayback() {
        assertFalse(VideoPlaybackValidator.isValidStreamUrl(""))
        assertFalse(VideoPlaybackValidator.isValidStreamUrl("ftp://bad-host/video.mp4"))
        assertFalse(VideoPlaybackValidator.isValidStreamUrl("invalid-url"))
        assertTrue(VideoPlaybackValidator.isValidStreamUrl("https://sharedabba.com/stream.mp4"))

        val unauthPayload = VideoPlaybackValidator.resolveAuthorizedPayload(
            "vid_featured_1",
            PlayerTestScenario.UNAUTHORIZED_VIDEO
        )
        assertFalse(unauthPayload.isPlayable)
        assertEquals(
            PlaybackAuthorizationStatus.UNAUTHORIZED_ACCESS,
            unauthPayload.authorizationStatus
        )

        val deletedPayload = VideoPlaybackValidator.resolveAuthorizedPayload(
            "vid_featured_1",
            PlayerTestScenario.DELETED_VIDEO
        )
        assertFalse(deletedPayload.isPlayable)
        assertEquals(
            PlaybackAuthorizationStatus.VIDEO_DELETED,
            deletedPayload.authorizationStatus
        )

        val invalidUrlPayload = VideoPlaybackValidator.resolveAuthorizedPayload(
            "corrupted://stream",
            PlayerTestScenario.INVALID_URL
        )
        assertFalse(invalidUrlPayload.isPlayable)
        assertEquals(
            PlaybackAuthorizationStatus.INVALID_STREAM_URL,
            invalidUrlPayload.authorizationStatus
        )
    }

    @Test
    fun stage4_longVideosAndInterruptedNetworkRecovery_workAsExpected() {
        val longPayload = VideoPlaybackValidator.resolveAuthorizedPayload(
            "vid_featured_1",
            PlayerTestScenario.LONG_VIDEO
        )
        assertTrue(longPayload.isPlayable)
        assertEquals("2:15:30", VideoPlaybackValidator.formatPlaybackTimeMs(longPayload.durationEstimateMs))
        assertEquals("00:42", VideoPlaybackValidator.formatPlaybackTimeMs(42_500L))

        val interruptedPayload = VideoPlaybackValidator.resolveAuthorizedPayload(
            "vid_featured_1",
            PlayerTestScenario.INTERRUPTED_INTERNET
        )
        assertFalse(interruptedPayload.isPlayable)
        assertEquals(
            PlaybackAuthorizationStatus.NETWORK_INTERRUPTED,
            interruptedPayload.authorizationStatus
        )
    }

    @Test
    fun stage4_videoStartsAreNeverCountedAsAdvertisingImpressions() {
        var adState = AdPlacementReadinessState()
        assertEquals(0, adState.videoStartsCount)
        assertEquals(0, adState.verifiedAdImpressionsCount)

        repeat(5) {
            adState = VideoPlaybackValidator.recordVideoStartWithoutFakeAdImpression(adState)
        }
        assertEquals(5, adState.videoStartsCount)
        assertEquals(0, adState.verifiedAdImpressionsCount)
    }

    @Test
    fun stage5_crossUserAccessGuard_preventsUnauthorizedDataAccess() {
        assertTrue(UserAccessGuard.isAuthorizedForUser("uid_creator_1", "uid_creator_1"))
        assertFalse(UserAccessGuard.isAuthorizedForUser("uid_creator_1", "uid_creator_2"))
        assertFalse(UserAccessGuard.isAuthorizedForUser(null, "uid_creator_1"))

        UserAccessGuard.requireMatchingUser("uid_creator_1", "uid_creator_1")

        assertThrows(SecurityException::class.java) {
            UserAccessGuard.requireMatchingUser("uid_creator_1", "uid_creator_2")
        }
        assertThrows(SecurityException::class.java) {
            UserAccessGuard.requireMatchingUser(null, "uid_creator_1")
        }
    }

    @Test
    fun stage5_expiredTokenAuthenticator_recoversWithFreshBearerToken() {
        val authenticator = TokenRefreshAuthenticator(
            forceRefreshTokenProvider = { "fresh_recovered_jwt_token_xyz" }
        )
        val originalRequest = Request.Builder()
            .url("https://api.sharedabba.com/v1/profile")
            .header("Authorization", "Bearer expired_token_abc")
            .build()

        val unauthorizedResponse = Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody())
            .build()

        val retriedRequest = authenticator.authenticate(null, unauthorizedResponse)
        assertNotNull(retriedRequest)
        assertEquals("Bearer fresh_recovered_jwt_token_xyz", retriedRequest?.header("Authorization"))
        assertEquals("true", retriedRequest?.header("X-Token-Refreshed"))

        // Verify loop protection stops if the second attempt also fails with 401
        val secondUnauthorized = Response.Builder()
            .request(retriedRequest!!)
            .priorResponse(unauthorizedResponse.newBuilder().body(null).build())
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody())
            .build()
        assertNull(authenticator.authenticate(null, secondUnauthorized))
    }

    @Test
    fun stage5_tokenFingerprintAndExpiryChecks_areAccurate() {
        val fp = SessionTokenCoordinator.sha256Fingerprint("sample_id_token")
        assertEquals(16, fp.length)
        assertTrue(SessionTokenCoordinator.isTokenExpiredAt(1000L, 1500L))
        assertFalse(SessionTokenCoordinator.isTokenExpiredAt(2000L, 1500L))
    }

    @Test
    fun stage6_everyVideoRetainsOriginalIdUploaderIdShareUrlAndOwnershipRecord() {
        val config = EnvironmentConfig()
        val repo = WebsiteVideoLibraryRepository(config, NetworkClient.createApiService(config))

        val homeFeed = repo.getHomeFeed()
        assertTrue(homeFeed.featuredVideos.isNotEmpty())
        assertTrue(homeFeed.recentVideos.isNotEmpty())
        assertTrue(homeFeed.popularVideos.isNotEmpty())
        assertTrue(homeFeed.featuredVideos.all { it.hasCompleteProvenance() })
        assertTrue(homeFeed.recentVideos.all { it.hasCompleteProvenance() })
        assertTrue(homeFeed.popularVideos.all { it.hasCompleteProvenance() })

        val searchResults = repo.searchVideos("Media3")
        assertTrue(searchResults.isNotEmpty())
        assertTrue(searchResults.all { it.hasCompleteProvenance() })

        val myVideos = repo.getMyVideosForAccount("uid_same_account_99", "Rudra Creator")
        assertTrue(myVideos.isNotEmpty())
        assertTrue(myVideos.all { it.uploaderId == "uid_same_account_99" })
        assertTrue(myVideos.all { it.ownershipRecord.uploaderId == "uid_same_account_99" })
        assertTrue(myVideos.all { !it.ownershipRecord.isPhoneOnlyStorage })

        val canonicalShare = ShareDabbaShareUrlGenerator.buildCanonicalShareUrl(
            websiteBaseUrl = "https://sharedabba.com/",
            videoId = "vid_featured_1"
        )
        assertEquals("https://sharedabba.com/watch/vid_featured_1", canonicalShare)

        val playbackResult = repo.retrieveAuthorizedPlaybackUrl("vid_featured_1")
        assertTrue(playbackResult is NetworkResult.Success)
    }

    @Test
    fun stage6_uploadApiBlocksPhoneOnlyStorageAndReportsMissingEndpointsWhenUnconfigured() {
        val config = EnvironmentConfig()
        val repo = WebsiteVideoLibraryRepository(config, NetworkClient.createApiService(config))

        val uploadAttempt = repo.attemptAuthorizedWebsiteUpload(
            authenticatedUserId = "uid_creator_1",
            targetUploaderId = "uid_creator_1",
            title = "New Creator Video",
            category = "Tech & Architecture",
            selectedMediaUriString = "content://media/picker/video/101"
        )
        assertTrue(uploadAttempt is AuthorizedUploadAttemptResult.BlockedMissingBackendEndpoints)
        val blocked = uploadAttempt as AuthorizedUploadAttemptResult.BlockedMissingBackendEndpoints
        assertFalse(blocked.report.allowsPhoneOnlyProductionStorage)
        assertFalse(blocked.report.usesSeparateUploadSystem)
        assertEquals(4, blocked.report.missingBackendEndpoints.size)
    }

    @Test
    fun stage7_deepLinksFromWhatsAppTelegramChromeAndCustomScheme_resolveExactVideoRequested() {
        val whatsappLink = DeepLinkRouter.resolveDeepLink(
            "https://sharedabba.com/v/vid_featured_2?utm_source=whatsapp&utm_medium=chat"
        )
        assertTrue(whatsappLink.canOpenInNativePlayer)
        assertEquals(DeepLinkSourceApp.WHATSAPP, whatsappLink.parsedLink.detectedSourceApp)
        assertEquals("vid_featured_2", whatsappLink.playerTargetKey)
        assertEquals("Creator Studio Masterclass: Maximizing Eligible Views", whatsappLink.video?.title)

        val telegramLink = DeepLinkRouter.resolveDeepLink(
            "https://sharedabba.com/v/vid_recent_2?tg_r=1&utm_source=telegram"
        )
        assertTrue(telegramLink.canOpenInNativePlayer)
        assertEquals(DeepLinkSourceApp.TELEGRAM, telegramLink.parsedLink.detectedSourceApp)
        assertEquals("vid_recent_2", telegramLink.playerTargetKey)

        val chromeLink = DeepLinkRouter.resolveDeepLink(
            "https://www.sharedabba.com/watch/vid_pop_1?source=chrome"
        )
        assertTrue(chromeLink.canOpenInNativePlayer)
        assertEquals(DeepLinkSourceApp.CHROME_BROWSER, chromeLink.parsedLink.detectedSourceApp)
        assertEquals("vid_pop_1", chromeLink.playerTargetKey)

        val customSchemeLink = DeepLinkRouter.resolveDeepLink("sharedabba://v/vid_creator_2")
        assertTrue(customSchemeLink.canOpenInNativePlayer)
        assertEquals(DeepLinkSourceApp.CUSTOM_APP_SCHEME, customSchemeLink.parsedLink.detectedSourceApp)
        assertEquals("vid_creator_2", customSchemeLink.playerTargetKey)

        // Verify VideoPlaybackValidator resolves the exact video from a canonical /v/ URL
        val playerPayload = VideoPlaybackValidator.resolveAuthorizedPayload(
            "https://sharedabba.com/v/vid_recent_2?utm_source=telegram"
        )
        assertTrue(playerPayload.isPlayable)
        assertEquals("vid_recent_2", playerPayload.videoId)
    }

    @Test
    fun stage7_privateDeletedAndNotFoundDeepLinks_enforceAuthAndErrorHandling() {
        val privateUnauth = DeepLinkRouter.resolveDeepLink(
            rawUrl = "https://sharedabba.com/v/vid_private_403",
            isUserAuthenticated = false
        )
        assertFalse(privateUnauth.canOpenInNativePlayer)
        assertEquals(DeepLinkResolutionStatus.PRIVATE_VIDEO_AUTH_REQUIRED, privateUnauth.status)

        val deletedLink = DeepLinkRouter.resolveDeepLink("https://sharedabba.com/v/vid_deleted_410")
        assertFalse(deletedLink.canOpenInNativePlayer)
        assertEquals(DeepLinkResolutionStatus.VIDEO_DELETED_410, deletedLink.status)

        val notFoundLink = DeepLinkRouter.resolveDeepLink("https://sharedabba.com/v/vid_missing_999")
        assertFalse(notFoundLink.canOpenInNativePlayer)
        assertEquals(DeepLinkResolutionStatus.VIDEO_NOT_FOUND_404, notFoundLink.status)

        // Verify assetlinks.json configuration honesty & structure
        assertFalse(AssetLinksConfigurationSpec.IS_VERIFIED_APP_LINKS_ACTIVE_ON_PRODUCTION_DOMAIN)
        val assetLinksJson = AssetLinksConfigurationSpec.buildAssetLinksJson()
        assertTrue(assetLinksJson.contains("delegate_permission/common.handle_all_urls"))
        assertTrue(assetLinksJson.contains("com.aistudio.sharedabba.vstream"))
    }
}
