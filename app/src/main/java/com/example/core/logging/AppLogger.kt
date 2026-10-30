package com.example.core.logging

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: String,
    val level: String,
    val tag: String,
    val message: String
)

/**
 * Centralized structured logger and diagnostic ring buffer for ShareDabba.
 */
object AppLogger {
    private const val MAX_LOGS = 50
    private val _recentLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val recentLogs: StateFlow<List<LogEntry>> = _recentLogs.asStateFlow()

    private val bearerRegex = Regex("(?i)Bearer\\s+[A-Za-z0-9._~+/=-]+")
    private val jwtRegex = Regex("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")
    private val keyValueSecretRegex = Regex(
        "(?i)(password|passwd|secret|api_key|apikey|client_secret|private_key|payout_secret|token)\\s*([=:])\\s*([^\\s,;\"']+)"
    )

    /**
     * Stage 11 Security Control — No Sensitive Data in Logs:
     * Deterministically scrubs Bearer tokens, JWTs, passwords, API keys, and payment secrets
     * before writing to Android Logcat or storing in the in-app diagnostic ring buffer.
     */
    fun redactSensitiveData(raw: String): String {
        if (raw.isEmpty()) return raw
        return raw
            .replace(bearerRegex, "Bearer [REDACTED_TOKEN]")
            .replace(jwtRegex, "[REDACTED_JWT]")
            .replace(keyValueSecretRegex) { matchResult ->
                val key = matchResult.groupValues[1]
                val separator = matchResult.groupValues[2]
                "$key$separator[REDACTED_SECRET]"
            }
    }

    private fun record(level: String, tag: String, message: String) {
        val safeMessage = redactSensitiveData(message)
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = LogEntry(timestamp = time, level = level, tag = tag, message = safeMessage)
        _recentLogs.value = (listOf(entry) + _recentLogs.value).take(MAX_LOGS)
    }

    fun d(tag: String, message: String) {
        val safeMessage = redactSensitiveData(message)
        try {
            Log.d(tag, safeMessage)
        } catch (_: Throwable) {
        }
        record("DEBUG", tag, safeMessage)
    }

    fun i(tag: String, message: String) {
        val safeMessage = redactSensitiveData(message)
        try {
            Log.i(tag, safeMessage)
        } catch (_: Throwable) {
        }
        record("INFO", tag, safeMessage)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val combined = if (throwable != null) "$message (${throwable.message})" else message
        val safeMessage = redactSensitiveData(combined)
        try {
            Log.w(tag, safeMessage)
        } catch (_: Throwable) {
        }
        record("WARN", tag, safeMessage)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val combined = if (throwable != null) "$message (${throwable.message})" else message
        val safeMessage = redactSensitiveData(combined)
        try {
            Log.e(tag, safeMessage)
        } catch (_: Throwable) {
        }
        record("ERROR", tag, safeMessage)
    }
}
