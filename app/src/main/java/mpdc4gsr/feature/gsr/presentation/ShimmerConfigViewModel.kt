package mpdc4gsr.feature.gsr.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewModelScope
import mpdc4gsr.core.ui.BaseViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.feature.gsr.domain.usecase.*

/**
 * Modern Shimmer Config ViewModel - Clean Architecture MVVM Implementation
 *
 * Uses use cases instead of direct repository/manager access for proper layer separation.
 * Manages Shimmer device configuration, scanning, and connections with reactive patterns.
 */
class ShimmerConfigViewModel(
    private val scanDevicesUseCase: ScanShimmerDevicesUseCase,
    private val connectDeviceUseCase: ConnectShimmerDeviceUseCase,
    private val disconnectDeviceUseCase: DisconnectShimmerDeviceUseCase,
    private val getBatteryLevelUseCase: GetDeviceBatteryUseCase,
    private val checkConnectionUseCase: CheckDeviceConnectionUseCase
) : BaseViewModel() {

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
    private val _shimmerUiState = MutableStateFlow(ShimmerConfigUiState())
    val shimmerUiState: StateFlow<ShimmerConfigUiState> = _shimmerUiState.asStateFlow()

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

    /**
     * Start scanning for Shimmer devices using use case
     */
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

    /**
     * Connect to a Shimmer device using use case
     */
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

    /**
     * Disconnect from Shimmer device using use case
     */
    fun disconnectDevice(deviceAddress: String) {
        viewModelScope.launch {
            disconnectDeviceUseCase(deviceAddress)
            _shimmerConnectionState.value = ConnectionState.Disconnected
            _configEvents.emit(ConfigEvent.DeviceDisconnected)
        }
    }

    /**
     * Get battery level for a device using use case
     */
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

    /**
     * Check if device is connected using use case
     */
    fun isDeviceConnected(deviceAddress: String): Boolean {
        return checkConnectionUseCase(deviceAddress)
    }

    /**
     * Check and update permission state
     */
    fun checkPermissions(context: Context) {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        _permissionState.value = PermissionState(
            hasAllPermissions = missingPermissions.isEmpty(),
            missingPermissions = missingPermissions
        )
    }

    /**
     * Handle permission result
     */
    fun onPermissionsGranted() {
        _permissionState.value = PermissionState(hasAllPermissions = true, missingPermissions = emptyList())
    }
}

/**
 * UI State for Shimmer Config screen
 */
data class ShimmerConfigUiState(
    val isScanning: Boolean = false,
    val batteryLevel: Int? = null,
    val error: String? = null
)

/**
 * Connection state sealed class
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceAddress: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * Permission state data class
 */
data class PermissionState(
    val hasAllPermissions: Boolean,
    val missingPermissions: List<String>
)

/**
 * Config events sealed class
 */
sealed class ConfigEvent {
    data class DeviceConnected(val deviceAddress: String) : ConfigEvent()
    object DeviceDisconnected : ConfigEvent()
    data class Error(val message: String) : ConfigEvent()
}

/**
 * Config action sealed class
 */
sealed class ConfigAction {
    object StartScan : ConfigAction()
    data class ConnectDevice(val deviceAddress: String) : ConfigAction()
    data class DisconnectDevice(val deviceAddress: String) : ConfigAction()
}
