package com.mpdc4gsr.module.thermalunified.lite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * ViewModel for IR Monitor Lite Fragment Compose
 * Manages the state for thermal monitoring in lite mode
 */
class IRMonitorLiteViewModel : ViewModel() {

    companion object {
        // Temperature simulation constants
        private const val BASE_TEMPERATURE = 25f
        private const val SINE_FREQUENCY = 0.1
        private const val SINE_AMPLITUDE = 5f
        private const val COSINE_FREQUENCY = 0.05
        private const val COSINE_AMPLITUDE = 2f
        private const val NOISE_RANGE = 1f
        private const val TEMP_VARIATION_RANGE = 3f
        private const val SAMPLING_INTERVAL_MS = 1000L
        private const val CONNECTION_DELAY_MS = 2000L
    }

    // State flows for the fragment
    private val _monitoringState = MutableStateFlow(MonitoringState.INACTIVE)
    val monitoringState: StateFlow<MonitoringState> = _monitoringState.asStateFlow()

    private val _temperatureData = MutableStateFlow<TemperatureData?>(null)
    val temperatureData: StateFlow<TemperatureData?> = _temperatureData.asStateFlow()

    private val _deviceConnectionState = MutableStateFlow(DeviceConnectionState.DISCONNECTED)
    val deviceConnectionState: StateFlow<DeviceConnectionState> = _deviceConnectionState.asStateFlow()

    private val _monitoringData = MutableStateFlow<MonitoringData?>(null)
    val monitoringData: StateFlow<MonitoringData?> = _monitoringData.asStateFlow()

    // Region mode state for temperature measurement
    private val _regionMode = MutableStateFlow(RegionMode.POINT)
    val regionMode: StateFlow<RegionMode> = _regionMode.asStateFlow()

    // Internal state
    private var startTime: Long = 0
    private var sampleCount: Int = 0
    private var temperatureSum: Float = 0f
    private var monitoringJob: Job? = null

    init {
        // Initialize with simulated device connection
        simulateDeviceConnection()
    }

    fun toggleMonitoring() {
        when (_monitoringState.value) {
            MonitoringState.ACTIVE -> stopMonitoring()
            MonitoringState.INACTIVE -> startMonitoring()
            MonitoringState.ERROR -> startMonitoring() // Allow retry
        }
    }

    fun startMonitoring() {
        if (_deviceConnectionState.value != DeviceConnectionState.CONNECTED) {
            _monitoringState.value = MonitoringState.ERROR
            return
        }

        // Cancel any existing monitoring job before starting a new one
        monitoringJob?.cancel()

        _monitoringState.value = MonitoringState.ACTIVE
        startTime = System.currentTimeMillis()
        sampleCount = 0
        temperatureSum = 0f

        startTemperatureMonitoring()
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        _monitoringState.value = MonitoringState.INACTIVE
    }

    fun clearMonitoringData() {
        _monitoringData.value = null
        _temperatureData.value = null
        sampleCount = 0
        temperatureSum = 0f
    }

    /**
     * Set the region mode for temperature measurement
     * @param mode The region mode as a string (e.g., "POINT", "LINE", "RECTANGLE", "CENTER", "CLEAN")
     */
    fun setRegionMode(mode: String) {
        val newMode = when (mode.uppercase()) {
            "POINT" -> RegionMode.POINT
            "LINE" -> RegionMode.LINE
            "RECTANGLE" -> RegionMode.RECTANGLE
            "CENTER" -> RegionMode.CENTER
            "CLEAN" -> RegionMode.CLEAN
            else -> RegionMode.POINT // Default to POINT mode
        }
        _regionMode.value = newMode
    }

    /**
     * Set the region mode directly using the enum
     */
    fun setRegionMode(mode: RegionMode) {
        _regionMode.value = mode
    }

    private fun simulateDeviceConnection() {
        viewModelScope.launch {
            _deviceConnectionState.value = DeviceConnectionState.CONNECTING
            delay(CONNECTION_DELAY_MS)
            _deviceConnectionState.value = DeviceConnectionState.CONNECTED
        }
    }

    private fun startTemperatureMonitoring() {
        monitoringJob = viewModelScope.launch {
            var timeStep = 0

            while (_monitoringState.value == MonitoringState.ACTIVE) {
                // Simulate temperature reading with constants
                val baseTemp = BASE_TEMPERATURE +
                        sin(timeStep * SINE_FREQUENCY) * SINE_AMPLITUDE +
                        cos(timeStep * COSINE_FREQUENCY) * COSINE_AMPLITUDE
                val noise = Random.nextFloat() * (NOISE_RANGE * 2) - NOISE_RANGE
                val currentTemp = baseTemp + noise
                val maxTemp = currentTemp + Random.nextFloat() * TEMP_VARIATION_RANGE
                val minTemp = currentTemp - Random.nextFloat() * TEMP_VARIATION_RANGE

                // Update temperature data
                _temperatureData.value = TemperatureData(
                    currentTemp = currentTemp.toFloat(),
                    maxTemp = maxTemp.toFloat(),
                    minTemp = minTemp.toFloat()
                )

                // Update monitoring data
                sampleCount++
                temperatureSum += currentTemp.toFloat()
                val averageTemp = (temperatureSum / sampleCount)
                val currentTime = System.currentTimeMillis()
                val duration = formatDuration(currentTime - startTime)

                _monitoringData.value = MonitoringData(
                    duration = duration,
                    sampleCount = sampleCount,
                    averageTemp = averageTemp,
                    startTime = startTime
                )

                timeStep++
                delay(SAMPLING_INTERVAL_MS)
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
            minutes > 0 -> String.format("%02d:%02d", minutes, seconds % 60)
            else -> String.format("00:%02d", seconds)
        }
    }

    override fun onCleared() {
        super.onCleared()
        monitoringJob?.cancel()
        stopMonitoring()
    }
}

// Data classes
data class TemperatureData(
    val currentTemp: Float,
    val maxTemp: Float,
    val minTemp: Float
)

data class MonitoringData(
    val duration: String,
    val sampleCount: Int,
    val averageTemp: Float,
    val startTime: Long
) {
    fun getDataItems(): List<Pair<String, String>> = listOf(
        "Duration" to duration,
        "Samples" to sampleCount.toString(),
        "Average" to "${String.format("%.1f", averageTemp)}°C",
        "Started" to SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(startTime))
    )
}

enum class DeviceConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

enum class MonitoringState {
    INACTIVE, ACTIVE, ERROR
}

/**
 * Temperature measurement region modes
 * Corresponds to TemperatureView.REGION_MODE_* constants
 */
enum class RegionMode {
    RESET,      // REGION_MODE_RESET = -1
    POINT,      // REGION_MODE_POINT = 0
    LINE,       // REGION_MODE_LINE = 1
    RECTANGLE,  // REGION_MODE_RECTANGLE = 2
    CENTER,     // REGION_MODE_CENTER = 3
    CLEAN       // REGION_MODE_CLEAN = 5
}

// Remove the helper extension as we're now using kotlin.random.Random