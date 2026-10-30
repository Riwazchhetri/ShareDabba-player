package com.example.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.config.EnvironmentConfig
import com.example.core.logging.AppLogger
import com.example.data.repository.StreamCacheRepository
import com.example.network.IntegrationContractRegistry
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.common.ShareDabbaSectionCard
import com.example.ui.common.ShareDabbaTopBar
import com.example.ui.navigation.ShareDabbaDestination
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaPurple
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWarning
import com.example.ui.theme.ShareDabbaWhite
import kotlinx.coroutines.launch

/**
 * Screen 18: Settings Screen
 * Displays EnvironmentConfig, Stage 2 Backend Integration Contract status, Room Local Cache controls,
 * quick links to Help & Support and Onboarding Tour, and AppLogger diagnostics.
 */
@Composable
fun SettingsScreen(
    environmentConfig: EnvironmentConfig,
    streamCacheRepository: StreamCacheRepository,
    onNavigate: (String) -> Unit = {},
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val logs by AppLogger.recentLogs.collectAsStateWithLifecycle()
    val cachedStreams by streamCacheRepository.cachedStreams.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen")
    ) {
        ShareDabbaTopBar(
            title = "Settings & Diagnostics",
            subtitle = "com.example.settings • Environment & Cache",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShareDabbaSectionCard(
                title = "Environment Configuration",
                packagePath = "App ID: ${environmentConfig.applicationId} (v${environmentConfig.versionName})",
                description = "Web URL: ${environmentConfig.websiteBaseUrl}\nAPI URL: ${environmentConfig.apiBaseUrl}",
                icon = Icons.Default.Settings,
                testTag = "settings_env_card"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onNavigate(ShareDabbaDestination.HelpSupport.route) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_open_help_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShareDabbaCyan,
                        contentColor = ShareDabbaBlack
                    )
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Help & Support")
                }

                OutlinedButton(
                    onClick = { onNavigate(ShareDabbaDestination.Onboarding.route) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_open_onboarding_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = ShareDabbaPurple
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Onboarding", color = ShareDabbaWhite)
                }
            }

            // Stage 2 Integration Contract Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                    .testTag("stage2_contract_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stage 2 Backend Integration Contract",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaWhite
                        )
                        ShareDabbaGradientBadge(text = "PENDING REPO / DOCS")
                    }
                    Text(
                        text = "Backend Connection Complete: ${IntegrationContractRegistry.IS_BACKEND_CONNECTION_COMPLETE} (No endpoints or schemas invented)",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaWarning
                    )
                    IntegrationContractRegistry.discoveryChecklist.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.number}. ${item.domain}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ShareDabbaWhite
                            )
                            Text(
                                text = item.status.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = ShareDabbaTextSecondary
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Room Local Cache Management",
                        style = MaterialTheme.typography.titleMedium,
                        color = ShareDabbaWhite
                    )
                    Text(
                        text = "Cached stream entries on device: ${cachedStreams.size} (Room is used strictly as a local cache, not a replacement for the ShareDabba website database).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShareDabbaTextSecondary
                    )
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                streamCacheRepository.clearCache()
                                AppLogger.i("Settings", "Cleared local Room stream cache.")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_room_cache_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = ShareDabbaCyan
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Clear Local Room Cache", color = ShareDabbaWhite)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = ShareDabbaCyan
                        )
                        Text(
                            text = "AppLogger Diagnostic Console",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaWhite
                        )
                    }
                    if (logs.isEmpty()) {
                        Text(
                            text = "No recent diagnostic events logged.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ShareDabbaTextSecondary
                        )
                    } else {
                        logs.take(12).forEach { entry ->
                            Text(
                                text = "[${entry.timestamp}] ${entry.level}/${entry.tag}: ${entry.message}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShareDabbaTextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
