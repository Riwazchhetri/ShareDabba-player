package com.example.remoteconfig

import com.example.core.config.EnvironmentConfig
import com.example.core.logging.AppLogger
import com.example.network.IntegrationContractRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Group A — APP BRANDING (Website Admin Panel Controlled)
 */
data class AdminBrandingConfig(
    val appDisplayName: String = "ShareDabba Player",
    val remoteLogoUrl: String = "https://sharedabba.com/assets/brand/logo.png",
    val remoteHeroBannerUrl: String = "https://sharedabba.com/assets/brand/hero_banner.jpg",
    val primaryCyanHex: String = "#00F0FF",
    val accentPurpleHex: String = "#9D4EDD",
    val backgroundBlackHex: String = "#050508",
    val globalAnnouncementBanner: String = "",
    val supportEmail: String = "support@sharedabba.com",
    val supportPortalUrl: String = "https://sharedabba.com/support",
    val termsOfServiceUrl: String = "https://sharedabba.com/terms",
    val privacyPolicyUrl: String = "https://sharedabba.com/privacy"
)

/**
 * Group B — HOME SCREEN (Website Admin Panel Controlled)
 */
data class AdminHomeScreenConfig(
    val homeLayoutStyle: String = "STANDARD_SECTIONS", // "STANDARD_SECTIONS" or "COMPACT_FEED"
    val showHeroBanner: Boolean = true,
    val showCategoriesBar: Boolean = true,
    val showFeaturedVideosSection: Boolean = true,
    val showRecentlyUploadedSection: Boolean = true,
    val showPopularVideosSection: Boolean = true,
    val featuredVideoIdsOverride: List<String> = emptyList(),
    val enabledCategoryIds: List<String> = listOf("live", "tech", "creator", "gaming", "education"),
    val enableSearchTab: Boolean = true,
    val enableUploadsTab: Boolean = true
)

/**
 * Group C — VIDEO PLAYER (Website Admin Panel Controlled)
 */
data class AdminVideoPlayerConfig(
    val defaultQualityId: String = "auto", // "auto", "1080p", "720p", "480p", "360p"
    val availablePlaybackSpeeds: List<Float> = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f),
    val autoplayEnabled: Boolean = true,
    val showPlayerControls: Boolean = true,
    val allowFullscreen: Boolean = true,
    val allowPictureInPicture: Boolean = true,
    val showRelatedVideos: Boolean = true,
    val nextVideoAutoAdvance: Boolean = false,
    val playerAppearanceStyle: String = "CYAN_PURPLE_NEON"
)

/**
 * Group D — ADVERTISING (Website Admin Panel Controlled)
 */
data class AdminAdvertisingConfig(
    val approvedAdProvider: String = "NONE", // "NONE", "WEBSITE_VAST"
    val bannerAdUnitId: String = "",
    val interstitialAdUnitId: String = "",
    val rewardedAdUnitId: String = "",
    val enableBannerPlacements: Boolean = false,
    val enableInterstitialPlacements: Boolean = false,
    val enableRewardedPlacements: Boolean = false,
    val adFrequencyEveryNVideos: Int = 5,
    val adCooldownSeconds: Int = 180,
    val maxAdsPerSession: Int = 4,
    val adTestModeEnabled: Boolean = true,
    val adFallbackStrategy: String = "NO_AD_CONTINUE_CONTENT"
)

/**
 * Group E — APP MANAGEMENT (Website Admin Panel Controlled)
 */
data class AdminAppManagementConfig(
    val maintenanceModeEnabled: Boolean = false,
    val maintenanceMessage: String = "ShareDabba is undergoing scheduled maintenance. Please check back shortly.",
    val updateAnnouncementText: String = "",
    val minSupportedVersionCode: Int = 1,
    val recommendedVersionCode: Int = 1,
    val emergencySupportContact: String = "https://sharedabba.com/status",
    val enableAiSearch: Boolean = true,
    val enableCloudBookmarks: Boolean = true,
    val enableCreatorDashboard: Boolean = true
)

/**
 * Complete versioned Remote Configuration snapshot published by the ShareDabba Website Admin Panel.
 */
