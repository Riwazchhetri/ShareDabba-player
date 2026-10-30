package com.example.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ai.AiAssistantService
import com.example.core.error.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val aiAssistantService: AiAssistantService
) : ViewModel() {

    private val _fastSearchState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val fastSearchState: StateFlow<UiState<String>> = _fastSearchState.asStateFlow()

    private val _deepThinkingState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val deepThinkingState: StateFlow<UiState<String>> = _deepThinkingState.asStateFlow()

    fun runLowLatencySearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _fastSearchState.value = UiState.Loading
            try {
                aiAssistantService.streamFastResponse(query).collect { partial ->
                    _fastSearchState.value = UiState.Success(partial)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _fastSearchState.value = UiState.Error(
                    e.localizedMessage ?: "Low-latency search failed."
                )
            }
        }
    }

    fun runHighThinkingAnalysis(complexQuery: String) {
        if (complexQuery.isBlank()) return
        viewModelScope.launch {
            _deepThinkingState.value = UiState.Loading
            val result = aiAssistantService.generateDeepAnalysis(complexQuery)
            _deepThinkingState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "High-thinking analysis failed.") }
            )
        }
    }
}
