package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app.AppContainer
import com.example.app.ShareDabbaApplication
import com.example.authentication.AuthScreen
import com.example.authentication.authStateFlow
import com.example.core.security.configureAppCheck
import com.example.deeplinks.DeepLinkRouter
import com.example.onboarding.OnboardingScreen
import com.example.onboarding.SplashScreen
import com.example.ui.navigation.ShareDabbaMainScaffold
import com.example.ui.theme.ShareDabbaTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

private enum class UnauthGateStage {
    SPLASH,
    ONBOARDING,
    AUTH
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val initialTarget = DeepLinkRouter.extractStreamUrlFromIntent(intent)

        setContent {
            var activeDeepLinkTarget by rememberSaveable { mutableStateOf(initialTarget) }

            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { newIntent ->
                    val newTarget = DeepLinkRouter.extractStreamUrlFromIntent(newIntent)
                    if (!newTarget.isNullOrBlank()) {
                        activeDeepLinkTarget = newTarget
                    }
                }
                addOnNewIntentListener(listener)
                onDispose {
                    removeOnNewIntentListener(listener)
                }
            }

            ShareDabbaTheme {
                ShareDabbaRootApp(
                    initialDeepLinkStreamUrl = activeDeepLinkTarget
                )
            }
        }
    }
}

@Composable
fun ShareDabbaRootApp(
    auth: FirebaseAuth = Firebase.auth,
    initialDeepLinkStreamUrl: String? = null
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as? ShareDabbaApplication)?.container
        ?: AppContainer(context.applicationContext as android.app.Application)

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = auth.currentUser
    )
    val user = currentUser

    var unauthStage by rememberSaveable { mutableStateOf(UnauthGateStage.AUTH) }

    if (user == null) {
        when (unauthStage) {
            UnauthGateStage.SPLASH -> {
                SplashScreen(
                    onSplashFinished = { unauthStage = UnauthGateStage.ONBOARDING }
                )
            }

            UnauthGateStage.ONBOARDING -> {
                OnboardingScreen(
                    onOnboardingComplete = { unauthStage = UnauthGateStage.AUTH }
                )
            }

            UnauthGateStage.AUTH -> {
                AuthScreen(
                    onAuthSuccess = { /* AuthStateListener updates currentUser automatically */ },
                    onReopenOnboarding = { unauthStage = UnauthGateStage.SPLASH }
                )
            }
        }
    } else {
        ShareDabbaMainScaffold(
            container = appContainer,
            currentUser = user,
            initialDeepLinkStreamUrl = initialDeepLinkStreamUrl,
            onSignedOut = { /* AuthStateListener returns to AuthScreen */ }
        )
    }
}
