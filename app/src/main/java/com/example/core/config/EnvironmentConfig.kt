package com.example.core.config

import com.example.BuildConfig

/**
 * Environment configuration for the ShareDabba Android application.
 * Keeps website endpoints, application ID, and build variant flags centralized.
 */
data class EnvironmentConfig(
    val applicationId: String = BuildConfig.APPLICATION_ID,
    val versionName: String = BuildConfig.VERSION_NAME,
    val isDebug: Boolean = BuildConfig.DEBUG,
    val websiteBaseUrl: String = BuildConfig.SHAREDABBA_WEB_BASE_URL,
    val apiBaseUrl: String = BuildConfig.SHAREDABBA_API_BASE_URL,
    val lowLatencyAiModel: String = "gemini-3.1-flash-lite",
    val highThinkingAiModel: String = "gemini-4-argon"
)
