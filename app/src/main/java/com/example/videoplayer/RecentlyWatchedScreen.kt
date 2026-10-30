package com.example.videoplayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.fixtures.DevFixturesCatalog
import com.example.data.local.CachedStreamEntity
import com.example.data.repository.StreamCacheRepository
import com.example.ui.common.PreviewStateMode
import com.example.ui.common.ShareDabbaEmptyState
import com.example.ui.common.ShareDabbaErrorState
import com.example.ui.common.ShareDabbaLoadingState
import com.example.ui.common.ShareDabbaTopBar
import com.example.ui.common.StateModeSelectorRow
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaError
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWhite
import kotlinx.coroutines.launch

/**
 * Screen 10: Recently Watched Screen
 * Displays locally cached stream history from Room (`StreamCacheRepository`) with resume & clear controls.
 */
@Composable
fun RecentlyWatchedScreen(
    streamCacheRepository: StreamCacheRepository,
    onResumeStream: (String) -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val cachedStreams by streamCacheRepository.cachedStreams.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var stateMode by rememberSaveable { mutableStateOf(PreviewStateMode.DATA) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .testTag("recently_watched_screen"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ShareDabbaTopBar(
                title = "Recently Watched",
                subtitle = "Room Local Playback Cache • com.example.videoplayer",
                onBackClick = onBackClick
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StateModeSelectorRow(
                    currentMode = stateMode,
                    onModeChange = { stateMode = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val sample = DevFixturesCatalog.videos.first()
                                streamCacheRepository.cacheStreamPlayback(
                                    streamId = sample.id,
                                    title = sample.title,
                                    streamUrl = sample.streamUrl,
                                    positionMs = 42_500L
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("seed_recently_watched_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShareDabbaCyan,
                            contentColor = ShareDabbaBlack
                        )
                    ) {
                        Text("Record Sample Watch")
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                streamCacheRepository.clearCache()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("clear_recently_watched_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = ShareDabbaError
                        )
                        Spacer(modifier = Modifier.padding(3.dp))
                        Text("Clear History", color = ShareDabbaWhite)
                    }
                }
            }
        }

        when (stateMode) {
            PreviewStateMode.LOADING -> {
                item {
                    ShareDabbaLoadingState(message = "Reading local Room watch history...")
                }
            }

            PreviewStateMode.EMPTY -> {
                item {
                    ShareDabbaEmptyState(
                        title = "No Recently Watched Videos",
                        subtitle = "Videos you play in Media3 ExoPlayer are automatically cached here with their last playback timestamp.",
                        actionLabel = "Record Sample Watch",
                        onActionClick = {
                            scope.launch {
                                val sample = DevFixturesCatalog.videos.first()
                                streamCacheRepository.cacheStreamPlayback(
                                    streamId = sample.id,
                                    title = sample.title,
                                    streamUrl = sample.streamUrl,
                                    positionMs = 42_500L
                                )
                                stateMode = PreviewStateMode.DATA
                            }
                        }
                    )
                }
            }

            PreviewStateMode.ERROR -> {
                item {
                    ShareDabbaErrorState(
                        message = "Failed to query local Room watch history cursor.",
                        onRetry = { stateMode = PreviewStateMode.DATA }
                    )
                }
            }

            PreviewStateMode.DATA -> {
                if (cachedStreams.isEmpty()) {
                    item {
                        ShareDabbaEmptyState(
                            title = "Your Watch History is Empty",
                            subtitle = "Play any stream in the Native Video Player or tap 'Record Sample Watch' above to populate local Room history.",
                            actionLabel = "Record Sample Watch",
                            onActionClick = {
                                scope.launch {
                                    val sample = DevFixturesCatalog.videos.first()
                                    streamCacheRepository.cacheStreamPlayback(
                                        streamId = sample.id,
                                        title = sample.title,
                                        streamUrl = sample.streamUrl,
                                        positionMs = 42_500L
                                    )
                                }
                            }
                        )
                    }
                } else {
                    items(cachedStreams, key = { it.streamId }) { entry ->
                        RecentlyWatchedItemCard(
                            entry = entry,
                            onResume = { onResumeStream(entry.streamUrl) },
                            onDelete = {
                                scope.launch {
                                    streamCacheRepository.removeCachedStream(entry.streamId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentlyWatchedItemCard(
    entry: CachedStreamEntity,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
            .testTag("recently_watched_item_${entry.streamId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ShareDabbaWhite
                )
                Text(
                    text = "Resume at ${VideoPlaybackValidator.formatPlaybackTimeMs(entry.lastPlaybackPositionMs)} • Local Room Cache",
                    style = MaterialTheme.typography.labelSmall,
                    color = ShareDabbaCyan
                )
                Text(
                    text = entry.streamUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = ShareDabbaTextSecondary,
                    maxLines = 1
                )
            }
            Row {
                IconButton(
                    onClick = onResume,
                    modifier = Modifier.testTag("resume_watched_${entry.streamId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume ${entry.title}",
                        tint = ShareDabbaCyan
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("remove_watched_${entry.streamId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove ${entry.title}",
                        tint = ShareDabbaError
                    )
                }
            }
        }
    }
}
