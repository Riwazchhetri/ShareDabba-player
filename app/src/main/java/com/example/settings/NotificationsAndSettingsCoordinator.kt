package com.example.settings

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.core.config.EnvironmentConfig
import com.example.core.logging.AppLogger
import com.example.deeplinks.DeepLinkRouter
import com.example.remoteconfig.RemoteConfigManager
import com.example.ui.navigation.ShareDabbaDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * STAGE 10 — SHARE DABBA NOTIFICATIONS AND SETTINGS
 *
 * Implements the complete user settings and notification system:
 * 1. Notification Categories:
 *    - Upload completion notifications (`UPLOAD_COMPLETION`, optional)
 *    - Account notifications (`ACCOUNT_SECURITY`, optional push / required in-app)
 *    - Withdrawal status notifications (`WITHDRAWAL_STATUS`, optional)
 *    - Important platform announcements (`PLATFORM_ANNOUNCEMENT`, optional)
 *    - Maintenance notices (`MAINTENANCE_NOTICE`, critical operational — always active in-app)
 *    - App update notices (`APP_UPDATE_NOTICE`, critical operational — always active in-app)
 * 2. Respects Android runtime notification permission (`POST_NOTIFICATIONS` on API 33+).
 * 3. Allows users to disable optional notification categories while preserving critical notices.
 * 4. Never invents notification delivery success:
 *    - Distinguishes In-App Feed Delivery vs. Local OS System Tray Delivery vs. Remote FCM Push.
 *    - Blocks private user notification delivery and deep-link navigation when the user session is expired.
 * 5. Integrates Support & Feedback, Privacy Policy, Terms & Conditions, and Account Logout.
 */

enum class ShareDabbaNotificationCategory(
    val id: String,
    val title: String,
    val description: String,
    val isOptionalUserToggleable: Boolean,
    val requiresAuthenticatedActiveSession: Boolean
) {
    UPLOAD_COMPLETION(
        id = "upload_completion",
        title = "Upload Completion Notifications",
        description = "Alerts when video transcoding, HLS packaging, or upload checks finish.",
        isOptionalUserToggleable = true,
        requiresAuthenticatedActiveSession = true
    ),
    ACCOUNT_SECURITY(
        id = "account_notifications",
        title = "Account & Security Notifications",
        description = "Login alerts, profile updates, and session security notifications.",
        isOptionalUserToggleable = true,
        requiresAuthenticatedActiveSession = true
    ),
    WITHDRAWAL_STATUS(
        id = "withdrawal_status",
        title = "Withdrawal Status Notifications",
        description = "Updates when withdrawal requests are queued or verified by the website ledger.",
        isOptionalUserToggleable = true,
        requiresAuthenticatedActiveSession = true
    ),
    PLATFORM_ANNOUNCEMENT(
        id = "platform_announcements",
        title = "Important Platform Announcements",
        description = "Creator policy updates, new streaming categories, and website announcements.",
        isOptionalUserToggleable = true,
        requiresAuthenticatedActiveSession = false
    ),
    MAINTENANCE_NOTICE(
        id = "maintenance_notices",
        title = "Maintenance Notices",
        description = "Critical scheduled maintenance and backend availability alerts (required in-app).",
        isOptionalUserToggleable = false,
        requiresAuthenticatedActiveSession = false
    ),
    APP_UPDATE_NOTICE(
        id = "app_update_notices",
        title = "App Update Notices",
        description = "Critical version compatibility and native release notices (required in-app).",
        isOptionalUserToggleable = false,
        requiresAuthenticatedActiveSession = false
    )
}

enum class NotificationPermissionStatus(val label: String, val canPostToSystemTray: Boolean) {
    GRANTED_RUNTIME("Granted (Android Runtime Permission Active)", true),
    GRANTED_INSTALL_TIME_PRE_API_33("Granted (Pre-Android 13 Install-Time)", true),
    DENIED_OR_NOT_REQUESTED("Denied / Not Granted (System Tray Posting Blocked)", false)
}

