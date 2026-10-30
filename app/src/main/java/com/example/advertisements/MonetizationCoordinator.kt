package com.example.advertisements

import com.example.authentication.UserAccessGuard
import com.example.core.config.EnvironmentConfig
import com.example.data.fixtures.DevFixturesCatalog
import com.example.data.fixtures.EarningsRecordFixture
import com.example.data.fixtures.WithdrawalRecordFixture
import com.example.remoteconfig.AdminAdvertisingConfig
import com.example.remoteconfig.RemoteConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * STAGE 9 — SHARE DABBA ADVERTISING AND MONETIZATION INTEGRATION
 *
 * Connects Android advertising, view tracking, creator earnings, and withdrawal requests
 * to the authoritative ShareDabba website backend without creating separate client-side
 * earnings calculations or a separate wallet.
 *
 * Core Guarantees:
 * 1. All financial calculations, eligible view qualification, and payout authorizations
 *    remain strictly server-side.
 * 2. Missing advertising SDK providers or unconfigured backend payout endpoints are
 *    reported transparently instead of simulating fake revenue or successful payments.
 * 3. Client-side ad impressions, ad clicks, and confirmed server revenue are tracked as
 *    strictly distinct metrics; client ad callbacks never increment confirmed revenue.
 * 4. Playback view tracking preserves original uploader attribution, deduplicates events,
 *    blocks artificial sub-threshold views, prevents cross-platform double counting, and
 *    separates Recorded Views from Server-Verified Eligible Monetized Views.
 * 5. Withdrawal submissions require authenticated identity matching (`UserAccessGuard`),
 *    enforce idempotency/duplicate submission guards, and NEVER mark a payment as
 *    successful/paid based on a client response alone.
 */

enum class AdPlacementType(val label: String) {
    BANNER("Banner Placement"),
    INTERSTITIAL("Pre-Roll / Interstitial Placement"),
    REWARDED("Opt-In Rewarded Placement")
}

enum class UserAdConsentStatus(val label: String, val canRequestAds: Boolean) {
    OBTAINED_PERSONALIZED("Consent Obtained (Personalized Ads Permitted)", true),
    OBTAINED_NON_PERSONALIZED("Consent Obtained (Non-Personalized Only)", true),
    NOT_OBTAINED_BLOCKED("Consent Not Obtained / Declined (Ads Blocked)", false)
}

enum class AdDecisionOutcome {
    APPROVED_READY_FOR_SDK,
    BLOCKED_NO_USER_CONSENT,
    BLOCKED_PROVIDER_UNCONFIGURED,
    BLOCKED_PLACEMENT_DISABLED,
    BLOCKED_SESSION_CAP_REACHED,
    BLOCKED_COOLDOWN_ACTIVE,
    BLOCKED_FREQUENCY_INTERVAL_NOT_MET,
    BLOCKED_DUPLICATE_IMPRESSION,
    FAILED_AD_REQUEST_FALLBACK_APPLIED
}

data class AdEvaluationResult(
    val outcome: AdDecisionOutcome,
    val placement: AdPlacementType,
    val providerName: String,
    val adUnitId: String,
    val fallbackApplied: String,
    val explanation: String
)

/**
 * Strictly separates Ad Impressions, Ad Clicks, and Confirmed Server Revenue.
 * Client events NEVER increment `confirmedServerRevenueUsd`.
 */
data class AdTelemetrySnapshot(
    val consentStatus: UserAdConsentStatus = UserAdConsentStatus.OBTAINED_NON_PERSONALIZED,
    val recordedAdImpressions: Int = 0,
    val duplicateImpressionsBlocked: Int = 0,
    val recordedAdClicks: Int = 0,
    val failedAdRequestsHandled: Int = 0,
    val videosWatchedSinceLastAd: Int = 0,
    val lastAdShownEpochMs: Long = 0L,
    val confirmedServerRevenueDisplay: String = "$0.00 (Server-Authoritative Only — Never Client-Calculated)",
    val lastEvaluation: AdEvaluationResult? = null,
    val missingSdkOrConfigReport: List<String> = listOf(
        "Approved Ad Provider in Remote Config is NONE_CONFIGURED (awaiting website Admin Panel ad unit publication).",
        "No fake or unapproved third-party ad SDK is bundled; failed or unconfigured ad slots fall back to NO_AD_CONTINUE_CONTENT."
    )
)

