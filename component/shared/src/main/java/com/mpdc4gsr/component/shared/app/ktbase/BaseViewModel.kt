package com.mpdc4gsr.component.shared.app.ktbase

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

open class BaseViewModel :
    ViewModel(),
    LifecycleObserver {
    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val isRefreshing: Boolean = false,
    )

    sealed class UiEvent {
        data class ShowError(
            val message: String,
        ) : UiEvent()

        data class ShowMessage(
            val message: String,
        ) : UiEvent()

        object NavigateBack : UiEvent()
    }

    protected val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    protected val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    protected val exceptionHandler =
        CoroutineExceptionHandler { _, exception ->
            handleError(exception)
        }

    protected open fun handleError(exception: Throwable) {
        val errorMessage = exception.message ?: "Unknown error occurred"
        _uiState.update { it.copy(error = errorMessage, isLoading = false) }
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowError(errorMessage))
        }
    }

    protected fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    protected fun setRefreshing(isRefreshing: Boolean) {
        _uiState.update { it.copy(isRefreshing = isRefreshing) }
    }

    open fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    protected fun launchWithErrorHandling(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            block()
        }
    }

    protected fun launchWithLoading(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            setLoading(true)
            try {
                block()
            } finally {
                setLoading(false)
            }
        }
    }
}