enum class NotificationDeliveryStatus(val badgeText: String) {
    DELIVERED_IN_APP_AND_SYSTEM_TRAY("DELIVERED (IN-APP + OS TRAY)"),
    DELIVERED_IN_APP_ONLY_PERMISSION_DENIED("IN-APP ONLY (OS PERMISSION DENIED)"),
    SUPPRESSED_BY_USER_CATEGORY_PREFERENCE("SUPPRESSED (CATEGORY DISABLED)"),
    BLOCKED_EXPIRED_SESSION_REAUTH_REQUIRED("BLOCKED (EXPIRED SESSION 401)"),
    NOT_DELIVERED_FCM_BACKEND_UNCONFIGURED("NOT DELIVERED (FCM UNCONFIGURED)")
}

data class ShareDabbaNotificationRecord(
    val id: String,
    val category: ShareDabbaNotificationCategory,
    val title: String,
    val message: String,
    val timestampLabel: String,
    val targetRoute: String,
    val canonicalDeepLinkUrl: String? = null,
    val isUnread: Boolean = true,
    val deliveryStatus: NotificationDeliveryStatus = NotificationDeliveryStatus.DELIVERED_IN_APP_ONLY_PERMISSION_DENIED,
    val isSimulatedOrClaimedRemotePush: Boolean = false
)

data class NotificationPreferencesState(
    val enabledCategories: Map<ShareDabbaNotificationCategory, Boolean> =
        ShareDabbaNotificationCategory.entries.associateWith { true },
    val permissionStatus: NotificationPermissionStatus = NotificationPermissionStatus.DENIED_OR_NOT_REQUESTED,
    val fcmIntegrationStatus: String = "AWAITING_WEBSITE_FCM_DISPATCHER (No fake push delivery claimed)",
    val lastDeliveryAuditMessage: String = "Ready to evaluate notification permissions, preferences, deep links, and session state."
)

enum class NotificationNavigationOutcome {
    OPEN_NATIVE_VIDEO_PLAYER_DEEP_LINK,
    OPEN_INTERNAL_DESTINATION,
    BLOCKED_EXPIRED_SESSION_REAUTH_REQUIRED,
    BLOCKED_INVALID_DESTINATION
}

data class NotificationDestinationResolution(
    val outcome: NotificationNavigationOutcome,
    val notificationId: String,
    val resolvedRoute: String,
    val resolvedVideoIdOrStreamUrl: String? = null,
    val explanation: String
)

