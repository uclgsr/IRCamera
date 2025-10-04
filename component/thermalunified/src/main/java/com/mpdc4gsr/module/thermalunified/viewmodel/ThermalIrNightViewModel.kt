package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThermalIrNightViewModel : BaseViewModel() {

    private val _selectedMode = MutableStateFlow(0)
    val selectedMode: StateFlow<Int> = _selectedMode.asStateFlow()

    private val _nightModeEnabled = MutableStateFlow(true)
    val nightModeEnabled: StateFlow<Boolean> = _nightModeEnabled.asStateFlow()

    private val _showOverlay = MutableStateFlow(true)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    fun selectMode(mode: Int) {
        launchWithErrorHandling {
            _selectedMode.value = mode
        }
    }

    fun toggleNightMode() {
        launchWithErrorHandling {
            _nightModeEnabled.value = !_nightModeEnabled.value
        }
    }

    fun toggleOverlay() {
        launchWithErrorHandling {
            _showOverlay.value = !_showOverlay.value
        }
    }

    fun toggleRecording() {
        launchWithErrorHandling {
            _isRecording.value = !_isRecording.value
        }
    }

    fun showInfo() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Show Info"))
        }
    }

    fun lockTemperatureRange() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Lock Temperature Range"))
        }
    }

    fun editTemperatureSettings() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Edit Temperature Settings"))
        }
    }

    fun openColorPalette() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Open Color Palette"))
        }
    }

    fun openSettings() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Open Settings"))
        }
    }

    fun openGallery() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Open Gallery"))
        }
    }

    fun showMoreOptions() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Show More Options"))
        }
    }
}
