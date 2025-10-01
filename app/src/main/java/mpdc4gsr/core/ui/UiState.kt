package mpdc4gsr.core.ui

/**
 * Sealed classes for type-safe UI state management
 * Replaces error codes and boolean flags with exhaustive state handling
 *
 * NOTE: ConnectionState, RecordingState, and AppError are defined in ConnectionState.kt
 * This file only contains the generic UiState sealed class.
 */
sealed class UiState {
    object Loading : UiState()
    object Idle : UiState()
    data class Success<T>(val data: T) : UiState()
    data class Error(val exception: AppError) : UiState()
}