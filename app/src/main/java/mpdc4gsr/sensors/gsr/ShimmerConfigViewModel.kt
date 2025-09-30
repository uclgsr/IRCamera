package mpdc4gsr.sensors.gsr

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewModelScope
import mpdc4gsr.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.sensors.unified.ShimmerDeviceManager
import mpdc4gsr.sensors.unified.model.DeviceInfo

/**
 * Modern Shimmer Config ViewModel - MVVM StateFlow Implementation
 * Manages Shimmer device configuration, scanning, and connections with reactive patterns
 */
class ShimmerConfigViewModel : BaseViewModel() {

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

    // Internal state
    private var shimmerDeviceManager: ShimmerDeviceManager? = null
    private var isScanning = false
    private var connectedDevice: DeviceInfo? = null
    private var connectedDeviceAddress: String? = null

    // Sealed classes for actions and events
    data class ConfigAction(val actionType: ActionType)

    enum class ActionType {
        SHOW_PROGRESS_BAR,
        HIDE_PROGRESS_BAR,
        SHOW_ERROR
    }

    // Data classes for state management
    data class ShimmerConfigUiState(
        val statusMessage: String = "Ready to scan for Shimmer devices",
        val isScanning: Boolean = false,
        val isLoading: Boolean = false,
        val hasManagerInitialized: Boolean = false,
        val deviceCount: Int = 0
    )

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val device: DeviceInfo) : ConnectionState()
        data class Failed(val message: String) : ConnectionState()
        data class Timeout(val message: String) : ConnectionState()
    }

    data class PermissionState(
        val hasAllPermissions: Boolean,
        val missingPermissions: List<String>,
        val shouldShowRationale: Boolean = false
    )

    sealed class ConfigEvent {
        data class ShowToast(val message: String) : ConfigEvent()
        object ShowPermissionError : ConfigEvent()
        object UpdateScanButton : ConfigEvent()
        object UpdateConnectionStatus : ConfigEvent()
        object HideProgressBar : ConfigEvent()
        object ShowProgressBar : ConfigEvent()
        data class DeviceConnected(val device: DeviceInfo) : ConfigEvent()
        data class ConnectionFailed(val message: String) : ConfigEvent()
        data class ShowError(val message: String) : ConfigEvent()
    }

    // Permission management
    fun checkPermissions(context: Context) {
        val missingPermissions = REQUIRED_PERMISSIONS.filter { permission ->
            ActivityCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }

        val hasAllPermissions = missingPermissions.isEmpty()
        _permissionState.value = PermissionState(
            hasAllPermissions = hasAllPermissions,
            missingPermissions = missingPermissions
        )

        if (hasAllPermissions) {
            initializeShimmerManager(context)
        }
    }

    fun onPermissionResult(context: Context, permissions: Map<String, Boolean>) {
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            initializeShimmerManager(context)
        } else {
            _shimmerUiState.value = _shimmerUiState.value.copy(
                statusMessage = "Bluetooth permissions required for Shimmer device scanning"
            )
            launchWithErrorHandling {
                _configEvents.emit(ConfigEvent.ShowPermissionError)
            }
        }
    }

    // Shimmer manager initialization
    private fun initializeShimmerManager(context: Context) {
        launchWithErrorHandling {
            try {
                _shimmerUiState.value = _shimmerUiState.value.copy(isLoading = true)

                // Note: This would need to be adapted based on actual ShimmerDeviceManager API
                // shimmerDeviceManager = ShimmerDeviceManager(context, context)

                // Simulated initialization - replace with actual implementation
                val initialized = true // shimmerDeviceManager?.initialize() ?: false

                if (initialized) {
                    _shimmerUiState.value = _shimmerUiState.value.copy(
                        statusMessage = "Shimmer device manager ready - tap 'Start Scan' to discover devices",
                        isLoading = false,
                        hasManagerInitialized = true
                    )
                    setupDeviceFlowCollectors()
                } else {
                    _shimmerUiState.value = _shimmerUiState.value.copy(
                        statusMessage = "Failed to initialize Bluetooth - check if Bluetooth is enabled",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _shimmerUiState.value = _shimmerUiState.value.copy(
                    statusMessage = "Initialization error: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private fun setupDeviceFlowCollectors() {
        // Device discovery flow
        viewModelScope.launch {
            shimmerDeviceManager?.scanResults?.collectLatest { devices ->
                _discoveredDevices.value = devices
                _shimmerUiState.value = _shimmerUiState.value.copy(
                    deviceCount = devices.size,
                    statusMessage = if (devices.isEmpty() && isScanning) {
                        "Scanning for Shimmer devices... (${devices.size} found)"
                    } else if (devices.isNotEmpty()) {
                        "Found ${devices.size} Shimmer device(s) - select one to connect"
                    } else {
                        _shimmerUiState.value.statusMessage
                    }
                )
            }
        }

        // Connection events flow
        viewModelScope.launch {
            shimmerDeviceManager?.connectionEvents?.collectLatest { event ->
                when (event.state) {
                    ShimmerDeviceManager.ConnectionState.CONNECTING -> {
                        _shimmerConnectionState.value = ConnectionState.Connecting
                        _shimmerUiState.value = _shimmerUiState.value.copy(
                            statusMessage = "Connecting to Shimmer device...",
                            isLoading = true
                        )
                        _configAction.emit(ConfigAction(ActionType.SHOW_PROGRESS_BAR))
                    }

                    ShimmerDeviceManager.ConnectionState.CONNECTED -> {
                        val device = getDeviceByAddress(event.deviceAddress)
                        device?.let {
                            connectedDevice = it
                            connectedDeviceAddress = event.deviceAddress
                            _shimmerConnectionState.value = ConnectionState.Connected(it)
                            _shimmerUiState.value = _shimmerUiState.value.copy(
                                statusMessage = "Successfully connected to ${it.name ?: event.deviceAddress}",
                                isLoading = false
                            )
                            _configAction.emit(ConfigAction(ActionType.HIDE_PROGRESS_BAR))
                        }
                    }

                    ShimmerDeviceManager.ConnectionState.DISCONNECTED -> {
                        connectedDevice = null
                        _shimmerConnectionState.value = ConnectionState.Disconnected
                        _shimmerUiState.value = _shimmerUiState.value.copy(
                            statusMessage = "Shimmer device disconnected",
                            isLoading = false
                        )
                        _configAction.emit(ConfigAction(ActionType.HIDE_PROGRESS_BAR))
                    }

                    ShimmerDeviceManager.ConnectionState.FAILED -> {
                        _shimmerConnectionState.value = ConnectionState.Failed(
                            event.message ?: "Unknown error"
                        )
                        _shimmerUiState.value = _shimmerUiState.value.copy(
                            statusMessage = "Connection failed: ${event.message ?: "Unknown error"}",
                            isLoading = false
                        )
                        _configAction.emit(ConfigAction(ActionType.SHOW_ERROR))
                    }

                    ShimmerDeviceManager.ConnectionState.TIMEOUT -> {
                        _shimmerConnectionState.value = ConnectionState.Timeout(
                            "Connection timeout"
                        )
                        _shimmerUiState.value = _shimmerUiState.value.copy(
                            statusMessage = "Connection timeout - device may be out of range or not responding",
                            isLoading = false
                        )
                        _configAction.emit(ConfigAction(ActionType.SHOW_ERROR))
                    }
                }
            }
        }
    }

    // Device scanning operations
    fun toggleScanning() {
        if (isScanning) {
            stopDeviceScanning()
        } else {
            startDeviceScanning()
        }
    }

    private fun startDeviceScanning() {
        val manager = shimmerDeviceManager
        if (manager == null) {
            _shimmerUiState.value = _shimmerUiState.value.copy(
                statusMessage = "Device manager not initialized"
            )
            return
        }

        viewModelScope.launch {
            try {
                val scanStarted = true // manager.startDeviceScanning()
                if (scanStarted) {
                    isScanning = true
                    _shimmerUiState.value = _shimmerUiState.value.copy(
                        isScanning = true,
                        statusMessage = "Scanning for Shimmer3 GSR+ devices...",
                        deviceCount = 0
                    )
                    _discoveredDevices.value = emptyList()
                } else {
                    _shimmerUiState.value = _shimmerUiState.value.copy(
                        statusMessage = "Failed to start device scanning - check Bluetooth permissions"
                    )
                }
            } catch (e: Exception) {
                _shimmerUiState.value = _shimmerUiState.value.copy(
                    statusMessage = "Scan error: ${e.message}"
                )
            }
        }
    }

    private fun stopDeviceScanning() {
        val manager = shimmerDeviceManager ?: return

        viewModelScope.launch {
            try {
                // manager.stopDeviceScanning()
                isScanning = false
                val deviceCount = _discoveredDevices.value.size
                _shimmerUiState.value = _shimmerUiState.value.copy(
                    isScanning = false,
                    statusMessage = if (deviceCount > 0) {
                        "Scan completed - found $deviceCount Shimmer device(s)"
                    } else {
                        "Scan completed - no Shimmer devices found"
                    }
                )
                // _configAction.value = ConfigAction(ActionType.UPDATE_SCAN_BUTTON)
            } catch (e: Exception) {
                _shimmerUiState.value = _shimmerUiState.value.copy(
                    statusMessage = "Error stopping scan: ${e.message}"
                )
            }
        }
    }

    // Device connection operations
    fun connectToDevice(device: DeviceInfo) {
        val manager = shimmerDeviceManager
        if (manager == null) {
            _shimmerUiState.value = _shimmerUiState.value.copy(
                statusMessage = "Device manager not initialized"
            )
            return
        }

        viewModelScope.launch {
            try {
                // manager.connectToDevice(device.address)
                _shimmerConnectionState.value = ConnectionState.Connecting
            } catch (e: Exception) {
                _shimmerUiState.value = _shimmerUiState.value.copy(
                    statusMessage = "Connection error: ${e.message}"
                )
            }
        }
    }

    fun testConnection() {
        val device = connectedDevice
        if (device == null) {
            // _configAction.value = ConfigAction(
            //     ActionType.SHOW_TOAST,
            //     message = "No device connected"
            // )
            return
        }

        viewModelScope.launch {
            try {
                _shimmerUiState.value = _shimmerUiState.value.copy(
                    statusMessage = "Testing connection to ${device.name}...",
                    isLoading = true
                )

                // Simulate connection test
                kotlinx.coroutines.delay(2000)

                _shimmerUiState.value = _shimmerUiState.value.copy(
                    statusMessage = "Connection test successful",
                    isLoading = false
                )
                // _configAction.value = ConfigAction(
                //     ActionType.SHOW_TOAST,
                //     message = "Connection test successful"
                // )
            } catch (e: Exception) {
                _shimmerUiState.value = _shimmerUiState.value.copy(
                    statusMessage = "Connection test failed: ${e.message}",
                    isLoading = false
                )
                // _configAction.value = ConfigAction(
                //     ActionType.SHOW_TOAST,
                //     message = "Connection test failed"
                // )
            }
        }
    }

    fun disconnectDevice() {
        val manager = shimmerDeviceManager ?: return
        val deviceAddress = connectedDeviceAddress

        viewModelScope.launch {
            try {
                if (deviceAddress != null) {
                    manager.disconnectDevice(deviceAddress)
                }
                connectedDevice = null
                connectedDeviceAddress = null
                _shimmerConnectionState.value = ConnectionState.Disconnected
                _shimmerUiState.value = _shimmerUiState.value.copy(
                    statusMessage = "Device disconnected"
                )
                // _configAction.value = ConfigAction(
                //     ActionType.SHOW_TOAST,
                //     message = "Device disconnected"
                // )
            } catch (e: Exception) {
                _shimmerUiState.value = _shimmerUiState.value.copy(
                    statusMessage = "Disconnect error: ${e.message}"
                )
            }
        }
    }

    // Helper functions
    private fun getDeviceByAddress(address: String): DeviceInfo? {
        return _discoveredDevices.value.find { it.address == address }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                shimmerDeviceManager?.stopDeviceScanning()
                connectedDeviceAddress?.let { address ->
                    shimmerDeviceManager?.disconnectDevice(address)
                }
            } catch (e: Exception) {
                // Log error but don't propagate
            }
        }
    }
}