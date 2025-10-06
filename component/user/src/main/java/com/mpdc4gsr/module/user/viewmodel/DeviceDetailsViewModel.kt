package com.mpdc4gsr.module.user.viewmodel
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
class DeviceDetailsViewModel : BaseViewModel() {
    companion object {
        private const val TC007_DEMO_SERIAL = "TC007-DEMO-SN"
        private const val TC007_MODEL = "TC007"
        private const val TS004_DEMO_SERIAL = "TS004-DEMO-SN"
        private const val TS004_MODEL = "TS004"
        private const val ERROR_SERIAL = "Error loading SN"
        private const val ERROR_MODEL = "Error loading model"
    }
    private val _serialNumber = MutableStateFlow("N/A")
    val serialNumber: StateFlow<String> = _serialNumber.asStateFlow()
    private val _deviceModel = MutableStateFlow("N/A")
    val deviceModel: StateFlow<String> = _deviceModel.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _copyMessage = MutableStateFlow("")
    val copyMessage: StateFlow<String> = _copyMessage.asStateFlow()
    fun loadDeviceDetails(isTC007: Boolean) {
        launchWithErrorHandling {
            _isLoading.value = true
            try {
                // Note: Original TS004Repository functionality was removed
                // Setting default values as per the original implementation
                if (isTC007) {
                    _serialNumber.value = TC007_DEMO_SERIAL
                    _deviceModel.value = TC007_MODEL
                } else {
                    _serialNumber.value = TS004_DEMO_SERIAL
                    _deviceModel.value = TS004_MODEL
                }
            } catch (e: Exception) {
                _serialNumber.value = ERROR_SERIAL
                _deviceModel.value = ERROR_MODEL
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun getCopyText(): String {
        return "SN:${_serialNumber.value}  Device Model:${_deviceModel.value}"
    }
    fun setCopyMessage(message: String) {
        _copyMessage.value = message
    }
}