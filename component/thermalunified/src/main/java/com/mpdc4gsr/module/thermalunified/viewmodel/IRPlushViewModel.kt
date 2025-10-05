package com.mpdc4gsr.module.thermalunified.viewmodel

import android.view.SurfaceView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.WorkspacePremium
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IRPlushViewModel : BaseViewModel() {

    companion object {
        private const val CALIBRATION_DELAY_MS = 2000L
    }

    // Dual view state management
    private val _dualViewState = MutableStateFlow(DualViewState.INACTIVE)
    val dualViewState: StateFlow<DualViewState> = _dualViewState.asStateFlow()

    // Temperature data management
    private val _temperatureData = MutableStateFlow(
        TemperatureData(
            irCenterTemp = 0.0f,
            irMaxTemp = 0.0f,
            irMinTemp = 0.0f,
            ambientTemp = 0.0f
        )
    )
    val temperatureData: StateFlow<TemperatureData> = _temperatureData.asStateFlow()

    // Processing mode management
    private val _processingMode = MutableStateFlow(ProcessingMode.STANDARD)
    val processingMode: StateFlow<ProcessingMode> = _processingMode.asStateFlow()

    // Recording state management
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    fun toggleRecording() {
        _isRecording.value = !_isRecording.value
    }

    fun initializeDualView(surfaceView: SurfaceView) {
        // Only update state, don't store the SurfaceView reference
        _dualViewState.value = DualViewState.ACTIVE
    }

    fun changeProcessingMode(mode: ProcessingMode) {
        _processingMode.value = mode
    }

    fun calibrateDualView() {
        launchWithErrorHandling {
            _dualViewState.value = DualViewState.CALIBRATING
            // Simulation of calibration process
            kotlinx.coroutines.delay(CALIBRATION_DELAY_MS)
            _dualViewState.value = DualViewState.ACTIVE
        }
    }

    fun resetSettings() {
        _processingMode.value = ProcessingMode.STANDARD
        _isRecording.value = false
        _temperatureData.value = TemperatureData(
            irCenterTemp = 0.0f,
            irMaxTemp = 0.0f,
            irMinTemp = 0.0f,
            ambientTemp = 0.0f
        )
    }

    fun updateTemperatureData(
        centerTemp: Float,
        maxTemp: Float,
        minTemp: Float,
        ambientTemp: Float
    ) {
        _temperatureData.value = TemperatureData(
            irCenterTemp = centerTemp,
            irMaxTemp = maxTemp,
            irMinTemp = minTemp,
            ambientTemp = ambientTemp
        )
    }

    // Data class definitions
    data class TemperatureData(
        val irCenterTemp: Float,
        val irMaxTemp: Float,
        val irMinTemp: Float,
        val ambientTemp: Float
    )

    enum class DualViewState {
        INACTIVE, ACTIVE, CALIBRATING, ERROR
    }

    enum class ProcessingMode(val displayName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
        STANDARD("Standard", Icons.Default.CameraAlt),
        ENHANCED("Enhanced", Icons.Default.AutoAwesome),
        PROFESSIONAL("Professional", Icons.Default.WorkspacePremium),
        FUSION("Fusion", Icons.Default.Merge)
    }

    fun showAdvancedSettings() {
        // Placeholder for advanced settings functionality
    }
}