package com.mpdc4gsr.module.user.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.viewmodel.FirmwareViewModel
import kotlinx.coroutines.launch

class MoreFragmentViewModel : BaseViewModel() {

    private val _fragmentAction = MutableLiveData<FragmentAction>()
    val fragmentAction: LiveData<FragmentAction> = _fragmentAction

    private val _firmwareState = MutableLiveData<FirmwareState>()
    val firmwareState: LiveData<FirmwareState> = _firmwareState

    private val _upgradePointVisible = MutableLiveData<Boolean>()
    val upgradePointVisible: LiveData<Boolean> = _upgradePointVisible

    private val _deviceTypeState = MutableLiveData<DeviceTypeState>()
    val deviceTypeState: LiveData<DeviceTypeState> = _deviceTypeState

    sealed class FragmentAction {
        data class Navigate(val route: String, val extras: Map<String, Any> = emptyMap()) :
            FragmentAction()

        data class ShowFirmwareDialog(val data: FirmwareViewModel.FirmwareData) : FragmentAction()
        object ShowResetConfirmation : FragmentAction()
    }

    sealed class FirmwareState {
        object Checking : FirmwareState()
        data class Available(val data: FirmwareViewModel.FirmwareData) : FirmwareState()
        object UpToDate : FirmwareState()
        data class Failed(val isBindError: Boolean) : FirmwareState()
    }

    data class DeviceTypeState(
        val isTC007: Boolean,
        val modelText: String,
        val correctionText: String,
        val dualModeVisible: Boolean
    )

    fun setDeviceType(isTC007: Boolean) {
        _deviceTypeState.value = DeviceTypeState(
            isTC007 = isTC007,
            modelText = if (isTC007) "TC007" else "TS004",
            correctionText = if (isTC007) "TC007 Correction" else "TS004 Correction",
            dualModeVisible = !isTC007
        )
    }

    fun navigateToModel() {
        val deviceType = _deviceTypeState.value
        if (deviceType != null) {
            _fragmentAction.value = FragmentAction.Navigate(
                RouterConfig.IR_MODEL,
                mapOf(ExtraKeyConfig.IS_TC007 to deviceType.isTC007)
            )
        }
    }

    fun navigateToCorrection() {
        val deviceType = _deviceTypeState.value
        if (deviceType != null) {
            _fragmentAction.value = FragmentAction.Navigate(
                RouterConfig.IR_CORRECTION,
                mapOf(ExtraKeyConfig.IS_TC007 to deviceType.isTC007)
            )
        }
    }

    fun navigateToDual() {
        _fragmentAction.value = FragmentAction.Navigate(RouterConfig.IR_DUAL)
    }

    fun navigateToUnit() {
        _fragmentAction.value = FragmentAction.Navigate(RouterConfig.UNIT)
    }

    fun navigateToDeviceInformation() {
        val deviceType = _deviceTypeState.value
        if (deviceType != null) {
            _fragmentAction.value = FragmentAction.Navigate(
                RouterConfig.DEVICE_INFORMATION,
                mapOf(ExtraKeyConfig.IS_TC007 to deviceType.isTC007)
            )
        }
    }

    fun checkFirmwareUpdate() {
        viewModelScope.launch {
            _firmwareState.value = FirmwareState.Checking
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
        _fragmentAction.value = FragmentAction.ShowResetConfirmation
    }
}