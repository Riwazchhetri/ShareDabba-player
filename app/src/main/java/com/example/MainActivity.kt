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
import com.example.authentication.authStateFlow
import com.example.core.security.configureAppCheck
import com.example.guest.GuestPlaybackScreen
import com.example.ui.navigation.ShareDabbaMainScaffold
import com.example.ui.theme.ShareDabbaTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        val initialLink = intent?.dataString

        setContent {
            var activeLink by rememberSaveable { mutableStateOf(initialLink) }
            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { newIntent ->
                    newIntent.dataString?.takeIf { it.isNotBlank() }?.let { activeLink = it }
                }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }
            ShareDabbaTheme {
                ShareDabbaRootApp(initialDeepLink = activeLink)
            }
        }
    }
}

@Composable
fun ShareDabbaRootApp(
    auth: FirebaseAuth = Firebase.auth,
    initialDeepLink: String? = null
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as? ShareDabbaApplication)?.container
        ?: AppContainer(context.applicationContext as android.app.Application)
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    if (currentUser == null) {
        GuestPlaybackScreen(initialLink = initialDeepLink)
    } else {
        ShareDabbaMainScaffold(
            container = appContainer,
            currentUser = currentUser!!,
            initialDeepLinkStreamUrl = initialDeepLink,
            onSignedOut = { }
        )
    }
}
