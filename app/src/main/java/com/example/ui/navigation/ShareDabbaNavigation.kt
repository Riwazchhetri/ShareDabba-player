package com.example.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.advertisements.AdvertisementsScreen
import com.example.app.AppContainer
import com.example.categories.CategoriesScreen
import com.example.categories.VideoDetailsScreen
import com.example.deeplinks.DeepLinksScreen
import com.example.earnings.EarningsScreen
import com.example.earnings.UserDashboardScreen
import com.example.home.HomeScreen
import com.example.home.HomeViewModel
import com.example.onboarding.OnboardingScreen
import com.example.onboarding.SplashScreen
import com.example.profile.HelpSupportScreen
import com.example.profile.NotificationsScreen
import com.example.profile.ProfileScreen
import com.example.remoteconfig.RemoteConfigScreen
import com.example.search.SearchScreen
import com.example.search.SearchViewModel
import com.example.settings.SettingsScreen
import com.example.ui.theme.ShareDabbaBlack
import com.example.ui.theme.ShareDabbaCyan
import com.example.ui.theme.ShareDabbaSurface
import com.example.ui.theme.ShareDabbaTextSecondary
import com.example.ui.theme.ShareDabbaWhite
import com.example.uploads.UploadsScreen
import com.example.videoplayer.RecentlyWatchedScreen
import com.example.videoplayer.VideoPlayerScreen
import com.example.wallet.WalletScreen
import com.example.wallet.WithdrawalHistoryScreen
import com.google.firebase.auth.FirebaseUser

sealed class ShareDabbaDestination(
    val route: String,
    val label: String,
    val icon: ImageVector? = null
) {
    // Primary Bottom / Rail Navigation
    data object Home : ShareDabbaDestination("home", "Home", Icons.Default.Home)
    data object Search : ShareDabbaDestination("search", "Search", Icons.Default.Search)
    data object Player : ShareDabbaDestination("player", "Player", Icons.Default.PlayCircle)
    data object Uploads : ShareDabbaDestination("uploads", "Uploads", Icons.Default.CloudUpload)
    data object Profile : ShareDabbaDestination("profile", "Hub", Icons.Default.Person)

    // Stage 3 Complete Screen Routes (All 19 Screens)
    data object Splash : ShareDabbaDestination("splash", "Splash")
    data object Onboarding : ShareDabbaDestination("onboarding", "Onboarding")
    data object Categories : ShareDabbaDestination("categories", "Categories")
    data object VideoDetails : ShareDabbaDestination("video_details/{videoId}", "Video Details") {
        fun createRoute(videoId: String): String = "video_details/$videoId"
    }
    data object RecentlyWatched : ShareDabbaDestination("recently_watched", "Recently Watched")
    data object UserDashboard : ShareDabbaDestination("user_dashboard", "User Dashboard")
    data object Earnings : ShareDabbaDestination("earnings", "Earnings")
    data object Wallet : ShareDabbaDestination("wallet", "Wallet")
    data object WithdrawalHistory : ShareDabbaDestination("withdrawal_history", "Withdrawal History")
    data object Notifications : ShareDabbaDestination("notifications", "Notifications")
    data object Settings : ShareDabbaDestination("settings", "Settings")
    data object HelpSupport : ShareDabbaDestination("help_support", "Help & Support")

    // Architecture Diagnostic Screens
    data object RemoteConfig : ShareDabbaDestination("remote_config", "Remote Config")
    data object Advertisements : ShareDabbaDestination("advertisements", "Advertisements")
    data object DeepLinks : ShareDabbaDestination("deep_links", "Deep Links")
}

val primaryNavigationDestinations = listOf(
    ShareDabbaDestination.Home,
    ShareDabbaDestination.Search,
    ShareDabbaDestination.Player,
    ShareDabbaDestination.Uploads,
    ShareDabbaDestination.Profile
)

