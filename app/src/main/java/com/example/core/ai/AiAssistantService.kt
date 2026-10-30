package com.example.core.ai

import android.util.Log
import com.example.core.logging.AppLogger
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "AiAssistantService"

/**
 * Provides Firebase AI Logic inference for ShareDabba:
 * 1. Low-latency streaming/quick responses using `gemini-3.1-flash-lite`.
 * 2. Deep reasoning & creator/stream architectural analysis using `gemini-4-argon`
 *    with `thinkingLevel = ThinkingLevel.HIGH` (without setting maxOutputTokens).
 */
class AiAssistantService {

    private val lowLatencyModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.1-flash-lite"
        )
    }

    private val highThinkingModel by lazy {
        val highThinkingConfig = generationConfig {
            thinkingConfig = thinkingConfig {
                thinkingLevel = ThinkingLevel.HIGH
            }
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = highThinkingConfig
        )
    }

    fun streamFastResponse(prompt: String): Flow<String> = flow {
        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            lowLatencyModel.generateContentStream(
                "You are the ShareDabba Android streaming assistant. Provide a concise, helpful response:\n$prompt"
            ).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
    }

    suspend fun generateFastSuggestions(query: String): Result<String> {
        return try {
            val response = withTimeout(60_000L) {
                lowLatencyModel.generateContent(
                    "Provide 3 concise ShareDabba video search tags or stream discovery topics for: $query"
                )
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                AppLogger.i(TAG, "Low-latency gemini-3.1-flash-lite response received.")
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Model returned an empty response."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Low-latency generation timed out", e)
            Result.failure(IllegalStateException("Generation timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Low-latency generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateDeepAnalysis(complexQuery: String): Result<String> {
        return try {
            val response = withTimeout(60_000L) {
                highThinkingModel.generateContent(
                    "Analyze this complex ShareDabba streaming or creator strategy query in depth:\n$complexQuery"
                )
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                AppLogger.i(TAG, "High-thinking gemini-4-argon response received.")
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Model returned an empty response."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "High-thinking generation timed out", e)
            Result.failure(IllegalStateException("Deep analysis timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "High-thinking generation failed", e)
            Result.failure(e)
        }
    }
}
