package com.example.remoteconfig

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.config.EnvironmentConfig
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.common.ShareDabbaTopBar
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaError
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaPurple
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWarning
import com.example.ui.theme.ShareDabbaWhite

// Retained for backward compatibility with any external references
data class RemoteFeatureFlags(
    val enableAdaptiveHls: Boolean = true,
    val enableHighThinkingAi: Boolean = true,
    val enableLowLatencySearch: Boolean = true,
    val maxRoomCacheEntries: Int = 100
)

/**
 * STAGE 8 — SHARE DABBA REMOTE ADMIN CONTROL SCREEN
 * Connects the Android app to the Website Admin Panel's published configuration contract:
 * - A. App Branding
 * - B. Home Screen
 * - C. Video Player
 * - D. Advertising
 * - E. App Management
 * Enforces safe defaults, interval throttling, value validation, anti-script/DCL protection,
 * configuration versioning, and 1-tap rollback.
 */
@Composable
fun RemoteConfigScreen(
    environmentConfig: EnvironmentConfig,
    remoteConfigManager: RemoteConfigManager = remember(environmentConfig) {
        RemoteConfigManager(environmentConfig)
    },
    onBackClick: () -> Unit = {}
) {
    BackHandler(onBack = onBackClick)

    val activeConfig by remoteConfigManager.activeConfig.collectAsStateWithLifecycle()
    val previousConfig by remoteConfigManager.previousValidConfig.collectAsStateWithLifecycle()
    val validationReport by remoteConfigManager.lastValidationReport.collectAsStateWithLifecycle()
    val statusBanner by remoteConfigManager.statusBanner.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .testTag("remote_config_screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ShareDabbaTopBar(
                title = "Remote Admin Control",
                subtitle = "Stage 8 • Website Admin Panel Config v${activeConfig.configVersion}",
                onBackClick = onBackClick
            )
        }

        // Lifecycle, Throttling, Versioning & Rollback Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                    .testTag("rc_lifecycle_card"),
                shape = RoundedCornerShape(16.dp),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = ShareDabbaCyan
                            )
                            Text(
                                text = "Config Sync, Versioning & Rollback",
                                style = MaterialTheme.typography.titleMedium,
                                color = ShareDabbaWhite
                            )
                        }
                        ShareDabbaGradientBadge(text = "v${activeConfig.configVersion}")
                    }

                    Text(
                        text = statusBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = ShareDabbaCyan,
                        modifier = Modifier.testTag("rc_status_banner")
                    )

                    Text(
                        text = "Network Fetches: ${remoteConfigManager.networkFetchCount} • Throttled Skips: ${remoteConfigManager.skippedThrottledFetchCount} • Prior Snapshot: ${previousConfig?.let { "v${it.configVersion}" } ?: "None"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaTextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                remoteConfigManager.refreshFromWebsiteAdminPanel(forceRefresh = false)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rc_throttled_refresh_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShareDabbaCyan,
                                contentColor = ShareDabbaBlack
                            )
                        ) {
                            Text("Sync (Throttled)")
                        }

                        OutlinedButton(
                            onClick = {
                                remoteConfigManager.rollbackToPreviousValidConfig()
                            },
                            enabled = previousConfig != null,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rc_rollback_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = ShareDabbaCyan
                            )
                            Spacer(modifier = Modifier.padding(3.dp))
                            Text("Rollback", color = ShareDabbaWhite)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                remoteConfigManager.applyAdminPanelConfigUpdate(
                                    activeConfig.copy(
                                        configVersion = activeConfig.configVersion + 1,
                                        sourceLabel = "SECURITY_TEST_PAYLOAD",
                                        branding = activeConfig.branding.copy(
                                            globalAnnouncementBanner = "<script>eval('malicious_payload')</script>"
                                        ),
                                        advertising = activeConfig.advertising.copy(
                                            approvedAdProvider = "UNBUNDLED_NATIVE_AD_SDK"
                                        )
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rc_test_security_validation_button")
                        ) {
                            Text("Test Script & SDK Guard", color = ShareDabbaWarning)
                        }

                        OutlinedButton(
                            onClick = { remoteConfigManager.resetToSafeDefaults() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rc_reset_defaults_button")
                        ) {
                            Text("Safe Defaults", color = ShareDabbaWhite)
                        }
                    }

                    if (validationReport.rejectedUnsafeCodeOrScriptPayloads.isNotEmpty() ||
                        validationReport.nativeReleaseRequiredNotices.isNotEmpty()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ShareDabbaBlack, RoundedCornerShape(10.dp))
                                .border(1.dp, ShareDabbaWarning, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            validationReport.rejectedUnsafeCodeOrScriptPayloads.forEach { msg ->
                                Text(
                                    text = "SECURITY GUARD: $msg (Arbitrary code/scripts are never executed)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ShareDabbaError
                                )
                            }
                            validationReport.nativeReleaseRequiredNotices.forEach { notice ->
                                Text(
                                    text = "NEW RELEASE REQUIRED: $notice",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ShareDabbaWarning
                                )
                            }
                        }
                    }
                }
            }
        }

        // GROUP A — APP BRANDING
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                    .testTag("rc_group_a_branding"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = ShareDabbaCyan)
                        Text(
                            text = "A. App Branding (Functional)",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaWhite
                        )
                    }
                    Text(
                        text = "Display Name: ${activeConfig.branding.appDisplayName} • Colours: ${activeConfig.branding.primaryCyanHex} / ${activeConfig.branding.accentPurpleHex} / ${activeConfig.branding.backgroundBlackHex}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShareDabbaTextSecondary
                    )
                    Text(
                        text = "Support: ${activeConfig.branding.supportEmail} • Terms: ${activeConfig.branding.termsOfServiceUrl}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaCyan
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Publish Global Announcement Banner on Home",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ShareDabbaWhite,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = activeConfig.branding.globalAnnouncementBanner.isNotBlank(),
                            onCheckedChange = { enabled ->
                                remoteConfigManager.applyAdminPanelConfigUpdate(
                                    activeConfig.copy(
                                        configVersion = activeConfig.configVersion + 1,
                                        sourceLabel = "ADMIN_BRANDING_UPDATE",
                                        branding = activeConfig.branding.copy(
                                            globalAnnouncementBanner = if (enabled) {
                                                "Website Admin Announcement: Creator 4K HLS Transcoding is now live!"
                                            } else {
                                                ""
                                            }
                                        )
                                    )
                                )
                            },
                            modifier = Modifier.testTag("rc_toggle_announcement")
                        )
                    }
                }
            }
        }

        // GROUP B — HOME SCREEN
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                    .testTag("rc_group_b_home"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = ShareDabbaCyan)
                        Text(
                            text = "B. Home Screen Visibility (Functional)",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaWhite
                        )
                    }

                    AdminToggleRow(
                        label = "Show Hero Banner on Home",
                        checked = activeConfig.homeScreen.showHeroBanner,
                        testTag = "rc_toggle_hero_banner",
                        onCheckedChange = { checked ->
                            remoteConfigManager.applyAdminPanelConfigUpdate(
                                activeConfig.copy(
                                    configVersion = activeConfig.configVersion + 1,
                                    sourceLabel = "ADMIN_HOME_UPDATE",
                                    homeScreen = activeConfig.homeScreen.copy(showHeroBanner = checked)
                                )
                            )
                        }
                    )

                    AdminToggleRow(
                        label = "Show Categories Filter Bar",
                        checked = activeConfig.homeScreen.showCategoriesBar,
                        testTag = "rc_toggle_categories_bar",
                        onCheckedChange = { checked ->
                            remoteConfigManager.applyAdminPanelConfigUpdate(
                                activeConfig.copy(
                                    configVersion = activeConfig.configVersion + 1,
                                    sourceLabel = "ADMIN_HOME_UPDATE",
                                    homeScreen = activeConfig.homeScreen.copy(showCategoriesBar = checked)
                                )
                            )
                        }
                    )

                    AdminToggleRow(
                        label = "Show Featured Videos Section",
                        checked = activeConfig.homeScreen.showFeaturedVideosSection,
                        testTag = "rc_toggle_featured_section",
                        onCheckedChange = { checked ->
                            remoteConfigManager.applyAdminPanelConfigUpdate(
                                activeConfig.copy(
                                    configVersion = activeConfig.configVersion + 1,
                                    sourceLabel = "ADMIN_HOME_UPDATE",
                                    homeScreen = activeConfig.homeScreen.copy(showFeaturedVideosSection = checked)
                                )
                            )
                        }
                    )

                    AdminToggleRow(
                        label = "Show Recently Uploaded & Popular Sections",
                        checked = activeConfig.homeScreen.showRecentlyUploadedSection &&
                            activeConfig.homeScreen.showPopularVideosSection,
                        testTag = "rc_toggle_recent_popular_sections",
                        onCheckedChange = { checked ->
                            remoteConfigManager.applyAdminPanelConfigUpdate(
                                activeConfig.copy(
                                    configVersion = activeConfig.configVersion + 1,
                                    sourceLabel = "ADMIN_HOME_UPDATE",
                                    homeScreen = activeConfig.homeScreen.copy(
                                        showRecentlyUploadedSection = checked,
                                        showPopularVideosSection = checked
                                    )
                                )
                            )
                        }
                    )
                }
            }
        }

        // GROUP C — VIDEO PLAYER
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                    .testTag("rc_group_c_player"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, tint = ShareDabbaCyan)
                        Text(
                            text = "C. Video Player Controls (Functional)",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaWhite
                        )
                    }
                    Text(
                        text = "Default Quality: ${activeConfig.videoPlayer.defaultQualityId} • Speeds: ${activeConfig.videoPlayer.availablePlaybackSpeeds.joinToString()}x",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShareDabbaTextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("auto", "1080p", "720p", "480p", "360p").forEach { q ->
                            FilterChip(
                                selected = activeConfig.videoPlayer.defaultQualityId == q,
                                onClick = {
                                    remoteConfigManager.applyAdminPanelConfigUpdate(
                                        activeConfig.copy(
                                            configVersion = activeConfig.configVersion + 1,
                                            sourceLabel = "ADMIN_PLAYER_QUALITY",
                                            videoPlayer = activeConfig.videoPlayer.copy(defaultQualityId = q)
                                        )
                                    )
                                },
                                label = { Text(q) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ShareDabbaCyan,
                                    selectedLabelColor = ShareDabbaBlack
                                )
                            )
                        }
                    }

                    AdminToggleRow(
                        label = "Allow Fullscreen & Picture-in-Picture",
                        checked = activeConfig.videoPlayer.allowFullscreen &&
                            activeConfig.videoPlayer.allowPictureInPicture,
                        testTag = "rc_toggle_fullscreen_pip",
                        onCheckedChange = { checked ->
                            remoteConfigManager.applyAdminPanelConfigUpdate(
                                activeConfig.copy(
                                    configVersion = activeConfig.configVersion + 1,
                                    sourceLabel = "ADMIN_PLAYER_UPDATE",
                                    videoPlayer = activeConfig.videoPlayer.copy(
                                        allowFullscreen = checked,
                                        allowPictureInPicture = checked
                                    )
                                )
                            )
                        }
                    )
                }
            }
        }

        // GROUP D & E — ADVERTISING & APP MANAGEMENT
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                    .testTag("rc_group_d_e_ads_mgmt"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = ShareDabbaPurple)
                        Text(
                            text = "D. Advertising & E. App Management",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaWhite
                        )
                    }
                    Text(
                        text = "Ad Provider: ${activeConfig.advertising.approvedAdProvider} • Cooldown: ${activeConfig.advertising.adCooldownSeconds}s • Max Ads/Session: ${activeConfig.advertising.maxAdsPerSession} • Fallback: ${activeConfig.advertising.adFallbackStrategy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShareDabbaTextSecondary
                    )
                    Text(
                        text = "Min Supported VersionCode: ${activeConfig.appManagement.minSupportedVersionCode} • Recommended: ${activeConfig.appManagement.recommendedVersionCode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaCyan
                    )

                    AdminToggleRow(
                        label = "Enable Scheduled Maintenance Mode Banner",
                        checked = activeConfig.appManagement.maintenanceModeEnabled,
                        testTag = "rc_toggle_maintenance_mode",
                        onCheckedChange = { checked ->
                            remoteConfigManager.applyAdminPanelConfigUpdate(
                                activeConfig.copy(
                                    configVersion = activeConfig.configVersion + 1,
                                    sourceLabel = "ADMIN_MAINTENANCE_TOGGLE",
                                    appManagement = activeConfig.appManagement.copy(
                                        maintenanceModeEnabled = checked
                                    )
                                )
                            )
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AdminToggleRow(
    label: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ShareDabbaWhite,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
