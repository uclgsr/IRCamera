package com.mpdc4gsr.module.thermalunified.viewmodel
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
class IRMonitorChartLiteViewModel : BaseViewModel() {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    private val _recordingTime = MutableStateFlow("00:00:00")
    val recordingTime: StateFlow<String> = _recordingTime.asStateFlow()
    private val _showOverlay = MutableStateFlow(true)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()
    private val _currentTemp = MutableStateFlow(25.0f)
    val currentTemp: StateFlow<Float> = _currentTemp.asStateFlow()
    private val _highTemp = MutableStateFlow(30.0f)
    val highTemp: StateFlow<Float> = _highTemp.asStateFlow()
    private val _lowTemp = MutableStateFlow(20.0f)
    val lowTemp: StateFlow<Float> = _lowTemp.asStateFlow()
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    fun toggleRecording() {
        launchWithErrorHandling {
            _isRecording.value = !_isRecording.value
            if (!_isRecording.value) {
                _recordingTime.value = "00:00:00"
            }
        }
    }
    fun toggleOverlay() {
        launchWithErrorHandling {
            _showOverlay.value = !_showOverlay.value
        }
    }
    fun updateTemperature(current: Float, high: Float, low: Float) {
        launchWithErrorHandling {
            _currentTemp.value = current
            _highTemp.value = high
            _lowTemp.value = low
        }
    }
    fun startMonitoring() {
        launchWithLoading {
            _isMonitoring.value = true
        }
    }
    fun stopMonitoring() {
        launchWithErrorHandling {
            _isMonitoring.value = false
            _isRecording.value = false
        }
    }
}
