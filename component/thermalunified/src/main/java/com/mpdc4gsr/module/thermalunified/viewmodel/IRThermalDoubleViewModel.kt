package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

    private val _showInfoDialog = MutableStateFlow(false)
    val showInfoDialog: StateFlow<Boolean> = _showInfoDialog.asStateFlow()

    private val _temperatureLocked = MutableStateFlow(false)
    val temperatureLocked: StateFlow<Boolean> = _temperatureLocked.asStateFlow()

    private val _action = MutableLiveData<ThermalAction>()
    val action: LiveData<ThermalAction> = _action

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
            _showInfoDialog.value = true
        }
    }

    fun dismissInfoDialog() {
        _showInfoDialog.value = false
    }

    fun toggleTISR() {
        launchWithErrorHandling {
            _action.postValue(ThermalAction.NavigateToTISRSettings)
        }
    }

    fun lockTemperatureRange() {
        launchWithErrorHandling {
            _temperatureLocked.value = !_temperatureLocked.value
            _uiEvents.emit(
                UiEvent.ShowMessage(
                    if (_temperatureLocked.value) "Temperature range locked" 
                    else "Temperature range unlocked"
                )
            )
        }
    }

    fun editTemperatureSettings() {
        launchWithErrorHandling {
            _action.postValue(ThermalAction.ShowTemperatureEditor)
        }
    }

    fun openGallery() {
        launchWithErrorHandling {
            _action.postValue(ThermalAction.NavigateToGallery)
        }
    }

    fun captureCamera() {
        launchWithErrorHandling {
            val timestamp = System.currentTimeMillis()
            val fileName = "thermal_capture_$timestamp.jpg"
            _action.postValue(ThermalAction.CapturePhoto(fileName))
            _uiEvents.emit(UiEvent.ShowMessage("Photo captured: $fileName"))
        }
    }

    fun showMoreOptions() {
        launchWithErrorHandling {
            _action.postValue(ThermalAction.ShowMoreOptions)
        }
    }

    sealed class ThermalAction {
        object NavigateToGallery : ThermalAction()
        object NavigateToTISRSettings : ThermalAction()
        object ShowTemperatureEditor : ThermalAction()
        object ShowMoreOptions : ThermalAction()
        data class CapturePhoto(val fileName: String) : ThermalAction()
    }
}
