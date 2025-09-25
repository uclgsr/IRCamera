package mpdc4gsr.viewmodel

/**
 * Sealed classes for type-safe UI state management
 * Replaces error codes and boolean flags with exhaustive state handling
 */
sealed class UiState {
    object Loading : UiState()
    object Idle : UiState()
    data class Success<T>(val data: T) : UiState()
    data class Error(val exception: AppError) : UiState()
}

sealed class AppError(val message: String, val cause: Throwable? = null) {
    data class NetworkError(val errorMessage: String, val errorCode: Int? = null) : AppError(errorMessage)
    data class SensorError(val sensorType: String, val errorMessage: String) : AppError(errorMessage)
    data class RecordingError(val operation: String, val errorMessage: String) : AppError(errorMessage)
    data class UnknownError(val errorMessage: String, val throwable: Throwable? = null) : AppError(errorMessage, throwable)
}

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceInfo: String? = null) : ConnectionState()
    data class Error(val error: AppError) : ConnectionState()
}

sealed class RecordingState {
    object Idle : RecordingState()
    object Starting : RecordingState()
    data class Recording(val sessionId: String, val duration: Long = 0) : RecordingState()
    object Paused : RecordingState()
    object Stopping : RecordingState()
    data class Error(val error: AppError) : RecordingState()
}