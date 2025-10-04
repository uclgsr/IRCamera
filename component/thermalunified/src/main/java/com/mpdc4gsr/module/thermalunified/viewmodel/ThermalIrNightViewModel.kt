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
            emitEvent("show_info")
        }
    }

    fun lockTemperatureRange() {
        launchWithErrorHandling {
            emitEvent("lock_temperature_range")
        }
    }

    fun editTemperatureSettings() {
        launchWithErrorHandling {
            emitEvent("edit_temperature_settings")
        }
    }

    fun openColorPalette() {
        launchWithErrorHandling {
            emitEvent("open_color_palette")
        }
    }

    fun openSettings() {
        launchWithErrorHandling {
            emitEvent("open_settings")
        }
    }

    fun openGallery() {
        launchWithErrorHandling {
            emitEvent("open_gallery")
        }
    }

    fun showMoreOptions() {
        launchWithErrorHandling {
            emitEvent("show_more_options")
        }
    }
}
