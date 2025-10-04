package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

    private val _showInfoDialog = MutableStateFlow(false)
    val showInfoDialog: StateFlow<Boolean> = _showInfoDialog.asStateFlow()

    private val _temperatureLocked = MutableStateFlow(false)
    val temperatureLocked: StateFlow<Boolean> = _temperatureLocked.asStateFlow()

    private val _action = MutableLiveData<NightThermalAction>()
    val action: LiveData<NightThermalAction> = _action

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
            _showInfoDialog.value = true
        }
    }

    fun dismissInfoDialog() {
        _showInfoDialog.value = false
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
            _action.postValue(NightThermalAction.ShowTemperatureEditor)
        }
    }

    fun openColorPalette() {
        launchWithErrorHandling {
            _action.postValue(NightThermalAction.ShowColorPalette)
        }
    }

    fun openSettings() {
        launchWithErrorHandling {
            _action.postValue(NightThermalAction.NavigateToSettings)
        }
    }

    fun openGallery() {
        launchWithErrorHandling {
            _action.postValue(NightThermalAction.NavigateToGallery)
        }
    }

    fun showMoreOptions() {
        launchWithErrorHandling {
            _action.postValue(NightThermalAction.ShowMoreOptions)
        }
    }

    sealed class NightThermalAction {
        object NavigateToGallery : NightThermalAction()
        object NavigateToSettings : NightThermalAction()
        object ShowTemperatureEditor : NightThermalAction()
        object ShowColorPalette : NightThermalAction()
        object ShowMoreOptions : NightThermalAction()
    }
}
