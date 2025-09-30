package com.mpdc4gsr.module.thermalunified.lite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * ViewModel for IR Monitor Lite Fragment Compose
 * Manages the state for thermal monitoring in lite mode
 */
class IRMonitorLiteViewModel : ViewModel() {

    // State flows for the fragment
    private val _monitoringState = MutableStateFlow(MonitoringState.INACTIVE)
    val monitoringState: StateFlow<MonitoringState> = _monitoringState.asStateFlow()

    private val _temperatureData = MutableStateFlow<TemperatureData?>(null)
    val temperatureData: StateFlow<TemperatureData?> = _temperatureData.asStateFlow()

    private val _deviceConnectionState = MutableStateFlow(DeviceConnectionState.DISCONNECTED)
    val deviceConnectionState: StateFlow<DeviceConnectionState> = _deviceConnectionState.asStateFlow()

    private val _monitoringData = MutableStateFlow<MonitoringData?>(null)
    val monitoringData: StateFlow<MonitoringData?> = _monitoringData.asStateFlow()

    // Internal state
    private var startTime: Long = 0
    private var sampleCount: Int = 0
    private var temperatureSum: Float = 0f

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

        _monitoringState.value = MonitoringState.ACTIVE
        startTime = System.currentTimeMillis()
        sampleCount = 0
        temperatureSum = 0f

        startTemperatureMonitoring()
    }

    fun stopMonitoring() {
        _monitoringState.value = MonitoringState.INACTIVE
    }

    fun clearMonitoringData() {
        _monitoringData.value = null
        _temperatureData.value = null
        sampleCount = 0
        temperatureSum = 0f
    }

    private fun simulateDeviceConnection() {
        viewModelScope.launch {
            _deviceConnectionState.value = DeviceConnectionState.CONNECTING
            delay(2000) // Simulate connection time
            _deviceConnectionState.value = DeviceConnectionState.CONNECTED
        }
    }

    private fun startTemperatureMonitoring() {
        viewModelScope.launch {
            var timeStep = 0

            while (_monitoringState.value == MonitoringState.ACTIVE) {
                // Simulate temperature reading
                val baseTemp = 25f + sin(timeStep * 0.1) * 5f + cos(timeStep * 0.05) * 2f
                val noise = (-1f..1f).random()
                val currentTemp = baseTemp + noise
                val maxTemp = currentTemp + (1f..3f).random()
                val minTemp = currentTemp - (1f..3f).random()

                // Update temperature data
                _temperatureData.value = TemperatureData(
                    currentTemp = currentTemp,
                    maxTemp = maxTemp,
                    minTemp = minTemp
                )

                // Update monitoring data
                sampleCount++
                temperatureSum += currentTemp
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
                delay(1000) // Sample every second
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

// Helper extension for range generation
private fun ClosedRange<Float>.random(): Float {
    return start + (Math.random() * (endInclusive - start)).toFloat()
}