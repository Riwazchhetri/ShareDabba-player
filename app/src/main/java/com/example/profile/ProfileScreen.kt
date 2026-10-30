package com.example.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.data.fixtures.DevFixturesCatalog
import com.example.data.fixtures.NotificationItemFixture
import com.example.data.fixtures.SupportFaqItem
import com.example.ui.common.DevFixtureNoticeBanner
import com.example.ui.common.PreviewStateMode
import com.example.ui.common.ShareDabbaEmptyState
import com.example.ui.common.ShareDabbaErrorState
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.common.ShareDabbaLoadingState
import com.example.ui.common.ShareDabbaSectionCard
import com.example.ui.common.ShareDabbaTopBar
import com.example.ui.common.StateModeSelectorRow
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
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

/**
 * Screen 17: Profile Screen (Updated for Stage 5 Authentication & Session Management)
 * Includes:
 * - Authenticated Google Identity & UID details
 * - Live Profile Update (Display Name update on shared Firebase Auth identity)
 * - Session & Token Lifecycle Inspector (Token Refresh + Simulate Expiry & Verify Recovery)
 * - Direct navigation to all 19 Stage 3 screens & Sign Out
 */
@Composable
fun ProfileScreen(
    currentUser: FirebaseUser?,
    sessionTokenCoordinator: SessionTokenCoordinator,
    onNavigate: (String) -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val sessionStatus by sessionTokenCoordinator.sessionStatus.collectAsStateWithLifecycle()

    var displayNameInput by rememberSaveable(currentUser?.displayName) {
        mutableStateOf(currentUser?.displayName.orEmpty())
    }
    var profileFeedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser?.uid) {
        sessionTokenCoordinator.syncOrRefreshSession(forceRefresh = false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .verticalScroll(rememberScrollState())
            .testTag("profile_screen")
    ) {
        ShareDabbaTopBar(
            title = "Profile & Auth Hub",
            subtitle = "STAGE 5 • Unified Identity & Session Lifecycle"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Authenticated User & Profile Update Card (Stage 5 Test #3: "Update profile")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(18.dp))
                    .testTag("profile_identity_card"),
                shape = RoundedCornerShape(18.dp),
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
                        ShareDabbaGradientBadge(text = "AUTHENTICATED SESSION")
                        ShareDabbaGradientBadge(text = "SAME WEB UID")
                    }
                    Text(
                        text = sessionStatus.displayName?.takeIf { it.isNotBlank() }
                            ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
                            ?: "ShareDabba Creator",
                        style = MaterialTheme.typography.titleLarge,
                        color = ShareDabbaWhite
                    )
                    Text(
                        text = currentUser?.email ?: "UID: ${currentUser?.uid.orEmpty()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShareDabbaCyan
                    )
                    Text(
                        text = "UID: ${currentUser?.uid.orEmpty()} • Provider: ${sessionStatus.providerId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaTextSecondary
                    )

                    OutlinedTextField(
                        value = displayNameInput,
                        onValueChange = { displayNameInput = it },
                        label = { Text("Update Creator Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_display_name_input")
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                val res = sessionTokenCoordinator.updateAuthenticatedUserProfile(displayNameInput)
                                profileFeedback = res.fold(
                                    onSuccess = { "Updated profile display name to '${it.displayName}' and refreshed ID token." },
                                    onFailure = { "Profile update error: ${it.localizedMessage}" }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_profile_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShareDabbaCyan,
                            contentColor = ShareDabbaBlack
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Update Profile on Auth Backend")
                    }

                    if (!profileFeedback.isNullOrBlank()) {
                        Text(
                            text = profileFeedback.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaCyan,
                            modifier = Modifier.testTag("profile_update_feedback")
                        )
                    }
                }
            }

            // Stage 5 Session Refresh & Expired Token Recovery Card (Test #6: "Expire the session and verify recovery")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(18.dp))
                    .testTag("session_token_inspector_card"),
                shape = RoundedCornerShape(18.dp),
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
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = ShareDabbaCyan
                            )
                            Text(
                                text = "Session & Token Lifecycle",
                                style = MaterialTheme.typography.titleMedium,
                                color = ShareDabbaWhite
                            )
                        }
                        ShareDabbaGradientBadge(
                            text = if (sessionStatus.isTokenExpired) "EXPIRED (401)" else "ACTIVE TOKEN"
                        )
                    }

                    Text(
                        text = "Token SHA-256 Fingerprint: ${sessionStatus.tokenFingerprintSha256 ?: "Refreshing..."}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaCyan
                    )
                    Text(
                        text = sessionStatus.lastRefreshedLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (sessionStatus.isTokenExpired) ShareDabbaWarning else ShareDabbaTextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                sessionTokenCoordinator.simulateExpiredSessionForTesting()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("simulate_session_expiry_button")
                        ) {
                            Text("Expire Session", color = ShareDabbaWarning)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    sessionTokenCoordinator.syncOrRefreshSession(forceRefresh = true)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("refresh_session_token_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShareDabbaPurple,
                                contentColor = ShareDabbaWhite
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.padding(3.dp))
                            Text("Refresh & Recover")
                        }
                    }
                }
            }

            ShareDabbaSectionCard(
                title = "User Dashboard",
                packagePath = "Total Uploads • Views • Balances",
                description = "Inspect recorded views, eligible views, earnings, pending and available balance.",
                icon = Icons.Default.Dashboard,
                testTag = "profile_nav_user_dashboard",
                onClick = { onNavigate(ShareDabbaDestination.UserDashboard.route) }
            )

            ShareDabbaSectionCard(
                title = "My Uploads",
                packagePath = "com.example.uploads",
                description = "Manage uploaded videos and stage new video files via Android Photo Picker.",
                icon = Icons.Default.CloudUpload,
                testTag = "profile_nav_uploads",
                onClick = { onNavigate(ShareDabbaDestination.Uploads.route) }
            )

            ShareDabbaSectionCard(
                title = "Recently Watched",
                packagePath = "Room Local Playback Cache",
                description = "View and resume recently watched streams cached on your device.",
                icon = Icons.Default.History,
                testTag = "profile_nav_recently_watched",
                onClick = { onNavigate(ShareDabbaDestination.RecentlyWatched.route) }
            )

            ShareDabbaSectionCard(
                title = "Categories",
                packagePath = "Browse By Stream Genre",
                description = "Filter ShareDabba streams across Live, Esports, Creator Tech, Music, and Education.",
                icon = Icons.Default.Category,
                testTag = "profile_nav_categories",
                onClick = { onNavigate(ShareDabbaDestination.Categories.route) }
            )

            ShareDabbaSectionCard(
                title = "Creator Earnings",
                packagePath = "com.example.earnings",
                description = "View per-video recorded vs. eligible views and revenue breakdown.",
                icon = Icons.Default.MonetizationOn,
                testTag = "profile_nav_earnings",
                onClick = { onNavigate(ShareDabbaDestination.Earnings.route) }
            )

            ShareDabbaSectionCard(
                title = "ShareDabba Wallet",
                packagePath = "com.example.wallet",
                description = "Unified website wallet balance and payout status.",
                icon = Icons.Default.AccountBalanceWallet,
                testTag = "profile_nav_wallet",
                onClick = { onNavigate(ShareDabbaDestination.Wallet.route) }
            )

            ShareDabbaSectionCard(
                title = "Withdrawal History",
                packagePath = "com.example.wallet",
                description = "Inspect past and pending creator withdrawal records.",
                icon = Icons.Default.History,
                testTag = "profile_nav_withdrawal_history",
                onClick = { onNavigate(ShareDabbaDestination.WithdrawalHistory.route) }
            )

            ShareDabbaSectionCard(
                title = "Notifications",
                packagePath = "Creator & Platform Alerts",
                description = "Check upload processing, eligible view syncs, and security alerts.",
                icon = Icons.Default.Notifications,
                testTag = "profile_nav_notifications",
                onClick = { onNavigate(ShareDabbaDestination.Notifications.route) }
            )

            ShareDabbaSectionCard(
                title = "Help & Support",
                packagePath = "FAQs & Diagnostic Support",
                description = "Browse creator FAQs, copy diagnostic info, or contact ShareDabba support.",
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                testTag = "profile_nav_help_support",
                onClick = { onNavigate(ShareDabbaDestination.HelpSupport.route) }
            )

            ShareDabbaSectionCard(
                title = "Settings & Stage 2 Contract",
                packagePath = "com.example.settings",
                description = "Inspect EnvironmentConfig, Stage 2 API contract, Room cache, and AppLogger.",
                icon = Icons.Default.Settings,
                testTag = "profile_nav_settings",
                onClick = { onNavigate(ShareDabbaDestination.Settings.route) }
            )

            ShareDabbaSectionCard(
                title = "Remote Configuration",
                packagePath = "com.example.remoteconfig",
                description = "Inspect runtime streaming and AI model feature flags.",
                icon = Icons.Default.Tune,
                testTag = "profile_nav_remoteconfig",
                onClick = { onNavigate(ShareDabbaDestination.RemoteConfig.route) }
            )

            ShareDabbaSectionCard(
                title = "Advertisements & VAST",
                packagePath = "com.example.advertisements",
                description = "Inspect Media3 ad slot and companion banner architecture.",
                icon = Icons.Default.Campaign,
                testTag = "profile_nav_ads",
                onClick = { onNavigate(ShareDabbaDestination.Advertisements.route) }
            )

            ShareDabbaSectionCard(
                title = "Deep Links Router",
                packagePath = "com.example.deeplinks",
                description = "Verify https://sharedabba.com/watch/* deep link resolution.",
                icon = Icons.Default.Link,
                testTag = "profile_nav_deeplinks",
                onClick = { onNavigate(ShareDabbaDestination.DeepLinks.route) }
            )

            ShareDabbaSectionCard(
                title = "Preview Splash & Onboarding Tour",
                packagePath = "Screens 1 & 2",
                description = "Re-open the ShareDabba Splash and 3-step Onboarding experience anytime.",
                icon = Icons.Default.PlayCircle,
                testTag = "profile_nav_onboarding",
                onClick = { onNavigate(ShareDabbaDestination.Splash.route) }
            )

            OutlinedButton(
                onClick = {
                    scope.launch {
                        sessionTokenCoordinator.clearSessionMetadata()
                    }
                    signOutFromShareDabba(
                        credentialManager = credentialManager,
                        onSignOutComplete = onSignedOut,
                        scope = scope
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sign Out",
                    tint = ShareDabbaError
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Sign Out of ShareDabba", color = ShareDabbaError)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Screen 16: Notifications Screen
 */
@Composable
fun NotificationsScreen(
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    var stateMode by rememberSaveable { mutableStateOf(PreviewStateMode.DATA) }
    val notifications = remember {
        mutableStateListOf<NotificationItemFixture>().apply {
            addAll(DevFixturesCatalog.initialNotifications)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .testTag("notifications_screen"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ShareDabbaTopBar(
                title = "Notifications",
                subtitle = "Creator & Platform Alerts",
                onBackClick = onBackClick
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DevFixtureNoticeBanner()
                StateModeSelectorRow(
                    currentMode = stateMode,
                    onModeChange = { stateMode = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val updated = notifications.map { it.copy(isUnread = false) }
                            notifications.clear()
                            notifications.addAll(updated)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("notifications_mark_all_read_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShareDabbaCyan,
                            contentColor = ShareDabbaBlack
                        )
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Mark All Read")
                    }

                    OutlinedButton(
                        onClick = { notifications.clear() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("notifications_clear_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = ShareDabbaError
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Clear All", color = ShareDabbaWhite)
                    }
                }
            }
        }

        when (stateMode) {
            PreviewStateMode.LOADING -> {
                item {
                    ShareDabbaLoadingState(message = "Loading notifications...")
                }
            }

            PreviewStateMode.EMPTY -> {
                item {
                    ShareDabbaEmptyState(
                        title = "You're All Caught Up",
                        subtitle = "No new notifications at this time.",
                        actionLabel = "Restore Sample Alerts",
                        onActionClick = {
                            notifications.clear()
                            notifications.addAll(DevFixturesCatalog.initialNotifications)
                            stateMode = PreviewStateMode.DATA
                        }
                    )
                }
            }

            PreviewStateMode.ERROR -> {
                item {
                    ShareDabbaErrorState(
                        message = "Could not sync notifications feed.",
                        onRetry = { stateMode = PreviewStateMode.DATA }
                    )
                }
            }

            PreviewStateMode.DATA -> {
                if (notifications.isEmpty()) {
                    item {
                        ShareDabbaEmptyState(
                            title = "No Notifications",
                            subtitle = "All notifications have been cleared.",
                            actionLabel = "Restore Sample Alerts",
                            onActionClick = {
                                notifications.addAll(DevFixturesCatalog.initialNotifications)
                            }
                        )
                    }
                } else {
                    items(notifications, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (item.isUnread) ShareDabbaCyan else ShareDabbaOutline,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onNavigate(item.targetRoute) }
                                .testTag("notification_item_${item.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ShareDabbaGradientBadge(
                                        text = if (item.isUnread) "NEW" else "READ"
                                    )
                                    Text(
                                        text = item.timestampLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ShareDabbaTextSecondary
                                    )
                                }
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ShareDabbaWhite
                                )
                                Text(
                                    text = item.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ShareDabbaTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 19: Help and Support Screen
 */
@Composable
fun HelpSupportScreen(
    environmentConfig: EnvironmentConfig,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    var copiedFeedback by remember { mutableStateOf<String?>(null) }
    var expandedFaqId by rememberSaveable { mutableStateOf<String?>(DevFixturesCatalog.supportFaqs.first().id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .verticalScroll(rememberScrollState())
            .testTag("help_support_screen")
    ) {
        ShareDabbaTopBar(
            title = "Help & Support",
            subtitle = "Creator FAQs • Platform Diagnostics • Support",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                    ShareDabbaGradientBadge(text = "SHARE DABBA SUPPORT CENTER")
                    Text(
                        text = "Need assistance with streams, uploads, or creator payouts?",
                        style = MaterialTheme.typography.titleMedium,
                        color = ShareDabbaWhite
                    )
                    Text(
                        text = "Contact the ShareDabba team via email or copy your build diagnostic summary below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShareDabbaTextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:support@sharedabba.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "ShareDabba Android Support Request")
                                }
                                runCatching {
                                    context.startActivity(emailIntent)
                                }.onFailure {
                                    copiedFeedback = "Support email: support@sharedabba.com"
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("help_email_support_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShareDabbaCyan,
                                contentColor = ShareDabbaBlack
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Email Support")
                        }

                        OutlinedButton(
                            onClick = {
                                val report = "ShareDabba Android v${environmentConfig.versionName} (${environmentConfig.applicationId}) • API: ${environmentConfig.apiBaseUrl}"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("ShareDabba Diagnostics", report))
                                copiedFeedback = "Copied diagnostic summary to clipboard."
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("help_copy_diagnostics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = ShareDabbaPurple
                            )
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Copy Info", color = ShareDabbaWhite)
                        }
                    }

                    if (!copiedFeedback.isNullOrBlank()) {
                        Text(
                            text = copiedFeedback.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaCyan
                        )
                    }
                }
            }

            Text(
                text = "Frequently Asked Questions",
                style = MaterialTheme.typography.titleLarge,
                color = ShareDabbaWhite
            )

            DevFixturesCatalog.supportFaqs.forEach { faq ->
                FaqAccordionCard(
                    faq = faq,
                    expanded = expandedFaqId == faq.id,
                    onToggle = {
                        expandedFaqId = if (expandedFaqId == faq.id) null else faq.id
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FaqAccordionCard(
    faq: SupportFaqItem,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
            .clickable(onClick = onToggle)
            .testTag("faq_card_${faq.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.titleMedium,
                    color = ShareDabbaWhite,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse FAQ" else "Expand FAQ",
                    tint = ShareDabbaCyan
                )
            }
            if (expanded) {
                Text(
                    text = faq.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShareDabbaTextSecondary
                )
            }
        }
    }
}
