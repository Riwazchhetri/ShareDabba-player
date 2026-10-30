package com.example.authentication

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.logging.AppLogger
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

private val Context.authSessionDataStore by preferencesDataStore(name = "sharedabba_secure_session_meta")

/**
 * Represents the current authentication & session token health state for Stage 5.
 * Never stores plain-text passwords, service-account credentials, or raw admin secrets.
 */
data class SessionAuthStatus(
    val isAuthenticated: Boolean,
    val uid: String?,
    val email: String?,
    val displayName: String?,
    val tokenFingerprintSha256: String?,
    val tokenExpirationEpochSec: Long,
    val isTokenExpired: Boolean,
    val lastRefreshedLabel: String,
    val providerId: String = "google.com (CredentialManager + FirebaseAuth)",
    val stage2BackendAuthNote: String = "Connected to Firebase Auth + Google Identity. Ready to exchange Bearer ID tokens with ShareDabba website API."
)

/**
 * Enforces cross-user isolation on the Android client in addition to Firestore security rules.
 * Prevents any component from accessing or mutating another user's private data.
 */
object UserAccessGuard {
    fun requireMatchingUser(
        authenticatedUid: String?,
        targetOwnerUid: String
    ) {
        if (authenticatedUid.isNullOrBlank()) {
            throw SecurityException("Unauthenticated access denied: user must be logged in.")
        }
        if (authenticatedUid != targetOwnerUid) {
            throw SecurityException(
                "Cross-user access denied: authenticated user ($authenticatedUid) cannot access private data belonging to ($targetOwnerUid)."
            )
        }
    }

    fun isAuthorizedForUser(
        authenticatedUid: String?,
        targetOwnerUid: String
    ): Boolean {
        return !authenticatedUid.isNullOrBlank() && authenticatedUid == targetOwnerUid
    }
}

/**
 * Manages session token lifecycle, forced token refresh, expired token recovery,
 * profile updates, password/account recovery, and non-sensitive session metadata persistence.
 *
 * Security guarantees:
 * - Zero plain-text passwords stored.
 * - Zero service-account or admin secrets bundled.
 * - Only stores SHA-256 fingerprint of the active ID token and expiry epoch in DataStore.
 */
