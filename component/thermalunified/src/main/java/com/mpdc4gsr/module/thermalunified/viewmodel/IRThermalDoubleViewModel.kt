package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for IR Thermal Double mode functionality
 * Manages dual-mode thermal imaging with temperature/observe modes
 */
class IRThermalDoubleViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(ThermalDoubleUiState())
    val uiState: StateFlow<ThermalDoubleUiState> = _uiState.asStateFlow()

    fun setMode(mode: ThermalMode) {
        launchWithErrorHandling {
            _uiState.value = _uiState.value.copy(selectedMode = mode)
        }
    }

    fun toggleOverlay() {
        launchWithErrorHandling {
            val current = _uiState.value
            _uiState.value = current.copy(showOverlay = !current.showOverlay)
        }
    }

    fun toggleTrendChart() {
        launchWithErrorHandling {
            val current = _uiState.value
            _uiState.value = current.copy(showTrendChart = !current.showTrendChart)
        }
    }

    fun toggleCompass() {
        launchWithErrorHandling {
            val current = _uiState.value
            _uiState.value = current.copy(showCompass = !current.showCompass)
        }
    }

    fun toggleRecording() {
        launchWithErrorHandling {
            val current = _uiState.value
            _uiState.value = current.copy(isRecording = !current.isRecording)
        }
    }

    enum class ThermalMode(val displayName: String) {
        TEMPERATURE("Temperature"),
        OBSERVE("Observe")
    }

    data class ThermalDoubleUiState(
        val selectedMode: ThermalMode = ThermalMode.TEMPERATURE,
        val showOverlay: Boolean = true,
        val showTrendChart: Boolean = false,
        val showCompass: Boolean = false,
        val isRecording: Boolean = false
    )
}