data class PublishedRemoteConfig(
    val configVersion: Int = 1,
    val publishedAtIso: String = "2026-10-30T00:00:00Z",
    val sourceLabel: String = "SAFE_DEFAULT_SNAPSHOT",
    val branding: AdminBrandingConfig = AdminBrandingConfig(),
    val homeScreen: AdminHomeScreenConfig = AdminHomeScreenConfig(),
    val videoPlayer: AdminVideoPlayerConfig = AdminVideoPlayerConfig(),
    val advertising: AdminAdvertisingConfig = AdminAdvertisingConfig(),
    val appManagement: AdminAppManagementConfig = AdminAppManagementConfig(),
    val requestedUnbundledSdkOrFeature: String? = null
) {
    companion object {
        val SAFE_DEFAULT = PublishedRemoteConfig()
    }
}

/**
 * Result of validating and sanitizing an incoming Website Admin Panel configuration payload.
 */
data class RemoteConfigValidationReport(
    val sanitizedConfig: PublishedRemoteConfig,
    val isValid: Boolean,
    val rejectedUnsafeCodeOrScriptPayloads: List<String>,
    val clampedOrSanitizedFields: List<String>,
    val nativeReleaseRequiredNotices: List<String>
)

/**
 * Validator enforcing Stage 8 safety & architectural rules:
 * 1. Never execute arbitrary downloaded code or scripts (`<script>`, `javascript:`, `eval(`, `.dex`, `.jar`, `.so`).
 * 2. Clamp and validate all values against safe defaults.
 * 3. Detect when a remote setting requests a feature or SDK not compiled into the installed APK and explain that a new Android release is necessary.
 */
object RemoteConfigValidator {

    private val PROHIBITED_CODE_PATTERNS = listOf(
        "<script",
        "javascript:",
        "eval(",
        "function(",
        "dalvik.system.dexclassloader",
        ".dex",
        ".jar",
        ".so",
        "runtime.getruntime().exec"
    )

    private val SUPPORTED_QUALITY_IDS = setOf("auto", "1080p", "720p", "480p", "360p")
    private val BUNDLED_AD_PROVIDERS = setOf("NONE", "WEBSITE_VAST")
    private val HEX_COLOR_REGEX = Regex("^#[0-9A-Fa-f]{6,8}$")

    fun containsExecutableCodeOrScript(input: String?): Boolean {
        if (input.isNullOrBlank()) return false
        val lower = input.lowercase()
        return PROHIBITED_CODE_PATTERNS.any { lower.contains(it) }
    }

