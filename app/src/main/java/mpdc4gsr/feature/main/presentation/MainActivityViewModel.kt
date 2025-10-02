package mpdc4gsr.feature.main.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.*

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainActivityViewModel"
    }

    private val _gsrConnectionState = MutableStateFlow(GSRConnectionState.DISCONNECTED)
    val gsrConnectionState: StateFlow<GSRConnectionState> = _gsrConnectionState.asStateFlow()

    private val _gsrBatteryLevel = MutableStateFlow<Int?>(null)
    val gsrBatteryLevel: StateFlow<Int?> = _gsrBatteryLevel.asStateFlow()

    data class GSRDataState(
        val currentValue: Float = 0f,
        val batteryLevel: Int = 0,
        val recentReadings: List<Float> = emptyList(),
        val averageValue: Float = 0f,
        val minValue: Float = 0f,
        val maxValue: Float = 0f
    )

    private val _gsrData = MutableStateFlow(GSRDataState())
    val gsrData: StateFlow<GSRDataState> = _gsrData.asStateFlow()

    private val _sessionState = MutableStateFlow(SessionState.IDLE)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _thermalCameraState = MutableStateFlow(SensorState())
    val thermalCameraState: StateFlow<SensorState> = _thermalCameraState.asStateFlow()

    private val _gsrSensorState = MutableStateFlow(SensorState())
    val gsrSensorState: StateFlow<SensorState> = _gsrSensorState.asStateFlow()

    enum class GSRConnectionState { DISCONNECTED, DISCOVERING, CONNECTING, CONNECTED, ERROR }
    enum class SessionState { IDLE, STARTING, RECORDING, PAUSED, STOPPING, ERROR }
    enum class SensorStatus { DISCONNECTED, CONNECTING, CONNECTED, STREAMING, ERROR, SIMULATION }

    data class SensorState(
        val status: SensorStatus = SensorStatus.DISCONNECTED,
        val message: String? = null,
        val isRecording: Boolean = false,
        val lastUpdate: Long = System.currentTimeMillis()
    )

    init {
        Log.d(TAG, "MainActivityViewModel initialized.")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "MainActivityViewModel cleared")
    }
}
