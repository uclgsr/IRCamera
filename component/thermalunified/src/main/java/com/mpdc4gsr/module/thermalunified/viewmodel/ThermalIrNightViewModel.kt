package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Thermal IR Night mode functionality
 * Manages night-optimized thermal imaging state
 */
class ThermalIrNightViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(ThermalNightUiState())
    val uiState: StateFlow<ThermalNightUiState> = _uiState.asStateFlow()

    fun setMode(mode: NightMode) {
        launchWithErrorHandling {
            _uiState.value = _uiState.value.copy(selectedMode = mode)
        }
    }

    fun toggleNightMode() {
        launchWithErrorHandling {
            val current = _uiState.value
            _uiState.value = current.copy(nightModeEnabled = !current.nightModeEnabled)
        }
    }

    fun toggleOverlay() {
        launchWithErrorHandling {
            val current = _uiState.value
            _uiState.value = current.copy(showOverlay = !current.showOverlay)
        }
    }

    fun toggleRecording() {
        launchWithErrorHandling {
            val current = _uiState.value
            _uiState.value = current.copy(isRecording = !current.isRecording)
        }
    }

    enum class NightMode(val displayName: String) {
        ENHANCED("Enhanced"),
        STANDARD("Standard"),
        ULTRA("Ultra")
    }

    data class ThermalNightUiState(
        val selectedMode: NightMode = NightMode.ENHANCED,
        val nightModeEnabled: Boolean = true,
        val showOverlay: Boolean = true,
        val isRecording: Boolean = false
    )
}
