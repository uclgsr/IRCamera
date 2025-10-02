package mpdc4gsr.feature.gsr.presentation

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.UnifiedGSRRecorder
import mpdc4gsr.core.ui.AppBaseViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * ViewModel for GSR Sensor Screen
 * Manages UnifiedGSRRecorder lifecycle and data flow
 */
class GSRSensorViewModel(
    private val application: Application
) : AppBaseViewModel() {

    companion object {
        // Reuse SimpleDateFormat instance for better performance
        private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        private const val MAX_HISTORY_SIZE = 100
    }

    data class GSRSensorState(
        val isConnected: Boolean = false,
        val isRecording: Boolean = false,
        val currentGSR: Float = 0f,
        val skinConductance: Float = 0f,
        val deviceBattery: Int = 0,
        val samplingRate: Int = 128,
        val gsrHistory: List<Float> = emptyList(),
        val error: String? = null
    )

    private val _sensorState = MutableStateFlow(GSRSensorState())
    val sensorState: StateFlow<GSRSensorState> = _sensorState.asStateFlow()

    // Expose recorder for lifecycle management from UI layer
    var gsrRecorder: UnifiedGSRRecorder? = null
        private set

    /**
     * Initialize GSR recorder without lifecycle owner
     * Lifecycle should be managed from UI layer using DisposableEffect
     */
    fun initializeRecorder(context: android.content.Context, lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        viewModelScope.launch {
            try {
                gsrRecorder = UnifiedGSRRecorder(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    samplingRateHz = 128
                )
                
                val initialized = gsrRecorder?.initialize() ?: false
                if (initialized) {
                    _sensorState.update { it.copy(isConnected = true, error = null) }
                    startDataCollection()
                } else {
                    _sensorState.update { it.copy(error = "Failed to initialize GSR recorder") }
                }
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Error initializing: ${e.message}") }
            }
        }
    }

    /**
     * Connect to GSR device
     */
    fun connectDevice() {
        viewModelScope.launch {
            try {
                // Scan for devices and connect to the first available one
                val devices = gsrRecorder?.getDiscoveredDevices()
                if (devices.isNullOrEmpty()) {
                    _sensorState.update { it.copy(error = "No devices found. Please scan first.") }
                    return@launch
                }
                
                // Connect to first available device
                val device = devices.firstOrNull()
                if (device != null) {
                    // Actually connect to the device using the recorder
                    val connected = gsrRecorder?.connectToDevice(device) ?: false
                    if (connected) {
                        _sensorState.update { it.copy(isConnected = true, error = null) }
                    } else {
                        _sensorState.update { it.copy(error = "Failed to connect to device") }
                    }
                } else {
                    _sensorState.update { it.copy(error = "No valid device to connect") }
                }
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Connection failed: ${e.message}") }
            }
        }
    }

    /**
     * Disconnect from GSR device
     */
    fun disconnectDevice() {
        viewModelScope.launch {
            try {
                gsrRecorder?.stopRecording()
                _sensorState.update { it.copy(isConnected = false, isRecording = false) }
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Disconnect failed: ${e.message}") }
            }
        }
    }

    /**
     * Start recording GSR data
     */
    fun startRecording() {
        viewModelScope.launch {
            try {
                val sessionDir = application.getExternalFilesDir("gsr_sessions")?.absolutePath 
                    ?: application.filesDir.absolutePath
                
                val currentTimeMs = System.currentTimeMillis()
                val currentMonotonicNs = System.nanoTime()
                
                val metadata = mpdc4gsr.core.data.SessionMetadata(
                    sessionId = "gsr_${currentTimeMs}",
                    sessionStartTimestampMs = currentTimeMs,
                    sessionStartMonotonicNs = currentMonotonicNs,
                    sessionStartIso = ISO_DATE_FORMAT.format(java.util.Date(currentTimeMs))
                )
                
                gsrRecorder?.startRecording(sessionDir, metadata)
                _sensorState.update { it.copy(isRecording = true, error = null) }
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Recording start failed: ${e.message}") }
            }
        }
    }

    /**
     * Stop recording GSR data
     */
    fun stopRecording() {
        viewModelScope.launch {
            try {
                gsrRecorder?.stopRecording()
                _sensorState.update { it.copy(isRecording = false, error = null) }
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Recording stop failed: ${e.message}") }
            }
        }
    }

    /**
     * Start collecting GSR data from recorder
     */
    private fun startDataCollection() {
        // Collect recording status
        viewModelScope.launch {
            gsrRecorder?.getStatusFlow()?.collect { status ->
                _sensorState.update { currentState ->
                    currentState.copy(
                        isRecording = status.isRecording,
                        samplingRate = status.currentDataRate.toInt()
                    )
                }
            }
        }
        
        // Collect actual GSR data samples
        viewModelScope.launch {
            gsrRecorder?.getDataStream()?.collect { gsrSample ->
                _sensorState.update { currentState ->
                    val newGSR = gsrSample.gsrMicrosiemens.toFloat()
                    val newHistory = (currentState.gsrHistory + newGSR).takeLast(MAX_HISTORY_SIZE)
                    
                    // Calculate skin conductance (same as GSR in microsiemens)
                    val skinConductance = newGSR
                    
                    currentState.copy(
                        currentGSR = newGSR,
                        skinConductance = skinConductance,
                        gsrHistory = newHistory
                    )
                }
            }
        }
    }

    /**
     * Export GSR data
     */
    fun exportData() {
        viewModelScope.launch {
            try {
                // Export functionality would be implemented here
                // For now, just log the action
                android.util.Log.d("GSRSensorViewModel", "Export data requested")
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Export failed: ${e.message}") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                gsrRecorder?.stopRecording()
                gsrRecorder?.cleanup()
            } catch (e: Exception) {
                android.util.Log.e("GSRSensorViewModel", "Error during cleanup", e)
            }
        }
    }
}