enum class ViewValidationOutcome {
    ACCEPTED_RECORDED_VIEW_PENDING_SERVER_ELIGIBILITY,
    REJECTED_MISSING_UPLOADER_ATTRIBUTION,
    REJECTED_ARTIFICIAL_SHORT_PLAYBACK,
    REJECTED_DUPLICATE_EVENT_ID,
    REJECTED_CROSS_PLATFORM_SESSION_DUPLICATE
}

data class PlaybackViewEventRequest(
    val eventId: String,
    val videoId: String,
    val uploaderId: String,
    val viewerUid: String,
    val watchDurationMs: Long,
    val sessionId: String,
    val clientPlatform: String = "ANDROID"
) {
    val crossPlatformDedupeKey: String
        get() = "${viewerUid.trim()}:${videoId.trim()}:${sessionId.trim()}"
}

data class ViewTrackingResult(
    val outcome: ViewValidationOutcome,
    val eventId: String,
    val videoId: String,
    val uploaderId: String,
    val clientRecordedViewsCount: Int,
    val serverEligibleMonetizedViewsCount: Int,
    val explanation: String
)

data class ViewTrackingSnapshot(
    val clientRecordedViewsSessionCount: Int = 0,
    val serverVerifiedEligibleViewsSessionCount: Int = 0,
    val duplicateViewsBlockedCount: Int = 0,
    val artificialViewsBlockedCount: Int = 0,
    val trackedEvents: List<ViewTrackingResult> = emptyList()
)

enum class WithdrawalSubmissionStatus(val badgeLabel: String) {
    SUBMITTED_AWAITING_BACKEND_VERIFICATION("AWAITING SERVER VERIFICATION"),
    BLOCKED_DUPLICATE_SUBMISSION("BLOCKED: DUPLICATE REQUEST"),
    BLOCKED_UNAUTHENTICATED_OR_MISMATCHED_USER("BLOCKED: AUTH REQUIRED"),
    BLOCKED_INVALID_AMOUNT_OR_DESTINATION("BLOCKED: INVALID INPUT")
}

data class WithdrawalSubmissionRecord(
    val requestId: String,
    val idempotencyKey: String,
    val creatorUid: String,
    val amountDisplay: String,
    val destinationSummary: String,
    val status: WithdrawalSubmissionStatus,
    val isMarkedPaidByClientAlone: Boolean = false,
    val serverStatusMessage: String,
    val timestampMs: Long
)

data class CreatorMonetizationLedgerSnapshot(
    val isLiveBackendConnected: Boolean,
    val pendingEarningsDisplay: String,
    val confirmedEarningsDisplay: String,
    val availableBalanceDisplay: String,
    val recordedViewsTotal: Long,
    val eligibleMonetizedViewsTotal: Long,
    val earningsBreakdown: List<EarningsRecordFixture>,
    val withdrawalHistory: List<WithdrawalRecordFixture>,
    val sessionSubmittedWithdrawals: List<WithdrawalSubmissionRecord>,
    val missingBackendIntegrationReport: List<String>
)

/**
 * Coordinates Stage 9 Advertising, Legitimate View Tracking, Server-Authoritative Earnings,
 * and Authenticated Withdrawal Submissions.
 */
