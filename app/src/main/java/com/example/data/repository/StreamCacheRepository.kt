package com.example.data.repository

import com.example.data.local.CachedStreamEntity
import com.example.data.local.StreamCacheDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository abstracting local Room cache access for recently played streams.
 * Strictly for device-local playback caching (not a replacement for the website database).
 */
class StreamCacheRepository(private val dao: StreamCacheDao) {
    val cachedStreams: Flow<List<CachedStreamEntity>> = dao.observeCachedStreams()

    suspend fun cacheStreamPlayback(
        streamId: String,
        title: String,
        streamUrl: String,
        positionMs: Long
    ) {
        dao.upsertStreamCache(
            CachedStreamEntity(
                streamId = streamId,
                title = title,
                streamUrl = streamUrl,
                lastPlaybackPositionMs = positionMs,
                cachedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeCachedStream(streamId: String) {
        dao.removeCachedStream(streamId)
    }

    suspend fun clearCache() {
        dao.clearAllCache()
    }
}
