package com.mpdc4gsr.module.user.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.viewmodel.FirmwareViewModel
import kotlinx.coroutines.launch

class MoreActivityViewModel : BaseViewModel() {

    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    private val _firmwareState = MutableLiveData<FirmwareState>()
    val firmwareState: LiveData<FirmwareState> = _firmwareState

    private val _upgradePointVisible = MutableLiveData<Boolean>()
    val upgradePointVisible: LiveData<Boolean> = _upgradePointVisible

    sealed class NavigationEvent(val route: String, val extras: Map<String, Any> = emptyMap()) {
        object DeviceInformation : NavigationEvent(
            RouterConfig.DEVICE_INFORMATION,
            mapOf(ExtraKeyConfig.IS_TC007 to false)
        )
        object TISR : NavigationEvent(RouterConfig.TISR)
        object AutoSave : NavigationEvent(RouterConfig.AUTO_SAVE)
        object StorageSpace : NavigationEvent(RouterConfig.STORAGE_SPACE)
    }

    sealed class FirmwareState {
        object Checking : FirmwareState()
        data class Available(val data: FirmwareViewModel.FirmwareData) : FirmwareState()
        object UpToDate : FirmwareState()
        data class Failed(val isBindError: Boolean) : FirmwareState()
    }

    fun navigateToDeviceInformation() {
        _navigationEvent.value = NavigationEvent.DeviceInformation
    }

    fun navigateToTISR() {
        _navigationEvent.value = NavigationEvent.TISR
    }

    fun navigateToAutoSave() {
        _navigationEvent.value = NavigationEvent.AutoSave
    }

    fun navigateToStorageSpace() {
        _navigationEvent.value = NavigationEvent.StorageSpace
    }

    fun checkFirmwareUpdate() {
        viewModelScope.launch {
            _firmwareState.value = FirmwareState.Checking
            // Firmware checking logic would be coordinated here
            // This integrates with existing FirmwareViewModel
        }
    }

    fun onFirmwareDataReceived(data: FirmwareViewModel.FirmwareData?) {
        _firmwareState.value = if (data != null) {
            FirmwareState.Available(data)
        } else {
            FirmwareState.UpToDate
        }
        _upgradePointVisible.value = data != null
    }

    fun onFirmwareFailed(isBindError: Boolean) {
        _firmwareState.value = FirmwareState.Failed(isBindError)
        _upgradePointVisible.value = false
    }

    fun requestFactoryReset() {
        // Factory reset coordination through ViewModel
        viewModelScope.launch {
            // Reset logic would be coordinated here
        }
    }

    fun requestDisconnect() {
        // Device disconnection coordination
        viewModelScope.launch {
            // Disconnection logic would be coordinated here
        }
    }
}