package mpdc4gsr.activities

// import android.app.Application
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for Simplified Main Activity
 * Manages simplified interface state and system monitoring
 */
class SimplifiedMainViewModel : BaseViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val systemStatus: SystemStatus = SystemStatus(
            overallHealth = HealthStatus.HEALTHY,
            isRecording = false
        ),
        val connectionStatus: ConnectionStatus = ConnectionStatus(
            thermalCameraConnected = false,
            gsrSensorConnected = false
        ),
        val isRecording: Boolean = false,
        val recordingDuration: String = "00:00:00",
        val recentSessions: List<RecentSession> = emptyList()
    )

    private val _viewState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _viewState.asStateFlow()

    private var recordingStartTime: Long = 0
    private var recordingTimerJob: Job? = null

    init {
        refreshStatus()
        loadRecentSessions()
    }

    /**
     * Refresh system status
     */
    fun refreshStatus() {
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(isLoading = true, error = null)

            try {
                // Simulate system status check
                delay(1000) // Simulate checking time

                val thermalConnected = checkThermalCameraConnection()
                val gsrConnected = checkGSRSensorConnection()

                val overallHealth = when {
                    !thermalConnected && !gsrConnected -> HealthStatus.ERROR
                    !thermalConnected || !gsrConnected -> HealthStatus.WARNING
                    else -> HealthStatus.HEALTHY
                }

                _viewState.value = _viewState.value.copy(
                    isLoading = false,
                    systemStatus = SystemStatus(
                        overallHealth = overallHealth,
                        isRecording = _viewState.value.isRecording
                    ),
                    connectionStatus = ConnectionStatus(
                        thermalCameraConnected = thermalConnected,
                        gsrSensorConnected = gsrConnected
                    )
                )

            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(
                    isLoading = false,
                    error = "Failed to refresh status: ${e.message}"
                )
            }
        }
    }

    /**
     * Toggle recording state
     */
    fun toggleRecording() {
        if (_viewState.value.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    /**
     * Start recording
     */
    fun startRecording() {
        viewModelScope.launch {
            try {
                recordingStartTime = System.currentTimeMillis()

                _viewState.value = _viewState.value.copy(
                    isRecording = true,
                    systemStatus = _viewState.value.systemStatus.copy(isRecording = true)
                )

                startRecordingTimer()

            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(
                    error = "Failed to start recording: ${e.message}"
                )
            }
        }
    }

    /**
     * Stop recording
     */
    fun stopRecording() {
        viewModelScope.launch {
            try {
                recordingTimerJob?.cancel()

                _viewState.value = _viewState.value.copy(
                    isRecording = false,
                    recordingDuration = "00:00:00",
                    systemStatus = _viewState.value.systemStatus.copy(isRecording = false)
                )

                // Simulate saving session
                saveRecordingSession()

            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(
                    error = "Failed to stop recording: ${e.message}"
                )
            }
        }
    }

    /**
     * Launch thermal camera activity
     */
    fun launchThermalCamera() {
        viewModelScope.launch {
            try {
                // In a real implementation, you would launch the thermal camera activity
                _viewState.value = _viewState.value.copy(
                    error = "Thermal camera functionality would be launched here"
                )

            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(
                    error = "Failed to launch thermal camera: ${e.message}"
                )
            }
        }
    }

    /**
     * Launch GSR sensor activity
     */
    fun launchGSRSensor() {
        viewModelScope.launch {
            try {
                // In a real implementation, you would launch the GSR sensor activity
                _viewState.value = _viewState.value.copy(
                    error = "GSR sensor functionality would be launched here"
                )

            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(
                    error = "Failed to launch GSR sensor: ${e.message}"
                )
            }
        }
    }

    /**
     * Launch settings activity
     */
    fun launchSettings() {
        viewModelScope.launch {
            try {
                // In a real implementation, you would launch the settings activity
                _viewState.value = _viewState.value.copy(
                    error = "Settings would be launched here"
                )

            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(
                    error = "Failed to launch settings: ${e.message}"
                )
            }
        }
    }

    /**
     * Open a recent session
     */
    fun openSession(session: RecentSession) {
        viewModelScope.launch {
            try {
                // In a real implementation, you would open the session details
                _viewState.value = _viewState.value.copy(
                    error = "Session '${session.name}' would be opened here"
                )

            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(
                    error = "Failed to open session: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearViewError() {
        _viewState.value = _viewState.value.copy(error = null)
    }

    /**
     * Load recent sessions
     */
    private fun loadRecentSessions() {
        viewModelScope.launch {
            try {
                // Simulate loading recent sessions
                val sessions = listOf(
                    RecentSession(
                        id = "1",
                        name = "GSR Session 2024-01-15",
                        date = "Jan 15, 2024 14:30"
                    ),
                    RecentSession(
                        id = "2",
                        name = "Thermal Recording",
                        date = "Jan 14, 2024 16:45"
                    ),
                    RecentSession(
                        id = "3",
                        name = "Multi-Modal Session",
                        date = "Jan 13, 2024 09:15"
                    )
                )

                _viewState.value = _viewState.value.copy(recentSessions = sessions)

            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(
                    error = "Failed to load recent sessions: ${e.message}"
                )
            }
        }
    }

    /**
     * Start recording timer
     */
    private fun startRecordingTimer() {
        recordingTimerJob = viewModelScope.launch {
            while (_viewState.value.isRecording) {
                val elapsed = System.currentTimeMillis() - recordingStartTime
                val duration = formatDuration(elapsed)

                _viewState.value = _viewState.value.copy(recordingDuration = duration)

                delay(1000) // Update every second
            }
        }
    }

    /**
     * Save recording session
     */
    private fun saveRecordingSession() {
        viewModelScope.launch {
            try {
                val sessionName =
                    "Recording ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}"
                val newSession = RecentSession(
                    id = System.currentTimeMillis().toString(),
                    name = sessionName,
                    date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())
                )

                val updatedSessions = listOf(newSession) + _viewState.value.recentSessions
                _viewState.value = _viewState.value.copy(recentSessions = updatedSessions)

            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(
                    error = "Failed to save session: ${e.message}"
                )
            }
        }
    }

    /**
     * Check thermal camera connection
     */
    private suspend fun checkThermalCameraConnection(): Boolean {
        // Simulate connection check
        delay(500)
        return (0..1).random() == 1 // 50% chance of being connected
    }

    /**
     * Check GSR sensor connection
     */
    private suspend fun checkGSRSensorConnection(): Boolean {
        // Simulate connection check
        delay(500)
        return (0..1).random() == 1 // 50% chance of being connected
    }

    /**
     * Format duration in milliseconds to HH:MM:SS
     */
    private fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }

    override fun onCleared() {
        super.onCleared()
        recordingTimerJob?.cancel()
    }
}

// Data classes for SimplifiedMainViewModel
data class SystemStatus(
    val overallHealth: HealthStatus,
    val isRecording: Boolean
)

data class ConnectionStatus(
    val thermalCameraConnected: Boolean,
    val gsrSensorConnected: Boolean
)

data class RecentSession(
    val id: String,
    val name: String,
    val date: String
)

enum class HealthStatus(val displayName: String) {
    HEALTHY("Healthy"),
    WARNING("Warning"),
    ERROR("Error")
}
