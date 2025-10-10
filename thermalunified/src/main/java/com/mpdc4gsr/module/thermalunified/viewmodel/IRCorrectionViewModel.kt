package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IRCorrectionViewModel : BaseViewModel() {
    // State management for correction functionality
    private val _correctionState = MutableStateFlow(CorrectionState.INACTIVE)
    val correctionState: StateFlow<CorrectionState> = _correctionState.asStateFlow()
    private val _temperatureData = MutableStateFlow<TemperatureData?>(null)
    val temperatureData: StateFlow<TemperatureData?> = _temperatureData.asStateFlow()
    private val _calibrationStatus = MutableStateFlow(CalibrationStatus.NONE)
    val calibrationStatus: StateFlow<CalibrationStatus> = _calibrationStatus.asStateFlow()
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Current correction parameters
    private var currentCorrectionValue: Float = 0f
    private var currentTemperaturePoint: Triple<Float, Int, Int>? = null

    init {
        // Initialize with default temperature data
        _temperatureData.value =
            TemperatureData(
                currentTemp = 25.0f,
                correctedTemp = 25.0f,
                offsetValue = 0.0f,
            )
    }

    fun toggleCorrection() {
        viewModelScope.launch {
            try {
                when (_correctionState.value) {
                    CorrectionState.INACTIVE -> {
                        _correctionState.value = CorrectionState.ACTIVE
                        startTemperatureMonitoring()
                    }

                    CorrectionState.ACTIVE -> {
                        _correctionState.value = CorrectionState.INACTIVE
                        stopTemperatureMonitoring()
                    }

                    CorrectionState.CALIBRATING -> {
                        // Cannot toggle while calibrating
                    }
                }
            } catch (e: Exception) {
                // Handle the exception by logging and updating error state
                handleError(e)
                _correctionState.value = CorrectionState.INACTIVE
            }
        }
    }

    fun updateTemperaturePoint(
        temp: Float,
        x: Int,
        y: Int,
    ) {
        currentTemperaturePoint = Triple(temp, x, y)
        updateTemperatureData(temp)
    }

    fun updateCorrectionValue(value: Float) {
        currentCorrectionValue = value
        currentTemperaturePoint?.let { (baseTemp, _, _) ->
            updateTemperatureData(baseTemp)
        }
    }

    fun startCalibration() {
        launchWithErrorHandling {
            _isProcessing.value = true
            _correctionState.value = CorrectionState.CALIBRATING
            _calibrationStatus.value = CalibrationStatus.NEEDS_CALIBRATION
            try {
                // Simulate calibration process
                kotlinx.coroutines.delay(2000) // Simulate calibration time
                _calibrationStatus.value = CalibrationStatus.CALIBRATED
                _correctionState.value = CorrectionState.ACTIVE
                // Restart temperature monitoring after calibration completes
                startTemperatureMonitoring()
            } catch (e: Exception) {
                handleError(e)
                _correctionState.value = CorrectionState.INACTIVE
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun resetCorrection() {
        launchWithErrorHandling {
            currentCorrectionValue = 0f
            currentTemperaturePoint = null
            _calibrationStatus.value = CalibrationStatus.NONE
            _correctionState.value = CorrectionState.INACTIVE
            // Reset temperature data
            _temperatureData.value =
                TemperatureData(
                    currentTemp = 25.0f,
                    correctedTemp = 25.0f,
                    offsetValue = 0.0f,
                )
        }
    }

    fun saveSettings() {
        launchWithErrorHandling {
            _isProcessing.value = true
            try {
                // Simulate saving settings
                kotlinx.coroutines.delay(1000)
                // Show success message
                _uiEvents.emit(BaseViewModel.UiEvent.ShowMessage("Correction settings saved successfully"))
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun startTemperatureMonitoring() {
        viewModelScope.launch {
            // Simulate temperature monitoring with some variation
            while (_correctionState.value == CorrectionState.ACTIVE) {
                currentTemperaturePoint?.let { (baseTemp, _, _) ->
                    // Add some realistic temperature variation
                    val variation = (Math.random() - 0.5).toFloat() * 0.5f
                    updateTemperatureData(baseTemp + variation)
                }
                kotlinx.coroutines.delay(500) // Update every 500ms
            }
        }
    }

    private fun stopTemperatureMonitoring() {
        // Temperature monitoring is stopped by the coroutine condition check
    }

    private fun updateTemperatureData(currentTemp: Float) {
        val correctedTemp = currentTemp + currentCorrectionValue
        _temperatureData.value =
            TemperatureData(
                currentTemp = currentTemp,
                correctedTemp = correctedTemp,
                offsetValue = currentCorrectionValue,
            )
    }
}

data class TemperatureData(
    val currentTemp: Float,
    val correctedTemp: Float,
    val offsetValue: Float,
)

enum class CorrectionState {
    INACTIVE,
    ACTIVE,
    CALIBRATING,
}

enum class CalibrationStatus {
    NONE,
    CALIBRATED,
    NEEDS_CALIBRATION,
}
