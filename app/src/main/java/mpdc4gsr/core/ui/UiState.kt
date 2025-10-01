package mpdc4gsr.viewmodel

/**
 * Sealed classes for type-safe UI state management
 * Replaces error codes and boolean flags with exhaustive state handling
 * 
 * NOTE: ConnectionState, RecordingState, and AppError have been moved to mpdc4gsr.core.ui
 * for Clean Architecture compliance. Type aliases provided below for backward compatibility.
 */
sealed class UiState {
    object Loading : UiState()
    object Idle : UiState()
    data class Success<T>(val data: T) : UiState()
    data class Error(val exception: mpdc4gsr.core.ui.AppError) : UiState()
}

// Type aliases for backward compatibility
// These classes have been moved to core.ui for Clean Architecture compliance
typealias ConnectionState = mpdc4gsr.core.ui.ConnectionState
typealias RecordingState = mpdc4gsr.core.ui.RecordingState
typealias AppError = mpdc4gsr.core.ui.AppError