class MonetizationCoordinator(
    private val environmentConfig: EnvironmentConfig,
    private val remoteConfigManager: RemoteConfigManager
) {
    companion object {
        const val MIN_LEGITIMATE_WATCH_DURATION_MS = 5_000L
    }

    private val recordedImpressionIds = mutableSetOf<String>()
    private val recordedViewEventIds = mutableSetOf<String>()
    private val recordedCrossPlatformViewKeys = mutableSetOf<String>()
    private val submittedWithdrawalIdempotencyKeys = mutableSetOf<String>()
    private val inFlightWithdrawalSignatures = mutableSetOf<String>()

    private val _adTelemetry = MutableStateFlow(AdTelemetrySnapshot())
    val adTelemetry: StateFlow<AdTelemetrySnapshot> = _adTelemetry.asStateFlow()

    private val _viewTracking = MutableStateFlow(ViewTrackingSnapshot())
    val viewTracking: StateFlow<ViewTrackingSnapshot> = _viewTracking.asStateFlow()

    private val _ledgerSnapshot = MutableStateFlow(buildInitialLedgerSnapshot())
    val ledgerSnapshot: StateFlow<CreatorMonetizationLedgerSnapshot> = _ledgerSnapshot.asStateFlow()

    private fun buildInitialLedgerSnapshot(): CreatorMonetizationLedgerSnapshot {
        val metrics = DevFixturesCatalog.dashboardMetrics
        return CreatorMonetizationLedgerSnapshot(
            isLiveBackendConnected = false,
            pendingEarningsDisplay = metrics.pendingBalanceDisplay,
            confirmedEarningsDisplay = metrics.earningsDisplay,
            availableBalanceDisplay = metrics.availableBalanceDisplay,
            recordedViewsTotal = metrics.recordedViews,
            eligibleMonetizedViewsTotal = metrics.eligibleViews,
            earningsBreakdown = DevFixturesCatalog.earningsBreakdown,
            withdrawalHistory = DevFixturesCatalog.withdrawalHistory,
            sessionSubmittedWithdrawals = emptyList(),
            missingBackendIntegrationReport = listOf(
                "GET ${environmentConfig.apiBaseUrl}creator/earnings — Awaiting ShareDabba website repository connection for live confirmed & pending earnings.",
                "POST ${environmentConfig.apiBaseUrl}videos/{id}/playback-events — Awaiting website server endpoint to qualify recorded views into eligible monetized views.",
                "GET/POST ${environmentConfig.apiBaseUrl}wallet/withdrawals — Awaiting website payout provider integration. Client never simulates successful payouts."
            )
        )
    }

    /**
     * Updates user ad consent state (GDPR / UMP compliance).
     */
    fun updateUserAdConsent(consentStatus: UserAdConsentStatus) {
        _adTelemetry.value = _adTelemetry.value.copy(consentStatus = consentStatus)
    }

    /**
     * Increments the counter of videos watched in the current session so frequency rules
     * (`adFrequencyEveryNVideos`) can be evaluated accurately.
     */
    fun onVideoPlaybackStartedForAdPacing() {
        _adTelemetry.value = _adTelemetry.value.copy(
            videosWatchedSinceLastAd = _adTelemetry.value.videosWatchedSinceLastAd + 1
        )
    }

    /**
     * Evaluates whether an ad placement can be requested and rendered according to:
     * 1. User consent requirements
     * 2. Remote configuration (`AdminAdvertisingConfig`)
     * 3. Placement enablement (`bannerEnabled`, `interstitialEnabled`, `rewardedAdsPermitted`)
     * 4. Session maximum cap (`maxAdsPerSession`)
     * 5. Cooldown timer (`adCooldownSeconds`)
     * 6. Video interval frequency (`adFrequencyEveryNVideos`)
     * 7. Impression deduplication (`impressionId`)
     */
    fun evaluateAndRecordAdImpression(
        placement: AdPlacementType,
        impressionId: String,
        currentTimeMs: Long = System.currentTimeMillis(),
        adConfigOverride: AdminAdvertisingConfig? = null
    ): AdEvaluationResult {
        val adConfig = adConfigOverride ?: remoteConfigManager.activeConfig.value.advertising
        val currentTelemetry = _adTelemetry.value

        // 1. Respect user consent requirements
        if (!currentTelemetry.consentStatus.canRequestAds) {
            val result = AdEvaluationResult(
                outcome = AdDecisionOutcome.BLOCKED_NO_USER_CONSENT,
                placement = placement,
                providerName = adConfig.approvedAdProvider,
                adUnitId = "",
                fallbackApplied = adConfig.adFallbackStrategy,
                explanation = "Ad request blocked because user ad consent is ${currentTelemetry.consentStatus.name}."
            )
            _adTelemetry.value = currentTelemetry.copy(lastEvaluation = result)
            return result
        }

        // 2. Prevent repeated/duplicate ad impressions for the same placement event
        val normalizedId = impressionId.trim()
        if (normalizedId.isNotEmpty() && !recordedImpressionIds.add(normalizedId)) {
            val result = AdEvaluationResult(
                outcome = AdDecisionOutcome.BLOCKED_DUPLICATE_IMPRESSION,
                placement = placement,
                providerName = adConfig.approvedAdProvider,
                adUnitId = resolveAdUnitId(adConfig, placement),
                fallbackApplied = adConfig.adFallbackStrategy,
                explanation = "Duplicate ad impression '$normalizedId' blocked to prevent repeated impression inflation."
            )
            _adTelemetry.value = currentTelemetry.copy(
                duplicateImpressionsBlocked = currentTelemetry.duplicateImpressionsBlocked + 1,
                lastEvaluation = result
            )
            return result
        }

        // 3. Check placement enablement in remote configuration
        val placementEnabled = when (placement) {
            AdPlacementType.BANNER -> adConfig.enableBannerPlacements
            AdPlacementType.INTERSTITIAL -> adConfig.enableInterstitialPlacements
            AdPlacementType.REWARDED -> adConfig.enableRewardedPlacements
        }
        if (!placementEnabled) {
            val result = AdEvaluationResult(
                outcome = AdDecisionOutcome.BLOCKED_PLACEMENT_DISABLED,
                placement = placement,
                providerName = adConfig.approvedAdProvider,
                adUnitId = resolveAdUnitId(adConfig, placement),
                fallbackApplied = adConfig.adFallbackStrategy,
                explanation = "${placement.label} is disabled by the website Admin Panel remote configuration."
            )
            _adTelemetry.value = currentTelemetry.copy(lastEvaluation = result)
            return result
        }

        // 4. Enforce maximum ads per session limit
        if (currentTelemetry.recordedAdImpressions >= adConfig.maxAdsPerSession) {
            val result = AdEvaluationResult(
                outcome = AdDecisionOutcome.BLOCKED_SESSION_CAP_REACHED,
                placement = placement,
                providerName = adConfig.approvedAdProvider,
                adUnitId = resolveAdUnitId(adConfig, placement),
                fallbackApplied = adConfig.adFallbackStrategy,
                explanation = "Session ad cap (${adConfig.maxAdsPerSession} max ads/session) reached. Continuing content without ad."
            )
            _adTelemetry.value = currentTelemetry.copy(lastEvaluation = result)
            return result
        }

        // 5. Enforce cooldown between ads
        if (currentTelemetry.lastAdShownEpochMs > 0L) {
            val elapsedSeconds = (currentTimeMs - currentTelemetry.lastAdShownEpochMs) / 1000L
            if (elapsedSeconds < adConfig.adCooldownSeconds) {
                val remaining = adConfig.adCooldownSeconds - elapsedSeconds
                val result = AdEvaluationResult(
                    outcome = AdDecisionOutcome.BLOCKED_COOLDOWN_ACTIVE,
                    placement = placement,
                    providerName = adConfig.approvedAdProvider,
                    adUnitId = resolveAdUnitId(adConfig, placement),
                    fallbackApplied = adConfig.adFallbackStrategy,
                    explanation = "Ad cooldown active (${remaining}s remaining of ${adConfig.adCooldownSeconds}s cooldown)."
                )
                _adTelemetry.value = currentTelemetry.copy(lastEvaluation = result)
                return result
            }
        }

        // 6. Enforce video interval frequency for interstitials
        if (placement == AdPlacementType.INTERSTITIAL &&
            currentTelemetry.videosWatchedSinceLastAd < adConfig.adFrequencyEveryNVideos
        ) {
            val result = AdEvaluationResult(
                outcome = AdDecisionOutcome.BLOCKED_FREQUENCY_INTERVAL_NOT_MET,
                placement = placement,
                providerName = adConfig.approvedAdProvider,
                adUnitId = resolveAdUnitId(adConfig, placement),
                fallbackApplied = adConfig.adFallbackStrategy,
                explanation = "Frequency limit requires ${adConfig.adFrequencyEveryNVideos} videos between interstitials (currently ${currentTelemetry.videosWatchedSinceLastAd})."
            )
            _adTelemetry.value = currentTelemetry.copy(lastEvaluation = result)
            return result
        }

        // 7. Check whether an approved ad provider & unit ID are configured by the website backend
        val adUnitId = resolveAdUnitId(adConfig, placement)
        if (adConfig.approvedAdProvider == "NONE" ||
            adConfig.approvedAdProvider == "NONE_CONFIGURED" ||
            adUnitId.isBlank()
        ) {
            val result = AdEvaluationResult(
                outcome = AdDecisionOutcome.BLOCKED_PROVIDER_UNCONFIGURED,
                placement = placement,
                providerName = adConfig.approvedAdProvider,
                adUnitId = adUnitId,
                fallbackApplied = adConfig.adFallbackStrategy,
                explanation = "No approved Android ad provider or ad unit ID is configured on the website backend. Applying fallback '${adConfig.adFallbackStrategy}' without fake ads."
            )
            _adTelemetry.value = currentTelemetry.copy(lastEvaluation = result)
            return result
        }

        // Approved and valid impression recorded — note that confirmedServerRevenueDisplay is NOT changed!
        val result = AdEvaluationResult(
            outcome = AdDecisionOutcome.APPROVED_READY_FOR_SDK,
            placement = placement,
            providerName = adConfig.approvedAdProvider,
            adUnitId = adUnitId,
            fallbackApplied = "NONE_REQUIRED",
            explanation = "Impression '$normalizedId' recorded for ${adConfig.approvedAdProvider} ($adUnitId). Confirmed revenue awaits backend ad-network settlement."
        )
        _adTelemetry.value = currentTelemetry.copy(
            recordedAdImpressions = currentTelemetry.recordedAdImpressions + 1,
            videosWatchedSinceLastAd = 0,
            lastAdShownEpochMs = currentTimeMs,
            lastEvaluation = result
        )
        return result
    }

    /**
     * Records an ad click separately from ad impressions and confirmed server revenue.
     */
    fun recordAdClick() {
        val current = _adTelemetry.value
        _adTelemetry.value = current.copy(
            recordedAdClicks = current.recordedAdClicks + 1
        )
    }

    /**
     * Handles a failed ad request from the ad network by recording the failure and
     * applying the configured fallback (`NO_AD_CONTINUE_CONTENT`) so video playback is never blocked.
     */
    fun handleFailedAdRequest(placement: AdPlacementType, errorReason: String): AdEvaluationResult {
        val adConfig = remoteConfigManager.activeConfig.value.advertising
        val current = _adTelemetry.value
        val result = AdEvaluationResult(
            outcome = AdDecisionOutcome.FAILED_AD_REQUEST_FALLBACK_APPLIED,
            placement = placement,
            providerName = adConfig.approvedAdProvider,
            adUnitId = resolveAdUnitId(adConfig, placement),
            fallbackApplied = adConfig.adFallbackStrategy,
            explanation = "Ad request failed ($errorReason). Applied fallback '${adConfig.adFallbackStrategy}' so playback continues seamlessly."
        )
        _adTelemetry.value = current.copy(
            failedAdRequestsHandled = current.failedAdRequestsHandled + 1,
            lastEvaluation = result
        )
        return result
    }

    private fun resolveAdUnitId(config: AdminAdvertisingConfig, placement: AdPlacementType): String {
        return when (placement) {
            AdPlacementType.BANNER -> config.bannerAdUnitId
            AdPlacementType.INTERSTITIAL -> config.interstitialAdUnitId
            AdPlacementType.REWARDED -> config.rewardedAdUnitId
        }
    }

    /**
     * Reports a playback view event for server-side validation while:
     * - Preserving uploader attribution (`videoId` + `uploaderId`)
     * - Rejecting artificial/bot views below `MIN_LEGITIMATE_WATCH_DURATION_MS`
     * - Deduplicating repeated `eventId`s
     * - Preventing cross-platform double counting via `viewerUid:videoId:sessionId`
     * - Strictly distinguishing client-recorded views from server-verified eligible monetized views
     */
    fun reportPlaybackViewEvent(request: PlaybackViewEventRequest): ViewTrackingResult {
        val current = _viewTracking.value

        // 1. Preserve uploader attribution
        if (request.videoId.isBlank() || request.uploaderId.isBlank()) {
            val result = ViewTrackingResult(
                outcome = ViewValidationOutcome.REJECTED_MISSING_UPLOADER_ATTRIBUTION,
                eventId = request.eventId,
                videoId = request.videoId,
                uploaderId = request.uploaderId,
                clientRecordedViewsCount = current.clientRecordedViewsSessionCount,
                serverEligibleMonetizedViewsCount = current.serverVerifiedEligibleViewsSessionCount,
                explanation = "Rejected playback event: original videoId and uploaderId attribution are mandatory."
            )
            _viewTracking.value = current.copy(trackedEvents = listOf(result) + current.trackedEvents.take(9))
            return result
        }

        // 2. Prevent artificial / rapid-refresh views
        if (request.watchDurationMs < MIN_LEGITIMATE_WATCH_DURATION_MS) {
            val result = ViewTrackingResult(
                outcome = ViewValidationOutcome.REJECTED_ARTIFICIAL_SHORT_PLAYBACK,
                eventId = request.eventId,
                videoId = request.videoId,
                uploaderId = request.uploaderId,
                clientRecordedViewsCount = current.clientRecordedViewsSessionCount,
                serverEligibleMonetizedViewsCount = current.serverVerifiedEligibleViewsSessionCount,
                explanation = "Rejected artificial/short playback (${request.watchDurationMs}ms < ${MIN_LEGITIMATE_WATCH_DURATION_MS}ms threshold)."
            )
            _viewTracking.value = current.copy(
                artificialViewsBlockedCount = current.artificialViewsBlockedCount + 1,
                trackedEvents = listOf(result) + current.trackedEvents.take(9)
            )
            return result
        }

        // 3. Deduplicate duplicate event IDs
        val normalizedEventId = request.eventId.trim()
        if (normalizedEventId.isEmpty() || !recordedViewEventIds.add(normalizedEventId)) {
            val result = ViewTrackingResult(
                outcome = ViewValidationOutcome.REJECTED_DUPLICATE_EVENT_ID,
                eventId = request.eventId,
                videoId = request.videoId,
                uploaderId = request.uploaderId,
                clientRecordedViewsCount = current.clientRecordedViewsSessionCount,
                serverEligibleMonetizedViewsCount = current.serverVerifiedEligibleViewsSessionCount,
                explanation = "Duplicate playback event '$normalizedEventId' ignored."
            )
            _viewTracking.value = current.copy(
                duplicateViewsBlockedCount = current.duplicateViewsBlockedCount + 1,
                trackedEvents = listOf(result) + current.trackedEvents.take(9)
            )
            return result
        }

        // 4. Prevent cross-platform double counting (same viewer + video + session)
        val crossPlatformKey = request.crossPlatformDedupeKey
        if (!recordedCrossPlatformViewKeys.add(crossPlatformKey)) {
            val result = ViewTrackingResult(
                outcome = ViewValidationOutcome.REJECTED_CROSS_PLATFORM_SESSION_DUPLICATE,
                eventId = request.eventId,
                videoId = request.videoId,
                uploaderId = request.uploaderId,
                clientRecordedViewsCount = current.clientRecordedViewsSessionCount,
                serverEligibleMonetizedViewsCount = current.serverVerifiedEligibleViewsSessionCount,
                explanation = "Cross-platform session key '$crossPlatformKey' already counted; prevented Web/Android double counting."
            )
            _viewTracking.value = current.copy(
                duplicateViewsBlockedCount = current.duplicateViewsBlockedCount + 1,
                trackedEvents = listOf(result) + current.trackedEvents.take(9)
            )
            return result
        }

        // 5. Record legitimate client playback view, while keeping server-verified eligible views strictly server-controlled
        val newRecordedCount = current.clientRecordedViewsSessionCount + 1
        val result = ViewTrackingResult(
            outcome = ViewValidationOutcome.ACCEPTED_RECORDED_VIEW_PENDING_SERVER_ELIGIBILITY,
            eventId = normalizedEventId,
            videoId = request.videoId,
            uploaderId = request.uploaderId,
            clientRecordedViewsCount = newRecordedCount,
            serverEligibleMonetizedViewsCount = current.serverVerifiedEligibleViewsSessionCount,
            explanation = "Recorded legitimate ANDROID view for video '${request.videoId}' (uploader '${request.uploaderId}'). Eligible monetized view count remains ${current.serverVerifiedEligibleViewsSessionCount} until website backend validation."
        )
        _viewTracking.value = current.copy(
            clientRecordedViewsSessionCount = newRecordedCount,
            trackedEvents = listOf(result) + current.trackedEvents.take(9)
        )
        return result
    }

    /**
     * Submits an authenticated withdrawal request to the website backend contract.
     *
     * Rules enforced:
     * - Requires authenticated user matching `requestedCreatorUid` via `UserAccessGuard`.
     * - Rejects blank/invalid amount or payout destination.
     * - Prevents duplicate withdrawal submissions via `idempotencyKey` and signature lock.
     * - NEVER marks a payment as paid/successful based on a client response alone
     *   (`isMarkedPaidByClientAlone = false`, status = `SUBMITTED_AWAITING_BACKEND_VERIFICATION`).
     */
    fun submitAuthenticatedWithdrawalRequest(
        authenticatedUid: String?,
        requestedCreatorUid: String,
        amountUsd: Double,
        destinationSummary: String,
        idempotencyKey: String,
        timestampMs: Long = System.currentTimeMillis()
    ): WithdrawalSubmissionRecord {
        // 1. Verify authentication and user ownership
        val hasAccess = UserAccessGuard.isAuthorizedForUser(
            authenticatedUid,
            requestedCreatorUid
        )
        if (!hasAccess) {
            return WithdrawalSubmissionRecord(
                requestId = "wd-blocked-${timestampMs % 100000}",
                idempotencyKey = idempotencyKey,
                creatorUid = requestedCreatorUid,
                amountDisplay = "$%.2f".format(amountUsd),
                destinationSummary = destinationSummary,
                status = WithdrawalSubmissionStatus.BLOCKED_UNAUTHENTICATED_OR_MISMATCHED_USER,
                isMarkedPaidByClientAlone = false,
                serverStatusMessage = "Blocked: Authenticated user must match creator wallet owner ($requestedCreatorUid).",
                timestampMs = timestampMs
            )
        }

        // 2. Validate amount & destination
        val cleanDestination = destinationSummary.trim()
        val cleanKey = idempotencyKey.trim()
        if (amountUsd <= 0.0 || cleanDestination.isEmpty() || cleanKey.isEmpty()) {
            return WithdrawalSubmissionRecord(
                requestId = "wd-invalid-${timestampMs % 100000}",
                idempotencyKey = cleanKey,
                creatorUid = requestedCreatorUid,
                amountDisplay = "$%.2f".format(amountUsd),
                destinationSummary = cleanDestination,
                status = WithdrawalSubmissionStatus.BLOCKED_INVALID_AMOUNT_OR_DESTINATION,
                isMarkedPaidByClientAlone = false,
                serverStatusMessage = "Blocked: Withdrawal amount must be positive and payout destination + idempotency key are required.",
                timestampMs = timestampMs
            )
        }

        // 3. Prevent duplicate submissions (by idempotency key or identical pending creator+amount+destination signature)
        val requestSignature = "$requestedCreatorUid:${"%.2f".format(amountUsd)}:$cleanDestination"
        if (!submittedWithdrawalIdempotencyKeys.add(cleanKey) || !inFlightWithdrawalSignatures.add(requestSignature)) {
            val duplicateRecord = WithdrawalSubmissionRecord(
                requestId = "wd-dup-${timestampMs % 100000}",
                idempotencyKey = cleanKey,
                creatorUid = requestedCreatorUid,
                amountDisplay = "$%.2f".format(amountUsd),
                destinationSummary = cleanDestination,
                status = WithdrawalSubmissionStatus.BLOCKED_DUPLICATE_SUBMISSION,
                isMarkedPaidByClientAlone = false,
                serverStatusMessage = "Duplicate withdrawal prevented: idempotency key '$cleanKey' or identical pending payout is already queued.",
                timestampMs = timestampMs
            )
            _ledgerSnapshot.value = _ledgerSnapshot.value.copy(
                sessionSubmittedWithdrawals = listOf(duplicateRecord) + _ledgerSnapshot.value.sessionSubmittedWithdrawals
            )
            return duplicateRecord
        }

        // 4. Queue withdrawal for server verification — NEVER mark as paid on the client!
        val record = WithdrawalSubmissionRecord(
            requestId = "wd-req-${timestampMs % 100000}",
            idempotencyKey = cleanKey,
            creatorUid = requestedCreatorUid,
            amountDisplay = "$%.2f".format(amountUsd),
            destinationSummary = cleanDestination,
            status = WithdrawalSubmissionStatus.SUBMITTED_AWAITING_BACKEND_VERIFICATION,
            isMarkedPaidByClientAlone = false,
            serverStatusMessage = "Submitted with idempotencyKey='$cleanKey'. Status is AWAITING SERVER VERIFICATION (POST ${environmentConfig.apiBaseUrl}wallet/withdrawals). Payment is NEVER marked paid by client response alone.",
            timestampMs = timestampMs
        )
        _ledgerSnapshot.value = _ledgerSnapshot.value.copy(
            sessionSubmittedWithdrawals = listOf(record) + _ledgerSnapshot.value.sessionSubmittedWithdrawals
        )
        return record
    }
}
