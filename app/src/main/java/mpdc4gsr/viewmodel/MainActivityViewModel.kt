package mpdc4gsr.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main Activity ViewModel for sensor dashboard activities
 * Provides state management for thermal camera and GSR sensor
 */
class MainActivityViewModel : BaseViewModel() {
    
    // Sensor status enum
    enum class SensorStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        STREAMING,
        ERROR,
        SIMULATION
    }
    
    // GSR connection state enum
    enum class GSRConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        STREAMING,
        ERROR
    }
    
    // Sensor state data class
    data class SensorState(
        val status: SensorStatus = SensorStatus.DISCONNECTED,
        val deviceInfo: String? = null,
        val lastUpdate: Long = System.currentTimeMillis()
    )
    
    // Session state data class
    data class SessionState(
        val isRecording: Boolean = false,
        val sessionId: String? = null,
        val duration: Long = 0
    )
    
    // Thermal camera state
    private val _thermalCameraState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val thermalCameraState: StateFlow<ConnectionState> = _thermalCameraState.asStateFlow()
    
    // GSR sensor state  
    private val _gsrSensorState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val gsrSensorState: StateFlow<ConnectionState> = _gsrSensorState.asStateFlow()
    
    // GSR connection state (for backward compatibility)
    private val _gsrConnectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val gsrConnectionState: StateFlow<ConnectionState> = _gsrConnectionState.asStateFlow()
    
    // Battery level
    private val _gsrBatteryLevel = MutableStateFlow<Int?>(null)
    val gsrBatteryLevel: StateFlow<Int?> = _gsrBatteryLevel.asStateFlow()
    
    // Session state
    private val _sessionState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val sessionState: StateFlow<RecordingState> = _sessionState.asStateFlow()
    
    // Update methods
    fun updateThermalCameraState(state: ConnectionState) {
        _thermalCameraState.value = state
    }
    
    fun updateGSRSensorState(state: ConnectionState) {
        _gsrSensorState.value = state
        _gsrConnectionState.value = state
    }
    
    fun updateBatteryLevel(level: Int?) {
        _gsrBatteryLevel.value = level
    }
    
    fun updateSessionState(state: RecordingState) {
        _sessionState.value = state
    }
}
