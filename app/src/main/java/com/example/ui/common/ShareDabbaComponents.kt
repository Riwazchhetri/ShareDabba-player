package com.example.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.fixtures.DevFixturesCatalog
import com.example.data.fixtures.VideoItemFixture
import com.example.ui.theme.ShareDabbaAccentGradient
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCardElevated
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaError
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaPurple
import com.example.ui.theme.ShareDabbaSubtleCardGradient
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWarning
import com.example.ui.theme.ShareDabbaWhite

enum class PreviewStateMode(val label: String) {
    DATA("Fixture Data"),
    LOADING("Loading"),
    EMPTY("Empty"),
    ERROR("Error")
}

@Composable
fun ShareDabbaTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = ShareDabbaWhite
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_sharedabba_logo),
                    contentDescription = "ShareDabba Logo",
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = ShareDabbaWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingContent != null) {
                trailingContent()
            }
            if (actions != null) {
                actions()
            }
        }
    }
}

@Composable
fun ShareDabbaGradientBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(ShareDabbaAccentGradient)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = ShareDabbaWhite
        )
    }
}

@Composable
fun DevFixtureNoticeBanner(
    modifier: Modifier = Modifier,
    message: String = "Showing separated Stage 3 development fixtures until the ShareDabba website API is connected."
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ShareDabbaWarning.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .testTag("dev_fixture_banner"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Science,
                contentDescription = "Development Fixture Indicator",
                tint = ShareDabbaWarning,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DevFixturesCatalog.FIXTURE_BADGE_TEXT,
                    style = MaterialTheme.typography.labelSmall,
                    color = ShareDabbaWarning
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShareDabbaTextSecondary
                )
            }
        }
    }
}

@Composable
fun StateModeSelectorRow(
    currentMode: PreviewStateMode,
    onModeChange: (PreviewStateMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PreviewStateMode.entries.forEach { mode ->
            FilterChip(
                selected = currentMode == mode,
                onClick = { onModeChange(mode) },
                label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ShareDabbaCyan.copy(alpha = 0.2f),
                    selectedLabelColor = ShareDabbaCyan
                ),
                modifier = Modifier.testTag("state_mode_${mode.name.lowercase()}")
            )
        }
    }
}

@Composable
fun ShareDabbaLoadingState(
    message: String = "Loading ShareDabba data...",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
            .testTag("common_loading_state"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = ShareDabbaCyan,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ShareDabbaTextSecondary
            )
        }
    }
}

@Composable
fun ShareDabbaEmptyState(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
            .testTag("common_empty_state"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = title,
                tint = ShareDabbaCyan,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = ShareDabbaWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = ShareDabbaTextSecondary
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onActionClick,
                    modifier = Modifier.testTag("empty_state_action_button")
                ) {
                    Text(actionLabel, color = ShareDabbaCyan)
                }
            }
        }
    }
}

@Composable
fun ShareDabbaErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ShareDabbaError.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .testTag("common_error_state"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = ShareDabbaError,
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = "Unable to Load Content",
                style = MaterialTheme.typography.titleMedium,
                color = ShareDabbaWhite
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ShareDabbaTextSecondary
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ShareDabbaCyan,
                    contentColor = ShareDabbaBlack
                ),
                modifier = Modifier.testTag("error_state_retry_button")
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun ShareDabbaVideoCard(
    video: VideoItemFixture,
    onCardClick: () -> Unit,
    onWatchClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    ShareDabbaVideoCard(
        video = video,
        onVideoClick = onCardClick,
        onWatchNowClick = onWatchClick,
        modifier = modifier,
        customTestTag = testTag
    )
}

@Composable
fun ShareDabbaVideoCard(
    video: VideoItemFixture,
    onVideoClick: () -> Unit,
    onWatchNowClick: () -> Unit,
    modifier: Modifier = Modifier,
    customTestTag: String? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(18.dp))
            .clickable(onClick = onVideoClick)
            .testTag(customTestTag ?: "video_card_${video.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .background(ShareDabbaSubtleCardGradient)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bg_hero_streaming),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    ShareDabbaGradientBadge(text = video.category)
                    if (video.isDevFixture) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ShareDabbaBlack.copy(alpha = 0.8f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "FIXTURE",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShareDabbaWarning
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ShareDabbaBlack.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = video.durationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaWhite
                    )
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ShareDabbaWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${video.uploaderName} • ${video.uploaderHandle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaCyan
                        )
                        Text(
                            text = "${video.viewsLabel} • ${video.uploadedAgo}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ShareDabbaTextSecondary
                        )
                        Text(
                            text = "ID: ${video.id} • Owner: ${video.uploaderId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onWatchNowClick,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ShareDabbaPurple.copy(alpha = 0.25f))
                            .testTag("watch_now_btn_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Watch ${video.title}",
                            tint = ShareDabbaCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShareDabbaSectionCard(
    title: String,
    packagePath: String,
    description: String,
    icon: ImageVector,
    testTag: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ShareDabbaSubtleCardGradient)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ShareDabbaCardElevated)
                    .border(1.dp, ShareDabbaAccentGradient, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ShareDabbaCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ShareDabbaWhite
                )
                Text(
                    text = packagePath,
                    style = MaterialTheme.typography.labelSmall,
                    color = ShareDabbaCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShareDabbaTextSecondary
                )
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open $title",
                    tint = ShareDabbaCyan
                )
            }
        }
    }
}
