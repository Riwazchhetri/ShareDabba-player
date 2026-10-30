package com.example.onboarding

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.theme.ShareDabbaAccentGradient
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaPurple
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWhite
import kotlinx.coroutines.delay

/**
 * Screen 1: Splash Screen
 * Displays ShareDabba brand identity, version badge, and auto-advances or allows instant continue.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(900L)
        onSplashFinished()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("splash_screen"),
        color = ShareDabbaBlack
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(ShareDabbaSurface)
                        .border(2.dp, ShareDabbaAccentGradient, RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_sharedabba_logo),
                        contentDescription = "ShareDabba Logo",
                        modifier = Modifier.size(72.dp)
                    )
                }

                ShareDabbaGradientBadge(text = "SHARE DABBA STREAMING")

                Text(
                    text = "ShareDabba",
                    style = MaterialTheme.typography.displayLarge,
                    color = ShareDabbaWhite
                )

                Text(
                    text = "High-Performance Android Streaming & Creator Hub",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShareDabbaTextSecondary,
                    textAlign = TextAlign.Center
                )

                CircularProgressIndicator(
                    color = ShareDabbaCyan,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp
                )

                TextButton(
                    onClick = onSplashFinished,
                    modifier = Modifier.testTag("splash_continue_button")
                ) {
                    Text(
                        text = "Continue Now",
                        color = ShareDabbaCyan,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

private data class OnboardingStep(
    val badge: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

/**
 * Screen 2: Onboarding Screen
 * Interactive 3-step onboarding flow with Back, Next, Skip, and Get Started actions.
 */
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    val steps = listOf(
        OnboardingStep(
            badge = "STEP 1 OF 3 • NATIVE PLAYBACK",
            title = "Media3 ExoPlayer Streaming",
            description = "Watch Featured, Popular, and Live creator streams with adaptive bitrate playback and local Room resume caching.",
            icon = Icons.Default.PlayCircle
        ),
        OnboardingStep(
            badge = "STEP 2 OF 3 • CREATOR ECONOMY",
            title = "Transparent Creator Dashboard",
            description = "Monitor your uploads, recorded views, eligible views, earnings, and unified ShareDabba website wallet status.",
            icon = Icons.Default.MonetizationOn
        ),
        OnboardingStep(
            badge = "STEP 3 OF 3 • AI & CLOUD SYNC",
            title = "Instant AI Search & Cloud Bookmarks",
            description = "Discover streams with low-latency gemini-3.1-flash-lite, run deep creator analysis with gemini-4-argon, and sync bookmarks across devices.",
            icon = Icons.Default.AutoAwesome
        )
    )

    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[currentIndex]

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        color = ShareDabbaBlack
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShareDabbaGradientBadge(text = step.badge)
                    TextButton(
                        onClick = onOnboardingComplete,
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text("Skip", color = ShareDabbaTextSecondary)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, ShareDabbaAccentGradient, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(ShareDabbaBlack)
                                .border(1.dp, ShareDabbaCyan, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = step.title,
                                tint = ShareDabbaCyan,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = ShareDabbaWhite,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ShareDabbaTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Step Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentIndex) 24.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentIndex) ShareDabbaCyan else ShareDabbaPurple.copy(alpha = 0.4f)
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentIndex > 0) {
                        OutlinedButton(
                            onClick = { currentIndex -= 1 },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("onboarding_prev_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Previous", color = ShareDabbaWhite)
                        }
                    }

                    Button(
                        onClick = {
                            if (currentIndex < steps.lastIndex) {
                                currentIndex += 1
                            } else {
                                onOnboardingComplete()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("onboarding_next_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShareDabbaCyan,
                            contentColor = ShareDabbaBlack
                        )
                    ) {
                        Text(
                            text = if (currentIndex < steps.lastIndex) "Next" else "Get Started",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaBlack
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}