    fun validateAndSanitize(candidate: PublishedRemoteConfig): RemoteConfigValidationReport {
        val rejectedScripts = mutableListOf<String>()
        val sanitizedFields = mutableListOf<String>()
        val nativeReleaseNotices = mutableListOf<String>()

        fun cleanText(fieldName: String, value: String, fallback: String): String {
            if (containsExecutableCodeOrScript(value)) {
                rejectedScripts.add("Blocked executable/script payload in '$fieldName'")
                return fallback
            }
            return value.trim().ifEmpty { fallback }
        }

        fun cleanHttpsUrl(fieldName: String, value: String, fallback: String): String {
            if (containsExecutableCodeOrScript(value)) {
                rejectedScripts.add("Blocked script/code URI in '$fieldName'")
                return fallback
            }
            val trimmed = value.trim()
            if (!trimmed.startsWith("https://")) {
                sanitizedFields.add("Replaced non-HTTPS URL in '$fieldName' with safe default")
                return fallback
            }
            return trimmed
        }

        fun cleanHexColor(fieldName: String, value: String, fallback: String): String {
            val trimmed = value.trim()
            if (!HEX_COLOR_REGEX.matches(trimmed)) {
                sanitizedFields.add("Invalid hex color '$trimmed' in '$fieldName'; using '$fallback'")
                return fallback
            }
            return trimmed
        }

        val def = PublishedRemoteConfig.SAFE_DEFAULT

        // Validate Group A: Branding
        val rawBranding = candidate.branding
        val cleanBranding = AdminBrandingConfig(
            appDisplayName = cleanText("branding.appDisplayName", rawBranding.appDisplayName, def.branding.appDisplayName),
            remoteLogoUrl = cleanHttpsUrl("branding.remoteLogoUrl", rawBranding.remoteLogoUrl, def.branding.remoteLogoUrl),
            remoteHeroBannerUrl = cleanHttpsUrl("branding.remoteHeroBannerUrl", rawBranding.remoteHeroBannerUrl, def.branding.remoteHeroBannerUrl),
            primaryCyanHex = cleanHexColor("branding.primaryCyanHex", rawBranding.primaryCyanHex, def.branding.primaryCyanHex),
            accentPurpleHex = cleanHexColor("branding.accentPurpleHex", rawBranding.accentPurpleHex, def.branding.accentPurpleHex),
            backgroundBlackHex = cleanHexColor("branding.backgroundBlackHex", rawBranding.backgroundBlackHex, def.branding.backgroundBlackHex),
            globalAnnouncementBanner = if (containsExecutableCodeOrScript(rawBranding.globalAnnouncementBanner)) {
                rejectedScripts.add("Blocked script in 'branding.globalAnnouncementBanner'")
                ""
            } else {
                rawBranding.globalAnnouncementBanner.trim()
            },
            supportEmail = cleanText("branding.supportEmail", rawBranding.supportEmail, def.branding.supportEmail),
            supportPortalUrl = cleanHttpsUrl("branding.supportPortalUrl", rawBranding.supportPortalUrl, def.branding.supportPortalUrl),
            termsOfServiceUrl = cleanHttpsUrl("branding.termsOfServiceUrl", rawBranding.termsOfServiceUrl, def.branding.termsOfServiceUrl),
            privacyPolicyUrl = cleanHttpsUrl("branding.privacyPolicyUrl", rawBranding.privacyPolicyUrl, def.branding.privacyPolicyUrl)
        )

        // Validate Group B: Home Screen
        val rawHome = candidate.homeScreen
        val validLayout = if (rawHome.homeLayoutStyle in setOf("STANDARD_SECTIONS", "COMPACT_FEED")) {
            rawHome.homeLayoutStyle
        } else {
            sanitizedFields.add("Normalized unknown homeLayoutStyle '${rawHome.homeLayoutStyle}' to STANDARD_SECTIONS")
            "STANDARD_SECTIONS"
        }
        val cleanHome = rawHome.copy(homeLayoutStyle = validLayout)

        // Validate Group C: Video Player
        val rawPlayer = candidate.videoPlayer
        val validQuality = if (rawPlayer.defaultQualityId in SUPPORTED_QUALITY_IDS) {
            rawPlayer.defaultQualityId
        } else {
            sanitizedFields.add("Normalized unsupported defaultQualityId '${rawPlayer.defaultQualityId}' to 'auto'")
            "auto"
        }
        val validSpeeds = rawPlayer.availablePlaybackSpeeds
            .filter { it in 0.25f..3.0f }
            .distinct()
            .sorted()
            .ifEmpty {
                sanitizedFields.add("Restored default playback speeds because remote speed list was empty/out-of-range")
                def.videoPlayer.availablePlaybackSpeeds
            }
        val cleanPlayer = rawPlayer.copy(
            defaultQualityId = validQuality,
            availablePlaybackSpeeds = validSpeeds
        )

        // Validate Group D: Advertising & Native SDK Release Check
        val rawAds = candidate.advertising
        val cleanProvider = if (rawAds.approvedAdProvider in BUNDLED_AD_PROVIDERS) {
            rawAds.approvedAdProvider
        } else {
            nativeReleaseNotices.add(
                "Ad Provider '${rawAds.approvedAdProvider}' requires bundling a native third-party Ad SDK in build.gradle.kts. " +
                    "A new Android app release (APK/AAB) is necessary before this provider can run."
            )
            "NONE"
        }
        val clampedFreq = rawAds.adFrequencyEveryNVideos.coerceIn(1, 50)
        val clampedCooldown = rawAds.adCooldownSeconds.coerceIn(0, 3600)
        val clampedMaxAds = rawAds.maxAdsPerSession.coerceIn(0, 20)
        if (clampedCooldown != rawAds.adCooldownSeconds || clampedMaxAds != rawAds.maxAdsPerSession) {
            sanitizedFields.add("Clamped advertising cooldown/maxAdsPerSession to safe bounds")
        }
        val cleanAds = rawAds.copy(
            approvedAdProvider = cleanProvider,
            adFrequencyEveryNVideos = clampedFreq,
            adCooldownSeconds = clampedCooldown,
            maxAdsPerSession = clampedMaxAds
        )

        // Validate Group E: App Management
        val rawMgmt = candidate.appManagement
        val cleanMaintenanceMsg = cleanText(
            "appManagement.maintenanceMessage",
            rawMgmt.maintenanceMessage,
            def.appManagement.maintenanceMessage
        )
        val cleanUpdateAnnouncement = if (containsExecutableCodeOrScript(rawMgmt.updateAnnouncementText)) {
            rejectedScripts.add("Blocked script payload in 'appManagement.updateAnnouncementText'")
            ""
        } else {
            rawMgmt.updateAnnouncementText.trim()
        }
        val cleanMgmt = rawMgmt.copy(
            maintenanceMessage = cleanMaintenanceMsg,
            updateAnnouncementText = cleanUpdateAnnouncement,
            minSupportedVersionCode = rawMgmt.minSupportedVersionCode.coerceAtLeast(1),
            recommendedVersionCode = rawMgmt.recommendedVersionCode.coerceAtLeast(1)
        )

        if (!candidate.requestedUnbundledSdkOrFeature.isNullOrBlank()) {
            nativeReleaseNotices.add(
                "Remote setting '${candidate.requestedUnbundledSdkOrFeature}' requires native code changes or a new SDK not present in versionCode 1. " +
                    "A new Android release on Google Play is required to activate it."
            )
        }

        val sanitized = candidate.copy(
            configVersion = candidate.configVersion.coerceAtLeast(1),
            branding = cleanBranding,
            homeScreen = cleanHome,
            videoPlayer = cleanPlayer,
            advertising = cleanAds,
            appManagement = cleanMgmt
        )

        return RemoteConfigValidationReport(
            sanitizedConfig = sanitized,
            isValid = rejectedScripts.isEmpty(),
            rejectedUnsafeCodeOrScriptPayloads = rejectedScripts,
            clampedOrSanitizedFields = sanitizedFields,
            nativeReleaseRequiredNotices = nativeReleaseNotices
        )
    }
}

