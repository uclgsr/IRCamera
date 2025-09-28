package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Device Details
 * Manages device information display and copy functionality
 */
class DeviceDetailsViewModel : BaseViewModel() {
    
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
                    _serialNumber.value = "TC007-DEMO-SN"
                    _deviceModel.value = "TC007"
                } else {
                    _serialNumber.value = "TS004-DEMO-SN"
                    _deviceModel.value = "TS004"
                }
            } catch (e: Exception) {
                _serialNumber.value = "Error loading SN"
                _deviceModel.value = "Error loading model"
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