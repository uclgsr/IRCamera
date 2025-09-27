package mpdc4gsr.sensors.gsr

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for GSR Settings - Phase 4 MVVM Implementation  
 * Manages GSR sensor configuration, permissions, and device management with Repository pattern
 */
class GSRSettingsViewModel : BaseViewModel() {

    private lateinit var repository: GSRSettingsRepository
    private var gsrSensorRecorder: GSRSensorRecorder? = null

    // LiveData from Repository StateFlows
    val gsrSettings by lazy { repository.gsrSettings.asLiveData() }
    val deviceSettings by lazy { repository.deviceSettings.asLiveData() }

    // UI State Management
    private val _permissionState = MutableLiveData<PermissionState>()
    val permissionState: LiveData<PermissionState> = _permissionState

    private val _deviceConnectionState = MutableLiveData<DeviceConnectionState>()
    val deviceConnectionState: LiveData<DeviceConnectionState> = _deviceConnectionState

    private val _availableDevices = MutableLiveData<List<DeviceInfo>>()
    val availableDevices: LiveData<List<DeviceInfo>> = _availableDevices

    private val _scanningState = MutableLiveData<ScanningState>()
    val scanningState: LiveData<ScanningState> = _scanningState

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _settingsAction = MutableLiveData<SettingsAction?>()
    val settingsAction: LiveData<SettingsAction?> = _settingsAction

    // Combined state for UI optimization
    val uiState by lazy {
        combine(repository.gsrSettings, repository.deviceSettings) { gsrSettings, deviceSettings ->
            UIState(gsrSettings, deviceSettings)
        }.asLiveData()
    }

    data class UIState(
        val gsrSettings: GSRSettingsRepository.GSRSettings,
        val deviceSettings: GSRSettingsRepository.DeviceSettings
    )

    data class PermissionState(
        val hasAllPermissions: Boolean,
        val missingPermissions: List<String>,
        val shouldShowRationale: List<String>
    )

    data class DeviceConnectionState(
        val isConnected: Boolean,
        val deviceInfo: DeviceInfo? = null,
        val connectionStatus: String = "Disconnected",
        val signalStrength: Int = 0
    )

    data class DeviceInfo(
        val id: String,
        val name: String,
        val address: String,
        val isConnected: Boolean = false,
        val batteryLevel: Int? = null,
        val signalStrength: Int = 0
    )

    enum class ScanningState {
        IDLE, SCANNING, COMPLETED, FAILED
    }

    data class SettingsAction(
        val type: ActionType,
        val message: String? = null,
        val data: Any? = null
    )

    enum class ActionType {
        SHOW_PERMISSION_DIALOG,
        SHOW_PERMISSION_DENIED_DIALOG,
        SHOW_PERMISSION_PERMANENTLY_DENIED_DIALOG,
        OPEN_APP_SETTINGS,
        DEVICE_SCAN_COMPLETED,
        DEVICE_CONNECTED,
        DEVICE_DISCONNECTED,
        SETTINGS_EXPORTED,
        SETTINGS_IMPORTED,
        CALIBRATION_STARTED,
        CALIBRATION_COMPLETED
    }

    fun initialize(context: Context) {
        repository = GSRSettingsRepository(context)
        checkPermissions(context)
        initializeGSRRecorder(context)
    }

    private fun initializeGSRRecorder(context: Context) {
        viewModelScope.launch {
            try {
                val currentSettings = repository.gsrSettings.value
                // Note: LifecycleOwner will be set when the activity is available
                // For now, we initialize without the recording controller
                gsrSensorRecorder = GSRSensorRecorder(
                    context,
                    "gsr_settings_${System.currentTimeMillis()}",
                    currentSettings.samplingRate,
                    null // RecordingController will be set later when LifecycleOwner is available
                )
                _deviceConnectionState.value = DeviceConnectionState(
                    isConnected = false,
                    connectionStatus = "Ready"
                )
            } catch (e: Exception) {
                _error.value = "Failed to initialize GSR recorder: ${e.message}"
            }
        }
    }

    fun checkPermissions(context: Context) {
        val missingPermissions = getMissingPermissions(context)
        val shouldShowRationale = mutableListOf<String>()

        // Check rationale for missing permissions
        missingPermissions.forEach { permission ->
            // Note: shouldShowRequestPermissionRationale check would be handled in Activity
            // ViewModel focuses on permission state management
        }

        _permissionState.value = PermissionState(
            hasAllPermissions = missingPermissions.isEmpty(),
            missingPermissions = missingPermissions,
            shouldShowRationale = shouldShowRationale
        )
    }

    fun onPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        val deniedPermissions = mutableListOf<String>()
        val permanentlyDeniedPermissions = mutableListOf<String>()

