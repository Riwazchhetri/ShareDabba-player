package com.example.app

import android.app.Application
import com.example.R
import com.example.advertisements.MonetizationCoordinator
import com.example.authentication.SessionTokenCoordinator
import com.example.core.ai.AiAssistantService
import com.example.core.config.EnvironmentConfig
import com.example.core.logging.AppLogger
import com.example.data.local.ShareDabbaCacheDatabase
import com.example.data.repository.BookmarkRepository
import com.example.data.repository.StreamCacheRepository
import com.example.data.repository.WebsiteVideoLibraryRepository
import com.example.domain.usecase.SyncBookmarkUseCase
import com.example.network.NetworkClient
import com.example.network.ShareDabbaApiService
import com.example.remoteconfig.RemoteConfigManager
import com.example.settings.NotificationsAndSettingsCoordinator
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Dependency Injection container holding application-scoped services and repositories.
 * Note: Firebase App Check is intentionally initialized in MainActivity.onCreate(intent)
 * rather than Application.onCreate() to preserve launch Intent debug token extraction.
 */
class AppContainer(context: Application) {
    val environmentConfig = EnvironmentConfig()

    val databaseId: String = context.getString(R.string.firestore_database_id)

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(databaseId)
    }

    val localCacheDatabase: ShareDabbaCacheDatabase by lazy {
        ShareDabbaCacheDatabase.getInstance(context)
    }

    val bookmarkRepository: BookmarkRepository by lazy {
        BookmarkRepository(firestore)
    }

    val streamCacheRepository: StreamCacheRepository by lazy {
        StreamCacheRepository(localCacheDatabase.streamCacheDao())
    }

    val syncBookmarkUseCase: SyncBookmarkUseCase by lazy {
        SyncBookmarkUseCase(bookmarkRepository, streamCacheRepository)
    }

    val sessionTokenCoordinator: SessionTokenCoordinator by lazy {
        SessionTokenCoordinator(context.applicationContext)
    }

    val apiService: ShareDabbaApiService by lazy {
        NetworkClient.createApiService(environmentConfig)
    }

    val websiteVideoLibraryRepository: WebsiteVideoLibraryRepository by lazy {
        WebsiteVideoLibraryRepository(environmentConfig, apiService)
    }

    val remoteConfigManager: RemoteConfigManager by lazy {
        RemoteConfigManager(environmentConfig)
    }

    val monetizationCoordinator: MonetizationCoordinator by lazy {
        MonetizationCoordinator(environmentConfig, remoteConfigManager)
    }

    val notificationsAndSettingsCoordinator: NotificationsAndSettingsCoordinator by lazy {
        NotificationsAndSettingsCoordinator(
            context = context.applicationContext,
            environmentConfig = environmentConfig,
            remoteConfigManager = remoteConfigManager
        )
    }

    val aiAssistantService: AiAssistantService by lazy {
        AiAssistantService()
    }
}

class ShareDabbaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.remoteConfigManager.fetchOnAppLaunch()
        AppLogger.i(
            "ShareDabbaApp",
            "ShareDabbaApplication initialized (appId=${container.environmentConfig.applicationId})"
        )
    }
}
