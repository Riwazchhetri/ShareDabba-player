package com.example.search

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.error.UiState
import com.example.data.fixtures.DevFixturesCatalog
import com.example.ui.common.DevFixtureNoticeBanner
import com.example.ui.common.PreviewStateMode
import com.example.ui.common.ShareDabbaEmptyState
import com.example.ui.common.ShareDabbaErrorState
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.common.ShareDabbaLoadingState
import com.example.ui.common.ShareDabbaTopBar
import com.example.ui.common.ShareDabbaVideoCard
import com.example.ui.common.StateModeSelectorRow
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaError
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaPurple
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWhite

/**
 * Screen 6: Search Screen
 * Combines:
 * - Instant video & creator catalog search with clickable Video Cards
 * - Firebase AI Logic Low-Latency stream search (gemini-3.1-flash-lite)
 * - Firebase AI Logic High-Thinking strategist (gemini-4-argon with ThinkingLevel.HIGH)
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpenVideoDetails: (String) -> Unit = {},
    onPlayStream: (String) -> Unit = {}
) {
    val fastState by viewModel.fastSearchState.collectAsStateWithLifecycle()
    val deepState by viewModel.deepThinkingState.collectAsStateWithLifecycle()

    var catalogQuery by rememberSaveable { mutableStateOf("") }
    var stateMode by rememberSaveable { mutableStateOf(PreviewStateMode.DATA) }

    var fastQuery by rememberSaveable {
        mutableStateOf("4K 60fps adaptive HLS creator live streams")
    }
    var complexQuery by rememberSaveable {
        mutableStateOf("Design an optimal adaptive bitrate ladder and CDN segment caching strategy for ShareDabba creators streaming on mobile networks.")
    }

    val matchingVideos = DevFixturesCatalog.videos.filter { video ->
        val q = catalogQuery.trim()
        q.isEmpty() ||
            video.title.contains(q, ignoreCase = true) ||
            video.uploaderName.contains(q, ignoreCase = true) ||
            video.category.contains(q, ignoreCase = true) ||
            video.description.contains(q, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .verticalScroll(rememberScrollState())
            .testTag("search_screen")
    ) {
        ShareDabbaTopBar(
            title = "Search & AI Discovery",
            subtitle = "com.example.search • Video Catalog + Firebase AI Logic"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DevFixtureNoticeBanner()

            // Catalog Search Box
            OutlinedTextField(
                value = catalogQuery,
                onValueChange = { catalogQuery = it },
                label = { Text("Search ShareDabba Videos, Creators, or Categories") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ShareDabbaCyan
                    )
                },
                trailingIcon = {
                    if (catalogQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { catalogQuery = "" },
                            modifier = Modifier.testTag("clear_catalog_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search Query",
                                tint = ShareDabbaTextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_search_input")
            )

            StateModeSelectorRow(
                currentMode = stateMode,
                onModeChange = { stateMode = it }
            )

            when (stateMode) {
                PreviewStateMode.LOADING -> {
                    ShareDabbaLoadingState(message = "Searching ShareDabba catalog...")
                }

                PreviewStateMode.EMPTY -> {
                    ShareDabbaEmptyState(
                        title = "No Matching Videos Found",
                        subtitle = "Try searching for 'ExoPlayer', 'Esports', 'Creator', or clear your search filter.",
                        actionLabel = "Show All Videos",
                        onActionClick = {
                            catalogQuery = ""
                            stateMode = PreviewStateMode.DATA
                        }
                    )
                }

                PreviewStateMode.ERROR -> {
                    ShareDabbaErrorState(
                        message = "Search index query timed out. Check network connection and retry.",
                        onRetry = { stateMode = PreviewStateMode.DATA }
                    )
                }

                PreviewStateMode.DATA -> {
                    if (matchingVideos.isEmpty()) {
                        ShareDabbaEmptyState(
                            title = "No Videos Match \"$catalogQuery\"",
                            subtitle = "Reset your search keyword to view all development fixture streams.",
                            actionLabel = "Clear Filter",
                            onActionClick = { catalogQuery = "" }
                        )
                    } else {
                        Text(
                            text = "Catalog Results (${matchingVideos.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaWhite
                        )
                        matchingVideos.forEach { video ->
                            ShareDabbaVideoCard(
                                video = video,
                                onCardClick = { onOpenVideoDetails(video.id) },
                                onWatchClick = { onPlayStream(video.streamUrl) },
                                testTag = "search_result_video_${video.id}"
                            )
                        }
                    }
                }
            }

            // Card 1: Low-Latency Responses (gemini-3.1-flash-lite)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = ShareDabbaCyan
                            )
                            Text(
                                text = "Low-Latency Stream Search",
                                style = MaterialTheme.typography.titleMedium,
                                color = ShareDabbaWhite
                            )
                        }
                        ShareDabbaGradientBadge(text = "gemini-3.1-flash-lite")
                    }

                    Text(
                        text = "Instant streaming discovery and tag generation powered by low-latency model streaming.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShareDabbaTextSecondary
                    )

                    OutlinedTextField(
                        value = fastQuery,
                        onValueChange = { fastQuery = it },
                        label = { Text("Quick Discovery Prompt") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fast_search_input")
                    )

                    Button(
                        onClick = { viewModel.runLowLatencySearch(fastQuery) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fast_search_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShareDabbaCyan,
                            contentColor = ShareDabbaBlack
                        )
                    ) {
                        Text("Run Low-Latency Search")
                    }

                    AiResponseStateBox(state = fastState, testTag = "fast_search_result")
                }
            }

            // Card 2: High Thinking Mode (gemini-4-argon + ThinkingLevel.HIGH)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ShareDabbaOutline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = ShareDabbaPurple
                            )
                            Text(
                                text = "High-Thinking Creator Strategist",
                                style = MaterialTheme.typography.titleMedium,
                                color = ShareDabbaWhite
                            )
                        }
                        ShareDabbaGradientBadge(text = "gemini-4-argon • HIGH")
                    }

                    Text(
                        text = "Uses gemini-4-argon with ThinkingLevel.HIGH for complex streaming, encoding, and creator architecture queries.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShareDabbaTextSecondary
                    )

                    OutlinedTextField(
                        value = complexQuery,
                        onValueChange = { complexQuery = it },
                        label = { Text("Complex Query (ThinkingLevel.HIGH)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deep_thinking_input")
                    )

                    Button(
                        onClick = { viewModel.runHighThinkingAnalysis(complexQuery) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deep_thinking_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShareDabbaPurple,
                            contentColor = ShareDabbaWhite
                        )
                    ) {
                        Text("Run Deep Thinking Analysis")
                    }

                    AiResponseStateBox(state = deepState, testTag = "deep_thinking_result")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AiResponseStateBox(
    state: UiState<String>,
    testTag: String
) {
    when (state) {
        is UiState.Idle -> Unit
        is UiState.Loading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = ShareDabbaCyan,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Generating response via Firebase AI Logic...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        is UiState.Error -> {
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = ShareDabbaError,
                modifier = Modifier.testTag(testTag)
            )
        }

        is UiState.Success -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag),
                colors = CardDefaults.cardColors(containerColor = ShareDabbaBlack),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = state.data,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShareDabbaWhite,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
