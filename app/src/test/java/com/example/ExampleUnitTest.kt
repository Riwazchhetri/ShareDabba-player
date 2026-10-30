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

    @Test
    fun stage8_remoteConfigLaunchFetchIntervalThrottlingVersioningAndRollback_workProperly() {
        val config = EnvironmentConfig()
        val manager = com.example.remoteconfig.RemoteConfigManager(config)

        val initial = manager.fetchOnAppLaunch(currentTimeMs = 1_000_000L)
        assertEquals(1, initial.configVersion)
        assertEquals(1, manager.networkFetchCount)
        assertEquals(0, manager.skippedThrottledFetchCount)

        // Calling refresh 30 seconds later should be throttled to avoid unnecessary network requests
        manager.refreshFromWebsiteAdminPanel(forceRefresh = false, currentTimeMs = 1_030_000L)
        assertEquals(1, manager.networkFetchCount)
        assertEquals(1, manager.skippedThrottledFetchCount)

        // Apply a Website Admin Panel update (v2)
        val v2Candidate = initial.copy(
            configVersion = 2,
            sourceLabel = "ADMIN_PANEL_V2",
            homeScreen = initial.homeScreen.copy(showHeroBanner = false),
            videoPlayer = initial.videoPlayer.copy(defaultQualityId = "1080p")
        )
        val report = manager.applyAdminPanelConfigUpdate(v2Candidate)
        assertTrue(report.isValid)
        assertEquals(2, manager.activeConfig.value.configVersion)
        assertFalse(manager.activeConfig.value.homeScreen.showHeroBanner)
        assertEquals("1080p", manager.activeConfig.value.videoPlayer.defaultQualityId)

        // Rollback to v1
        assertTrue(manager.rollbackToPreviousValidConfig())
        assertEquals(1, manager.activeConfig.value.configVersion)
        assertTrue(manager.activeConfig.value.homeScreen.showHeroBanner)
    }

    @Test
    fun stage8_remoteConfigValidatorRejectsArbitraryScriptsAndFlagsNativeReleaseRequirements() {
        val base = com.example.remoteconfig.PublishedRemoteConfig.SAFE_DEFAULT
        val maliciousAndUnbundled = base.copy(
            configVersion = 3,
            branding = base.branding.copy(
                globalAnnouncementBanner = "<script>eval('steal_token()')</script>",
                remoteLogoUrl = "javascript:alert(1)"
            ),
            advertising = base.advertising.copy(
                approvedAdProvider = "UNBUNDLED_THIRD_PARTY_SDK",
                adCooldownSeconds = -50,
                maxAdsPerSession = 999
            ),
            requestedUnbundledSdkOrFeature = "NATIVE_VR_HEADSET_RENDERER"
        )

        val report = com.example.remoteconfig.RemoteConfigValidator.validateAndSanitize(maliciousAndUnbundled)
        assertFalse(report.isValid)
        assertTrue(report.rejectedUnsafeCodeOrScriptPayloads.size >= 2)
        assertEquals("", report.sanitizedConfig.branding.globalAnnouncementBanner)
        assertEquals(base.branding.remoteLogoUrl, report.sanitizedConfig.branding.remoteLogoUrl)
        assertEquals("NONE", report.sanitizedConfig.advertising.approvedAdProvider)
        assertEquals(0, report.sanitizedConfig.advertising.adCooldownSeconds)
        assertEquals(20, report.sanitizedConfig.advertising.maxAdsPerSession)
        assertEquals(2, report.nativeReleaseRequiredNotices.size)
    }

    @Test
    fun stage9_advertisingCoordinator_enforcesConsentFrequencyCooldownDeduplicationAndRevenueSeparation() {
        val env = EnvironmentConfig()
        val remoteConfigManager = com.example.remoteconfig.RemoteConfigManager(env)
        val coordinator = com.example.advertisements.MonetizationCoordinator(env, remoteConfigManager)

        val approvedAdConfig = com.example.remoteconfig.AdminAdvertisingConfig(
            approvedAdProvider = "GOOGLE_ADMOB_APPROVED",
            bannerAdUnitId = "ca-app-pub-test/banner-1",
            interstitialAdUnitId = "ca-app-pub-test/interstitial-1",
            enableBannerPlacements = true,
            enableInterstitialPlacements = true,
            adFrequencyEveryNVideos = 2,
            adCooldownSeconds = 60,
            maxAdsPerSession = 2
        )

        // 1. Block when user consent is declined
        coordinator.updateUserAdConsent(com.example.advertisements.UserAdConsentStatus.NOT_OBTAINED_BLOCKED)
        val noConsentEval = coordinator.evaluateAndRecordAdImpression(
            placement = com.example.advertisements.AdPlacementType.BANNER,
            impressionId = "imp-1",
            currentTimeMs = 10_000L,
            adConfigOverride = approvedAdConfig
        )
        assertEquals(
            com.example.advertisements.AdDecisionOutcome.BLOCKED_NO_USER_CONSENT,
            noConsentEval.outcome
        )
        assertEquals(0, coordinator.adTelemetry.value.recordedAdImpressions)

        // 2. Restore consent and verify interstitial frequency pacing (requires 2 videos watched)
        coordinator.updateUserAdConsent(com.example.advertisements.UserAdConsentStatus.OBTAINED_NON_PERSONALIZED)
        coordinator.onVideoPlaybackStartedForAdPacing() // 1 video watched
        val freqBlocked = coordinator.evaluateAndRecordAdImpression(
            placement = com.example.advertisements.AdPlacementType.INTERSTITIAL,
            impressionId = "imp-freq-early",
            currentTimeMs = 15_000L,
            adConfigOverride = approvedAdConfig
        )
        assertEquals(
            com.example.advertisements.AdDecisionOutcome.BLOCKED_FREQUENCY_INTERVAL_NOT_MET,
            freqBlocked.outcome
        )

        coordinator.onVideoPlaybackStartedForAdPacing() // 2nd video watched -> frequency satisfied
        val validInterstitial = coordinator.evaluateAndRecordAdImpression(
            placement = com.example.advertisements.AdPlacementType.INTERSTITIAL,
            impressionId = "imp-valid-1",
            currentTimeMs = 20_000L,
            adConfigOverride = approvedAdConfig
        )
        assertEquals(
            com.example.advertisements.AdDecisionOutcome.APPROVED_READY_FOR_SDK,
            validInterstitial.outcome
        )
        assertEquals(1, coordinator.adTelemetry.value.recordedAdImpressions)

        // 3. Duplicate impression ID must be blocked
        val duplicateImpression = coordinator.evaluateAndRecordAdImpression(
            placement = com.example.advertisements.AdPlacementType.BANNER,
            impressionId = "imp-valid-1",
            currentTimeMs = 90_000L,
            adConfigOverride = approvedAdConfig
        )
        assertEquals(
            com.example.advertisements.AdDecisionOutcome.BLOCKED_DUPLICATE_IMPRESSION,
            duplicateImpression.outcome
        )
        assertEquals(1, coordinator.adTelemetry.value.duplicateImpressionsBlocked)

        // 4. Cooldown enforcement (within 60s of 20_000L)
        val cooldownBlocked = coordinator.evaluateAndRecordAdImpression(
            placement = com.example.advertisements.AdPlacementType.BANNER,
            impressionId = "imp-cooldown-test",
            currentTimeMs = 50_000L,
            adConfigOverride = approvedAdConfig
        )
        assertEquals(
            com.example.advertisements.AdDecisionOutcome.BLOCKED_COOLDOWN_ACTIVE,
            cooldownBlocked.outcome
        )

        // 5. Record clicks and failed ad requests; verify confirmed server revenue is NEVER client-incremented
        coordinator.recordAdClick()
        val failedFallback = coordinator.handleFailedAdRequest(
            placement = com.example.advertisements.AdPlacementType.BANNER,
            errorReason = "NO_FILL"
        )
        assertEquals(
            com.example.advertisements.AdDecisionOutcome.FAILED_AD_REQUEST_FALLBACK_APPLIED,
            failedFallback.outcome
        )
        assertEquals(1, coordinator.adTelemetry.value.recordedAdClicks)
        assertEquals(1, coordinator.adTelemetry.value.failedAdRequestsHandled)
        assertTrue(coordinator.adTelemetry.value.confirmedServerRevenueDisplay.startsWith("$0.00"))
    }

    @Test
    fun stage9_viewTracking_preservesAttributionBlocksArtificialAndCrossPlatformDuplicates() {
        val env = EnvironmentConfig()
        val coordinator = com.example.advertisements.MonetizationCoordinator(
            env,
            com.example.remoteconfig.RemoteConfigManager(env)
        )

        // 1. Missing uploader attribution is rejected
        val missingUploader = coordinator.reportPlaybackViewEvent(
            com.example.advertisements.PlaybackViewEventRequest(
                eventId = "evt-no-uploader",
                videoId = "sd-vid-101",
                uploaderId = "",
                viewerUid = "viewer-1",
                watchDurationMs = 15_000L,
                sessionId = "sess-1"
            )
        )
        assertEquals(
            com.example.advertisements.ViewValidationOutcome.REJECTED_MISSING_UPLOADER_ATTRIBUTION,
            missingUploader.outcome
        )

        // 2. Artificial short playback (< 5000ms) is rejected
        val botView = coordinator.reportPlaybackViewEvent(
            com.example.advertisements.PlaybackViewEventRequest(
                eventId = "evt-bot-1",
                videoId = "sd-vid-101",
                uploaderId = "usr-aarav-mehta",
                viewerUid = "viewer-1",
                watchDurationMs = 1_500L,
                sessionId = "sess-2"
            )
        )
        assertEquals(
            com.example.advertisements.ViewValidationOutcome.REJECTED_ARTIFICIAL_SHORT_PLAYBACK,
            botView.outcome
        )
        assertEquals(1, coordinator.viewTracking.value.artificialViewsBlockedCount)

        // 3. Legitimate playback increments Recorded Views, while Server Eligible Monetized Views stay server-authoritative (0)
        val legitView = coordinator.reportPlaybackViewEvent(
            com.example.advertisements.PlaybackViewEventRequest(
                eventId = "evt-legit-1",
                videoId = "sd-vid-101",
                uploaderId = "usr-aarav-mehta",
                viewerUid = "viewer-1",
                watchDurationMs = 22_000L,
                sessionId = "sess-shared-web-android"
            )
        )
        assertEquals(
            com.example.advertisements.ViewValidationOutcome.ACCEPTED_RECORDED_VIEW_PENDING_SERVER_ELIGIBILITY,
            legitView.outcome
        )
        assertEquals(1, coordinator.viewTracking.value.clientRecordedViewsSessionCount)
        assertEquals(0, coordinator.viewTracking.value.serverVerifiedEligibleViewsSessionCount)

        // 4. Cross-platform session duplicate (different eventId, same viewer + video + session) is rejected
        val crossPlatformDup = coordinator.reportPlaybackViewEvent(
            com.example.advertisements.PlaybackViewEventRequest(
                eventId = "evt-legit-2-different-id",
                videoId = "sd-vid-101",
                uploaderId = "usr-aarav-mehta",
                viewerUid = "viewer-1",
                watchDurationMs = 30_000L,
                sessionId = "sess-shared-web-android"
            )
        )
        assertEquals(
            com.example.advertisements.ViewValidationOutcome.REJECTED_CROSS_PLATFORM_SESSION_DUPLICATE,
            crossPlatformDup.outcome
        )
        assertEquals(1, coordinator.viewTracking.value.clientRecordedViewsSessionCount)
    }

    @Test
    fun stage9_withdrawals_requireAuthPreventDuplicatesAndNeverMarkPaidByClientAlone() {
        val env = EnvironmentConfig()
        val coordinator = com.example.advertisements.MonetizationCoordinator(
            env,
            com.example.remoteconfig.RemoteConfigManager(env)
        )

        // 1. Block mismatched / unauthenticated user
        val unauthorized = coordinator.submitAuthenticatedWithdrawalRequest(
            authenticatedUid = "uid-intruder",
            requestedCreatorUid = "usr-aarav-mehta",
            amountUsd = 50.0,
            destinationSummary = "UPI • aarav@okaxis",
            idempotencyKey = "wd-key-unauth"
        )
        assertEquals(
            com.example.advertisements.WithdrawalSubmissionStatus.BLOCKED_UNAUTHENTICATED_OR_MISMATCHED_USER,
            unauthorized.status
        )
        assertFalse(unauthorized.isMarkedPaidByClientAlone)

        // 2. Valid authenticated withdrawal is queued awaiting server verification (NEVER marked paid by client alone)
        val validRequest = coordinator.submitAuthenticatedWithdrawalRequest(
            authenticatedUid = "usr-aarav-mehta",
            requestedCreatorUid = "usr-aarav-mehta",
            amountUsd = 50.0,
            destinationSummary = "UPI • aarav@okaxis",
            idempotencyKey = "wd-key-100"
        )
        assertEquals(
            com.example.advertisements.WithdrawalSubmissionStatus.SUBMITTED_AWAITING_BACKEND_VERIFICATION,
            validRequest.status
        )
        assertFalse(validRequest.isMarkedPaidByClientAlone)

        // 3. Duplicate withdrawal submission (same idempotency key or identical pending payout) is blocked
        val duplicateRequest = coordinator.submitAuthenticatedWithdrawalRequest(
            authenticatedUid = "usr-aarav-mehta",
            requestedCreatorUid = "usr-aarav-mehta",
            amountUsd = 50.0,
            destinationSummary = "UPI • aarav@okaxis",
            idempotencyKey = "wd-key-100"
        )
        assertEquals(
            com.example.advertisements.WithdrawalSubmissionStatus.BLOCKED_DUPLICATE_SUBMISSION,
            duplicateRequest.status
        )
        assertFalse(duplicateRequest.isMarkedPaidByClientAlone)
    }

    @Test
    fun stage10_notificationsDeliveryPermissionsCategoryPreferencesAndFcmHonesty_workProperly() {
        val env = EnvironmentConfig()
        val coordinator = com.example.settings.NotificationsAndSettingsCoordinator(
            context = null,
            environmentConfig = env,
            remoteConfigManager = com.example.remoteconfig.RemoteConfigManager(env)
        )

        // Verify all 6 required notification categories are present in the initial feed
        val initialCategories = coordinator.notifications.value.map { it.category }.toSet()
        assertEquals(6, initialCategories.size)

        // 1. Unconfigured FCM push attempts must NEVER invent delivery success
        val unconfiguredFcm = coordinator.dispatchOrEvaluateNotification(
            id = "notif-fcm-unconfigured",
            category = com.example.settings.ShareDabbaNotificationCategory.PLATFORM_ANNOUNCEMENT,
            title = "Remote Push Test",
            message = "Testing unconfigured FCM push",
            targetRoute = "home",
            isUnconfiguredFcmPushAttempt = true
        )
        assertEquals(
            com.example.settings.NotificationDeliveryStatus.NOT_DELIVERED_FCM_BACKEND_UNCONFIGURED,
            unconfiguredFcm.deliveryStatus
        )

        // 2. When Android notification permission is denied, delivery is strictly IN-APP ONLY
        coordinator.updateNotificationPermissionGranted(false)
        val inAppOnly = coordinator.dispatchOrEvaluateNotification(
            id = "notif-upload-inapp",
            category = com.example.settings.ShareDabbaNotificationCategory.UPLOAD_COMPLETION,
            title = "Upload Complete",
            message = "Transcoding finished",
            targetRoute = "uploads"
        )
        assertEquals(
            com.example.settings.NotificationDeliveryStatus.DELIVERED_IN_APP_ONLY_PERMISSION_DENIED,
            inAppOnly.deliveryStatus
        )

        // 3. Granting Android notification permission allows local OS notification tray delivery
        coordinator.updateNotificationPermissionGranted(true)
        val trayDelivered = coordinator.dispatchOrEvaluateNotification(
            id = "notif-upload-tray",
            category = com.example.settings.ShareDabbaNotificationCategory.UPLOAD_COMPLETION,
            title = "Upload Ready",
            message = "Ready to watch",
            targetRoute = "uploads"
        )
        assertEquals(
            com.example.settings.NotificationDeliveryStatus.DELIVERED_IN_APP_AND_SYSTEM_TRAY,
            trayDelivered.deliveryStatus
        )

        // 4. Optional categories can be disabled by the user; required maintenance notices cannot be disabled
        assertTrue(
            coordinator.setCategoryEnabled(
                com.example.settings.ShareDabbaNotificationCategory.WITHDRAWAL_STATUS,
                false
            )
        )
        val suppressedWithdrawal = coordinator.dispatchOrEvaluateNotification(
            id = "notif-wd-suppressed",
            category = com.example.settings.ShareDabbaNotificationCategory.WITHDRAWAL_STATUS,
            title = "Withdrawal Update",
            message = "Queued",
            targetRoute = "withdrawal_history"
        )
        assertEquals(
            com.example.settings.NotificationDeliveryStatus.SUPPRESSED_BY_USER_CATEGORY_PREFERENCE,
            suppressedWithdrawal.deliveryStatus
        )

        assertFalse(
            coordinator.setCategoryEnabled(
                com.example.settings.ShareDabbaNotificationCategory.MAINTENANCE_NOTICE,
                false
            )
        )
    }

    @Test
    fun stage10_notificationDeepLinksAndExpiredSessions_enforceExactRoutingAndSessionGuard() {
        val env = EnvironmentConfig()
        val coordinator = com.example.settings.NotificationsAndSettingsCoordinator(
            context = null,
            environmentConfig = env,
            remoteConfigManager = com.example.remoteconfig.RemoteConfigManager(env)
        )

        // 1. Expired session blocks private notification delivery
        val expiredSessionDelivery = coordinator.dispatchOrEvaluateNotification(
            id = "notif-expired-1",
            category = com.example.settings.ShareDabbaNotificationCategory.ACCOUNT_SECURITY,
            title = "Account Security Alert",
            message = "Sensitive account alert",
            targetRoute = "profile",
            isSessionExpired = true
        )
        assertEquals(
            com.example.settings.NotificationDeliveryStatus.BLOCKED_EXPIRED_SESSION_REAUTH_REQUIRED,
            expiredSessionDelivery.deliveryStatus
        )

        // 2. Tapping a notification with a canonical deep link opens the exact video in the native player when session is active
        val uploadRecord = coordinator.notifications.value.first {
            it.category == com.example.settings.ShareDabbaNotificationCategory.UPLOAD_COMPLETION
        }
        val activeDeepLinkNav = coordinator.resolveNotificationTapDestination(
            record = uploadRecord,
            isSessionExpired = false
        )
        assertEquals(
            com.example.settings.NotificationNavigationOutcome.OPEN_NATIVE_VIDEO_PLAYER_DEEP_LINK,
            activeDeepLinkNav.outcome
        )
        assertEquals("vid_featured_1", activeDeepLinkNav.resolvedVideoIdOrStreamUrl)

        // 3. Tapping a private notification when the session is expired blocks navigation and redirects to Profile recovery
        val expiredNav = coordinator.resolveNotificationTapDestination(
            record = uploadRecord,
            isSessionExpired = true
        )
        assertEquals(
            com.example.settings.NotificationNavigationOutcome.BLOCKED_EXPIRED_SESSION_REAUTH_REQUIRED,
            expiredNav.outcome
        )
        assertEquals("profile", expiredNav.resolvedRoute)
    }

    @Test
    fun stage11_securityAudit_blocksSensitiveLogLeaksCleartextApiAndAdminEscalation() {
        // 1. Verify AppLogger redacts Bearer tokens, JWTs, passwords, and payment secrets
        val rawLog = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOiJ1MSJ9.sig99 password=SuperSecret123 payout_secret:sk_live_abc"
        val redacted = com.example.core.logging.AppLogger.redactSensitiveData(rawLog)
        assertFalse(redacted.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"))
        assertFalse(redacted.contains("SuperSecret123"))
        assertFalse(redacted.contains("sk_live_abc"))
        assertTrue(redacted.contains("[REDACTED_TOKEN]"))
        assertTrue(redacted.contains("[REDACTED_SECRET]"))

        // 2. Verify AuthHeaderInterceptor blocks cleartext HTTP API calls
        val cleartextRequest = Request.Builder()
            .url("http://api.sharedabba.com/v1/videos")
            .build()
        val cleartextBlocked = try {
            AuthHeaderInterceptor.validateRequestSecurity(cleartextRequest)
            false
        } catch (_: SecurityException) {
            true
        }
        assertTrue(cleartextBlocked)

        // 3. Verify AuthHeaderInterceptor blocks admin privilege escalation paths
        val adminWriteRequest = Request.Builder()
            .url("https://api.sharedabba.com/admin/publish")
            .build()
        val adminEscalationBlocked = try {
            AuthHeaderInterceptor.validateRequestSecurity(adminWriteRequest)
            false
        } catch (_: SecurityException) {
            true
        }
        assertTrue(adminEscalationBlocked)
    }

    @Test
    fun stage11_completeQaAndSecurityAuditReport_coversAll25TestsAndHonestNotConfiguredStatus() {
        val report = com.example.settings.QaSecurityAuditRunner.runCompleteAudit()

        assertEquals(25, report.functionalTests.size)
        assertEquals(9, report.securityChecks.size)
        assertEquals(7, report.performanceChecks.size)
        assertEquals(5, report.compatibilityChecks.size)
        assertEquals(0, report.failCount)

        // All 9 security checks, 7 performance checks, and 5 compatibility checks must PASS
        assertTrue(report.securityChecks.all { it.status == com.example.settings.AuditVerdict.PASS })
        assertTrue(report.performanceChecks.all { it.status == com.example.settings.AuditVerdict.PASS })
        assertTrue(report.compatibilityChecks.all { it.status == com.example.settings.AuditVerdict.PASS })

        // Unconfigured backend endpoints & domain-hosted assetlinks.json are honestly reported as NOT_CONFIGURED
        assertEquals(5, report.notConfiguredCount)
        assertFalse(report.isProductionReadyClaimAllowed)
    }
}



