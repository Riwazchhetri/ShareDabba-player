package com.example.home

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Update
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.error.UiState
import com.example.data.fixtures.DevFixturesCatalog
import com.example.domain.model.StreamBookmark
import com.example.remoteconfig.PublishedRemoteConfig
import com.example.ui.common.DevFixtureNoticeBanner
import com.example.ui.common.PreviewStateMode
import com.example.ui.common.ShareDabbaEmptyState
import com.example.ui.common.ShareDabbaErrorState
import com.example.ui.common.ShareDabbaGradientBadge
import com.example.ui.common.ShareDabbaLoadingState
import com.example.ui.common.ShareDabbaTopBar
import com.example.ui.common.ShareDabbaVideoCard
import com.example.ui.common.StateModeSelectorRow
import com.example.ui.navigation.ShareDabbaDestination
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

/**
 * Screen 5: Home Screen
 * Features:
 * - Featured videos
 * - Recently uploaded videos
 * - Popular videos
 * - Categories
 * - Search shortcut
 * - Clear navigation to User Dashboard, Notifications, Recently Watched, Video Details, and Player
 * - Real Cloud Stream Bookmarks (Firestore + Room cache)
 * - Stage 8 Remote Admin Control integration (Branding, Announcements, Section/Banner visibility)
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    remoteConfig: PublishedRemoteConfig = PublishedRemoteConfig.SAFE_DEFAULT,
    onNavigate: (String) -> Unit,
    onOpenVideoDetails: (String) -> Unit,
    onPlayStream: (String) -> Unit
) {
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val cachedStreams by viewModel.cachedStreamsState.collectAsStateWithLifecycle()
    val feedback by viewModel.actionFeedback.collectAsStateWithLifecycle()

    var stateMode by rememberSaveable { mutableStateOf(PreviewStateMode.DATA) }
    var selectedCategoryName by rememberSaveable { mutableStateOf("All") }

    var newTitle by rememberSaveable { mutableStateOf("") }
    var newUrl by rememberSaveable {
        mutableStateOf("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4")
    }
    var newCategory by rememberSaveable { mutableStateOf("Live") }

    val filteredVideos = if (selectedCategoryName == "All") {
        DevFixturesCatalog.videos
    } else {
        DevFixturesCatalog.videos.filter { it.category.equals(selectedCategoryName, ignoreCase = true) }
    }

    val featuredVideos = filteredVideos.filter { it.isFeatured }.ifEmpty { filteredVideos.take(2) }
    val recentVideos = filteredVideos.filter { it.isRecent }.ifEmpty { filteredVideos.take(2) }
    val popularVideos = filteredVideos.filter { it.isPopular }.ifEmpty { filteredVideos.take(2) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ShareDabbaBlack)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ShareDabbaTopBar(
                title = remoteConfig.branding.appDisplayName,
                subtitle = "STREAMING PLATFORM • CONFIG v${remoteConfig.configVersion}",
                actions = {
                    IconButton(
                        onClick = { onNavigate(ShareDabbaDestination.RecentlyWatched.route) },
                        modifier = Modifier.testTag("home_action_recently_watched")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Recently Watched",
                            tint = ShareDabbaCyan
                        )
                    }
                    if (remoteConfig.appManagement.enableCreatorDashboard) {
                        IconButton(
                            onClick = { onNavigate(ShareDabbaDestination.UserDashboard.route) },
                            modifier = Modifier.testTag("home_action_dashboard")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = "User Dashboard",
                                tint = ShareDabbaWhite
                            )
                        }
                    }
                    IconButton(
                        onClick = { onNavigate(ShareDabbaDestination.Notifications.route) },
                        modifier = Modifier.testTag("home_action_notifications")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = ShareDabbaCyan
                        )
                    }
                }
            )
        }

        // Stage 8 Remote Admin Announcement / Maintenance Banner
        if (remoteConfig.appManagement.maintenanceModeEnabled ||
            remoteConfig.branding.globalAnnouncementBanner.isNotBlank() ||
            remoteConfig.appManagement.updateAnnouncementText.isNotBlank()
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(1.dp, ShareDabbaWarning, RoundedCornerShape(14.dp))
                        .testTag("home_remote_admin_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (remoteConfig.appManagement.maintenanceModeEnabled) {
                            Text(
                                text = "MAINTENANCE MODE ACTIVE: ${remoteConfig.appManagement.maintenanceMessage}",
                                style = MaterialTheme.typography.labelLarge,
                                color = ShareDabbaError
                            )
                        }
                        if (remoteConfig.branding.globalAnnouncementBanner.isNotBlank()) {
                            Text(
                                text = remoteConfig.branding.globalAnnouncementBanner,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ShareDabbaCyan
                            )
                        }
                        if (remoteConfig.appManagement.updateAnnouncementText.isNotBlank()) {
                            Text(
                                text = remoteConfig.appManagement.updateAnnouncementText,
                                style = MaterialTheme.typography.labelSmall,
                                color = ShareDabbaWarning
                            )
                        }
                    }
                }
            }
        }

        // Search Bar Shortcut & Quick Navigation Pills
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ShareDabbaOutline, RoundedCornerShape(16.dp))
                        .clickable { onNavigate(ShareDabbaDestination.Search.route) }
                        .testTag("home_search_shortcut"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ShareDabbaSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Open Search",
                                tint = ShareDabbaCyan
                            )
                            Text(
                                text = "Search videos, creators, or AI discovery...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ShareDabbaTextSecondary
                            )
                        }
                        ShareDabbaGradientBadge(text = "Search")
                    }
                }

                DevFixtureNoticeBanner()
                StateModeSelectorRow(
                    currentMode = stateMode,
                    onModeChange = { stateMode = it }
                )
            }
        }

        when (stateMode) {
            PreviewStateMode.LOADING -> {
                item {
                    ShareDabbaLoadingState(message = "Loading ShareDabba home feed & categories...")
                }
            }

            PreviewStateMode.EMPTY -> {
                item {
                    ShareDabbaEmptyState(
                        title = "No Videos Available in Feed",
                        subtitle = "No streams match your current filter. Reset the state toggle or explore all categories.",
                        actionLabel = "Restore Feed",
                        onActionClick = {
                            selectedCategoryName = "All"
                            stateMode = PreviewStateMode.DATA
                        }
                    )
                }
            }

            PreviewStateMode.ERROR -> {
                item {
                    ShareDabbaErrorState(
                        message = "Unable to reach ShareDabba video catalog endpoint. Verify network connectivity or inspect Stage 2 Integration Contract.",
                        onRetry = { stateMode = PreviewStateMode.DATA }
                    )
                }
            }

            PreviewStateMode.DATA -> {
                // Hero Banner (Admin Controlled: homeScreen.showHeroBanner)
                if (remoteConfig.homeScreen.showHeroBanner) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(176.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.5.dp, ShareDabbaAccentGradient, RoundedCornerShape(22.dp))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.bg_hero_streaming),
                                contentDescription = "ShareDabba Streaming Hero Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ShareDabbaGradientBadge(text = "SHARE DABBA LIVE")
                                    ShareDabbaGradientBadge(text = "4K ADAPTIVE")
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Stream, Upload & Track Creator Revenue",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = ShareDabbaWhite
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val firstFeatured = featuredVideos.firstOrNull()
                                                if (firstFeatured != null) {
                                                    onOpenVideoDetails(firstFeatured.id)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ShareDabbaCyan,
                                                contentColor = ShareDabbaBlack
                                            ),
                                            modifier = Modifier.testTag("hero_watch_featured_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.padding(3.dp))
                                            Text("Featured Stream")
                                        }
                                        Button(
                                            onClick = { onNavigate(ShareDabbaDestination.UserDashboard.route) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ShareDabbaPurple,
                                                contentColor = ShareDabbaWhite
                                            ),
                                            modifier = Modifier.testTag("hero_dashboard_button")
                                        ) {
                                            Text("Creator Dashboard")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Categories Bar (Admin Controlled: homeScreen.showCategoriesBar)
                if (remoteConfig.homeScreen.showCategoriesBar) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = ShareDabbaCyan
                                    )
                                    Text(
                                        text = "Categories",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = ShareDabbaWhite
                                    )
                                }
                                TextButton(
                                    onClick = { onNavigate(ShareDabbaDestination.Categories.route) },
                                    modifier = Modifier.testTag("home_see_all_categories_button")
                                ) {
                                    Text("Browse All", color = ShareDabbaCyan)
                                }
                            }

                            val categoryNames = listOf("All") + DevFixturesCatalog.categories.map { it.name }
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(categoryNames) { category ->
                                    FilterChip(
                                        selected = selectedCategoryName == category,
                                        onClick = { selectedCategoryName = category },
                                        label = { Text(category) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ShareDabbaCyan,
                                            selectedLabelColor = ShareDabbaBlack,
                                            containerColor = ShareDabbaSurface,
                                            labelColor = ShareDabbaWhite
                                        ),
                                        modifier = Modifier.testTag("home_category_chip_$category")
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 1: Featured Videos (Admin Controlled: homeScreen.showFeaturedVideosSection)
                if (remoteConfig.homeScreen.showFeaturedVideosSection) {
                    item {
                        SectionHeaderRow(
                            title = "Featured Videos",
                            badge = "${featuredVideos.size} Featured",
                            icon = Icons.Default.Star
                        )
                    }

                    items(featuredVideos, key = { "featured_${it.id}" }) { video ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ShareDabbaVideoCard(
                                video = video,
                                onCardClick = { onOpenVideoDetails(video.id) },
                                onWatchClick = { onPlayStream(video.streamUrl) },
                                testTag = "home_featured_${video.id}"
                            )
                        }
                    }
                }

                // Section 2: Recently Uploaded Videos (Admin Controlled: homeScreen.showRecentlyUploadedSection)
                if (remoteConfig.homeScreen.showRecentlyUploadedSection) {
                    item {
                        SectionHeaderRow(
                            title = "Recently Uploaded Videos",
                            badge = "Latest Drops",
                            icon = Icons.Default.Update
                        )
                    }

                    items(recentVideos, key = { "recent_${it.id}" }) { video ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ShareDabbaVideoCard(
                                video = video,
                                onCardClick = { onOpenVideoDetails(video.id) },
                                onWatchClick = { onPlayStream(video.streamUrl) },
                                testTag = "home_recent_${video.id}"
                            )
                        }
                    }
                }

                // Section 3: Popular Videos (Admin Controlled: homeScreen.showPopularVideosSection)
                if (remoteConfig.homeScreen.showPopularVideosSection) {
                    item {
                        SectionHeaderRow(
                            title = "Popular Videos",
                            badge = "Trending Now",
                            icon = Icons.Default.TrendingUp
                        )
                    }

                    items(popularVideos, key = { "popular_${it.id}" }) { video ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ShareDabbaVideoCard(
                                video = video,
                                onCardClick = { onOpenVideoDetails(video.id) },
                                onWatchClick = { onPlayStream(video.streamUrl) },
                                testTag = "home_popular_${video.id}"
                            )
                        }
                    }
                }
            }
        }

        // Cloud Stream Bookmarks (Real Firestore + Room Cache)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
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
                        Text(
                            text = "Cloud Stream Bookmarks",
                            style = MaterialTheme.typography.titleMedium,
                            color = ShareDabbaWhite
                        )
                        ShareDabbaGradientBadge(text = "Firestore + Room")
                    }

                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Stream Bookmark Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_bookmark_title_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newUrl,
                            onValueChange = { newUrl = it },
                            label = { Text("Stream URL") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_bookmark_url_input")
                        )
                        OutlinedTextField(
                            value = newCategory,
                            onValueChange = { newCategory = it },
                            label = { Text("Tag") },
                            singleLine = true,
                            modifier = Modifier.weight(0.45f)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.addQuickBookmark(newTitle, newUrl, newCategory)
                            newTitle = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_bookmark_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShareDabbaCyan,
                            contentColor = ShareDabbaBlack
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Save Stream Bookmark")
                    }

                    if (!feedback.isNullOrBlank()) {
                        Text(
                            text = feedback.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaCyan
                        )
                    }

                    when (val state = bookmarksState) {
                        is UiState.Loading, is UiState.Idle -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = ShareDabbaCyan,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Syncing bookmarks from Firestore...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        is UiState.Error -> {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ShareDabbaError
                            )
                        }

                        is UiState.Success -> {
                            if (state.data.isEmpty()) {
                                Text(
                                    text = "No cloud bookmarks yet. Save a ShareDabba stream above or from Video Details.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ShareDabbaTextSecondary
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.data.forEach { bookmark ->
                                        BookmarkItemRow(
                                            bookmark = bookmark,
                                            onPlay = { onPlayStream(bookmark.streamUrl) },
                                            onDelete = { viewModel.deleteBookmark(bookmark.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recently Watched Cache: ${cachedStreams.size} item(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShareDabbaTextSecondary
                        )
                        TextButton(
                            onClick = { onNavigate(ShareDabbaDestination.RecentlyWatched.route) },
                            modifier = Modifier.testTag("open_recently_watched_button")
                        ) {
                            Text("Open Recently Watched", color = ShareDabbaCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    badge: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ShareDabbaCyan
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = ShareDabbaWhite
            )
        }
        ShareDabbaGradientBadge(text = badge)
    }
}

@Composable
private fun BookmarkItemRow(
    bookmark: StreamBookmark,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ShareDabbaBlack)
            .border(1.dp, ShareDabbaOutline, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ShareDabbaWhite
                )
                ShareDabbaGradientBadge(text = bookmark.category)
            }
            Text(
                text = bookmark.streamUrl,
                style = MaterialTheme.typography.labelSmall,
                color = ShareDabbaTextSecondary,
                maxLines = 1
            )
        }
        Row {
            IconButton(
                onClick = onPlay,
                modifier = Modifier.testTag("play_bookmark_${bookmark.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play ${bookmark.title}",
                    tint = ShareDabbaCyan
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_bookmark_${bookmark.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${bookmark.title}",
                    tint = ShareDabbaError
                )
            }
        }
    }
}