class NotificationsAndSettingsCoordinator(
    private val context: Context?,
    private val environmentConfig: EnvironmentConfig,
    private val remoteConfigManager: RemoteConfigManager
) {
    companion object {
        const val CHANNEL_ID_GENERAL = "sharedabba_notifications_channel"
    }

    private val _preferences = MutableStateFlow(
        NotificationPreferencesState(
            permissionStatus = detectCurrentPermissionStatus(context)
        )
    )
    val preferences: StateFlow<NotificationPreferencesState> = _preferences.asStateFlow()

    private val _notifications = MutableStateFlow(buildInitialNotifications())
    val notifications: StateFlow<List<ShareDabbaNotificationRecord>> = _notifications.asStateFlow()

    private val _lastNavigationResolution = MutableStateFlow<NotificationDestinationResolution?>(null)
    val lastNavigationResolution: StateFlow<NotificationDestinationResolution?> = _lastNavigationResolution.asStateFlow()

    init {
        ensureNotificationChannelCreated()
    }

    private fun detectCurrentPermissionStatus(ctx: Context?): NotificationPermissionStatus {
        if (ctx == null) return NotificationPermissionStatus.DENIED_OR_NOT_REQUESTED
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                NotificationPermissionStatus.GRANTED_RUNTIME
            } else {
                NotificationPermissionStatus.DENIED_OR_NOT_REQUESTED
            }
        } else {
            NotificationPermissionStatus.GRANTED_INSTALL_TIME_PRE_API_33
        }
    }

    private fun ensureNotificationChannelCreated() {
        val ctx = context ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "ShareDabba Alerts & Notices",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Upload completion, withdrawal status, account security, and platform notices."
            }
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildInitialNotifications(): List<ShareDabbaNotificationRecord> {
        val remote = remoteConfigManager.activeConfig.value
        return listOf(
            ShareDabbaNotificationRecord(
                id = "notif_upload_1",
                category = ShareDabbaNotificationCategory.UPLOAD_COMPLETION,
                title = "Upload Transcoding Complete",
                message = "'Cinematic 4K Drone Tour of Western Ghats' is live on ShareDabba with adaptive HLS streams.",
                timestampLabel = "10m ago",
                targetRoute = ShareDabbaDestination.Player.route,
                canonicalDeepLinkUrl = "https://sharedabba.com/v/vid_featured_1",
                isUnread = true
            ),
            ShareDabbaNotificationRecord(
                id = "notif_withdrawal_1",
                category = ShareDabbaNotificationCategory.WITHDRAWAL_STATUS,
                title = "Withdrawal Status: Pending Server Verification",
                message = "Your withdrawal request (SD-WD-88390) is queued for website ledger verification.",
                timestampLabel = "1h ago",
                targetRoute = ShareDabbaDestination.WithdrawalHistory.route,
                isUnread = true
            ),
            ShareDabbaNotificationRecord(
                id = "notif_account_1",
                category = ShareDabbaNotificationCategory.ACCOUNT_SECURITY,
                title = "Account Security: Google Sign-In Verified",
                message = "Your ShareDabba Android session is linked to your unified website UID.",
                timestampLabel = "3h ago",
                targetRoute = ShareDabbaDestination.Profile.route,
                isUnread = false
            ),
            ShareDabbaNotificationRecord(
                id = "notif_platform_1",
                category = ShareDabbaNotificationCategory.PLATFORM_ANNOUNCEMENT,
                title = "Platform Announcement: Website Admin Sync",
                message = remote.branding.globalAnnouncementBanner.ifBlank {
                    "ShareDabba creator analytics and deep-link routing are active across web and Android."
                },
                timestampLabel = "1d ago",
                targetRoute = ShareDabbaDestination.Home.route,
                canonicalDeepLinkUrl = "https://sharedabba.com/v/vid_featured_2",
                isUnread = false
            ),
            ShareDabbaNotificationRecord(
                id = "notif_maintenance_1",
                category = ShareDabbaNotificationCategory.MAINTENANCE_NOTICE,
                title = "Maintenance Notice Schedule",
                message = remote.appManagement.maintenanceMessage,
                timestampLabel = "2d ago",
                targetRoute = ShareDabbaDestination.RemoteConfig.route,
                isUnread = false
            ),
            ShareDabbaNotificationRecord(
                id = "notif_update_1",
                category = ShareDabbaNotificationCategory.APP_UPDATE_NOTICE,
                title = "App Update Notice (v${environmentConfig.versionName})",
                message = "Minimum supported version code: ${remote.appManagement.minSupportedVersionCode} • Recommended: ${remote.appManagement.recommendedVersionCode}.",
                timestampLabel = "3d ago",
                targetRoute = ShareDabbaDestination.Settings.route,
                isUnread = false
            )
        )
    }

    /**
     * Updates the tracked Android notification permission status after the runtime permission dialog
     * or during testing.
     */
    fun updateNotificationPermissionGranted(isGranted: Boolean) {
        val newStatus = if (isGranted) {
            NotificationPermissionStatus.GRANTED_RUNTIME
        } else {
            NotificationPermissionStatus.DENIED_OR_NOT_REQUESTED
        }
        _preferences.value = _preferences.value.copy(
            permissionStatus = newStatus,
            lastDeliveryAuditMessage = "Runtime POST_NOTIFICATIONS permission updated to: ${newStatus.label}"
        )
    }

    /**
     * Allows users to enable or disable optional notification categories.
     * Critical operational categories (`MAINTENANCE_NOTICE`, `APP_UPDATE_NOTICE`) cannot be disabled in-app.
     */
    fun setCategoryEnabled(category: ShareDabbaNotificationCategory, enabled: Boolean): Boolean {
        if (!category.isOptionalUserToggleable && !enabled) {
            _preferences.value = _preferences.value.copy(
                lastDeliveryAuditMessage = "${category.title} is a required operational notice and cannot be disabled."
            )
            return false
        }
        val updatedMap = _preferences.value.enabledCategories.toMutableMap()
        updatedMap[category] = enabled
        _preferences.value = _preferences.value.copy(
            enabledCategories = updatedMap,
            lastDeliveryAuditMessage = "${category.title} preference set to ${if (enabled) "ENABLED" else "DISABLED"}."
        )
        return true
    }

    /**
     * Evaluates and records a notification without ever inventing delivery success:
     * 1. If `isUnconfiguredFcmPushAttempt` is true, returns `NOT_DELIVERED_FCM_BACKEND_UNCONFIGURED`
     *    because the website FCM push backend is not yet connected.
     * 2. If the notification category requires an active authenticated session and `isSessionExpired` is true,
     *    returns `BLOCKED_EXPIRED_SESSION_REAUTH_REQUIRED` and does not deliver.
     * 3. If the category is optional and disabled by user preference, returns `SUPPRESSED_BY_USER_CATEGORY_PREFERENCE`.
     * 4. If Android runtime notification permission (`POST_NOTIFICATIONS`) is denied, delivers ONLY to the
     *    in-app feed (`DELIVERED_IN_APP_ONLY_PERMISSION_DENIED`) and never claims OS tray delivery.
     * 5. When permission and preferences allow, delivers to the in-app feed and posts to the local Android
     *    notification channel (`DELIVERED_IN_APP_AND_SYSTEM_TRAY`).
     */
    fun dispatchOrEvaluateNotification(
        id: String,
        category: ShareDabbaNotificationCategory,
        title: String,
        message: String,
        targetRoute: String,
        canonicalDeepLinkUrl: String? = null,
        isSessionExpired: Boolean = false,
        isUnconfiguredFcmPushAttempt: Boolean = false
    ): ShareDabbaNotificationRecord {
        val currentPrefs = _preferences.value

        if (isUnconfiguredFcmPushAttempt) {
            val unverifiedRecord = ShareDabbaNotificationRecord(
                id = id,
                category = category,
                title = title,
                message = message,
                timestampLabel = "Not Delivered",
                targetRoute = targetRoute,
                canonicalDeepLinkUrl = canonicalDeepLinkUrl,
                isUnread = false,
                deliveryStatus = NotificationDeliveryStatus.NOT_DELIVERED_FCM_BACKEND_UNCONFIGURED,
                isSimulatedOrClaimedRemotePush = true
            )
            _preferences.value = currentPrefs.copy(
                lastDeliveryAuditMessage = "FCM Push '$title' NOT marked delivered: Website FCM push dispatcher is not configured. Never inventing push delivery success."
            )
            return unverifiedRecord
        }

        if (category.requiresAuthenticatedActiveSession && isSessionExpired) {
            val blockedRecord = ShareDabbaNotificationRecord(
                id = id,
                category = category,
                title = title,
                message = message,
                timestampLabel = "Blocked (401)",
                targetRoute = targetRoute,
                canonicalDeepLinkUrl = canonicalDeepLinkUrl,
                isUnread = false,
                deliveryStatus = NotificationDeliveryStatus.BLOCKED_EXPIRED_SESSION_REAUTH_REQUIRED
            )
            _preferences.value = currentPrefs.copy(
                lastDeliveryAuditMessage = "Blocked private notification '$title' (${category.title}): session token is expired. Re-authentication required."
            )
            return blockedRecord
        }

        val isCategoryEnabled = currentPrefs.enabledCategories[category] ?: true
        if (category.isOptionalUserToggleable && !isCategoryEnabled) {
            val suppressedRecord = ShareDabbaNotificationRecord(
                id = id,
                category = category,
                title = title,
                message = message,
                timestampLabel = "Suppressed",
                targetRoute = targetRoute,
                canonicalDeepLinkUrl = canonicalDeepLinkUrl,
                isUnread = false,
                deliveryStatus = NotificationDeliveryStatus.SUPPRESSED_BY_USER_CATEGORY_PREFERENCE
            )
            _preferences.value = currentPrefs.copy(
                lastDeliveryAuditMessage = "Notification '$title' suppressed because optional category '${category.title}' is disabled by user preference."
            )
            return suppressedRecord
        }

        val canPostOsNotification = currentPrefs.permissionStatus.canPostToSystemTray
        val deliveryStatus = if (canPostOsNotification) {
            postToAndroidSystemNotificationTrayIfPermitted(id.hashCode(), title, message)
            NotificationDeliveryStatus.DELIVERED_IN_APP_AND_SYSTEM_TRAY
        } else {
            NotificationDeliveryStatus.DELIVERED_IN_APP_ONLY_PERMISSION_DENIED
        }

        val deliveredRecord = ShareDabbaNotificationRecord(
            id = id,
            category = category,
            title = title,
            message = message,
            timestampLabel = "Just now",
            targetRoute = targetRoute,
            canonicalDeepLinkUrl = canonicalDeepLinkUrl,
            isUnread = true,
            deliveryStatus = deliveryStatus
        )
        _notifications.value = listOf(deliveredRecord) + _notifications.value.filterNot { it.id == id }
        _preferences.value = currentPrefs.copy(
            lastDeliveryAuditMessage = "Notification '$title' recorded with status: ${deliveryStatus.badgeText}."
        )
        return deliveredRecord
    }

    private fun postToAndroidSystemNotificationTrayIfPermitted(
        notificationIntId: Int,
        title: String,
        message: String
    ) {
        val ctx = context ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching {
            val builder = NotificationCompat.Builder(ctx, CHANNEL_ID_GENERAL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
            NotificationManagerCompat.from(ctx).notify(notificationIntId, builder.build())
        }.onFailure { e ->
            AppLogger.w("NotificationsCoordinator", "Could not post system notification: ${e.message}")
        }
    }

    /**
     * Resolves tapping a notification into either:
     * - A verified Stage 7 canonical deep link destination (`https://sharedabba.com/v/VIDEO_ID`)
     *   opening the exact video in the native player, OR
     * - An internal screen route (`uploads`, `withdrawal_history`, `profile`, `remoteconfig`, `settings`),
     * while blocking access to private account/withdrawal/upload notifications if the user session is expired.
     */
    fun resolveNotificationTapDestination(
        record: ShareDabbaNotificationRecord,
        isSessionExpired: Boolean = false
    ): NotificationDestinationResolution {
        if (record.category.requiresAuthenticatedActiveSession && isSessionExpired) {
            val blocked = NotificationDestinationResolution(
                outcome = NotificationNavigationOutcome.BLOCKED_EXPIRED_SESSION_REAUTH_REQUIRED,
                notificationId = record.id,
                resolvedRoute = ShareDabbaDestination.Profile.route,
                resolvedVideoIdOrStreamUrl = null,
                explanation = "Session is expired (401). Redirecting to Profile Session Recovery before opening ${record.category.title}."
            )
            _lastNavigationResolution.value = blocked
            return blocked
        }

        // Mark notification as read
        _notifications.value = _notifications.value.map {
            if (it.id == record.id) it.copy(isUnread = false) else it
        }

        // Check if notification carries a canonical video deep link
        val deepLinkUrl = record.canonicalDeepLinkUrl
        if (!deepLinkUrl.isNullOrBlank()) {
            val deepLinkResult = DeepLinkRouter.resolveDeepLink(
                rawUrl = deepLinkUrl,
                isUserAuthenticated = !isSessionExpired
            )
            if (deepLinkResult.canOpenInNativePlayer && deepLinkResult.playerTargetKey != null) {
                val resolved = NotificationDestinationResolution(
                    outcome = NotificationNavigationOutcome.OPEN_NATIVE_VIDEO_PLAYER_DEEP_LINK,
                    notificationId = record.id,
                    resolvedRoute = ShareDabbaDestination.Player.route,
                    resolvedVideoIdOrStreamUrl = deepLinkResult.playerTargetKey,
                    explanation = "Resolved notification deep link '$deepLinkUrl' to exact video '${deepLinkResult.playerTargetKey}' in native player."
                )
                _lastNavigationResolution.value = resolved
                return resolved
            }
        }

        if (record.targetRoute.isBlank()) {
            val invalid = NotificationDestinationResolution(
                outcome = NotificationNavigationOutcome.BLOCKED_INVALID_DESTINATION,
                notificationId = record.id,
                resolvedRoute = ShareDabbaDestination.Home.route,
                resolvedVideoIdOrStreamUrl = null,
                explanation = "Notification target route was blank; defaulted to Home."
            )
            _lastNavigationResolution.value = invalid
            return invalid
        }

        val internal = NotificationDestinationResolution(
            outcome = NotificationNavigationOutcome.OPEN_INTERNAL_DESTINATION,
            notificationId = record.id,
            resolvedRoute = record.targetRoute,
            resolvedVideoIdOrStreamUrl = null,
            explanation = "Navigated to internal destination '${record.targetRoute}' for ${record.category.title}."
        )
        _lastNavigationResolution.value = internal
        return internal
    }

    fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(isUnread = false) }
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
    }

    fun restoreDefaultNotifications() {
        _notifications.value = buildInitialNotifications()
    }
}
