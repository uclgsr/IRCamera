package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import com.mpdc4gsr.libunified.app.common.SharedManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IRThermalFragmentViewModel : BaseViewModel() {

    // Device connection state management
    private val _deviceConnectionState = MutableStateFlow(DeviceConnectionState())
    val deviceConnectionState: StateFlow<DeviceConnectionState> =
        _deviceConnectionState.asStateFlow()

    // Navigation events
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    // Permission state management
    private val _permissionState = MutableLiveData<PermissionState>()
    val permissionState: LiveData<PermissionState> = _permissionState

    // UI state management
    private val _thermalUiState = MutableStateFlow(ThermalUIState())
    val thermalUiState: StateFlow<ThermalUIState> = _thermalUiState.asStateFlow()

    // Action events for dialogs and operations
    private val _thermalAction = MutableLiveData<ThermalAction>()
    val thermalAction: LiveData<ThermalAction> = _thermalAction

    init {
        setupDeviceStateMonitoring()
    }

    private fun setupDeviceStateMonitoring() {
        viewModelScope.launch {
            // Monitor device connections and update UI state accordingly
            combine(
                _deviceConnectionState,
                _uiState
            ) { connectionState, uiState ->
                uiState.copy(
                    isConnected = connectionState.hasConnection,
                    isTC007Connected = connectionState.isTC007Connected,
                    showConnectButton = !connectionState.hasConnection && connectionState.hasUsbDevice
                )
            }.collect { newUiState ->
                _uiState.value = newUiState
            }
        }
    }

    fun checkDeviceConnection(isTC007: Boolean) {
        val hasConnection = if (isTC007) {
            WebSocketProxy.getInstance().isTC007Connect()
        } else {
            DeviceTools.isConnect(isAutoRequest = false)
        }

        val hasUsbDevice = DeviceTools.findUsbDevice() != null

        _deviceConnectionState.value = DeviceConnectionState(
            hasConnection = hasConnection,
            isTC007Connected = isTC007 && hasConnection,
            hasUsbDevice = hasUsbDevice,
            isTC007Device = isTC007
        )
    }

    fun onDeviceConnected(isTC007Device: Boolean) {
        if (!isTC007Device) {
            SharedManager.hasTcLine = true
        }

        _deviceConnectionState.value = _deviceConnectionState.value.copy(
            hasConnection = true,
            isTC007Connected = isTC007Device
        )
    }

    fun onDeviceDisconnected() {
        _deviceConnectionState.value = _deviceConnectionState.value.copy(
            hasConnection = false,
            isTC007Connected = false
        )
    }

    fun onSocketConnected(isTS004: Boolean, isTC007Device: Boolean) {
        if (isTC007Device && !isTS004) {
            _deviceConnectionState.value = _deviceConnectionState.value.copy(
                hasConnection = true,
                isTC007Connected = true
            )
        }
    }

    fun onSocketDisConnected(isTS004: Boolean, isTC007Device: Boolean) {
        if (isTC007Device && !isTS004) {
            _deviceConnectionState.value = _deviceConnectionState.value.copy(
                hasConnection = false,
                isTC007Connected = false
            )
        }
    }

    fun handleThermalOpen(isTC007: Boolean) {
        if (isTC007) {
            _navigationEvent.value = NavigationEvent.NavigateToTC007Thermal
        } else {
            when {
                DeviceTools.isTC001PlusConnect() -> {
                    _navigationEvent.value = NavigationEvent.StartThermalPlusActivity
                }

                DeviceTools.isTC001LiteConnect() -> {
                    _navigationEvent.value = NavigationEvent.NavigateToTCLite
                }

                DeviceTools.isHikConnect() -> {
                    _navigationEvent.value = NavigationEvent.NavigateToHikMain
                }

                else -> {
                    _navigationEvent.value = NavigationEvent.StartThermalNightActivity
                }
            }
        }
    }

    fun handleMainEnter() {
        val connectionState = _deviceConnectionState.value

        if (!connectionState.hasConnection) {
            if (!connectionState.hasUsbDevice) {
                _thermalAction.value = ThermalAction.ShowDeviceConnectTip
            } else {
                _permissionState.value = PermissionState.RequestCameraPermission
            }
        }
    }

    fun onPermissionGranted() {
        _thermalAction.value = ThermalAction.ShowConnectTip
    }

    fun onPermissionDenied(doNotAskAgain: Boolean) {
        if (doNotAskAgain) {
            _thermalAction.value = ThermalAction.ShowPermissionSettingsTip
        }
    }

    // Data classes for state management
    data class DeviceConnectionState(
        val hasConnection: Boolean = false,
        val isTC007Connected: Boolean = false,
        val hasUsbDevice: Boolean = false,
        val isTC007Device: Boolean = false
    )

    data class ThermalUIState(
        val isConnected: Boolean = false,
        val isTC007Connected: Boolean = false,
        val showConnectButton: Boolean = false,
        val isLoading: Boolean = false
    )

    sealed class NavigationEvent {
        object NavigateToTC007Thermal : NavigationEvent()
        object StartThermalPlusActivity : NavigationEvent()
        object NavigateToTCLite : NavigationEvent()
        object NavigateToHikMain : NavigationEvent()
        object StartThermalNightActivity : NavigationEvent()
    }

    sealed class ThermalAction {
        object ShowDeviceConnectTip : ThermalAction()
        object ShowConnectTip : ThermalAction()
        object ShowPermissionSettingsTip : ThermalAction()
    }

    sealed class PermissionState {
        object RequestCameraPermission : PermissionState()
        object PermissionGranted : PermissionState()
        data class PermissionDenied(val doNotAskAgain: Boolean) : PermissionState()
    }
}
