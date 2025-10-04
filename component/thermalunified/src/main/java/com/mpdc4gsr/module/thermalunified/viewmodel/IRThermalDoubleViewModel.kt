package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IRThermalDoubleViewModel : BaseViewModel() {

    private val _selectedMode = MutableStateFlow(0)
    val selectedMode: StateFlow<Int> = _selectedMode.asStateFlow()

    private val _showOverlay = MutableStateFlow(true)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()

    private val _showTrendChart = MutableStateFlow(false)
    val showTrendChart: StateFlow<Boolean> = _showTrendChart.asStateFlow()

    private val _showCompass = MutableStateFlow(false)
    val showCompass: StateFlow<Boolean> = _showCompass.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    fun selectMode(mode: Int) {
        launchWithErrorHandling {
            _selectedMode.value = mode
        }
    }

    fun toggleOverlay() {
        launchWithErrorHandling {
            _showOverlay.value = !_showOverlay.value
        }
    }

    fun toggleTrendChart() {
        launchWithErrorHandling {
            _showTrendChart.value = !_showTrendChart.value
        }
    }

    fun toggleCompass() {
        launchWithErrorHandling {
            _showCompass.value = !_showCompass.value
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

    fun toggleTISR() {
        launchWithErrorHandling {
            emitEvent("toggle_tisr")
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

    fun openGallery() {
        launchWithErrorHandling {
            emitEvent("open_gallery")
        }
    }

    fun captureCamera() {
        launchWithErrorHandling {
            emitEvent("capture_camera")
        }
    }

    fun showMoreOptions() {
        launchWithErrorHandling {
            emitEvent("show_more_options")
        }
    }
}