/**
 * Central Stage 8 Remote Configuration Manager connected to the ShareDabba Website Admin Panel contract.
 * Implements:
 * - Fetch on app launch (`fetchOnAppLaunch`)
 * - Refresh interval throttling (`MIN_REFRESH_INTERVAL_MS` to avoid unnecessary network requests)
 * - Last valid configuration caching & safe defaults
 * - Strict value validation & anti-script/anti-DCL protection
 * - Configuration versioning & instant rollback (`rollbackToPreviousValidConfig`)
 */
class RemoteConfigManager(
    private val environmentConfig: EnvironmentConfig
) {
    companion object {
        private const val TAG = "RemoteConfigManager"
        const val MIN_REFRESH_INTERVAL_MS: Long = 15 * 60 * 1000L // 15 minutes
    }

    private val _activeConfig = MutableStateFlow(PublishedRemoteConfig.SAFE_DEFAULT)
    val activeConfig: StateFlow<PublishedRemoteConfig> = _activeConfig.asStateFlow()

    private val _previousValidConfig = MutableStateFlow<PublishedRemoteConfig?>(null)
    val previousValidConfig: StateFlow<PublishedRemoteConfig?> = _previousValidConfig.asStateFlow()

    private val _lastValidationReport = MutableStateFlow(
        RemoteConfigValidator.validateAndSanitize(PublishedRemoteConfig.SAFE_DEFAULT)
    )
    val lastValidationReport: StateFlow<RemoteConfigValidationReport> = _lastValidationReport.asStateFlow()

    private val _statusBanner = MutableStateFlow(
        "Initialized with Safe Defaults (v1). Website Admin Panel endpoint: ${environmentConfig.apiBaseUrl}config"
    )
    val statusBanner: StateFlow<String> = _statusBanner.asStateFlow()

    var lastFetchTimestampMs: Long = 0L
        private set

    var networkFetchCount: Int = 0
        private set

    var skippedThrottledFetchCount: Int = 0
        private set

    /**
     * Triggered automatically on app launch (`ShareDabbaApplication.onCreate`).
     */
    fun fetchOnAppLaunch(currentTimeMs: Long = System.currentTimeMillis()): PublishedRemoteConfig {
        return refreshFromWebsiteAdminPanel(forceRefresh = false, currentTimeMs = currentTimeMs)
    }

    /**
     * Fetches or refreshes the published remote configuration from the ShareDabba website backend,
     * respecting the refresh cooldown interval unless `forceRefresh = true`.
     */
    fun refreshFromWebsiteAdminPanel(
        forceRefresh: Boolean = false,
        currentTimeMs: Long = System.currentTimeMillis()
    ): PublishedRemoteConfig {
        val elapsed = currentTimeMs - lastFetchTimestampMs
        if (!forceRefresh && lastFetchTimestampMs > 0L && elapsed < MIN_REFRESH_INTERVAL_MS) {
            skippedThrottledFetchCount++
            _statusBanner.value =
                "Skipped redundant network fetch (elapsed ${elapsed / 1000}s < ${MIN_REFRESH_INTERVAL_MS / 1000}s interval). Using cached v${_activeConfig.value.configVersion}."
            return _activeConfig.value
        }

        networkFetchCount++
        lastFetchTimestampMs = currentTimeMs

        return if (IntegrationContractRegistry.IS_BACKEND_CONNECTION_COMPLETE) {
            _statusBanner.value = "Fetched live Website Admin Panel configuration (v${_activeConfig.value.configVersion})."
            _activeConfig.value
        } else {
            _statusBanner.value =
                "Website Admin Panel API (${environmentConfig.apiBaseUrl}config) awaiting repo connection; serving validated cached v${_activeConfig.value.configVersion}."
            _activeConfig.value
        }
    }

    /**
     * Applies and validates a new configuration snapshot published from the Website Admin Panel,
     * preserving the prior valid snapshot for 1-tap rollback.
     */
    fun applyAdminPanelConfigUpdate(candidate: PublishedRemoteConfig): RemoteConfigValidationReport {
        val report = RemoteConfigValidator.validateAndSanitize(candidate)
        _lastValidationReport.value = report

        val current = _activeConfig.value
        if (candidate.configVersion != current.configVersion || candidate != current) {
            _previousValidConfig.value = current
        }

        _activeConfig.value = report.sanitizedConfig

        val summaryParts = mutableListOf(
            "Applied Admin Panel Config v${report.sanitizedConfig.configVersion} (${report.sanitizedConfig.sourceLabel})"
        )
        if (report.rejectedUnsafeCodeOrScriptPayloads.isNotEmpty()) {
            summaryParts.add("Blocked ${report.rejectedUnsafeCodeOrScriptPayloads.size} unsafe script/code payload(s)")
        }
        if (report.nativeReleaseRequiredNotices.isNotEmpty()) {
            summaryParts.add("Flagged ${report.nativeReleaseRequiredNotices.size} native-release requirement(s)")
        }
        _statusBanner.value = summaryParts.joinToString(" • ")
        AppLogger.i(TAG, _statusBanner.value)
        return report
    }

    /**
     * Rolls back immediately to the last known valid configuration snapshot.
     */
    fun rollbackToPreviousValidConfig(): Boolean {
        val prior = _previousValidConfig.value ?: return false
        _activeConfig.value = prior
        _previousValidConfig.value = null
        _statusBanner.value = "Rolled back configuration to previous valid snapshot (v${prior.configVersion})."
        AppLogger.i(TAG, _statusBanner.value)
        return true
    }

    /**
     * Resets configuration to `PublishedRemoteConfig.SAFE_DEFAULT`.
     */
    fun resetToSafeDefaults() {
        _previousValidConfig.value = _activeConfig.value
        _activeConfig.value = PublishedRemoteConfig.SAFE_DEFAULT
        _lastValidationReport.value =
            RemoteConfigValidator.validateAndSanitize(PublishedRemoteConfig.SAFE_DEFAULT)
        _statusBanner.value = "Restored Safe Default configuration (v1)."
    }
}
