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
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
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

    companion object {
        private const val TAG = "SystemViewModel"
    }

    private val _systemState = MutableStateFlow(SystemUiState())
    val systemState: StateFlow<SystemUiState> = _systemState.asStateFlow()

    fun startRecording(sessionId: String, participantId: String? = null, studyName: String? = null) {
        viewModelScope.launch {
            try {
                AppLogger.i(TAG, "Starting recording: $sessionId")
                _systemState.update { it.copy(error = null) }
                
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
                        AppLogger.i(TAG, "Recording started successfully")
                    }
                }.onFailure { error ->
                    _systemState.update { it.copy(error = error.message) }
                    AppLogger.e(TAG, "Failed to start recording", error)
                }
            } catch (e: Exception) {
                _systemState.update { it.copy(error = e.message) }
                AppLogger.e(TAG, "Exception starting recording", e)
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                AppLogger.i(TAG, "Stopping recording")
                _systemState.update { it.copy(error = null) }
                
                val result = stopRecordingUseCase()
                
                result.onSuccess { results ->
                    _systemState.update {
                        it.copy(
                            isRecording = false,
                            sessionId = null
                        )
                    }
                    AppLogger.i(TAG, "Recording stopped successfully: $results")
                }.onFailure { error ->
                    _systemState.update { it.copy(error = error.message) }
                    AppLogger.e(TAG, "Failed to stop recording", error)
                }
            } catch (e: Exception) {
                _systemState.update { it.copy(error = e.message) }
                AppLogger.e(TAG, "Exception stopping recording", e)
            }
        }
    }

    fun discoverControllers() {
        viewModelScope.launch {
            try {
                AppLogger.i(TAG, "Discovering PC controllers")
                _systemState.update { it.copy(isDiscovering = true, error = null) }
                
                discoverControllersUseCase().collect { controllers ->
                    _systemState.update {
                        it.copy(
                            availableControllers = controllers,
                            isDiscovering = false
                        )
                    }
                    AppLogger.i(TAG, "Found ${controllers.size} controllers")
                }
            } catch (e: Exception) {
                _systemState.update {
                    it.copy(
                        isDiscovering = false,
                        error = e.message
                    )
                }
                AppLogger.e(TAG, "Exception discovering controllers", e)
            }
        }
    }

    fun syncClocks() {
        viewModelScope.launch {
            try {
                AppLogger.i(TAG, "Syncing clocks")
                _systemState.update { it.copy(isSyncing = true, error = null) }
                
                val result = syncClocksUseCase()
                
                result.onSuccess { offsetMs ->
                    _systemState.update {
                        it.copy(
                            clockOffsetMs = offsetMs,
                            isSyncing = false
                        )
                    }
                    AppLogger.i(TAG, "Clock sync successful, offset: ${offsetMs}ms")
                }.onFailure { error ->
                    _systemState.update {
                        it.copy(
                            isSyncing = false,
                            error = error.message
                        )
                    }
                    AppLogger.e(TAG, "Failed to sync clocks", error)
                }
            } catch (e: Exception) {
                _systemState.update {
                    it.copy(
                        isSyncing = false,
                        error = e.message
                    )
                }
                AppLogger.e(TAG, "Exception syncing clocks", e)
            }
        }
    }

    fun clearSystemError() {
        _systemState.update { it.copy(error = null) }
    }
}