@Composable
fun ShareDabbaMainScaffold(
    container: AppContainer,
    currentUser: FirebaseUser,
    initialDeepLinkStreamUrl: String?,
    onSignedOut: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ShareDabbaDestination.Home.route

    var activeStreamUrl by rememberSaveable {
        mutableStateOf(initialDeepLinkStreamUrl)
    }

    androidx.compose.runtime.LaunchedEffect(initialDeepLinkStreamUrl) {
        if (!initialDeepLinkStreamUrl.isNullOrBlank()) {
            activeStreamUrl = initialDeepLinkStreamUrl
            if (currentRoute != ShareDabbaDestination.Player.route) {
                navController.navigate(ShareDabbaDestination.Player.route) {
                    launchSingleTop = true
                }
            }
        }
    }

    val activeRemoteConfig by container.remoteConfigManager.activeConfig.collectAsStateWithLifecycle()

    val homeViewModel: HomeViewModel = viewModel(
        key = "home_${currentUser.uid}",
        factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    syncBookmarkUseCase = container.syncBookmarkUseCase,
                    streamCacheRepository = container.streamCacheRepository,
                    currentUserId = currentUser.uid
                )
            }
        }
    )

    val searchViewModel: SearchViewModel = viewModel(
        key = "search_${currentUser.uid}",
        factory = viewModelFactory {
            initializer {
                SearchViewModel(aiAssistantService = container.aiAssistantService)
            }
        }
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = ShareDabbaBlack,
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = ShareDabbaSurface,
                        contentColor = ShareDabbaWhite,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        primaryNavigationDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    dest.icon?.let {
                                        Icon(
                                            imageVector = it,
                                            contentDescription = dest.label
                                        )
                                    }
                                },
                                label = { Text(dest.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ShareDabbaBlack,
                                    selectedTextColor = ShareDabbaCyan,
                                    indicatorColor = ShareDabbaCyan,
                                    unselectedIconColor = ShareDabbaTextSecondary,
                                    unselectedTextColor = ShareDabbaTextSecondary
                                ),
                                modifier = Modifier.testTag("nav_tab_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = ShareDabbaSurface,
                        contentColor = ShareDabbaWhite,
                        modifier = Modifier.testTag("side_navigation_rail")
                    ) {
                        primaryNavigationDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    dest.icon?.let {
                                        Icon(
                                            imageVector = it,
                                            contentDescription = dest.label
                                        )
                                    }
                                },
                                label = { Text(dest.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ShareDabbaBlack,
                                    selectedTextColor = ShareDabbaCyan,
                                    indicatorColor = ShareDabbaCyan,
                                    unselectedIconColor = ShareDabbaTextSecondary,
                                    unselectedTextColor = ShareDabbaTextSecondary
                                ),
                                modifier = Modifier.testTag("nav_rail_${dest.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (initialDeepLinkStreamUrl != null) {
                        ShareDabbaDestination.Player.route
                    } else {
                        ShareDabbaDestination.Home.route
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ShareDabbaDestination.Splash.route) {
                        SplashScreen(
                            onSplashFinished = {
                                navController.navigate(ShareDabbaDestination.Onboarding.route) {
                                    popUpTo(ShareDabbaDestination.Splash.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(ShareDabbaDestination.Onboarding.route) {
                        OnboardingScreen(
                            onOnboardingComplete = {
                                navController.navigate(ShareDabbaDestination.Home.route) {
                                    popUpTo(ShareDabbaDestination.Home.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(ShareDabbaDestination.Home.route) {
                        HomeScreen(
                            viewModel = homeViewModel,
                            remoteConfig = activeRemoteConfig,
                            onNavigate = { route -> navController.navigate(route) },
                            onOpenVideoDetails = { videoId ->
                                navController.navigate(ShareDabbaDestination.VideoDetails.createRoute(videoId))
                            },
                            onPlayStream = { streamUrl ->
                                activeStreamUrl = streamUrl
                                navController.navigate(ShareDabbaDestination.Player.route)
                            }
                        )
                    }
                    composable(ShareDabbaDestination.Search.route) {
                        SearchScreen(
                            viewModel = searchViewModel,
                            onOpenVideoDetails = { videoId ->
                                navController.navigate(ShareDabbaDestination.VideoDetails.createRoute(videoId))
                            },
                            onPlayStream = { streamUrl ->
                                activeStreamUrl = streamUrl
                                navController.navigate(ShareDabbaDestination.Player.route)
                            }
                        )
                    }
                    composable(ShareDabbaDestination.Categories.route) {
                        CategoriesScreen(
                            onOpenVideoDetails = { videoId ->
                                navController.navigate(ShareDabbaDestination.VideoDetails.createRoute(videoId))
                            },
                            onWatchVideo = { _, streamUrl ->
                                activeStreamUrl = streamUrl
                                navController.navigate(ShareDabbaDestination.Player.route)
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(ShareDabbaDestination.VideoDetails.route) { backStackEntry ->
                        val videoId = backStackEntry.arguments?.getString("videoId")
                        VideoDetailsScreen(
                            videoId = videoId,
                            syncBookmarkUseCase = container.syncBookmarkUseCase,
                            onWatchClick = { _, streamUrl ->
                                activeStreamUrl = streamUrl
                                navController.navigate(ShareDabbaDestination.Player.route)
                            },
                            onSelectRelatedVideo = { relatedId ->
                                navController.navigate(ShareDabbaDestination.VideoDetails.createRoute(relatedId))
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(ShareDabbaDestination.Player.route) {
                        VideoPlayerScreen(
                            initialStreamUrl = activeStreamUrl,
                            syncBookmarkUseCase = container.syncBookmarkUseCase,
                            streamCacheRepository = container.streamCacheRepository,
                            onOpenRecentlyWatched = {
                                navController.navigate(ShareDabbaDestination.RecentlyWatched.route)
                            }
                        )
                    }
                    composable(ShareDabbaDestination.RecentlyWatched.route) {
                        RecentlyWatchedScreen(
                            streamCacheRepository = container.streamCacheRepository,
                            onResumeStream = { streamUrl ->
                                activeStreamUrl = streamUrl
                                navController.navigate(ShareDabbaDestination.Player.route)
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(ShareDabbaDestination.Uploads.route) {
                        UploadsScreen(
                            environmentConfig = container.environmentConfig,
                            websiteVideoLibraryRepository = container.websiteVideoLibraryRepository,
                            authenticatedUserId = currentUser.uid,
                            authenticatedUserDisplayName = currentUser.displayName ?: currentUser.email,
                            onOpenVideoDetails = { videoId ->
                                navController.navigate(ShareDabbaDestination.VideoDetails.createRoute(videoId))
                            },
                            onPlayStream = { streamUrl ->
                                activeStreamUrl = streamUrl
                                navController.navigate(ShareDabbaDestination.Player.route)
                            },
                            onOpenDashboard = {
                                navController.navigate(ShareDabbaDestination.UserDashboard.route)
                            }
                        )
                    }
                    composable(ShareDabbaDestination.UserDashboard.route) {
                        UserDashboardScreen(
                            environmentConfig = container.environmentConfig,
                            onNavigate = { route -> navController.navigate(route) },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(ShareDabbaDestination.Earnings.route) {
                        EarningsScreen(
                            environmentConfig = container.environmentConfig,
                            onOpenWallet = {
                                navController.navigate(ShareDabbaDestination.Wallet.route)
                            },
                            onOpenWithdrawals = {
                                navController.navigate(ShareDabbaDestination.WithdrawalHistory.route)
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(ShareDabbaDestination.Wallet.route) {
                        WalletScreen(
                            environmentConfig = container.environmentConfig,
                            monetizationCoordinator = container.monetizationCoordinator,
                            authenticatedUserId = currentUser.uid,
                            onOpenWithdrawalHistory = {
                                navController.navigate(ShareDabbaDestination.WithdrawalHistory.route)
                            },
                            onOpenEarnings = {
                                navController.navigate(ShareDabbaDestination.Earnings.route)
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(ShareDabbaDestination.WithdrawalHistory.route) {
                        WithdrawalHistoryScreen(
                            monetizationCoordinator = container.monetizationCoordinator,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(ShareDabbaDestination.Notifications.route) {
                        NotificationsScreen(
                            notificationsCoordinator = container.notificationsAndSettingsCoordinator,
                            sessionTokenCoordinator = container.sessionTokenCoordinator,
                            onNavigate = { route -> navController.navigate(route) },
                            onOpenDeepLinkVideoInPlayer = { videoKey ->
                                activeStreamUrl = videoKey
                                navController.navigate(ShareDabbaDestination.Player.route)
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(ShareDabbaDestination.Profile.route) {
                        ProfileScreen(
                            currentUser = currentUser,
                            sessionTokenCoordinator = container.sessionTokenCoordinator,
                            onNavigate = { route -> navController.navigate(route) },
                            onSignedOut = onSignedOut
                        )
                    }
                    composable(ShareDabbaDestination.Settings.route) {
                        SettingsScreen(
                            environmentConfig = container.environmentConfig,
                            streamCacheRepository = container.streamCacheRepository,
                            notificationsCoordinator = container.notificationsAndSettingsCoordinator,
                            remoteConfigManager = container.remoteConfigManager,
                            sessionTokenCoordinator = container.sessionTokenCoordinator,
                            onNavigate = { route -> navController.navigate(route) },
                            onSignedOut = onSignedOut,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(ShareDabbaDestination.HelpSupport.route) {
                        HelpSupportScreen(
                            environmentConfig = container.environmentConfig,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(ShareDabbaDestination.RemoteConfig.route) {
                        RemoteConfigScreen(
                            environmentConfig = container.environmentConfig,
                            remoteConfigManager = container.remoteConfigManager,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(ShareDabbaDestination.Advertisements.route) {
                        AdvertisementsScreen(
                            monetizationCoordinator = container.monetizationCoordinator,
                            remoteConfigManager = container.remoteConfigManager,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(ShareDabbaDestination.DeepLinks.route) {
                        DeepLinksScreen(
                            initialDeepLink = activeStreamUrl,
                            onOpenInPlayer = { resolvedUrl ->
                                activeStreamUrl = resolvedUrl
                                navController.navigate(ShareDabbaDestination.Player.route)
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
