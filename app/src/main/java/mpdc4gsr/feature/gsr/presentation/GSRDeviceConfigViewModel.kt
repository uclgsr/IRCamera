package mpdc4gsr.feature.gsr.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.core.sensors.gsr.model.DeviceInfo
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.gsr.domain.usecase.*
import javax.inject.Inject

@HiltViewModel
class GSRDeviceConfigViewModel @Inject constructor(
    private val scanDevicesUseCase: ScanGSRDevicesUseCase,
    private val connectDeviceUseCase: ConnectGSRDeviceUseCase,
    private val disconnectDeviceUseCase: DisconnectGSRDeviceUseCase,
    private val getBatteryLevelUseCase: GetGSRDeviceBatteryUseCase,
    private val checkConnectionUseCase: CheckGSRDeviceConnectionUseCase
) : AppBaseViewModel() {
    companion object {
        private val REQUIRED_PERMISSIONS =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
    }

    // StateFlow for UI state management
    private val _shimmerUiState = MutableStateFlow(GSRDeviceConfigUiState())
    val shimmerUiState: StateFlow<GSRDeviceConfigUiState> = _shimmerUiState.asStateFlow()

    // Device management StateFlows
    private val _discoveredDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<DeviceInfo>> = _discoveredDevices.asStateFlow()
    private val _shimmerConnectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val shimmerConnectionState: StateFlow<ConnectionState> = _shimmerConnectionState.asStateFlow()

    // Permission management StateFlow
    private val _permissionState = MutableStateFlow(PermissionState(false, emptyList()))
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    // SharedFlow for one-time events
    private val _configEvents = MutableSharedFlow<ConfigEvent>()
    val configEvents: SharedFlow<ConfigEvent> = _configEvents.asSharedFlow()

    // SharedFlow for config actions
    private val _configAction = MutableSharedFlow<ConfigAction>()
    val configAction: SharedFlow<ConfigAction> = _configAction.asSharedFlow()

    fun startScan() {
        viewModelScope.launch {
            _shimmerUiState.update { it.copy(isScanning = true, error = null) }
            try {
                scanDevicesUseCase().collect { devices ->
                    _discoveredDevices.value = devices
                    _shimmerUiState.update { it.copy(isScanning = false) }
                }
            } catch (e: Exception) {
                _shimmerUiState.update {
                    it.copy(isScanning = false, error = e.message ?: "Scan failed")
                }
            }
        }
    }

    fun connectDevice(deviceAddress: String) {
        viewModelScope.launch {
            _shimmerConnectionState.value = ConnectionState.Connecting
            val result = connectDeviceUseCase(deviceAddress)
            result.fold(
                onSuccess = {
                    _shimmerConnectionState.value = ConnectionState.Connected(deviceAddress)
                    _configEvents.emit(ConfigEvent.DeviceConnected(deviceAddress))
                },
                onFailure = { error ->
                    _shimmerConnectionState.value = ConnectionState.Error(error.message ?: "Connection failed")
                    _configEvents.emit(ConfigEvent.Error(error.message ?: "Connection failed"))
                }
            )
        }
    }

    fun disconnectDevice(deviceAddress: String) {
        viewModelScope.launch {
            disconnectDeviceUseCase(deviceAddress)
            _shimmerConnectionState.value = ConnectionState.Disconnected
            _configEvents.emit(ConfigEvent.DeviceDisconnected)
        }
    }

    fun getBatteryLevel(deviceAddress: String) {
        viewModelScope.launch {
            val batteryLevel = getBatteryLevelUseCase(deviceAddress)
            batteryLevel?.let {
                _shimmerUiState.update { state ->
                    state.copy(batteryLevel = it)
                }
            }
        }
    }

    fun isDeviceConnected(deviceAddress: String): Boolean {
        return checkConnectionUseCase(deviceAddress)
    }

    fun checkPermissions(context: Context) {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        _permissionState.value = PermissionState(
            hasAllPermissions = missingPermissions.isEmpty(),
            missingPermissions = missingPermissions
        )
    }

    fun onPermissionsGranted() {
        _permissionState.value = PermissionState(hasAllPermissions = true, missingPermissions = emptyList())
    }
}

data class GSRDeviceConfigUiState(
    val isScanning: Boolean = false,
    val batteryLevel: Int? = null,
    val error: String? = null
)

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceAddress: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class PermissionState(
    val hasAllPermissions: Boolean,
    val missingPermissions: List<String>
)

sealed class ConfigEvent {
    data class DeviceConnected(val deviceAddress: String) : ConfigEvent()
    object DeviceDisconnected : ConfigEvent()
    data class Error(val message: String) : ConfigEvent()
}

sealed class ConfigAction {
    object StartScan : ConfigAction()
    data class ConnectDevice(val deviceAddress: String) : ConfigAction()
    data class DisconnectDevice(val deviceAddress: String) : ConfigAction()
}
