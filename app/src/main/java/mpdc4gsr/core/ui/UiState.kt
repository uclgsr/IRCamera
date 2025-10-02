package mpdc4gsr.core.ui

sealed class UiState {
    object Loading : UiState()
    object Idle : UiState()
    data class Success<T>(val data: T) : UiState()
    data class Error(val exception: AppError) : UiState()
}