package com.mpdc4gsr.component.thermal.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.component.shared.app.ktbase.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class IRMonitorCaptureViewModel : BaseViewModel() {
    // Data classes matching the fragment requirements
    data class TemperatureData(
        val centerTemp: Float,
        val maxTemp: Float,
        val minTemp: Float,
    )

    data class CaptureData(
        val id: Int,
        val timestamp: Long,
        val temperature: Float,
        val imagePath: String,
    )

    enum class DeviceConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR,
    }

    enum class CaptureState {
        INACTIVE,
        ACTIVE,
        CONTINUOUS,
        CAPTURING,
    }

    // StateFlow properties for UI state management
    private val _captureState = MutableStateFlow(CaptureState.INACTIVE)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()
    private val _temperatureData = MutableStateFlow<TemperatureData?>(null)
    val temperatureData: StateFlow<TemperatureData?> = _temperatureData.asStateFlow()
    private val _captureHistory = MutableStateFlow<List<CaptureData>>(emptyList())
    val captureHistory: StateFlow<List<CaptureData>> = _captureHistory.asStateFlow()
    private val _deviceConnectionState = MutableStateFlow(DeviceConnectionState.DISCONNECTED)
    val deviceConnectionState: StateFlow<DeviceConnectionState> = _deviceConnectionState.asStateFlow()

    // Internal state
    private var captureIdCounter = 1
    private var continuousCapturingJob: kotlinx.coroutines.Job? = null

    init {
        // Initialize with mock data for development
        initializeMockData()
        // Start temperature monitoring simulation
        startTemperatureMonitoring()
    }

    fun toggleCapture() {
        viewModelScope.launch {
            when (_captureState.value) {
                CaptureState.INACTIVE -> {
                    _captureState.value = CaptureState.ACTIVE
                    simulateDeviceConnection()
                }

                CaptureState.ACTIVE -> {
                    _captureState.value = CaptureState.INACTIVE
                    stopContinuousCapture()
                }

                CaptureState.CONTINUOUS -> {
                    stopContinuousCapture()
                    _captureState.value = CaptureState.ACTIVE
                }

                CaptureState.CAPTURING -> {
                    // Already capturing, ignore
                }
            }
        }
    }

    fun captureFrame() {
        if (_deviceConnectionState.value != DeviceConnectionState.CONNECTED) return
        viewModelScope.launch {
            _captureState.value = CaptureState.CAPTURING
            // Simulate capture delay
            delay(500)
            // Create capture data
            val currentTemp = _temperatureData.value?.centerTemp ?: 25.0f
            val capture =
                CaptureData(
                    id = captureIdCounter++,
                    timestamp = System.currentTimeMillis(),
                    temperature = currentTemp,
                    imagePath = "/mock/path/capture_${captureIdCounter - 1}.jpg",
                )
            // Add to history
            val currentHistory = _captureHistory.value.toMutableList()
            currentHistory.add(0, capture) // Add to beginning
            _captureHistory.value = currentHistory
            // Return to previous state
            _captureState.value =
                if (continuousCapturingJob?.isActive == true) {
                    CaptureState.CONTINUOUS
                } else {
                    CaptureState.ACTIVE
                }
        }
    }

    fun toggleContinuousCapture() {
        if (_deviceConnectionState.value != DeviceConnectionState.CONNECTED) return
        viewModelScope.launch {
            if (_captureState.value == CaptureState.CONTINUOUS) {
                stopContinuousCapture()
                _captureState.value = CaptureState.ACTIVE
            } else {
                startContinuousCapture()
            }
        }
    }

    fun clearCaptureHistory() {
        viewModelScope.launch {
            _captureHistory.value = emptyList()
        }
    }

    fun exportCaptures() {
        viewModelScope.launch {
            val captures = _captureHistory.value
            if (captures.isEmpty()) {
                return@launch
            }
            // Create export data with capture information
            val exportData =
                captures.map { capture ->
                    mapOf(
                        "id" to capture.id,
                        "timestamp" to capture.timestamp,
                        "temperature" to capture.temperature,
                        "imagePath" to capture.imagePath,
                    )
                }
            // In a real implementation, this would write to a file or share the data
            // For now, we log the export action
        }
    }

    fun deleteCapture(capture: CaptureData) {
        viewModelScope.launch {
            val currentHistory = _captureHistory.value.toMutableList()
            currentHistory.remove(capture)
            _captureHistory.value = currentHistory
        }
    }

    // Private helper methods
    private fun initializeMockData() {
        // Initialize with mock temperature data
        _temperatureData.value =
            TemperatureData(
                centerTemp = 25.0f,
                maxTemp = 28.5f,
                minTemp = 22.1f,
            )
        _deviceConnectionState.value = DeviceConnectionState.DISCONNECTED
    }

    private fun simulateDeviceConnection() {
        viewModelScope.launch {
            _deviceConnectionState.value = DeviceConnectionState.CONNECTING
            delay(2000) // Simulate connection delay
            _deviceConnectionState.value = DeviceConnectionState.CONNECTED
        }
    }

    private fun startTemperatureMonitoring() {
        viewModelScope.launch {
            while (true) {
                if (_deviceConnectionState.value == DeviceConnectionState.CONNECTED) {
                    // Simulate temperature readings with variation using Kotlin Random
                    val baseTemp = 25.0f
                    val variation = (Random.nextFloat() - 0.5f) * 5.0f
                    val centerTemp = baseTemp + variation
                    _temperatureData.value =
                        TemperatureData(
                            centerTemp = centerTemp,
                            maxTemp = centerTemp + (Random.nextFloat() * 3.0f),
                            minTemp = centerTemp - (Random.nextFloat() * 2.0f),
                        )
                }
                delay(1000) // Update every second
            }
        }
    }

    private fun startContinuousCapture() {
        _captureState.value = CaptureState.CONTINUOUS
        continuousCapturingJob =
            viewModelScope.launch {
                while (_captureState.value == CaptureState.CONTINUOUS) {
                    captureFrame()
                    delay(3000) // Capture every 3 seconds
                }
            }
    }

    private fun stopContinuousCapture() {
        continuousCapturingJob?.cancel()
        continuousCapturingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopContinuousCapture()
    }
}



