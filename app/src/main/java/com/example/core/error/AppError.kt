package com.example.core.error

/**
 * Unified domain and UI error hierarchy for ShareDabba.
 */
sealed class AppError(open val userMessage: String, open val cause: Throwable? = null) {
    data class NetworkError(
        override val userMessage: String = "Unable to reach ShareDabba API endpoint.",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)

    data class AuthError(
        override val userMessage: String = "Authentication required to perform this action.",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)

    data class DatabaseError(
        override val userMessage: String = "Database synchronization error.",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)

    data class AiError(
        override val userMessage: String = "AI inference failed.",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)
}

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