class SessionTokenCoordinator(
    private val context: Context? = null,
    private val firebaseAuthProvider: () -> FirebaseAuth = { Firebase.auth }
) {
    companion object {
        private const val TAG = "SessionCoordinator"
        private val KEY_LAST_UID = stringPreferencesKey("last_authenticated_uid")
        private val KEY_TOKEN_FINGERPRINT = stringPreferencesKey("token_sha256_fingerprint")
        private val KEY_TOKEN_EXPIRY_SEC = longPreferencesKey("token_expiry_epoch_sec")

        fun sha256Fingerprint(token: String): String {
            if (token.isBlank()) return ""
            val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
            return digest.take(8).joinToString("") { "%02x".format(it) }
        }

        fun isTokenExpiredAt(expirationEpochSec: Long, nowEpochSec: Long): Boolean {
            return expirationEpochSec <= nowEpochSec
        }
    }

    private val _sessionStatus = MutableStateFlow(
        SessionAuthStatus(
            isAuthenticated = false,
            uid = null,
            email = null,
            displayName = null,
            tokenFingerprintSha256 = null,
            tokenExpirationEpochSec = 0L,
            isTokenExpired = false,
            lastRefreshedLabel = "Not initialized"
        )
    )
    val sessionStatus: StateFlow<SessionAuthStatus> = _sessionStatus.asStateFlow()

    val storedTokenFingerprintFlow: Flow<String?>? = context?.authSessionDataStore?.data?.map { prefs ->
        prefs[KEY_TOKEN_FINGERPRINT]
    }

    /**
     * Inspects or refreshes the current FirebaseUser's ID token.
     * When [forceRefresh] is true, forces a network token refresh from Firebase Auth to recover
     * from an expired or revoked token.
     */
    suspend fun syncOrRefreshSession(forceRefresh: Boolean = false): Result<SessionAuthStatus> {
        return try {
            val user: FirebaseUser = firebaseAuthProvider().currentUser
                ?: return Result.failure(IllegalStateException("No authenticated user session."))

            val tokenResult = user.getIdToken(forceRefresh).await()
            val rawToken = tokenResult.token.orEmpty()
            val expirySec = tokenResult.expirationTimestamp
            val nowSec = System.currentTimeMillis() / 1000L
            val fingerprint = sha256Fingerprint(rawToken)
            val expired = isTokenExpiredAt(expirySec, nowSec)

            context?.authSessionDataStore?.edit { prefs ->
                prefs[KEY_LAST_UID] = user.uid
                prefs[KEY_TOKEN_FINGERPRINT] = fingerprint
                prefs[KEY_TOKEN_EXPIRY_SEC] = expirySec
            }

            val updated = SessionAuthStatus(
                isAuthenticated = true,
                uid = user.uid,
                email = user.email,
                displayName = user.displayName,
                tokenFingerprintSha256 = fingerprint,
                tokenExpirationEpochSec = expirySec,
                isTokenExpired = expired,
                lastRefreshedLabel = if (forceRefresh) {
                    "Force-refreshed ID token (SHA-256: $fingerprint)"
                } else {
                    "Verified active ID token (SHA-256: $fingerprint)"
                }
            )
            _sessionStatus.value = updated
            AppLogger.i(TAG, updated.lastRefreshedLabel)
            Result.success(updated)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to refresh session token", e)
            Result.failure(e)
        }
    }

    /**
     * Simulates an expired session token state for Stage 5 Test #6 ("Expire the session and verify recovery"),
     * allowing the user or test suite to verify that expired sessions are flagged and recovered via forceRefresh.
     */
    fun simulateExpiredSessionForTesting() {
        val current = _sessionStatus.value
        val pastExpirySec = (System.currentTimeMillis() / 1000L) - 3600L
        _sessionStatus.value = current.copy(
            tokenExpirationEpochSec = pastExpirySec,
            isTokenExpired = true,
            lastRefreshedLabel = "Session token expired (401). Triggering automatic or manual token refresh recovery."
        )
        AppLogger.w(TAG, "Simulated expired session token for Stage 5 recovery verification.")
    }

    /**
     * Updates the authenticated user's profile display name on the shared Firebase Auth identity
     * (Stage 5 Test #3: "Update profile").
     */
    suspend fun updateAuthenticatedUserProfile(newDisplayName: String): Result<SessionAuthStatus> {
        val trimmed = newDisplayName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Display name cannot be blank."))
        }
        return try {
            val user = firebaseAuthProvider().currentUser
                ?: return Result.failure(IllegalStateException("Must be logged in to update profile."))

            val request = userProfileChangeRequest {
                displayName = trimmed
            }
            user.updateProfile(request).await()
            user.reload().await()
            AppLogger.i(TAG, "Updated Firebase user profile displayName to '$trimmed'")
            syncOrRefreshSession(forceRefresh = true)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Profile update failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sends a password reset email via Firebase Auth if the account has an email address
     * and supports email recovery. Never handles or stores passwords on the client.
     */
    suspend fun sendAccountPasswordResetEmail(email: String): Result<String> {
        val trimmed = email.trim()
        if (trimmed.isEmpty() || !trimmed.contains("@")) {
            return Result.failure(IllegalArgumentException("Enter a valid account email address."))
        }
        return try {
            firebaseAuthProvider().sendPasswordResetEmail(trimmed).await()
            val msg = "Password reset instructions sent to $trimmed (if registered with email recovery)."
            AppLogger.i(TAG, msg)
            Result.success(msg)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Password reset request returned error", e)
            Result.failure(e)
        }
    }

    /**
     * Clears persisted session metadata upon logout.
     */
    suspend fun clearSessionMetadata() {
        context?.authSessionDataStore?.edit { it.clear() }
        _sessionStatus.value = SessionAuthStatus(
            isAuthenticated = false,
            uid = null,
            email = null,
            displayName = null,
            tokenFingerprintSha256 = null,
            tokenExpirationEpochSec = 0L,
            isTokenExpired = false,
            lastRefreshedLabel = "Signed out & cleared session metadata"
        )
    }
}
