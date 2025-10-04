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
            _uiEvents.emit(UiEvent.ShowMessage("Show Info"))
        }
    }

    fun toggleTISR() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Toggle TISR"))
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

    fun openGallery() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Open Gallery"))
        }
    }

    fun captureCamera() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Capture Camera"))
        }
    }

    fun showMoreOptions() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Show More Options"))
        }
    }
}
