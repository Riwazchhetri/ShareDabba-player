package com.example.authentication

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.theme.ShareDabbaAccentGradient
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaError
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaPurple
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWhite
import kotlinx.coroutines.launch

/**
 * Screens 3 & 4: Login Screen and Registration Screen (Google Sign-In via CredentialManager).
 * Connected to Firebase Authentication for Stage 5 unified user identity, session refresh,
 * and account recovery.
 */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onReopenOnboarding: (() -> Unit)? = null
) {
    var showRegistrationMode by rememberSaveable { mutableStateOf(false) }

    if (showRegistrationMode) {
        RegistrationScreen(
            onAuthSuccess = onAuthSuccess,
            onSwitchToLogin = { showRegistrationMode = false }
        )
    } else {
        LoginScreenContent(
            onAuthSuccess = onAuthSuccess,
            onSwitchToRegistration = { showRegistrationMode = true },
            onReopenOnboarding = onReopenOnboarding
        )
    }
}

@Composable
fun LoginScreenContent(
    onAuthSuccess: () -> Unit,
    onSwitchToRegistration: () -> Unit,
    onReopenOnboarding: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val sessionCoordinator = remember(context) { SessionTokenCoordinator(context.applicationContext) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRecoveryPanel by rememberSaveable { mutableStateOf(false) }
    var recoveryEmail by rememberSaveable { mutableStateOf("") }
    var recoveryFeedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { /* Stay on interactive sign-in screen */ },
            scope = coroutineScope
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("auth_screen"),
        color = ShareDabbaBlack
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(ShareDabbaSurface)
                        .border(1.5.dp, ShareDabbaAccentGradient, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_sharedabba_logo),
                        contentDescription = "ShareDabba Logo",
                        modifier = Modifier.size(60.dp)
                    )
                }

                ShareDabbaGradientBadge(text = "STAGE 5 • UNIFIED AUTHENTICATION")

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.displayLarge,
                        color = ShareDabbaWhite
                    )
                    Text(
                        text = stringResource(id = R.string.brand_tagline),
                        style = MaterialTheme.typography.labelLarge,
                        color = ShareDabbaCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.auth_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShareDabbaTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ShareDabbaOutline, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = ShareDabbaCyan
                            )
                            Text(
                                text = "Same User Identity Across Web & Android",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = ShareDabbaCyan
                            )
                            Text(
                                text = "Google Sign-In via Credential Manager • Zero Plain-Text Passwords • Auto Token Refresh",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_error_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = ShareDabbaError.copy(alpha = 0.16f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ShareDabbaError,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = coroutineScope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShareDabbaCyan,
                        contentColor = ShareDabbaBlack
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = ShareDabbaBlack,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Google Sign-In",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(id = R.string.sign_in_with_google),
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaBlack
                        )
                    }
                }

                OutlinedButton(
                    onClick = onSwitchToRegistration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("switch_to_registration_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = ShareDabbaCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New to ShareDabba? Create Creator Account", color = ShareDabbaWhite)
                }

                // Account & Password Recovery Support (Stage 5)
                TextButton(
                    onClick = { showRecoveryPanel = !showRecoveryPanel },
                    modifier = Modifier.testTag("toggle_account_recovery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        tint = ShareDabbaCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showRecoveryPanel) "Hide Account & Password Recovery" else "Account & Password Recovery",
                        color = ShareDabbaCyan
                    )
                }

                if (showRecoveryPanel) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                            .testTag("account_recovery_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Account & Password Recovery",
                                style = MaterialTheme.typography.titleSmall,
                                color = ShareDabbaWhite
                            )
                            Text(
                                text = "ShareDabba authenticates via Google Identity (OAuth2) without storing passwords. You can manage your Google security settings or request a Firebase recovery link if your web account has linked email recovery.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ShareDabbaTextSecondary
                            )
                            OutlinedTextField(
                                value = recoveryEmail,
                                onValueChange = { recoveryEmail = it },
                                label = { Text("Account Email Address") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recovery_email_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val res = sessionCoordinator.sendAccountPasswordResetEmail(recoveryEmail)
                                            recoveryFeedback = res.fold(
                                                onSuccess = { it },
                                                onFailure = { "Recovery status: ${it.localizedMessage}" }
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("send_recovery_email_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ShareDabbaCyan,
                                        contentColor = ShareDabbaBlack
                                    )
                                ) {
                                    Text("Send Recovery Link")
                                }
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            val browserIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://myaccount.google.com/security")
                                            ).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(browserIntent)
                                        }.onFailure {
                                            recoveryFeedback = "Manage Google security at https://myaccount.google.com/security"
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("google_security_portal_button")
                                ) {
                                    Text("Google Security", color = ShareDabbaWhite)
                                }
                            }
                            if (!recoveryFeedback.isNullOrBlank()) {
                                Text(
                                    text = recoveryFeedback.orEmpty(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ShareDabbaCyan
                                )
                            }
                        }
                    }
                }

                if (onReopenOnboarding != null) {
                    TextButton(
                        onClick = onReopenOnboarding,
                        modifier = Modifier.testTag("reopen_onboarding_button")
                    ) {
                        Text("View Platform Onboarding Tour", color = ShareDabbaTextSecondary)
                    }
                }
            }
        }
    }
}

/**
 * Screen 4: Registration Screen
 * Provides new-creator registration through Google Sign-In via CredentialManager
 * using the same Firebase Auth backend as Login.
 */
@Composable
fun RegistrationScreen(
    onAuthSuccess: () -> Unit,
    onSwitchToLogin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("registration_screen"),
        color = ShareDabbaBlack
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ShareDabbaGradientBadge(text = "SCREEN 4 • CREATOR REGISTRATION")

                Text(
                    text = "Join ShareDabba",
                    style = MaterialTheme.typography.displayLarge,
                    color = ShareDabbaWhite
                )

                Text(
                    text = "ShareDabba uses unified Google Identity registration on the same Firebase Auth backend. Selecting your Google account registers your creator identity and synchronizes your cloud workspace.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShareDabbaTextSecondary,
                    textAlign = TextAlign.Center
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ShareDabbaOutline, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RegistrationBenefitRow("Same User UID Across ShareDabba Web & Android")
                        RegistrationBenefitRow("Zero-Password Security via Google Credential Manager")
                        RegistrationBenefitRow("Protected API Bearer Token & Cross-User Isolation")
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShareDabbaError
                    )
                }

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = coroutineScope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("register_with_google_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShareDabbaPurple,
                        contentColor = ShareDabbaWhite
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = ShareDabbaWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Register with Google",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Register & Continue with Google",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaWhite
                        )
                    }
                }

                TextButton(
                    onClick = onSwitchToLogin,
                    modifier = Modifier.testTag("switch_to_login_button")
                ) {
                    Text("Already have an account? Back to Login", color = ShareDabbaCyan)
                }
            }
        }
    }
}

@Composable
private fun RegistrationBenefitRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = ShareDabbaCyan,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = ShareDabbaWhite
        )
    }
}
