package com.example.categories

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.fixtures.DevFixturesCatalog
import com.example.domain.usecase.SyncBookmarkUseCase
import com.example.ui.common.DevFixtureNoticeBanner
import com.example.ui.common.PreviewStateMode
import com.example.ui.common.ShareDabbaEmptyState
import com.example.ui.common.ShareDabbaErrorState
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.common.ShareDabbaLoadingState
import com.example.ui.common.ShareDabbaTopBar
import com.example.ui.common.ShareDabbaVideoCard
import com.example.ui.common.StateModeSelectorRow
import com.example.ui.theme.ShareDabbaAccentGradient
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaOutline
import com.example.ui.theme.ShareDabbaPurple
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWhite
import kotlinx.coroutines.launch

/**
 * Screen 7: Categories Screen
 * Browse ShareDabba categories and inspect filtered streams per category.
 */
@Composable
fun CategoriesScreen(
    initialCategoryName: String? = null,
    onOpenVideoDetails: (String) -> Unit,
    onWatchVideo: (String, String) -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    var selectedCategory by rememberSaveable {
        mutableStateOf(initialCategoryName ?: "All")
    }
    var stateMode by rememberSaveable { mutableStateOf(PreviewStateMode.DATA) }

    val allCategoryNames = listOf("All") + DevFixturesCatalog.categories.map { it.name }
    val filteredVideos = remember(selectedCategory) {
        if (selectedCategory == "All") {
            DevFixturesCatalog.videos
        } else {
            DevFixturesCatalog.videos.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .testTag("categories_screen"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ShareDabbaTopBar(
                title = "Categories",
                subtitle = "Screen 7 • Browse by Stream Genre",
                onBackClick = onBackClick
            )
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DevFixtureNoticeBanner()
                StateModeSelectorRow(
                    currentMode = stateMode,
                    onModeChange = { stateMode = it }
                )
            }
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allCategoryNames) { categoryName ->
                    FilterChip(
                        selected = selectedCategory == categoryName,
                        onClick = { selectedCategory = categoryName },
                        label = { Text(categoryName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ShareDabbaCyan,
                            selectedLabelColor = ShareDabbaBlack
                        ),
                        modifier = Modifier.testTag("category_chip_$categoryName")
                    )
                }
            }
        }

        when (stateMode) {
            PreviewStateMode.LOADING -> {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ShareDabbaLoadingState("Loading ShareDabba categories...")
                    }
                }
            }

            PreviewStateMode.EMPTY -> {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ShareDabbaEmptyState(
                            title = "No Videos in This Category",
                            subtitle = "Switch back to Fixture Data or choose another category chip.",
                            actionLabel = "Show All Categories",
                            onActionClick = {
                                selectedCategory = "All"
                                stateMode = PreviewStateMode.DATA
                            }
                        )
                    }
                }
            }

            PreviewStateMode.ERROR -> {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ShareDabbaErrorState(
                            message = "Simulated category endpoint error for Stage 3 UI state verification.",
                            onRetry = { stateMode = PreviewStateMode.DATA }
                        )
                    }
                }
            }

            PreviewStateMode.DATA -> {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DevFixturesCatalog.categories.forEach { cat ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (selectedCategory == cat.name) ShareDabbaCyan else ShareDabbaOutline,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedCategory = cat.name }
                                    .testTag("category_directory_card_${cat.id}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Category,
                                            contentDescription = cat.name,
                                            tint = ShareDabbaCyan
                                        )
                                        Column {
                                            Text(
                                                text = cat.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = ShareDabbaWhite
                                            )
                                            Text(
                                                text = cat.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = ShareDabbaTextSecondary
                                            )
                                        }
                                    }
                                    ShareDabbaGradientBadge(text = "${cat.videoCount} videos")
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Videos in $selectedCategory (${filteredVideos.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = ShareDabbaWhite,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                items(filteredVideos, key = { it.id }) { video ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ShareDabbaVideoCard(
                            video = video,
                            onVideoClick = { onOpenVideoDetails(video.id) },
                            onWatchNowClick = { onWatchVideo(video.title, video.streamUrl) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen 8: Video Details Screen
 * Displays:
 * - Thumbnail
 * - Title
 * - Uploader information
 * - Share button (launches Android system share sheet)
 * - Watch button (launches Native Video Player & caches in Room)
 * - Related videos
 */
@Composable
fun VideoDetailsScreen(
    videoId: String?,
    syncBookmarkUseCase: SyncBookmarkUseCase,
    onWatchClick: (title: String, streamUrl: String) -> Unit,
    onSelectRelatedVideo: (String) -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val video = remember(videoId) { DevFixturesCatalog.findVideoById(videoId) }
    val relatedVideos = remember(video.id) { DevFixturesCatalog.getRelatedVideos(video.id) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .testTag("video_details_screen"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ShareDabbaTopBar(
                title = "Video Details",
                subtitle = "Screen 8 • ${video.category}",
                onBackClick = onBackClick
            )
        }

        // Thumbnail
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, ShareDabbaAccentGradient, RoundedCornerShape(20.dp))
                    .testTag("video_details_thumbnail")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bg_hero_streaming),
                    contentDescription = "Thumbnail for ${video.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ShareDabbaGradientBadge(text = video.category)
                    ShareDabbaGradientBadge(text = video.durationLabel)
                }
            }
        }

        // Title, Metadata & Action Buttons (Watch, Share, Bookmark)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = ShareDabbaWhite,
                    modifier = Modifier.testTag("video_details_title")
                )

                Text(
                    text = "${video.viewsLabel} • Uploaded ${video.uploadedAgo}",
                    style = MaterialTheme.typography.labelLarge,
                    color = ShareDabbaCyan
                )

                // Uploader Information Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                        .testTag("video_details_uploader_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(ShareDabbaPurple.copy(alpha = 0.35f))
                                    .border(1.dp, ShareDabbaCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = video.uploaderName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ShareDabbaWhite
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = video.uploaderName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ShareDabbaWhite
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified Creator",
                                        tint = ShareDabbaCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "${video.uploaderHandle} • ${video.uploaderFollowers}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ShareDabbaTextSecondary
                                )
                            }
                        }
                        ShareDabbaGradientBadge(text = "CREATOR")
                    }
                }

                // Primary Watch & Share Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onWatchClick(video.title, video.streamUrl) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("video_details_watch_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShareDabbaCyan,
                            contentColor = ShareDabbaBlack
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Watch Now", style = MaterialTheme.typography.titleMedium, color = ShareDabbaBlack)
                    }

                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, video.title)
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Watch '${video.title}' on ShareDabba: ${video.shareUrl}"
                                )
                            }
                            val chooser = Intent.createChooser(sendIntent, "Share ShareDabba Video")
                            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(chooser)
                            statusBanner = "Opened system share sheet for ${video.shareUrl}"
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("video_details_share_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Video",
                            tint = ShareDabbaCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Link", color = ShareDabbaWhite)
                    }
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val result = syncBookmarkUseCase.addBookmarkAndCache(
                                title = video.title,
                                streamUrl = video.streamUrl,
                                category = video.category
                            )
                            statusBanner = result.fold(
                                onSuccess = { "Bookmarked '${video.title}' to Firestore & Room cache." },
                                onFailure = { "Bookmark error: ${it.localizedMessage}" }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_details_bookmark_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        tint = ShareDabbaCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to Cloud Bookmarks & Recently Watched", color = ShareDabbaWhite)
                }

                if (!statusBanner.isNullOrBlank()) {
                    Text(
                        text = statusBanner.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ShareDabbaCyan
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                        .testTag("video_details_ownership_record_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Description & Website Provenance",
                                style = MaterialTheme.typography.titleMedium,
                                color = ShareDabbaWhite
                            )
                            ShareDabbaGradientBadge(text = "Website Authoritative")
                        }
                        Text(
                            text = video.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ShareDabbaTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Original Video ID: ${video.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaCyan
                        )
                        Text(
                            text = "Original Uploader ID: ${video.uploaderId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaCyan
                        )
                        Text(
                            text = "Original Share URL: ${video.shareUrl}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaCyan
                        )
                        Text(
                            text = "Ownership Record: ${video.ownershipRecord.storageAuthority} • ${video.ownershipRecord.originPlatform}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaTextSecondary
                        )
                        Text(
                            text = "Thumbnail URL: ${video.thumbnailUrl}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaTextSecondary
                        )
                    }
                }
            }
        }

        // Related Videos Section
        item {
            Text(
                text = "Related Videos",
                style = MaterialTheme.typography.titleLarge,
                color = ShareDabbaWhite,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        items(relatedVideos, key = { it.id }) { related ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ShareDabbaVideoCard(
                    video = related,
                    onVideoClick = { onSelectRelatedVideo(related.id) },
                    onWatchNowClick = { onWatchClick(related.title, related.streamUrl) }
                )
            }
        }
    }
}
