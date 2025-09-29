package com.mpdc4gsr.module.user.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.viewmodel.FirmwareViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MoreFragmentViewModel : BaseViewModel() {

    // State flows for reactive UI updates
    private val _fragmentAction = MutableSharedFlow<FragmentAction>()
    val fragmentAction: SharedFlow<FragmentAction> = _fragmentAction.asSharedFlow()

    private val _firmwareState = MutableStateFlow<FirmwareState>(FirmwareState.Idle)
    val firmwareState: StateFlow<FirmwareState> = _firmwareState.asStateFlow()

    private val _upgradePointVisible = MutableStateFlow(false)
    val upgradePointVisible: StateFlow<Boolean> = _upgradePointVisible.asStateFlow()

    private val _deviceTypeState = MutableStateFlow<DeviceTypeState?>(null)
    val deviceTypeState: StateFlow<DeviceTypeState?> = _deviceTypeState.asStateFlow()

    // Combined UI state for complex scenarios
    private val _moreScreenState = MutableStateFlow(MoreScreenState())
    val moreScreenState: StateFlow<MoreScreenState> = _moreScreenState.asStateFlow()

    // Sealed classes for type-safe state management
    sealed class FragmentAction {
        data class Navigate(val route: String, val extras: Map<String, Any> = emptyMap()) :
            FragmentAction()

        data class ShowFirmwareDialog(val data: FirmwareViewModel.FirmwareData) : FragmentAction()
        object ShowResetConfirmation : FragmentAction()
        data class ShowError(val message: String) : FragmentAction()
        data class ShowSuccess(val message: String) : FragmentAction()
    }

    sealed class FirmwareState {
        object Idle : FirmwareState()
        object Checking : FirmwareState()
        data class Available(val data: FirmwareViewModel.FirmwareData) : FirmwareState()
        object UpToDate : FirmwareState()
        data class Failed(val isBindError: Boolean, val errorMessage: String? = null) :
            FirmwareState()
    }

    data class DeviceTypeState(
        val isTC007: Boolean,
        val modelText: String,
        val correctionText: String,
        val dualModeVisible: Boolean,
        val deviceId: String? = null,
        val firmwareVersion: String? = null
    )

    data class MoreScreenState(
        val isDeviceConnected: Boolean = false,
        val isCheckingUpdates: Boolean = false,
        val hasAvailableUpdate: Boolean = false,
        val lastUpdateCheck: Long? = null,
        val settingsEnabled: Boolean = true
    )

    fun setDeviceType(isTC007: Boolean, deviceId: String? = null, firmwareVersion: String? = null) {
        launchWithErrorHandling {
            _deviceTypeState.value = DeviceTypeState(
                isTC007 = isTC007,
                modelText = if (isTC007) "TC007" else "TS004",
                correctionText = if (isTC007) "TC007 Correction" else "TS004 Correction",
                dualModeVisible = !isTC007,
                deviceId = deviceId,
                firmwareVersion = firmwareVersion
            )

            // Update combined state
            _moreScreenState.value = _moreScreenState.value.copy(
                isDeviceConnected = true
            )
        }
    }

    fun navigateToModel() {
        launchWithErrorHandling {
            val deviceType = _deviceTypeState.value
            if (deviceType != null) {
                _fragmentAction.emit(
                    FragmentAction.Navigate(
                        RouterConfig.IR_MODEL,
                        mapOf(ExtraKeyConfig.IS_TC007 to deviceType.isTC007)
                    )
                )
            } else {
                _fragmentAction.emit(FragmentAction.ShowError("Device type not set"))
            }
        }
    }

    fun navigateToCorrection() {
        launchWithErrorHandling {
            val deviceType = _deviceTypeState.value
            if (deviceType != null) {
                _fragmentAction.emit(
                    FragmentAction.Navigate(
                        RouterConfig.IR_CORRECTION,
                        mapOf(ExtraKeyConfig.IS_TC007 to deviceType.isTC007)
                    )
                )
            } else {
                _fragmentAction.emit(FragmentAction.ShowError("Device type not set"))
            }
        }
    }

    fun navigateToDual() {
        launchWithErrorHandling {
            _fragmentAction.emit(FragmentAction.Navigate(RouterConfig.IR_DUAL))
        }
    }

    fun navigateToUnit() {
        launchWithErrorHandling {
            _fragmentAction.emit(FragmentAction.Navigate(RouterConfig.UNIT))
        }
    }

    fun navigateToDeviceInformation() {
        launchWithErrorHandling {
            val deviceType = _deviceTypeState.value
            if (deviceType != null) {
                _fragmentAction.emit(
                    FragmentAction.Navigate(
                        RouterConfig.DEVICE_INFORMATION,
                        mapOf(ExtraKeyConfig.IS_TC007 to deviceType.isTC007)
                    )
                )
            } else {
                _fragmentAction.emit(FragmentAction.ShowError("Device information not available"))
            }
        }
    }

    fun checkFirmwareUpdate() {
        launchWithLoading {
            try {
                _firmwareState.value = FirmwareState.Checking
                _moreScreenState.value = _moreScreenState.value.copy(
                    isCheckingUpdates = true,
                    lastUpdateCheck = System.currentTimeMillis()
                )

                // Simulate firmware check - in real implementation would call repository
                kotlinx.coroutines.delay(2000)

                // This would be replaced with actual firmware check logic
                _firmwareState.value = FirmwareState.UpToDate
                _moreScreenState.value = _moreScreenState.value.copy(
                    isCheckingUpdates = false,
                    hasAvailableUpdate = false
                )

                _fragmentAction.emit(FragmentAction.ShowSuccess("Firmware is up to date"))

            } catch (e: Exception) {
                _firmwareState.value = FirmwareState.Failed(false, e.message)
                _moreScreenState.value = _moreScreenState.value.copy(
                    isCheckingUpdates = false
                )
                _fragmentAction.emit(FragmentAction.ShowError("Failed to check firmware: ${e.message}"))
            }
        }
    }

    fun onFirmwareDataReceived(data: FirmwareViewModel.FirmwareData?) {
        launchWithErrorHandling {
            _firmwareState.value = if (data != null) {
                _moreScreenState.value = _moreScreenState.value.copy(hasAvailableUpdate = true)
                _upgradePointVisible.value = true
                FirmwareState.Available(data)
            } else {
                _moreScreenState.value = _moreScreenState.value.copy(hasAvailableUpdate = false)
                _upgradePointVisible.value = false
                FirmwareState.UpToDate
            }
        }
    }

    fun onFirmwareFailed(isBindError: Boolean, errorMessage: String? = null) {
        launchWithErrorHandling {
            _firmwareState.value = FirmwareState.Failed(isBindError, errorMessage)
            _upgradePointVisible.value = false
            _moreScreenState.value = _moreScreenState.value.copy(
                isCheckingUpdates = false,
                hasAvailableUpdate = false
            )

            val message = if (isBindError) {
                "Firmware service binding failed"
            } else {
                errorMessage ?: "Firmware check failed"
            }
            _fragmentAction.emit(FragmentAction.ShowError(message))
        }
    }

    fun showFirmwareDialog() {
        launchWithErrorHandling {
            val firmwareState = _firmwareState.value
            if (firmwareState is FirmwareState.Available) {
                _fragmentAction.emit(FragmentAction.ShowFirmwareDialog(firmwareState.data))
            }
        }
    }

    fun requestFactoryReset() {
        launchWithErrorHandling {
            _fragmentAction.emit(FragmentAction.ShowResetConfirmation)
        }
    }

    fun resetDevice() {
        launchWithLoading {
            try {
                // Simulate device reset - replace with actual implementation
                kotlinx.coroutines.delay(1000)
                _fragmentAction.emit(FragmentAction.ShowSuccess("Device reset successfully"))
            } catch (e: Exception) {
                _fragmentAction.emit(FragmentAction.ShowError("Failed to reset device: ${e.message}"))
            }
        }
    }

    // Lifecycle methods
    fun onDeviceConnected() {
        _moreScreenState.value = _moreScreenState.value.copy(isDeviceConnected = true)
    }

    fun onDeviceDisconnected() {
        _moreScreenState.value = _moreScreenState.value.copy(
            isDeviceConnected = false,
            hasAvailableUpdate = false
        )
        _deviceTypeState.value = null
        _firmwareState.value = FirmwareState.Idle
        _upgradePointVisible.value = false
    }
}