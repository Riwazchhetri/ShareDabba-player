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

    private fun record(level: String, tag: String, message: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = LogEntry(timestamp = time, level = level, tag = tag, message = message)
        _recentLogs.value = (listOf(entry) + _recentLogs.value).take(MAX_LOGS)
    }

    fun d(tag: String, message: String) {
        try {
            Log.d(tag, message)
        } catch (_: Throwable) {
        }
        record("DEBUG", tag, message)
    }

    fun i(tag: String, message: String) {
        try {
            Log.i(tag, message)
        } catch (_: Throwable) {
        }
        record("INFO", tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        try {
            Log.w(tag, message, throwable)
        } catch (_: Throwable) {
        }
        record("WARN", tag, if (throwable != null) "$message (${throwable.message})" else message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        try {
            Log.e(tag, message, throwable)
        } catch (_: Throwable) {
        }
        record("ERROR", tag, if (throwable != null) "$message (${throwable.message})" else message)
    }
}
