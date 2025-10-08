package mpdc4gsr.presentation.screens.gsr

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.UnifiedGSRRecorder
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.gsr.data.GSRSettingsRepository
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GSRSensorViewModel @Inject constructor(
    @ApplicationContext private val application: Context,
    private val settingsRepository: GSRSettingsRepository
) : AppBaseViewModel() {

    companion object {
        // Reuse SimpleDateFormat instance for better performance
        private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        private const val MAX_HISTORY_SIZE = 100

        // Reconnection configuration (can be made user-configurable)
        const val DEFAULT_MAX_RECONNECTION_ATTEMPTS = 3
        const val DEFAULT_BASE_RECONNECTION_DELAY_MS = 2000L

        // Device scanning delay
        private const val DEVICE_SCAN_DELAY_MS = 3000L
    }

    data class GSRSensorState(
        val isConnected: Boolean = false,
        val isRecording: Boolean = false,
        val currentGSR: Float = 0f,
        val skinConductance: Float = 0f,
        val deviceBattery: Int = 0,
        val samplingRate: Int = 128,
        val gsrHistory: List<Float> = emptyList(),
        val error: String? = null,
        val connectionStatus: String = "Disconnected",
        val isReconnecting: Boolean = false,
        val reconnectionAttempt: Int = 0,
        val maxReconnectionAttempts: Int = 0
    )

    data class ReconnectionConfig(
        val maxAttempts: Int = DEFAULT_MAX_RECONNECTION_ATTEMPTS,
        val baseDelayMs: Long = DEFAULT_BASE_RECONNECTION_DELAY_MS,
        val enabled: Boolean = true
    )

    private val _sensorState = MutableStateFlow(GSRSensorState())
    val sensorState: StateFlow<GSRSensorState> = _sensorState.asStateFlow()
    private var reconnectionConfig = ReconnectionConfig()
    private var lastConnectedDeviceAddress: String? = null
    private var wasRecordingBeforeDisconnect = false

    // Expose recorder for lifecycle management from UI layer
    var gsrRecorder: UnifiedGSRRecorder? = null
        private set

    fun initializeRecorder(
        context: Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        reconnectionConfig: ReconnectionConfig? = null
    ) {
        viewModelScope.launch {
            try {
                // Load reconnection config from settings if not provided
                val configToUse =
                    reconnectionConfig ?: settingsRepository.deviceSettings.value?.let { deviceSettings ->
                        ReconnectionConfig(
                            maxAttempts = deviceSettings.reconnectionAttempts,
                            baseDelayMs = deviceSettings.reconnectionBaseDelayMs,
                            enabled = deviceSettings.autoReconnect
                        )
                    } ?: ReconnectionConfig()
                this@GSRSensorViewModel.reconnectionConfig = configToUse
                gsrRecorder = UnifiedGSRRecorder(
                    context = context,
                    lifecycleOwner = lifecycleOwner
                )
                val initialized = gsrRecorder?.initialize() ?: false
                if (initialized) {
                    _sensorState.update {
                        it.copy(
                            isConnected = false,
                            connectionStatus = "Initialized",
                            error = null
                        )
                    }
                    startDataCollection()
                    startConnectionMonitoring()
                    observeSettingsChanges()
                } else {
                    _sensorState.update {
                        it.copy(
                            connectionStatus = "Initialization Failed",
                            error = "Failed to initialize GSR recorder"
                        )
                    }
                }
            } catch (e: Exception) {
                _sensorState.update {
                    it.copy(
                        connectionStatus = "Error",
                        error = "Error initializing: ${e.message}"
                    )
                }
            }
        }
    }

    private fun observeSettingsChanges() {
        viewModelScope.launch {
            settingsRepository?.deviceSettings?.collect { deviceSettings ->
                reconnectionConfig = ReconnectionConfig(
                    maxAttempts = deviceSettings.reconnectionAttempts,
                    baseDelayMs = deviceSettings.reconnectionBaseDelayMs,
                    enabled = deviceSettings.autoReconnect
                )
            }
        }
    }

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
                        lastConnectedDeviceAddress = device.address
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

    fun updateReconnectionConfig(config: ReconnectionConfig) {
        reconnectionConfig = config
    }

    fun getReconnectionConfig(): ReconnectionConfig = reconnectionConfig

    fun getSettingsRepository() = settingsRepository

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
                    sessionStartIso = ISO_DATE_FORMAT.format(Date(currentTimeMs))
                )
                gsrRecorder?.startRecording(sessionDir, metadata)
                _sensorState.update { it.copy(isRecording = true, error = null) }
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Recording start failed: ${e.message}") }
            }
        }
    }

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

    private fun startConnectionMonitoring() {
        viewModelScope.launch {
            gsrRecorder?.deviceStatus?.collect { status ->
                val isConnectedNow = status.contains("Connected", ignoreCase = true)
                val isDisconnected = status.contains("Disconnected", ignoreCase = true)
                _sensorState.update { currentState ->
                    val wasConnected = currentState.isConnected
                    // Detect disconnection and trigger reconnection
                    if (wasConnected && isDisconnected && !currentState.isReconnecting) {
                        // Save recording state before disconnection
                        wasRecordingBeforeDisconnect = currentState.isRecording
                        if (reconnectionConfig.enabled) {
                            viewModelScope.launch {
                                attemptReconnection()
                            }
                        }
                    }
                    currentState.copy(
                        isConnected = isConnectedNow,
                        connectionStatus = status
                    )
                }
            }
        }
    }

    private suspend fun attemptReconnection() {
        val maxAttempts = reconnectionConfig.maxAttempts
        val baseDelay = reconnectionConfig.baseDelayMs
        for (attempt in 1..maxAttempts) {
            _sensorState.update {
                it.copy(
                    isReconnecting = true,
                    reconnectionAttempt = attempt,
                    maxReconnectionAttempts = maxAttempts,
                    connectionStatus = "Reconnecting (attempt $attempt/$maxAttempts)..."
                )
            }
            // True exponential backoff: baseDelay * 2^(attempt-1)
            val delay = baseDelay * (1L shl (attempt - 1))
            kotlinx.coroutines.delay(delay)
            try {
                // Try to get cached devices first
                var devices = gsrRecorder?.getDiscoveredDevices() ?: emptyList()
                // If no cached devices and we have a last connected address, try to find it
                var targetDevice = devices.find { it.address == lastConnectedDeviceAddress }
                // If still no device found, trigger a quick scan
                if (targetDevice == null && devices.isEmpty()) {
                    _sensorState.update {
                        it.copy(
                            connectionStatus = "Scanning for device (attempt $attempt/$maxAttempts)..."
                        )
                    }
                    val scanSuccess = gsrRecorder?.startDeviceDiscovery() ?: false
                    if (scanSuccess) {
                        kotlinx.coroutines.delay(DEVICE_SCAN_DELAY_MS)
                        devices = gsrRecorder?.getDiscoveredDevices() ?: emptyList()
                        targetDevice = devices.find { it.address == lastConnectedDeviceAddress }
                            ?: devices.firstOrNull()
                    }
                } else if (targetDevice == null) {
                    // Use first available device if last connected not found
                    targetDevice = devices.firstOrNull()
                }
                if (targetDevice != null) {
                    val connected = gsrRecorder?.connectToDevice(targetDevice) ?: false
                    if (connected) {
                        _sensorState.update {
                            it.copy(
                                isConnected = true,
                                isReconnecting = false,
                                reconnectionAttempt = 0,
                                maxReconnectionAttempts = 0,
                                connectionStatus = "Reconnected",
                                error = null
                            )
                        }
                        // Resume recording if it was active before disconnection
                        if (wasRecordingBeforeDisconnect) {
                            kotlinx.coroutines.delay(1000) // Brief delay to ensure stable connection
                            startRecording()
                            wasRecordingBeforeDisconnect = false
                        }
                        return
                    }
                } else {
                }
            } catch (e: Exception) {
            }
        }
        // All attempts failed
        _sensorState.update {
            it.copy(
                isReconnecting = false,
                reconnectionAttempt = 0,
                maxReconnectionAttempts = 0,
                connectionStatus = "Connection Lost",
                error = "Failed to reconnect after $maxAttempts attempts"
            )
        }
        wasRecordingBeforeDisconnect = false // Reset flag
    }

    fun exportData() {
        viewModelScope.launch {
            try {
                // Export functionality would be implemented here
                // For now, just log the action
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
            }
        }
    }
}
