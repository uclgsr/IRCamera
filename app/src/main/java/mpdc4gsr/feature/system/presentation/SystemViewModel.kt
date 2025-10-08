package mpdc4gsr.feature.system.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.model.PCControllerInfo
import mpdc4gsr.feature.system.domain.usecase.*
import javax.inject.Inject

@HiltViewModel
class SystemViewModel @Inject constructor(
    private val startRecordingUseCase: StartRecordingUseCase,
    private val stopRecordingUseCase: StopRecordingUseCase,
    private val discoverControllersUseCase: DiscoverControllersUseCase,
    private val syncClocksUseCase: SyncClocksUseCase
) : ViewModel() {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _controllers = MutableStateFlow<List<PCControllerInfo>>(emptyList())
    val controllers: StateFlow<List<PCControllerInfo>> = _controllers.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    fun startRecording() {
        viewModelScope.launch {
            startRecordingUseCase().onSuccess {
                _isRecording.value = true
            }.onFailure { throwable ->
                _error.emit(throwable.message ?: "Failed to start recording")
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            stopRecordingUseCase().onSuccess {
                _isRecording.value = false
            }.onFailure { throwable ->
                _error.emit(throwable.message ?: "Failed to stop recording")
            }
        }
    }

    fun discoverControllers() {
        viewModelScope.launch {
            discoverControllersUseCase().collect { controllers ->
                _controllers.value = controllers
            }
        }
    }

    fun syncClocks(controllerId: String) {
        viewModelScope.launch {
            syncClocksUseCase(controllerId).onFailure { throwable ->
                _error.emit(throwable.message ?: "Failed to sync clocks")
            }
        }
    }
}
