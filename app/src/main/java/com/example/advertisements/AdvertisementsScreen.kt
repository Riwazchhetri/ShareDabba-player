package com.example.advertisements

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.config.EnvironmentConfig
import com.example.remoteconfig.AdminAdvertisingConfig
import com.example.remoteconfig.RemoteConfigManager
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.common.ShareDabbaSectionCard
import com.example.ui.common.ShareDabbaTopBar
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaPurple
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWhite

@Composable
fun AdvertisementsScreen(
    monetizationCoordinator: MonetizationCoordinator? = null,
    remoteConfigManager: RemoteConfigManager? = null,
    onBackClick: () -> Unit = {}
) {
    BackHandler(onBack = onBackClick)

    val fallbackEnv = remember { EnvironmentConfig() }
    val effectiveRemoteConfigManager = remember(remoteConfigManager) {
        remoteConfigManager ?: RemoteConfigManager(fallbackEnv)
    }
    val coordinator = remember(monetizationCoordinator) {
        monetizationCoordinator ?: MonetizationCoordinator(fallbackEnv, effectiveRemoteConfigManager)
    }

    val remoteConfig by effectiveRemoteConfigManager.activeConfig.collectAsStateWithLifecycle()
    val adTelemetry by coordinator.adTelemetry.collectAsStateWithLifecycle()
    val viewTracking by coordinator.viewTracking.collectAsStateWithLifecycle()

    var impressionCounter by remember { mutableIntStateOf(1) }
    var viewEventCounter by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .verticalScroll(rememberScrollState())
            .testTag("advertisements_screen")
    ) {
        ShareDabbaTopBar(
            title = "Advertising & View Tracking",
            subtitle = "Stage 9 • Consent, Pacing, Deduplication & View Attribution",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Missing Ad Provider & Anti-Fake-Ad Notice
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .testTag("ad_missing_provider_report_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShareDabbaGradientBadge(text = "STAGE 9 MONETIZATION INTEGRITY")
                    Text(
                        text = "Approved Ad Provider Status: ${remoteConfig.advertising.approvedAdProvider}",
                        style = MaterialTheme.typography.titleSmall,
                        color = ShareDabbaWhite
                    )
                    adTelemetry.missingSdkOrConfigReport.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall,
                            color = ShareDabbaTextSecondary
                        )
                    }
                    Text(
                        text = "Config rules: Every ${remoteConfig.advertising.adFrequencyEveryNVideos} videos • Cooldown ${remoteConfig.advertising.adCooldownSeconds}s • Session Max ${remoteConfig.advertising.maxAdsPerSession} • Fallback ${remoteConfig.advertising.adFallbackStrategy}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaCyan
                    )
                }
            }

            // 2. Strict Separation: Clicks vs. Impressions vs. Confirmed Revenue
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                    .testTag("ad_metrics_separation_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Clicks vs. Impressions vs. Confirmed Revenue",
                        style = MaterialTheme.typography.titleMedium,
                        color = ShareDabbaWhite
                    )
                    Text(
                        text = "Ad impressions: ${adTelemetry.recordedAdImpressions}  |  Duplicate impressions blocked: ${adTelemetry.duplicateImpressionsBlocked}  |  Ad clicks: ${adTelemetry.recordedAdClicks}  |  Failed ad fallbacks: ${adTelemetry.failedAdRequestsHandled}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShareDabbaCyan,
                        modifier = Modifier.testTag("ad_telemetry_counters_text")
                    )
                    Text(
                        text = "Confirmed Revenue: ${adTelemetry.confirmedServerRevenueDisplay}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ShareDabbaWhite,
                        modifier = Modifier.testTag("ad_confirmed_revenue_text")
                    )

                    // User Consent Selector
                    Text(
                        text = "User Ad Consent Requirement (GDPR / UMP):",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaTextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserAdConsentStatus.entries.forEach { consent ->
                            FilterChip(
                                selected = adTelemetry.consentStatus == consent,
                                onClick = { coordinator.updateUserAdConsent(consent) },
                                label = {
                                    Text(
                                        when (consent) {
                                            UserAdConsentStatus.OBTAINED_PERSONALIZED -> "Personalized"
                                            UserAdConsentStatus.OBTAINED_NON_PERSONALIZED -> "Non-Personal"
                                            UserAdConsentStatus.NOT_OBTAINED_BLOCKED -> "No Consent"
                                        }
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ShareDabbaCyan,
                                    selectedLabelColor = ShareDabbaBlack,
                                    containerColor = ShareDabbaBlack,
                                    labelColor = ShareDabbaWhite
                                ),
                                modifier = Modifier.testTag("ad_consent_${consent.name}")
                            )
                        }
                    }

                    // Interactive Ad Evaluation Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coordinator.onVideoPlaybackStartedForAdPacing()
                                coordinator.evaluateAndRecordAdImpression(
                                    placement = AdPlacementType.BANNER,
                                    impressionId = "imp-banner-${impressionCounter++}",
                                    adConfigOverride = AdminAdvertisingConfig(
                                        approvedAdProvider = "GOOGLE_ADMOB_APPROVED",
                                        bannerAdUnitId = "ca-app-pub-test/banner-01",
                                        enableBannerPlacements = true,
                                        adCooldownSeconds = 0,
                                        maxAdsPerSession = 6
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ad_record_valid_impression_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShareDabbaCyan,
                                contentColor = ShareDabbaBlack
                            )
                        ) {
                            Text("Test Impression")
                        }
                        OutlinedButton(
                            onClick = {
                                coordinator.evaluateAndRecordAdImpression(
                                    placement = AdPlacementType.BANNER,
                                    impressionId = "imp-duplicate-fixed-id",
                                    adConfigOverride = AdminAdvertisingConfig(
                                        approvedAdProvider = "GOOGLE_ADMOB_APPROVED",
                                        bannerAdUnitId = "ca-app-pub-test/banner-01",
                                        enableBannerPlacements = true,
                                        adCooldownSeconds = 0
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ad_test_duplicate_impression_button")
                        ) {
                            Text("Test Duplicate", color = ShareDabbaWhite)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { coordinator.recordAdClick() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ad_record_click_button")
                        ) {
                            Text("Record Ad Click", color = ShareDabbaWhite)
                        }
                        OutlinedButton(
                            onClick = {
                                coordinator.handleFailedAdRequest(
                                    placement = AdPlacementType.INTERSTITIAL,
                                    errorReason = "NO_FILL_NETWORK_TIMEOUT"
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ad_simulate_failed_request_button")
                        ) {
                            Text("Test Ad Failure", color = ShareDabbaWhite)
                        }
                    }

                    adTelemetry.lastEvaluation?.let { eval ->
                        Text(
                            text = "Last Ad Decision [${eval.outcome.name}]: ${eval.explanation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ShareDabbaTextSecondary,
                            modifier = Modifier.testTag("ad_last_evaluation_text")
                        )
                    }
                }
            }

            // 3. View Tracking: Recorded Views vs. Server-Verified Eligible Monetized Views
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                    .testTag("view_tracking_validation_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Legitimate View Tracking & Attribution",
                        style = MaterialTheme.typography.titleMedium,
                        color = ShareDabbaWhite
                    )
                    Text(
                        text = "Client Recorded Views: ${viewTracking.clientRecordedViewsSessionCount}  |  Server Eligible Monetized Views: ${viewTracking.serverVerifiedEligibleViewsSessionCount}  |  Duplicates Blocked: ${viewTracking.duplicateViewsBlockedCount}  |  Artificial (<5s) Blocked: ${viewTracking.artificialViewsBlockedCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShareDabbaCyan,
                        modifier = Modifier.testTag("view_tracking_counters_text")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val index = viewEventCounter++
                                coordinator.reportPlaybackViewEvent(
                                    PlaybackViewEventRequest(
                                        eventId = "evt-legit-$index",
                                        videoId = "sd-vid-101",
                                        uploaderId = "usr-aarav-mehta",
                                        viewerUid = "viewer-android-01",
                                        watchDurationMs = 18_500L,
                                        sessionId = "sess-$index"
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("view_report_legitimate_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShareDabbaPurple,
                                contentColor = ShareDabbaWhite
                            )
                        ) {
                            Text("Legitimate View")
                        }
                        OutlinedButton(
                            onClick = {
                                coordinator.reportPlaybackViewEvent(
                                    PlaybackViewEventRequest(
                                        eventId = "evt-short-${viewEventCounter++}",
                                        videoId = "sd-vid-101",
                                        uploaderId = "usr-aarav-mehta",
                                        viewerUid = "viewer-android-01",
                                        watchDurationMs = 1_200L,
                                        sessionId = "sess-bot"
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("view_report_artificial_button")
                        ) {
                            Text("Bot View (<5s)", color = ShareDabbaWhite)
                        }
                    }

                    viewTracking.trackedEvents.firstOrNull()?.let { latest ->
                        Text(
                            text = "Latest View Event [${latest.outcome.name}]: ${latest.explanation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ShareDabbaTextSecondary,
                            modifier = Modifier.testTag("view_latest_result_text")
                        )
                    }
                }
            }

            ShareDabbaSectionCard(
                title = "Approved Ad Placement Slots",
                packagePath = "com.example.advertisements",
                description = "Pre-roll, mid-roll, and companion ad slots are governed by remote configuration and user consent without bundling unapproved ad SDKs.",
                icon = Icons.Default.Campaign,
                testTag = "ad_slots_card"
            )
            ShareDabbaSectionCard(
                title = "Anti-Fraud & Cross-Platform Deduplication",
                packagePath = "com.example.advertisements.telemetry",
                description = "Playback events preserve original uploaderId attribution, enforce a 5-second minimum watch threshold, and prevent Web/Android cross-platform double counting.",
                icon = Icons.Default.Visibility,
                testTag = "ad_view_verification_card"
            )
            ShareDabbaSectionCard(
                title = "Play Creator Track Policy Compliance",
                packagePath = "com.example.advertisements.policy",
                description = "No fake ad impressions, simulated click farms, or client-side revenue calculations are permitted.",
                icon = Icons.Default.GppGood,
                testTag = "ad_policy_card"
            )
            ShareDabbaSectionCard(
                title = "Server-Side Revenue Verification",
                packagePath = "com.example.advertisements.guard",
                description = "Only verified ad impressions confirmed by the ShareDabba website backend contribute to creator earnings.",
                icon = Icons.Default.Shield,
                testTag = "ad_guard_card"
            )
        }
    }
}
