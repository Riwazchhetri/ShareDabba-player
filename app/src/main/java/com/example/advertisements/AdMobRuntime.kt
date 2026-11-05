package com.example.advertisements

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-wide signal used by Compose ad slots after GMA initialization completes. */
object AdMobRuntime {
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    fun markInitialized() {
        _isInitialized.value = true
    }
}
