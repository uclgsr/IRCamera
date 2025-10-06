package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.gsr.data.GSRSensorRecorder
import mpdc4gsr.feature.gsr.data.GSRSettingsRepository
import mpdc4gsr.feature.network.data.RecordingController

/**
 * Modern GSR Settings ViewModel - MVVM StateFlow Implementation
 * Manages GSR sensor configuration, permissions, and device management with Repository pattern
 */
class GSRSettingsViewModel : AppBaseViewModel() {

    data class UIState(
        val gsrSettings: GSRSettingsRepository.GSRSettings = GSRSettingsRepository.GSRSettings(),
        val deviceSettings: GSRSettingsRepository.DeviceSettings = GSRSettingsRepository.DeviceSettings(),
        val permissionState: PermissionState = PermissionState(false, emptyList(), emptyList()),
        val connectionState: DeviceConnectionState = DeviceConnectionState(false),
        val scanningState: ScanningState = ScanningState.IDLE,
        val isLoading: Boolean = false
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

    private lateinit var repository: GSRSettingsRepository
    private var gsrSensorRecorder: GSRSensorRecorder? = null

    // StateFlow from Repository
    val gsrSettings: StateFlow<GSRSettingsRepository.GSRSettings> by lazy {
        repository.gsrSettings.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            GSRSettingsRepository.GSRSettings()
        )
    }
    val deviceSettings: StateFlow<GSRSettingsRepository.DeviceSettings> by lazy {
        repository.deviceSettings.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            GSRSettingsRepository.DeviceSettings()
        )
    }

    // Modern UI State Management with StateFlow
    private val _permissionState =
        MutableStateFlow(PermissionState(false, emptyList(), emptyList()))
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    private val _deviceConnectionState = MutableStateFlow(DeviceConnectionState(false))
    val deviceConnectionState: StateFlow<DeviceConnectionState> =
        _deviceConnectionState.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val availableDevices: StateFlow<List<DeviceInfo>> = _availableDevices.asStateFlow()

    private val _scanningState = MutableStateFlow(ScanningState.IDLE)
    val scanningState: StateFlow<ScanningState> = _scanningState.asStateFlow()

    // SharedFlow for one-time events
    private val _settingsEvents = MutableSharedFlow<SettingsEvent>()
    val settingsEvents: SharedFlow<SettingsEvent> = _settingsEvents.asSharedFlow()

    // Combined state for UI optimization
    val settingsUiState: StateFlow<UIState> by lazy {
        combine(
            if (::repository.isInitialized) repository.gsrSettings else flowOf(GSRSettingsRepository.GSRSettings()),
            if (::repository.isInitialized) repository.deviceSettings else flowOf(
                GSRSettingsRepository.DeviceSettings()
            ),
            _permissionState,
            _deviceConnectionState,
            _scanningState
        ) { gsrSettings, deviceSettings, permissions, connection, scanning ->
            UIState(gsrSettings, deviceSettings, permissions, connection, scanning)
        }.stateIn(viewModelScope, SharingStarted.Lazily, UIState())
    }

    // Modern Event-driven architecture with SharedFlow
    sealed class SettingsEvent {
        data class ShowPermissionDialog(val permissions: List<String>) : SettingsEvent()
        data class ShowPermissionDeniedDialog(val permissions: List<String>) : SettingsEvent()
        data class ShowPermissionPermanentlyDeniedDialog(val permissions: List<String>) :
            SettingsEvent()

        object OpenAppSettings : SettingsEvent()
        data class DeviceScanCompleted(val message: String) : SettingsEvent()
        data class DeviceConnected(val device: DeviceInfo, val message: String) : SettingsEvent()
        data class DeviceDisconnected(val message: String) : SettingsEvent()
        data class SettingsExported(val data: Map<String, Any>, val message: String) :
            SettingsEvent()

        data class SettingsImported(val message: String) : SettingsEvent()
        data class CalibrationStarted(val message: String) : SettingsEvent()
        data class CalibrationCompleted(val message: String) : SettingsEvent()
        data class ShowToast(val message: String) : SettingsEvent()
        data class ShowError(val message: String) : SettingsEvent()
    }

    fun initialize(context: Context) {
        repository = GSRSettingsRepository(context)
        checkPermissions(context)
        initializeGSRRecorder(context)
    }

    private fun initializeGSRRecorder(context: Context) {
        launchWithErrorHandling {
            try {
                val currentSettings = repository.gsrSettings.value
                // Create a temporary RecordingController since it's required by the constructor
                val tempRecordingController = RecordingController(
                    context,
                    object : androidx.lifecycle.LifecycleOwner {
                        override val lifecycle: androidx.lifecycle.Lifecycle
                            get() = androidx.lifecycle.LifecycleRegistry(this)
                    }
                )
                gsrSensorRecorder = GSRSensorRecorder(
                    context,
                    "gsr_settings_${System.currentTimeMillis()}",
                    currentSettings.samplingRate,
                    tempRecordingController
                )
                _deviceConnectionState.value = DeviceConnectionState(
                    isConnected = false,
                    connectionStatus = "Ready"
                )
            } catch (e: Exception) {
                _settingsEvents.emit(SettingsEvent.ShowError("Failed to initialize GSR recorder: ${e.message}"))
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
                permanentlyDeniedPermissions.add(permissions[i])
            }
        }

        launchWithErrorHandling {
            when {
                deniedPermissions.isEmpty() -> {
                    _permissionState.value = _permissionState.value.copy(
                        hasAllPermissions = true,
                        missingPermissions = emptyList()
                    )
                    enableDeviceManagement()
                }

                permanentlyDeniedPermissions.isNotEmpty() -> {
                    _settingsEvents.emit(
                        SettingsEvent.ShowPermissionPermanentlyDeniedDialog(
                            permanentlyDeniedPermissions
                        )
                    )
                }

                else -> {
                    _settingsEvents.emit(SettingsEvent.ShowPermissionDeniedDialog(deniedPermissions))
                }
            }
        }
    }

    fun requestPermissions() {
        launchWithErrorHandling {
            val currentState = _permissionState.value
            if (currentState.missingPermissions.isNotEmpty()) {
                _settingsEvents.emit(SettingsEvent.ShowPermissionDialog(currentState.missingPermissions))
            }
        }
    }

    fun startDeviceScan() {
        if (_scanningState.value == ScanningState.SCANNING) return

        _scanningState.value = ScanningState.SCANNING
        launchWithErrorHandling {
            try {
                // Simulate device scanning
                val devices = scanForDevices()
                _availableDevices.value = devices
                _scanningState.value = ScanningState.COMPLETED
                _settingsEvents.emit(SettingsEvent.DeviceScanCompleted("Found ${devices.size} device(s)"))
            } catch (e: Exception) {
                _scanningState.value = ScanningState.FAILED
                _settingsEvents.emit(SettingsEvent.ShowError("Device scan failed: ${e.message}"))
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
        launchWithErrorHandling {
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

                _settingsEvents.emit(
                    SettingsEvent.DeviceConnected(
                        deviceInfo,
                        "Connected to ${deviceInfo.name}"
                    )
                )

            } catch (e: Exception) {
                _deviceConnectionState.value = DeviceConnectionState(
                    isConnected = false,
                    connectionStatus = "Connection failed"
                )
                _settingsEvents.emit(SettingsEvent.ShowError("Failed to connect to device: ${e.message}"))
            }
        }
    }

    fun disconnectDevice() {
        launchWithErrorHandling {
            try {
                _deviceConnectionState.value = DeviceConnectionState(
                    isConnected = false,
                    connectionStatus = "Disconnected"
                )
                _settingsEvents.emit(SettingsEvent.DeviceDisconnected("Device disconnected"))
            } catch (e: Exception) {
                _settingsEvents.emit(SettingsEvent.ShowError("Failed to disconnect device: ${e.message}"))
            }
        }
    }

    fun updateGSRSettings(settings: GSRSettingsRepository.GSRSettings) {
        launchWithErrorHandling {
            repository.updateGSRSettings(settings)
            // Restart GSR recorder with new settings if needed
            if (gsrSensorRecorder != null) {
                // Update sampling rate, etc.
            }
        }
    }

    fun updateSamplingRate(samplingRate: Int) {
        launchWithErrorHandling {
            val currentSettings = repository.gsrSettings.value
            repository.updateGSRSettings(currentSettings.copy(samplingRate = samplingRate))
        }
    }

    fun updateDeviceSettings(settings: GSRSettingsRepository.DeviceSettings) {
        launchWithErrorHandling {
            repository.updateDeviceSettings(settings)
        }
    }

    fun exportSettings() {
        launchWithErrorHandling {
            val settingsMap = repository.exportSettings()
            _settingsEvents.emit(
                SettingsEvent.SettingsExported(
                    settingsMap,
                    "Settings exported successfully"
                )
            )
        }
    }

    fun importSettings(settingsMap: Map<String, Any>) {
        launchWithErrorHandling {
            val success = repository.importSettings(settingsMap)
            if (success) {
                _settingsEvents.emit(SettingsEvent.SettingsImported("Settings imported successfully"))
            } else {
                _settingsEvents.emit(SettingsEvent.ShowError("Failed to import settings: Invalid format"))
            }
        }
    }

    fun resetToDefaults() {
        launchWithErrorHandling {
            repository.resetToDefaults()
        }
    }

    fun startCalibration() {
        launchWithErrorHandling {
            _settingsEvents.emit(SettingsEvent.CalibrationStarted("Starting GSR calibration..."))
            try {
                kotlinx.coroutines.delay(3000)
                _settingsEvents.emit(SettingsEvent.CalibrationCompleted("Calibration completed successfully"))
            } catch (e: Exception) {
                _settingsEvents.emit(SettingsEvent.ShowError("Calibration failed: ${e.message}"))
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

    companion object {
        private const val TAG = "GSRSettingsViewModel"
    }
}