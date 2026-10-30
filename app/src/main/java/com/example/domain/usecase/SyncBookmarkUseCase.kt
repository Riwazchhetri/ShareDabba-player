package com.example.domain.usecase

import com.example.data.repository.BookmarkRepository
import com.example.data.repository.StreamCacheRepository
import com.example.domain.model.StreamBookmark
import kotlinx.coroutines.flow.Flow

/**
 * Domain use case coordinating user stream bookmarks and local Room playback cache.
 */
class SyncBookmarkUseCase(
    private val bookmarkRepository: BookmarkRepository,
    private val streamCacheRepository: StreamCacheRepository
) {
    fun observeUserBookmarks(userId: String): Flow<List<StreamBookmark>> {
        return bookmarkRepository.observeBookmarks(userId)
    }

    suspend fun addBookmarkAndCache(
        title: String,
        streamUrl: String,
        category: String
    ): Result<String> {
        val result = bookmarkRepository.createBookmark(
            title = title,
            streamUrl = streamUrl,
            category = category
        )
        result.onSuccess { bookmarkId ->
            streamCacheRepository.cacheStreamPlayback(
                streamId = bookmarkId,
                title = title,
                streamUrl = streamUrl,
                positionMs = 0L
            )
        }
        return result
    }

    suspend fun deleteBookmark(bookmarkId: String): Result<Unit> {
        streamCacheRepository.removeCachedStream(bookmarkId)
        return bookmarkRepository.deleteBookmark(bookmarkId)
    }
}
