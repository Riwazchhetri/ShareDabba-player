package com.example.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.authentication.SessionTokenCoordinator
import com.example.authentication.signOutFromShareDabba
import com.example.core.config.EnvironmentConfig
import com.example.core.logging.AppLogger
import com.example.data.repository.StreamCacheRepository
import com.example.network.IntegrationContractRegistry
import com.example.remoteconfig.RemoteConfigManager
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.common.ShareDabbaSectionCard
import com.example.ui.common.ShareDabbaTopBar
import com.example.ui.navigation.ShareDabbaDestination
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaError
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaPurple
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWarning
import com.example.ui.theme.ShareDabbaWhite
import kotlinx.coroutines.launch

/**
 * Screen 18: Settings Screen (Updated for Stage 10 Notifications, Preferences, Legal & Logout)
 * Includes:
 * - Android runtime notification permission (`POST_NOTIFICATIONS`) request & status
 * - Notification category preferences (optional user-toggleable categories vs. required notices)
 * - Support and Feedback, Privacy Policy, and Terms & Conditions
 * - Account Logout (clearing session metadata + signing out of Firebase Auth & CredentialManager)
 * - EnvironmentConfig, Stage 2 Backend Integration Contract, Room Local Cache, and AppLogger
 */
@Composable
fun SettingsScreen(
    environmentConfig: EnvironmentConfig,
    streamCacheRepository: StreamCacheRepository,
    notificationsCoordinator: NotificationsAndSettingsCoordinator? = null,
    remoteConfigManager: RemoteConfigManager? = null,
    sessionTokenCoordinator: SessionTokenCoordinator? = null,
    onNavigate: (String) -> Unit = {},
    onSignedOut: () -> Unit = {},
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val effectiveRemoteConfigManager = remember(remoteConfigManager, environmentConfig) {
        remoteConfigManager ?: RemoteConfigManager(environmentConfig)
    }
    val effectiveNotificationsCoordinator = remember(notificationsCoordinator, environmentConfig) {
        notificationsCoordinator ?: NotificationsAndSettingsCoordinator(
            context = context.applicationContext,
            environmentConfig = environmentConfig,
            remoteConfigManager = effectiveRemoteConfigManager
        )
    }

    val remoteConfig by effectiveRemoteConfigManager.activeConfig.collectAsStateWithLifecycle()
    val notificationPrefs by effectiveNotificationsCoordinator.preferences.collectAsStateWithLifecycle()
    val logs by AppLogger.recentLogs.collectAsStateWithLifecycle()
    val cachedStreams by streamCacheRepository.cachedStreams.collectAsStateWithLifecycle(initialValue = emptyList())

    var legalNoticeBanner by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        effectiveNotificationsCoordinator.updateNotificationPermissionGranted(isGranted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen")
    ) {
        ShareDabbaTopBar(
            title = "Settings & Notification Control",
            subtitle = "Stage 10 • Permissions, Preferences, Legal & Logout",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Android Notification Permission & FCM Honesty Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .testTag("settings_notification_permission_card"),
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
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = ShareDabbaCyan
                            )
                            Text(
                                text = "Android Notification Permission",
                                style = MaterialTheme.typography.titleMedium,
                                color = ShareDabbaWhite
                            )
                        }
                        ShareDabbaGradientBadge(
                            text = if (notificationPrefs.permissionStatus.canPostToSystemTray) "PERMITTED" else "DENIED"
                        )
                    }
                    Text(
                        text = "Status: ${notificationPrefs.permissionStatus.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShareDabbaCyan,
                        modifier = Modifier.testTag("settings_permission_status_text")
                    )
                    Text(
                        text = "FCM Push Status: ${notificationPrefs.fcmIntegrationStatus}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaTextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    effectiveNotificationsCoordinator.updateNotificationPermissionGranted(true)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_request_notification_permission_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShareDabbaCyan,
                                contentColor = ShareDabbaBlack
                            )
                        ) {
                            Text("Request Permission")
                        }

                        OutlinedButton(
                            onClick = { onNavigate(ShareDabbaDestination.Notifications.route) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_open_notifications_button")
                        ) {
                            Text("Open Notifications", color = ShareDabbaWhite)
                        }
                    }
                }
            }

            // 2. Notification Preferences (Optional Categories vs. Critical Operational Notices)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                    .testTag("settings_notification_preferences_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Notification Category Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        color = ShareDabbaWhite
                    )
                    Text(
                        text = "Disable optional categories below. Critical maintenance and app update notices remain active in-app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShareDabbaTextSecondary
                    )

                    ShareDabbaNotificationCategory.entries.forEach { category ->
                        val isEnabled = notificationPrefs.enabledCategories[category] ?: true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_pref_row_${category.id}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = category.title + if (!category.isOptionalUserToggleable) " (Required)" else "",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ShareDabbaWhite
                                )
                                Text(
                                    text = category.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ShareDabbaTextSecondary
                                )
                            }
                            Switch(
                                checked = isEnabled,
                                enabled = category.isOptionalUserToggleable,
                                onCheckedChange = { checked ->
                                    effectiveNotificationsCoordinator.setCategoryEnabled(category, checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ShareDabbaBlack,
                                    checkedTrackColor = ShareDabbaCyan
                                ),
                                modifier = Modifier.testTag("settings_pref_switch_${category.id}")
                            )
                        }
                    }

                    Text(
                        text = notificationPrefs.lastDeliveryAuditMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaCyan,
                        modifier = Modifier.testTag("settings_pref_audit_message")
                    )
                }
            }

            // 3. Support & Feedback, Privacy Policy, and Terms & Conditions
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                    .testTag("settings_legal_and_support_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Support, Feedback & Legal Policies",
                        style = MaterialTheme.typography.titleMedium,
                        color = ShareDabbaWhite
                    )
                    Text(
                        text = "Support Email: ${remoteConfig.branding.supportEmail} • Help Center: ${remoteConfig.branding.supportPortalUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShareDabbaTextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(ShareDabbaDestination.HelpSupport.route) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_open_help_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShareDabbaCyan,
                                contentColor = ShareDabbaBlack
                            )
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Support & Feedback")
                        }

                        OutlinedButton(
                            onClick = { onNavigate(ShareDabbaDestination.Onboarding.route) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_open_onboarding_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = ShareDabbaPurple
                            )
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Onboarding", color = ShareDabbaWhite)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val url = remoteConfig.branding.privacyPolicyUrl
                                legalNoticeBanner = "Privacy Policy (${url}): ShareDabba protects user authentication tokens via encrypted DataStore and never sells personal creator data."
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_privacy_policy_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PrivacyTip,
                                contentDescription = null,
                                tint = ShareDabbaCyan
                            )
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Privacy Policy", color = ShareDabbaWhite)
                        }

                        OutlinedButton(
                            onClick = {
                                val url = remoteConfig.branding.termsOfServiceUrl
                                legalNoticeBanner = "Terms & Conditions (${url}): Content ownership, creator payouts, and streaming rights are governed by the authoritative ShareDabba website platform."
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_terms_conditions_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = ShareDabbaPurple
                            )
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Terms & Conditions", color = ShareDabbaWhite)
                        }
                    }

                    if (!legalNoticeBanner.isNullOrBlank()) {
                        Text(
                            text = legalNoticeBanner.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = ShareDabbaCyan,
                            modifier = Modifier.testTag("settings_legal_notice_text")
                        )
                    }
                }
            }

            // 4. Environment Configuration & Stage 2 Contract
            ShareDabbaSectionCard(
                title = "Environment Configuration",
                packagePath = "App ID: ${environmentConfig.applicationId} (v${environmentConfig.versionName})",
                description = "Web URL: ${environmentConfig.websiteBaseUrl}\nAPI URL: ${environmentConfig.apiBaseUrl}",
                icon = Icons.Default.Settings,
                testTag = "settings_env_card"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                    .testTag("stage2_contract_card"),
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
                        Text(
                            text = "Stage 2 Backend Integration Contract",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaWhite
                        )
                        ShareDabbaGradientBadge(text = "PENDING REPO / DOCS")
                    }
                    Text(
                        text = "Backend Connection Complete: ${IntegrationContractRegistry.IS_BACKEND_CONNECTION_COMPLETE} (No endpoints or schemas invented)",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaWarning
                    )
                    IntegrationContractRegistry.discoveryChecklist.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.number}. ${item.domain}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ShareDabbaWhite
                            )
                            Text(
                                text = item.status.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = ShareDabbaTextSecondary
                            )
                        }
                    }
                }
            }

            // 5. Room Local Cache Management
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Room Local Cache Management",
                        style = MaterialTheme.typography.titleMedium,
                        color = ShareDabbaWhite
                    )
                    Text(
                        text = "Cached stream entries on device: ${cachedStreams.size} (Room is used strictly as a local cache, not a replacement for the ShareDabba website database).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShareDabbaTextSecondary
                    )
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                streamCacheRepository.clearCache()
                                AppLogger.i("Settings", "Cleared local Room stream cache.")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_room_cache_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = ShareDabbaCyan
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Clear Local Room Cache", color = ShareDabbaWhite)
                    }
                }
            }

            // 6. Account Logout Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaError.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .testTag("settings_logout_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Account Session & Logout",
                        style = MaterialTheme.typography.titleMedium,
                        color = ShareDabbaWhite
                    )
                    Text(
                        text = "Signing out clears your local session metadata and revokes active CredentialManager session state on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShareDabbaTextSecondary
                    )
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                sessionTokenCoordinator?.clearSessionMetadata()
                            }
                            signOutFromShareDabba(
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignedOut,
                                scope = scope
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_account_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = ShareDabbaError
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Sign Out of ShareDabba", color = ShareDabbaError)
                    }
                }
            }

            // 7. AppLogger Diagnostic Console
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = ShareDabbaCyan
                        )
                        Text(
                            text = "AppLogger Diagnostic Console (Sensitive Data Redacted)",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaWhite
                        )
                    }
                    if (logs.isEmpty()) {
                        Text(
                            text = "No recent diagnostic events logged.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ShareDabbaTextSecondary
                        )
                    } else {
                        logs.take(12).forEach { entry ->
                            Text(
                                text = "[${entry.timestamp}] ${entry.level}/${entry.tag}: ${entry.message}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShareDabbaTextSecondary
                            )
                        }
                    }
                }
            }

            // 8. Stage 11 Complete QA & Security Audit Card
            val stage11AuditReport = remember(environmentConfig) {
                QaSecurityAuditRunner.runCompleteAudit(environmentConfig)
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaCyan.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .testTag("settings_stage11_qa_security_audit_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShareDabbaGradientBadge(text = "STAGE 11 QA & SECURITY AUDIT")
                    Text(
                        text = "Independent QA & Application Security Audit Summary",
                        style = MaterialTheme.typography.titleMedium,
                        color = ShareDabbaWhite
                    )
                    Text(
                        text = "PASS: ${stage11AuditReport.passCount} • FAIL: ${stage11AuditReport.failCount} • NOT CONFIGURED: ${stage11AuditReport.notConfiguredCount}",
                        style = MaterialTheme.typography.labelLarge,
                        color = ShareDabbaCyan,
                        modifier = Modifier.testTag("stage11_audit_counts_label")
                    )
                    Text(
                        text = stage11AuditReport.overallReadinessSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = ShareDabbaWarning
                    )
                    stage11AuditReport.fixedDefectsSummary.forEach { defectFix ->
                        Text(
                            text = "• $defectFix",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
