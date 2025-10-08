package mpdc4gsr.feature.system.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.model.PCControllerInfo
import mpdc4gsr.core.data.model.SessionMetadata
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.system.domain.usecase.DiscoverControllersUseCase
import mpdc4gsr.feature.system.domain.usecase.StartRecordingUseCase
import mpdc4gsr.feature.system.domain.usecase.StopRecordingUseCase
import mpdc4gsr.feature.system.domain.usecase.SyncClocksUseCase

data class SystemUiState(
    val isRecording: Boolean = false,
    val sessionId: String? = null,
    val participantId: String? = null,
    val studyName: String? = null,
    val availableControllers: List<PCControllerInfo> = emptyList(),
    val connectedController: PCControllerInfo? = null,
    val clockOffsetMs: Long = 0L,
    val isDiscovering: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val sessionMetadata: SessionMetadata? = null
)

class SystemViewModel(
    private val context: Context,
    private val startRecordingUseCase: StartRecordingUseCase,
    private val stopRecordingUseCase: StopRecordingUseCase,
    private val discoverControllersUseCase: DiscoverControllersUseCase,
    private val syncClocksUseCase: SyncClocksUseCase
) : AppBaseViewModel() {

    private val _systemState = MutableStateFlow(SystemUiState())
    val systemState: StateFlow<SystemUiState> = _systemState.asStateFlow()

    fun startRecording(sessionId: String, participantId: String? = null, studyName: String? = null) {
        viewModelScope.launch {
            val result = startRecordingUseCase(sessionId, participantId, studyName)
            result.onSuccess { success ->
                if (success) {
                    _systemState.update {
                        it.copy(
                            isRecording = true,
                            sessionId = sessionId,
                            participantId = participantId,
                            studyName = studyName
                        )
                    }
                }
            }.onFailure { error ->
                _systemState.update { it.copy(error = error.message) }
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            val result = stopRecordingUseCase()
            result.onSuccess {
                _systemState.update {
                    it.copy(
                        isRecording = false,
                        sessionId = null
                    )
                }
            }.onFailure { error ->
                _systemState.update { it.copy(error = error.message) }
            }
        }
    }

    fun discoverControllers() {
        viewModelScope.launch {
            _systemState.update { it.copy(isDiscovering = true) }
            discoverControllersUseCase().collect { controllers ->
                _systemState.update {
                    it.copy(
                        availableControllers = controllers,
                        isDiscovering = false
                    )
                }
            }
        }
    }

    fun syncClocks() {
        viewModelScope.launch {
            _systemState.update { it.copy(isSyncing = true) }
            val result = syncClocksUseCase()
            result.onSuccess { offsetMs ->
                _systemState.update {
                    it.copy(
                        clockOffsetMs = offsetMs,
                        isSyncing = false
                    )
                }
            }.onFailure { error ->
                _systemState.update {
                    it.copy(
                        isSyncing = false,
                        error = error.message
                    )
                }
            }
        }
    }

    fun clearSystemError() {
        _systemState.update { it.copy(error = null) }
    }
}
