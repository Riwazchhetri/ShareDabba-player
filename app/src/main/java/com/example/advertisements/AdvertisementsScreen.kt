package com.example.advertisements

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.common.ShareDabbaSectionCard
import com.example.ui.common.ShareDabbaTopBar
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWhite
import com.example.videoplayer.AdPlacementReadinessState
import com.example.videoplayer.VideoPlaybackValidator

@Composable
fun AdvertisementsScreen(
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val sampleAdState = remember {
        VideoPlaybackValidator.recordVideoStartWithoutFakeAdImpression(AdPlacementReadinessState())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .verticalScroll(rememberScrollState())
            .testTag("advertisements_screen")
    ) {
        ShareDabbaTopBar(
            title = "Advertisements Architecture",
            subtitle = "com.example.advertisements • Stage 4 Ad Readiness",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                    .testTag("ads_stage4_policy_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stage 4 Ad Placement Contract",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaWhite
                        )
                        ShareDabbaGradientBadge(text = "ZERO FAKE ADS")
                    }
                    Text(
                        text = sampleAdState.policySummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShareDabbaTextSecondary
                    )
                    Text(
                        text = "Website Ad Config Connected: ${sampleAdState.isWebsiteAdConfigConnected} • Verified Ad Impressions on Video Start: ${sampleAdState.verifiedAdImpressionsCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaCyan
                    )
                }
            }

            ShareDabbaSectionCard(
                title = "Media3 Pre-Roll & Mid-Roll Ad Slots",
                packagePath = "com.example.advertisements.AdSlotConfig",
                description = "Prepared for the ShareDabba website's documented VAST/VMAP ad configuration when available, without inserting synthetic or mock ads.",
                icon = Icons.Default.Slideshow,
                testTag = "ads_vast_card"
            )

            ShareDabbaSectionCard(
                title = "Impression Integrity Guardrail",
                packagePath = "VideoPlaybackValidator.recordVideoStartWithoutFakeAdImpression",
                description = "Video starts are tracked strictly as playback telemetry and are never counted as advertising impressions.",
                icon = Icons.Default.VerifiedUser,
                testTag = "ads_impression_guardrail_card"
            )

            ShareDabbaSectionCard(
                title = "Compliant Sponsor & Companion Placement",
                packagePath = "Play Policy Compliant Ad Container",
                description = "All ad placements are isolated from navigation controls and governed by ShareDabba remote configuration.",
                icon = Icons.Default.Campaign,
                testTag = "ads_compliance_card"
            )
        }
    }
}
