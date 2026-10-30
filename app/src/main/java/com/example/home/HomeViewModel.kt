package com.example.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.error.UiState
import com.example.core.logging.AppLogger
import com.example.data.local.CachedStreamEntity
import com.example.data.repository.StreamCacheRepository
import com.example.domain.model.StreamBookmark
import com.example.domain.usecase.SyncBookmarkUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val syncBookmarkUseCase: SyncBookmarkUseCase,
    private val streamCacheRepository: StreamCacheRepository,
    currentUserId: String
) : ViewModel() {

    val bookmarksState: StateFlow<UiState<List<StreamBookmark>>> =
        syncBookmarkUseCase.observeUserBookmarks(currentUserId)
            .map<List<StreamBookmark>, UiState<List<StreamBookmark>>> { UiState.Success(it) }
            .catch { error ->
                AppLogger.w(TAG, "Error observing bookmarks", error)
                emit(UiState.Error(error.message ?: "Failed to observe bookmarks"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val cachedStreamsState: StateFlow<List<CachedStreamEntity>> =
        streamCacheRepository.cachedStreams
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    fun addQuickBookmark(title: String, streamUrl: String, category: String) {
        if (title.isBlank() || streamUrl.isBlank()) {
            _actionFeedback.value = "Please enter both a stream title and URL."
            return
        }
        viewModelScope.launch {
            val result = syncBookmarkUseCase.addBookmarkAndCache(
                title = title,
                streamUrl = streamUrl,
                category = category.ifBlank { "Live" }
            )
            _actionFeedback.value = result.fold(
                onSuccess = { "Stream bookmark synced to Firestore & Room cache." },
                onFailure = { "Failed to save bookmark: ${it.localizedMessage}" }
            )
        }
    }

    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            val result = syncBookmarkUseCase.deleteBookmark(bookmarkId)
            _actionFeedback.value = result.fold(
                onSuccess = { "Bookmark removed." },
                onFailure = { "Delete failed: ${it.localizedMessage}" }
            )
        }
    }

    private companion object {
        const val TAG = "HomeViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
