package com.example.network

import com.example.core.config.EnvironmentConfig
import com.example.core.logging.AppLogger
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Standardized network response wrapper for ShareDabba website API integration.
 * Does not assume or invent backend JSON envelopes until verified from website source code.
 */
sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class HttpError(val code: Int, val rawErrorBody: String?) : NetworkResult<Nothing>
    data class NetworkFailure(val throwable: Throwable) : NetworkResult<Nothing>
    data class UnconfiguredContract(
        val reason: String = "Awaiting ShareDabba website API documentation (Stage 2)."
    ) : NetworkResult<Nothing>
}

enum class ContractDiscoveryStatus(val label: String) {
    AWAITING_REPO_DOCS("Awaiting Website Repo / API Docs"),
    ACTIVE_FIREBASE_AUTH("Active (Firebase Auth + Google Identity)"),
    VERIFIED("Verified")
}

data class IntegrationDomainItem(
    val number: Int,
    val domain: String,
    val status: ContractDiscoveryStatus = ContractDiscoveryStatus.AWAITING_REPO_DOCS,
    val notes: String = "Pending actual ShareDabba GitHub repository or API documentation."
)

object IntegrationContractRegistry {
    const val IS_BACKEND_CONNECTION_COMPLETE: Boolean = false
    const val IS_FIREBASE_AUTH_CONNECTED: Boolean = true

    val discoveryChecklist: List<IntegrationDomainItem> = listOf(
        IntegrationDomainItem(1, "Website Framework"),
        IntegrationDomainItem(2, "Backend Framework"),
        IntegrationDomainItem(3, "Database Provider"),
        IntegrationDomainItem(
            number = 4,
            domain = "Authentication Provider",
            status = ContractDiscoveryStatus.ACTIVE_FIREBASE_AUTH,
            notes = "Firebase Auth + Google Sign-In via Jetpack CredentialManager connected; Bearer token refresh active."
        ),
        IntegrationDomainItem(
            number = 5,
            domain = "File Storage Provider",
            status = ContractDiscoveryStatus.AWAITING_REPO_DOCS,
            notes = "Authoritative storage remains on ShareDabba website. Phone-only production video storage is strictly blocked."
        ),
        IntegrationDomainItem(
            number = 6,
            domain = "Video Playback Architecture",
            status = ContractDiscoveryStatus.VERIFIED,
            notes = "Media3 ExoPlayer (MP4/HLS/DASH) connected with WebsiteVideoLibraryRepository provenance & playback URL validation."
        ),
        IntegrationDomainItem(
            number = 7,
            domain = "Existing Share-Link Format",
            status = ContractDiscoveryStatus.VERIFIED,
            notes = "Canonical website share links (`https://sharedabba.com/watch/{videoId}`) enforced via ShareDabbaShareUrlGenerator."
        ),
        IntegrationDomainItem(
            number = 8,
            domain = "Existing API Endpoints",
            status = ContractDiscoveryStatus.AWAITING_REPO_DOCS,
            notes = "Missing website upload endpoints explicitly reported in Uploads module without inventing a second upload system."
        ),
        IntegrationDomainItem(9, "Admin Panel Architecture"),
        IntegrationDomainItem(10, "Advertising Integration"),
        IntegrationDomainItem(11, "Wallet & Earnings Architecture")
    )
}

/**
 * OkHttp Interceptor that protects authenticated API calls by attaching the current user's
 * Firebase ID token (`Authorization: Bearer <idToken>`) and current UID header (`X-ShareDabba-Uid`).
 */
class AuthHeaderInterceptor(
    private val tokenProvider: () -> String? = {
        try {
            val user = Firebase.auth.currentUser
            if (user != null) {
                runBlocking { user.getIdToken(false).await().token }
            } else {
                null
            }
        } catch (e: Exception) {
            AppLogger.w("AuthHeaderInterceptor", "Could not retrieve auth token for request", e)
            null
        }
    },
    private val uidProvider: () -> String? = {
        try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        }
    }
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenProvider()
        val uid = uidProvider()

        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "application/json")

        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        if (!uid.isNullOrBlank()) {
            requestBuilder.header("X-ShareDabba-Uid", uid)
        }

        return chain.proceed(requestBuilder.build())
    }
}

/**
 * Stage 5 Expired Token Authenticator:
 * Automatically intercepts `401 Unauthorized` responses from protected backend API calls,
 * forces a token refresh (`getIdToken(true)`), and retries the request once with the new Bearer token.
 */
class TokenRefreshAuthenticator(
    private val forceRefreshTokenProvider: () -> String? = {
        try {
            val user = Firebase.auth.currentUser
            if (user != null) {
                runBlocking { user.getIdToken(true).await().token }
            } else {
                null
            }
        } catch (e: Exception) {
            AppLogger.e("TokenRefreshAuthenticator", "Forced token refresh failed on 401", e)
            null
        }
    }
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) {
            AppLogger.w("TokenRefreshAuthenticator", "Giving up after 2 authentication retry attempts.")
            return null
        }
        val refreshedToken = forceRefreshTokenProvider()
        if (refreshedToken.isNullOrBlank()) {
            return null
        }
        AppLogger.i("TokenRefreshAuthenticator", "Recovered from 401 expired token via forced refresh.")
        return response.request.newBuilder()
            .header("Authorization", "Bearer $refreshedToken")
            .header("X-Token-Refreshed", "true")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}

/**
 * Retrofit API contract interface for the ShareDabba website backend.
 * Intentionally contains zero guessed or invented endpoint paths in Stage 2
 * until the website repository or API documentation is provided.
 */
interface ShareDabbaApiService

object NetworkClient {
    private const val TAG = "NetworkClient"

    fun createApiService(config: EnvironmentConfig): ShareDabbaApiService {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            AppLogger.d(TAG, message)
        }.apply {
            level = if (config.isDebug) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(AuthHeaderInterceptor())
            .authenticator(TokenRefreshAuthenticator())
            .addInterceptor(loggingInterceptor)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(config.apiBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ShareDabbaApiService::class.java)
    }
}
