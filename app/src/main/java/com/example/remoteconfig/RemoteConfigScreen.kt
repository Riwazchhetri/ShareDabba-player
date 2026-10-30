package com.example.remoteconfig

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.config.EnvironmentConfig
import com.example.ui.common.ShareDabbaSectionCard
import com.example.ui.common.ShareDabbaTopBar
import com.example.ui.theme.ShareDabbaBlack

data class RemoteFeatureFlags(
    val enableAdaptiveHls: Boolean = true,
    val enableHighThinkingAi: Boolean = true,
    val enableLowLatencySearch: Boolean = true,
    val maxRoomCacheEntries: Int = 100
)

@Composable
fun RemoteConfigScreen(
    environmentConfig: EnvironmentConfig,
    onBackClick: () -> Unit
) {
    val flags = RemoteFeatureFlags()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .verticalScroll(rememberScrollState())
            .testTag("remote_config_screen")
    ) {
        ShareDabbaTopBar(
            title = "Remote Configuration",
            subtitle = "com.example.remoteconfig",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShareDabbaSectionCard(
                title = "Media3 Adaptive Streaming Flag",
                packagePath = "enableAdaptiveHls = ${flags.enableAdaptiveHls}",
                description = "Controls adaptive HLS/DASH track selection in the Media3 ExoPlayer pipeline.",
                icon = Icons.Default.Tune,
                testTag = "rc_flag_hls"
            )
            ShareDabbaSectionCard(
                title = "AI Discovery & Thinking Models",
                packagePath = "lowLatency=${environmentConfig.lowLatencyAiModel} | highThinking=${environmentConfig.highThinkingAiModel}",
                description = "Runtime model routing configuration for ShareDabba AI search and deep creator analysis.",
                icon = Icons.Default.CloudSync,
                testTag = "rc_flag_ai"
            )
        }
    }
}
