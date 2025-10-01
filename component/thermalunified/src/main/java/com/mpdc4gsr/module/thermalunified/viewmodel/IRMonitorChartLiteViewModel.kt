package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for IR Monitor Chart Lite functionality
 * Manages real-time monitoring chart state with recording capabilities
 */
class IRMonitorChartLiteViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(ChartLiteUiState())
    val uiState: StateFlow<ChartLiteUiState> = _uiState.asStateFlow()

    fun toggleRecording() {
        launchWithErrorHandling {
            val current = _uiState.value
            _uiState.value = current.copy(
                isRecording = !current.isRecording,
                recordingTime = if (!current.isRecording) "00:00:00" else current.recordingTime
            )
        }
    }

    fun toggleOverlay() {
        launchWithErrorHandling {
            val current = _uiState.value
            _uiState.value = current.copy(showOverlay = !current.showOverlay)
        }
    }

    fun updateTemperature(current: Float, high: Float, low: Float) {
        launchWithErrorHandling {
            val state = _uiState.value
            _uiState.value = state.copy(
                currentTemp = current,
                highTemp = high,
                lowTemp = low
            )
        }
    }

    fun startMonitoring() {
        launchWithErrorHandling {
            _uiState.value = _uiState.value.copy(isMonitoring = true)
        }
    }

    fun stopMonitoring() {
        launchWithErrorHandling {
            _uiState.value = _uiState.value.copy(
                isMonitoring = false,
                isRecording = false
            )
        }
    }

    data class ChartLiteUiState(
        val isRecording: Boolean = false,
        val recordingTime: String = "00:00:00",
        val showOverlay: Boolean = true,
        val currentTemp: Float = 25.0f,
        val highTemp: Float = 30.0f,
        val lowTemp: Float = 20.0f,
        val isMonitoring: Boolean = false
    )
}