        for (i in permissions.indices) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i])
                // In a real scenario, we'd check if rationale should be shown
                // For now, assume they're permanently denied if denied
                permanentlyDeniedPermissions.add(permissions[i])
            }
        }

        when {
            deniedPermissions.isEmpty() -> {
                _permissionState.value = _permissionState.value?.copy(
                    hasAllPermissions = true,
                    missingPermissions = emptyList()
                )
                enableDeviceManagement()
            }
            permanentlyDeniedPermissions.isNotEmpty() -> {
                _settingsAction.value = SettingsAction(
                    type = ActionType.SHOW_PERMISSION_PERMANENTLY_DENIED_DIALOG,
                    data = permanentlyDeniedPermissions
                )
            }
            else -> {
                _settingsAction.value = SettingsAction(
                    type = ActionType.SHOW_PERMISSION_DENIED_DIALOG,
                    data = deniedPermissions
                )
            }
        }
    }

    fun requestPermissions() {
        val currentState = _permissionState.value
        if (currentState?.missingPermissions?.isNotEmpty() == true) {
            _settingsAction.value = SettingsAction(
                type = ActionType.SHOW_PERMISSION_DIALOG,
                data = currentState.missingPermissions
            )
        }
    }

    fun startDeviceScan() {
        if (_scanningState.value == ScanningState.SCANNING) return

        _scanningState.value = ScanningState.SCANNING
        viewModelScope.launch {
            try {
                // Simulate device scanning
                val devices = scanForDevices()
                _availableDevices.value = devices
                _scanningState.value = ScanningState.COMPLETED
                _settingsAction.value = SettingsAction(
                    type = ActionType.DEVICE_SCAN_COMPLETED,
                    message = "Found ${devices.size} device(s)"
                )
            } catch (e: Exception) {
                _scanningState.value = ScanningState.FAILED
                _error.value = "Device scan failed: ${e.message}"
            }
        }
    }

    private suspend fun scanForDevices(): List<DeviceInfo> {
        // Simulate device discovery
        return listOf(
            DeviceInfo("shimmer_001", "Shimmer GSR #001", "00:11:22:AA:BB:CC"),
            DeviceInfo("shimmer_002", "Shimmer GSR #002", "00:11:22:AA:BB:DD"),
            DeviceInfo("shimmer_003", "Shimmer GSR #003", "00:11:22:AA:BB:EE")
        )
    }

    fun connectToDevice(deviceInfo: DeviceInfo) {
        viewModelScope.launch {
            try {
                _deviceConnectionState.value = DeviceConnectionState(
                    isConnected = false,
                    deviceInfo = deviceInfo,
                    connectionStatus = "Connecting..."
                )

                // Simulate connection process
                kotlinx.coroutines.delay(2000)

                // Update device settings in repository
                val currentDeviceSettings = repository.deviceSettings.value
                repository.updateDeviceSettings(
                    currentDeviceSettings.copy(
                        selectedDeviceId = deviceInfo.id,
                        deviceName = deviceInfo.name
                    )
                )

                _deviceConnectionState.value = DeviceConnectionState(
                    isConnected = true,
                    deviceInfo = deviceInfo,
                    connectionStatus = "Connected",
                    signalStrength = 85
                )

                _settingsAction.value = SettingsAction(
                    type = ActionType.DEVICE_CONNECTED,
                    message = "Connected to ${deviceInfo.name}"
                )

            } catch (e: Exception) {
                _deviceConnectionState.value = DeviceConnectionState(
                    isConnected = false,
                    connectionStatus = "Connection failed"
                )
                _error.value = "Failed to connect to device: ${e.message}"
            }
        }
    }

    fun disconnectDevice() {
        viewModelScope.launch {
            try {
                _deviceConnectionState.value = DeviceConnectionState(
                    isConnected = false,
                    connectionStatus = "Disconnected"
                )

                _settingsAction.value = SettingsAction(
                    type = ActionType.DEVICE_DISCONNECTED,
                    message = "Device disconnected"
                )
            } catch (e: Exception) {
                _error.value = "Failed to disconnect device: ${e.message}"
            }
        }
    }

    fun updateGSRSettings(settings: GSRSettingsRepository.GSRSettings) {
        viewModelScope.launch {
            try {
                repository.updateGSRSettings(settings)
                // Restart GSR recorder with new settings if needed
                if (gsrSensorRecorder != null) {
                    // Update sampling rate, etc.
                }
            } catch (e: Exception) {
                _error.value = "Failed to update GSR settings: ${e.message}"
            }
        }
    }

    fun updateDeviceSettings(settings: GSRSettingsRepository.DeviceSettings) {
        viewModelScope.launch {
            try {
                repository.updateDeviceSettings(settings)
            } catch (e: Exception) {
                _error.value = "Failed to update device settings: ${e.message}"
            }
        }
    }

    fun exportSettings() {
        viewModelScope.launch {
            try {
                val settingsMap = repository.exportSettings()
                _settingsAction.value = SettingsAction(
                    type = ActionType.SETTINGS_EXPORTED,
                    message = "Settings exported successfully",
                    data = settingsMap
                )
            } catch (e: Exception) {
                _error.value = "Failed to export settings: ${e.message}"
            }
        }
    }

    fun importSettings(settingsMap: Map<String, Any>) {
        viewModelScope.launch {
            try {
                val success = repository.importSettings(settingsMap)
                if (success) {
                    _settingsAction.value = SettingsAction(
                        type = ActionType.SETTINGS_IMPORTED,
                        message = "Settings imported successfully"
                    )
                } else {
                    _error.value = "Failed to import settings: Invalid format"
                }
            } catch (e: Exception) {
                _error.value = "Failed to import settings: ${e.message}"
            }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                repository.resetToDefaults()
            } catch (e: Exception) {
                _error.value = "Failed to reset settings: ${e.message}"
            }
        }
    }

    private fun enableDeviceManagement() {
        // Enable device-related UI
    }

    private fun getMissingPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missing.add(android.Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missing.add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missing.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        return missing
    }

    fun clearError() {
        _error.value = null
    }

    fun clearAction() {
        _settingsAction.value = null
    }

    companion object {
        private const val TAG = "GSRSettingsViewModel"
    }
}