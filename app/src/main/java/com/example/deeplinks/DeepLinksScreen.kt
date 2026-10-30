package com.example.deeplinks

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.common.ShareDabbaSectionCard
import com.example.ui.common.ShareDabbaTopBar
import com.example.ui.theme.ShareDabbaAccentGradient
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaError
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaPurple
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWarning
import com.example.ui.theme.ShareDabbaWhite

@Composable
fun DeepLinksScreen(
    initialDeepLink: String?,
    onOpenInPlayer: (String) -> Unit,
    onBackClick: () -> Unit = {}
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    var testDeepLink by rememberSaveable {
        mutableStateOf(
            initialDeepLink ?: "https://sharedabba.com/v/vid_featured_2?utm_source=whatsapp&utm_medium=chat"
        )
    }
    var simulateAuthenticatedUser by rememberSaveable { mutableStateOf(true) }
    var actionFeedback by rememberSaveable { mutableStateOf<String?>(null) }

    val resolvedResult = remember(testDeepLink, simulateAuthenticatedUser) {
        DeepLinkRouter.resolveDeepLink(
            rawUrl = testDeepLink,
            isUserAuthenticated = simulateAuthenticatedUser,
            currentUserId = if (simulateAuthenticatedUser) "usr_sharedabba_creator" else null
        )
    }

    val landingModel = resolvedResult.landingPageModel

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .verticalScroll(rememberScrollState())
            .testTag("deeplinks_screen")
    ) {
        ShareDabbaTopBar(
            title = "Deep Links & App Links",
            subtitle = "Stage 7 • Canonical /v/VIDEO_ID & Landing Fallback",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShareDabbaSectionCard(
                title = "Registered Manifest App Links & Schemes",
                packagePath = "https://sharedabba.com/v/* • /watch/* • sharedabba://v/*",
                description = "Configured with android:autoVerify=\"true\" for HTTPS routes and custom scheme fallback. Candidate route (/v/VIDEO_ID) remains configurable pending website repo verification.",
                icon = Icons.Default.Link,
                testTag = "deeplinks_manifest_card"
            )

            // Cross-App Test Presets (WhatsApp, Telegram, Chrome, Landing Page, Private, Deleted)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Test Links from Supported Apps",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaWhite
                        )
                        ShareDabbaGradientBadge(text = resolvedResult.parsedLink.detectedSourceApp.label)
                    }

                    LazyRow(
                        contentPadding = PaddingValues(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(DeepLinkRouter.testPresets, key = { it.id }) { preset ->
                            FilterChip(
                                selected = testDeepLink == preset.sampleUrl,
                                onClick = {
                                    testDeepLink = preset.sampleUrl
                                    actionFeedback = "Loaded ${preset.label}: ${preset.description}"
                                },
                                label = { Text(preset.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ShareDabbaCyan,
                                    selectedLabelColor = ShareDabbaBlack
                                ),
                                modifier = Modifier.testTag("deeplink_preset_${preset.id}")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = testDeepLink,
                        onValueChange = { testDeepLink = it },
                        label = { Text("Incoming ShareDabba Link or URI") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deeplink_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Authentication-Aware Access Check",
                                style = MaterialTheme.typography.labelLarge,
                                color = ShareDabbaWhite
                            )
                            Text(
                                text = if (simulateAuthenticatedUser) {
                                    "Authenticated Session Active (Bearer Token attached)"
                                } else {
                                    "Unauthenticated Guest Mode (Private videos require login)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = ShareDabbaTextSecondary
                            )
                        }
                        Switch(
                            checked = simulateAuthenticatedUser,
                            onCheckedChange = { simulateAuthenticatedUser = it },
                            modifier = Modifier.testTag("deeplink_auth_toggle")
                        )
                    }

                    // Parsed & Resolved Diagnostic Summary
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ShareDabbaBlack, RoundedCornerShape(12.dp))
                            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Resolution Status: ${resolvedResult.status.badge}",
                            style = MaterialTheme.typography.labelLarge,
                            color = when (resolvedResult.status) {
                                DeepLinkResolutionStatus.RESOLVED_READY_FOR_PLAYER -> ShareDabbaCyan
                                DeepLinkResolutionStatus.PRIVATE_VIDEO_AUTH_REQUIRED -> ShareDabbaWarning
                                else -> ShareDabbaError
                            }
                        )
                        Text(
                            text = "Extracted Video ID: ${resolvedResult.parsedLink.extractedVideoId ?: "None"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaWhite
                        )
                        Text(
                            text = "Matched Route Pattern: ${resolvedResult.parsedLink.matchedRoutePattern}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaTextSecondary
                        )
                        Text(
                            text = "Detected Source App: ${resolvedResult.parsedLink.detectedSourceApp.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaTextSecondary
                        )
                        Text(
                            text = resolvedResult.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = ShareDabbaWhite
                        )
                    }

                    // Case 1 & Case 3: Open Exact Video in Native Player + Browser Fallback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val target = resolvedResult.playerTargetKey
                                if (target != null) {
                                    onOpenInPlayer(target)
                                } else {
                                    actionFeedback =
                                        "Cannot open in Native Player (${resolvedResult.status.badge}). Using browser fallback is recommended."
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("route_deeplink_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShareDabbaCyan,
                                contentColor = ShareDabbaBlack
                            )
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Exact Video")
                        }

                        OutlinedButton(
                            onClick = {
                                val fallbackUrl = resolvedResult.parsedLink.browserFallbackUrl
                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(browserIntent)
                                    actionFeedback = "Opened browser fallback: $fallbackUrl"
                                } catch (e: Exception) {
                                    actionFeedback = "Browser fallback URL ready: $fallbackUrl"
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("deeplink_browser_fallback_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                tint = ShareDabbaCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Browser Fallback", color = ShareDabbaWhite)
                        }
                    }

                    if (!actionFeedback.isNullOrBlank()) {
                        Text(
                            text = actionFeedback.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaCyan,
                            modifier = Modifier.testTag("deeplink_action_feedback")
                        )
                    }
                }
            }

            // CASE 2 — ShareDabba Web Landing Page Preview & App Handoff Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                    .testTag("deeplink_landing_page_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Web Landing Page & App Handoff",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaWhite
                        )
                        ShareDabbaGradientBadge(text = "Case 2 & Case 3")
                    }

                    // Video Thumbnail
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, ShareDabbaAccentGradient, RoundedCornerShape(14.dp))
                            .testTag("landing_page_thumbnail")
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.bg_hero_streaming),
                            contentDescription = "Landing page thumbnail for ${landingModel.videoTitle}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                                .background(ShareDabbaBlack.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Thumbnail: ${landingModel.thumbnailUrl}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShareDabbaCyan
                            )
                        }
                    }

                    // Video Title & Uploader
                    Text(
                        text = landingModel.videoTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = ShareDabbaWhite,
                        modifier = Modifier.testTag("landing_page_video_title")
                    )
                    Text(
                        text = "${landingModel.uploaderLabel} • Video ID: ${landingModel.videoId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShareDabbaTextSecondary
                    )

                    // Action 1: Open in ShareDabba (Case 3: Opens exact video requested, never homepage)
                    Button(
                        onClick = {
                            val target = resolvedResult.playerTargetKey ?: landingModel.videoId
                            onOpenInPlayer(target)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("landing_open_in_sharedabba_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShareDabbaCyan,
                            contentColor = ShareDabbaBlack
                        )
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open in ShareDabba (${landingModel.openInShareDabbaUri})")
                    }

                    // Action 2: Watch in Browser
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(landingModel.watchInBrowserUrl)
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                actionFeedback = "Launched Watch in Browser: ${landingModel.watchInBrowserUrl}"
                            } catch (_: Exception) {
                                actionFeedback = "Watch in Browser URL: ${landingModel.watchInBrowserUrl}"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("landing_watch_in_browser_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            tint = ShareDabbaCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Watch in Browser (${landingModel.watchInBrowserUrl})", color = ShareDabbaWhite)
                    }

                    // Action 3: Get the App when Official Play Store Listing is Available
                    OutlinedButton(
                        onClick = {
                            actionFeedback = landingModel.playStoreStatusText
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("landing_get_app_play_store_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shop,
                            contentDescription = null,
                            tint = ShareDabbaPurple
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(landingModel.playStoreStatusText, color = ShareDabbaWhite)
                    }
                }
            }

            // assetlinks.json Configuration & Verification Honesty Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaWarning.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .testTag("assetlinks_config_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Digital Asset Links Status",
                            tint = ShareDabbaWarning
                        )
                        Text(
                            text = "Digital Asset Links (assetlinks.json) Specification",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaWhite
                        )
                    }

                    Text(
                        text = AssetLinksConfigurationSpec.VERIFICATION_HONESTY_STATEMENT,
                        style = MaterialTheme.typography.bodySmall,
                        color = ShareDabbaWarning
                    )

                    Text(
                        text = "Target Host Path: ${AssetLinksConfigurationSpec.WELL_KNOWN_PATH}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaCyan
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ShareDabbaBlack, RoundedCornerShape(10.dp))
                            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = AssetLinksConfigurationSpec.buildAssetLinksJson(),
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
