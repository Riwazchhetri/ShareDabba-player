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
import androidx.core.util.Consumer
import com.example.core.security.configureAppCheck
import com.example.guest.GuestPlaybackScreen
import com.example.ui.theme.ShareDabbaTheme

/**
 * ShareDabba's public player entry point.
 *
 * Guest playback is intentionally independent of Firebase Authentication:
 * installing/opening the player must never require a Firebase user session.
 * This also keeps the app launchable when optional Firebase client config is
 * absent from a local/debug build.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // App Check is optional for guest playback. Its initializer handles
        // missing Firebase configuration internally and must not block launch.
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val initialLink = intent?.dataString
        setContent {
            var activeLink by rememberSaveable { mutableStateOf(initialLink) }

            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { newIntent ->
                    newIntent.dataString
                        ?.takeIf { it.isNotBlank() }
                        ?.let { activeLink = it }
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
fun ShareDabbaRootApp(initialDeepLink: String? = null) {
    // Keep the launcher focused on the agreed no-login, paste-a-link player.
    // Do not touch FirebaseAuth or construct the signed-in dashboard at startup.
    GuestPlaybackScreen(initialLink = initialDeepLink)
}
