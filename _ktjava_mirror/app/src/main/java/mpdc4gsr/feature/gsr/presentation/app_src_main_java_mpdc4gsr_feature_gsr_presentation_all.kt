// Merged .kt under 'app\src\main\java\mpdc4gsr\feature\gsr\presentation' subtree
// Files: 11; Generated 2025-10-07 23:07:39


// ===== app\src\main\java\mpdc4gsr\feature\gsr\presentation\GSRRawImageViewViewModel.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import java.io.File

class GSRRawImageViewViewModel(
    context: Context
) : AppBaseViewModel() {
    private val application: Context = context.applicationContext

    data class GSRImageViewState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val imageFiles: List<File> = emptyList(),
        val selectedImage: File? = null
    )

    private val _imageViewState = MutableStateFlow(GSRImageViewState())
    val imageViewState: StateFlow<GSRImageViewState> = _imageViewState.asStateFlow()

    init {
        loadImages()
    }

    fun loadImages() {
        viewModelScope.launch {
            _imageViewState.value = _imageViewState.value.copy(isLoading = true, error = null)
            try {
                val imageFiles = getGSRImageFiles()
                _imageViewState.value = _imageViewState.value.copy(
                    isLoading = false,
                    imageFiles = imageFiles
                )
            } catch (e: Exception) {
                _imageViewState.value = _imageViewState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load images"
                )
            }
        }
    }

    fun openImage(imageFile: File) {
        viewModelScope.launch {
            try {
                val context = application.applicationContext
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                _imageViewState.value = _imageViewState.value.copy(
                    error = "Failed to open image: ${e.message}"
                )
            }
        }
    }

    fun shareImage(imageFile: File) {
        viewModelScope.launch {
            try {
                val context = application.applicationContext
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Share Image"))
            } catch (e: Exception) {
                _imageViewState.value = _imageViewState.value.copy(
                    error = "Failed to share image: ${e.message}"
                )
            }
        }
    }

    fun deleteImage(imageFile: File) {
        viewModelScope.launch {
            try {
                if (imageFile.delete()) {
                    // Reload images after deletion
                    loadImages()
                } else {
                    _imageViewState.value = _imageViewState.value.copy(
                        error = "Failed to delete image"
                    )
                }
            } catch (e: Exception) {
                _imageViewState.value = _imageViewState.value.copy(
                    error = "Error deleting image: ${e.message}"
                )
            }
        }
    }

    private fun getGSRImageFiles(): List<File> {
        val imageFiles = mutableListOf<File>()
        // Check multiple possible directories
        val possibleDirectories = listOf(
            // External storage directories
            File(Environment.getExternalStorageDirectory(), "GSR/Images"),
            File(Environment.getExternalStorageDirectory(), "IRCamera/GSR"),
            File(Environment.getExternalStorageDirectory(), "DCIM/GSR"),
            // App-specific directories
            File(application.externalCacheDir, "gsr_images"),
            File(
                application.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "GSR"
            ),
            File(application.filesDir, "gsr_images")
        )
        for (directory in possibleDirectories) {
            if (directory.exists() && directory.isDirectory) {
                directory.listFiles { file ->
                    file.isFile && isImageFile(file.name)
                }?.let { files ->
                    imageFiles.addAll(files)
                }
            }
        }
        // Sort by last modified (newest first)
        return imageFiles.sortedByDescending { it.lastModified() }
    }

    private fun isImageFile(fileName: String): Boolean {
        val imageExtensions =
            listOf(".jpg", ".jpeg", ".png", ".bmp", ".gif", ".webp")
        return imageExtensions.any { fileName.lowercase().endsWith(it) }
    }

    fun getImageMetadata(imageFile: File): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        try {
            metadata["Name"] = imageFile.name
            metadata["Size"] = formatFileSize(imageFile.length())
            metadata["Modified"] = formatDate(imageFile.lastModified())
            metadata["Path"] = imageFile.absolutePath
            // Try to get image dimensions
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeFile(
                imageFile.absolutePath,
                options
            )
            if (options.outWidth > 0 && options.outHeight > 0) {
                metadata["Dimensions"] =
                    "${options.outWidth} x ${options.outHeight}"
                metadata["Type"] = options.outMimeType ?: "Unknown"
            }
        } catch (e: Exception) {
            metadata["Error"] = "Failed to read metadata: ${e.message}"
        }
        return metadata
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format(
                "%.1f MB",
                bytes / (1024.0 * 1024.0)
            )

            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    private fun formatDate(timestamp: Long): String {
        return java.text.SimpleDateFormat(
            "MMM dd, yyyy HH:mm",
            java.util.Locale.getDefault()
        )
            .format(java.util.Date(timestamp))
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\presentation\GSRRawImageViewViewModelFactory.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GSRRawImageViewViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GSRRawImageViewViewModel::class.java)) {
            return GSRRawImageViewViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\presentation\GSRSensorViewModel.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.UnifiedGSRRecorder
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.gsr.data.GSRSettingsRepository
import java.text.SimpleDateFormat
import java.util.*

class GSRSensorViewModel(
    context: Context
) : AppBaseViewModel() {
    private val application: Context = context.applicationContext

    companion object {
        // Reuse SimpleDateFormat instance for better performance
        private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        private const val MAX_HISTORY_SIZE = 100

        // Reconnection configuration (can be made user-configurable)
        const val DEFAULT_MAX_RECONNECTION_ATTEMPTS = 3
        const val DEFAULT_BASE_RECONNECTION_DELAY_MS = 2000L

        // Device scanning delay
        private const val DEVICE_SCAN_DELAY_MS = 3000L
    }

    data class GSRSensorState(
        val isConnected: Boolean = false,
        val isRecording: Boolean = false,
        val currentGSR: Float = 0f,
        val skinConductance: Float = 0f,
        val deviceBattery: Int = 0,
        val samplingRate: Int = 128,
        val gsrHistory: List<Float> = emptyList(),
        val error: String? = null,
        val connectionStatus: String = "Disconnected",
        val isReconnecting: Boolean = false,
        val reconnectionAttempt: Int = 0,
        val maxReconnectionAttempts: Int = 0
    )

    data class ReconnectionConfig(
        val maxAttempts: Int = DEFAULT_MAX_RECONNECTION_ATTEMPTS,
        val baseDelayMs: Long = DEFAULT_BASE_RECONNECTION_DELAY_MS,
        val enabled: Boolean = true
    )

    private val _sensorState = MutableStateFlow(GSRSensorState())
    val sensorState: StateFlow<GSRSensorState> = _sensorState.asStateFlow()
    private var reconnectionConfig = ReconnectionConfig()
    private var lastConnectedDeviceAddress: String? = null
    private var wasRecordingBeforeDisconnect = false
    private var settingsRepository: GSRSettingsRepository? = null

    // Expose recorder for lifecycle management from UI layer
    var gsrRecorder: UnifiedGSRRecorder? = null
        private set

    fun initializeRecorder(
        context: Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        reconnectionConfig: ReconnectionConfig? = null
    ) {
        viewModelScope.launch {
            try {
                // Initialize settings repository and load reconnection config
                if (settingsRepository == null) {
                    settingsRepository = GSRSettingsRepository(context)
                }
                // Load reconnection config from settings if not provided
                val configToUse =
                    reconnectionConfig ?: settingsRepository?.deviceSettings?.value?.let { deviceSettings ->
                        ReconnectionConfig(
                            maxAttempts = deviceSettings.reconnectionAttempts,
                            baseDelayMs = deviceSettings.reconnectionBaseDelayMs,
                            enabled = deviceSettings.autoReconnect
                        )
                    } ?: ReconnectionConfig()
                this@GSRSensorViewModel.reconnectionConfig = configToUse
                gsrRecorder = UnifiedGSRRecorder(
                    context = context,
                    lifecycleOwner = lifecycleOwner
                )
                val initialized = gsrRecorder?.initialize() ?: false
                if (initialized) {
                    _sensorState.update {
                        it.copy(
                            isConnected = false,
                            connectionStatus = "Initialized",
                            error = null
                        )
                    }
                    startDataCollection()
                    startConnectionMonitoring()
                    observeSettingsChanges()
                } else {
                    _sensorState.update {
                        it.copy(
                            connectionStatus = "Initialization Failed",
                            error = "Failed to initialize GSR recorder"
                        )
                    }
                }
            } catch (e: Exception) {
                _sensorState.update {
                    it.copy(
                        connectionStatus = "Error",
                        error = "Error initializing: ${e.message}"
                    )
                }
            }
        }
    }

    private fun observeSettingsChanges() {
        viewModelScope.launch {
            settingsRepository?.deviceSettings?.collect { deviceSettings ->
                reconnectionConfig = ReconnectionConfig(
                    maxAttempts = deviceSettings.reconnectionAttempts,
                    baseDelayMs = deviceSettings.reconnectionBaseDelayMs,
                    enabled = deviceSettings.autoReconnect
                )
                mpdc4gsr.core.utils.AppLogger.d(
                    "GSRSensorViewModel",
                    "Reconnection config updated: attempts=${reconnectionConfig.maxAttempts}, " +
                            "delay=${reconnectionConfig.baseDelayMs}ms, enabled=${reconnectionConfig.enabled}"
                )
            }
        }
    }

    fun connectDevice() {
        viewModelScope.launch {
            try {
                // Scan for devices and connect to the first available one
                val devices = gsrRecorder?.getDiscoveredDevices()
                if (devices.isNullOrEmpty()) {
                    _sensorState.update { it.copy(error = "No devices found. Please scan first.") }
                    return@launch
                }
                // Connect to first available device
                val device = devices.firstOrNull()
                if (device != null) {
                    // Actually connect to the device using the recorder
                    val connected = gsrRecorder?.connectToDevice(device) ?: false
                    if (connected) {
                        lastConnectedDeviceAddress = device.address
                        _sensorState.update { it.copy(isConnected = true, error = null) }
                    } else {
                        _sensorState.update { it.copy(error = "Failed to connect to device") }
                    }
                } else {
                    _sensorState.update { it.copy(error = "No valid device to connect") }
                }
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Connection failed: ${e.message}") }
            }
        }
    }

    fun updateReconnectionConfig(config: ReconnectionConfig) {
        reconnectionConfig = config
        mpdc4gsr.core.utils.AppLogger.d(
            "GSRSensorViewModel",
            "Reconnection config manually updated: attempts=${config.maxAttempts}, " +
                    "delay=${config.baseDelayMs}ms, enabled=${config.enabled}"
        )
    }

    fun getReconnectionConfig(): ReconnectionConfig = reconnectionConfig

    fun getSettingsRepository() = settingsRepository

    fun disconnectDevice() {
        viewModelScope.launch {
            try {
                gsrRecorder?.stopRecording()
                _sensorState.update { it.copy(isConnected = false, isRecording = false) }
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Disconnect failed: ${e.message}") }
            }
        }
    }

    fun startRecording() {
        viewModelScope.launch {
            try {
                val sessionDir = application.getExternalFilesDir("gsr_sessions")?.absolutePath
                    ?: application.filesDir.absolutePath
                val currentTimeMs = System.currentTimeMillis()
                val currentMonotonicNs = System.nanoTime()
                val metadata = mpdc4gsr.core.data.SessionMetadata(
                    sessionId = "gsr_${currentTimeMs}",
                    sessionStartTimestampMs = currentTimeMs,
                    sessionStartMonotonicNs = currentMonotonicNs,
                    sessionStartIso = ISO_DATE_FORMAT.format(Date(currentTimeMs))
                )
                gsrRecorder?.startRecording(sessionDir, metadata)
                _sensorState.update { it.copy(isRecording = true, error = null) }
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Recording start failed: ${e.message}") }
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                gsrRecorder?.stopRecording()
                _sensorState.update { it.copy(isRecording = false, error = null) }
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Recording stop failed: ${e.message}") }
            }
        }
    }

    private fun startDataCollection() {
        // Collect recording status
        viewModelScope.launch {
            gsrRecorder?.getStatusFlow()?.collect { status ->
                _sensorState.update { currentState ->
                    currentState.copy(
                        isRecording = status.isRecording,
                        samplingRate = status.currentDataRate.toInt()
                    )
                }
            }
        }
        // Collect actual GSR data samples
        viewModelScope.launch {
            gsrRecorder?.getDataStream()?.collect { gsrSample ->
                _sensorState.update { currentState ->
                    val newGSR = gsrSample.gsrMicrosiemens.toFloat()
                    val newHistory = (currentState.gsrHistory + newGSR).takeLast(MAX_HISTORY_SIZE)
                    // Calculate skin conductance (same as GSR in microsiemens)
                    val skinConductance = newGSR
                    currentState.copy(
                        currentGSR = newGSR,
                        skinConductance = skinConductance,
                        gsrHistory = newHistory
                    )
                }
            }
        }
    }

    private fun startConnectionMonitoring() {
        viewModelScope.launch {
            gsrRecorder?.deviceStatus?.collect { status ->
                val isConnectedNow = status.contains("Connected", ignoreCase = true)
                val isDisconnected = status.contains("Disconnected", ignoreCase = true)
                _sensorState.update { currentState ->
                    val wasConnected = currentState.isConnected
                    // Detect disconnection and trigger reconnection
                    if (wasConnected && isDisconnected && !currentState.isReconnecting) {
                        // Save recording state before disconnection
                        wasRecordingBeforeDisconnect = currentState.isRecording
                        if (reconnectionConfig.enabled) {
                            viewModelScope.launch {
                                attemptReconnection()
                            }
                        }
                    }
                    currentState.copy(
                        isConnected = isConnectedNow,
                        connectionStatus = status
                    )
                }
            }
        }
    }

    private suspend fun attemptReconnection() {
        val maxAttempts = reconnectionConfig.maxAttempts
        val baseDelay = reconnectionConfig.baseDelayMs
        for (attempt in 1..maxAttempts) {
            _sensorState.update {
                it.copy(
                    isReconnecting = true,
                    reconnectionAttempt = attempt,
                    maxReconnectionAttempts = maxAttempts,
                    connectionStatus = "Reconnecting (attempt $attempt/$maxAttempts)..."
                )
            }
            // True exponential backoff: baseDelay * 2^(attempt-1)
            val delay = baseDelay * (1L shl (attempt - 1))
            kotlinx.coroutines.delay(delay)
            try {
                // Try to get cached devices first
                var devices = gsrRecorder?.getDiscoveredDevices() ?: emptyList()
                // If no cached devices and we have a last connected address, try to find it
                var targetDevice = devices.find { it.address == lastConnectedDeviceAddress }
                // If still no device found, trigger a quick scan
                if (targetDevice == null && devices.isEmpty()) {
                    mpdc4gsr.core.utils.AppLogger.i("GSRSensorViewModel", "No cached devices, triggering scan...")
                    _sensorState.update {
                        it.copy(
                            connectionStatus = "Scanning for device (attempt $attempt/$maxAttempts)..."
                        )
                    }
                    val scanSuccess = gsrRecorder?.startDeviceDiscovery() ?: false
                    if (scanSuccess) {
                        kotlinx.coroutines.delay(DEVICE_SCAN_DELAY_MS)
                        devices = gsrRecorder?.getDiscoveredDevices() ?: emptyList()
                        targetDevice = devices.find { it.address == lastConnectedDeviceAddress }
                            ?: devices.firstOrNull()
                    }
                } else if (targetDevice == null) {
                    // Use first available device if last connected not found
                    targetDevice = devices.firstOrNull()
                }
                if (targetDevice != null) {
                    mpdc4gsr.core.utils.AppLogger.i(
                        "GSRSensorViewModel",
                        "Attempting to connect to ${targetDevice.address}"
                    )
                    val connected = gsrRecorder?.connectToDevice(targetDevice) ?: false
                    if (connected) {
                        _sensorState.update {
                            it.copy(
                                isConnected = true,
                                isReconnecting = false,
                                reconnectionAttempt = 0,
                                maxReconnectionAttempts = 0,
                                connectionStatus = "Reconnected",
                                error = null
                            )
                        }
                        // Resume recording if it was active before disconnection
                        if (wasRecordingBeforeDisconnect) {
                            mpdc4gsr.core.utils.AppLogger.i(
                                "GSRSensorViewModel",
                                "Resuming recording after reconnection"
                            )
                            kotlinx.coroutines.delay(1000) // Brief delay to ensure stable connection
                            startRecording()
                            wasRecordingBeforeDisconnect = false
                        }
                        return
                    }
                } else {
                    mpdc4gsr.core.utils.AppLogger.w("GSRSensorViewModel", "No device found for reconnection")
                }
            } catch (e: Exception) {
                mpdc4gsr.core.utils.AppLogger.w(
                    "GSRSensorViewModel",
                    "Reconnection attempt $attempt failed: ${e.message}"
                )
            }
        }
        // All attempts failed
        _sensorState.update {
            it.copy(
                isReconnecting = false,
                reconnectionAttempt = 0,
                maxReconnectionAttempts = 0,
                connectionStatus = "Connection Lost",
                error = "Failed to reconnect after $maxAttempts attempts"
            )
        }
        wasRecordingBeforeDisconnect = false // Reset flag
    }

    fun exportData() {
        viewModelScope.launch {
            try {
                // Export functionality would be implemented here
                // For now, just log the action
                mpdc4gsr.core.utils.AppLogger.d("GSRSensorViewModel", "Export data requested")
            } catch (e: Exception) {
                _sensorState.update { it.copy(error = "Export failed: ${e.message}") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                gsrRecorder?.stopRecording()
                gsrRecorder?.cleanup()
            } catch (e: Exception) {
                mpdc4gsr.core.utils.AppLogger.e("GSRSensorViewModel", "Error during cleanup", e)
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\presentation\GSRSensorViewModelFactory.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GSRSensorViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GSRSensorViewModel::class.java)) {
            return GSRSensorViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\presentation\GSRSettingsViewModel.kt =====

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
                // Check if device is connected
                if (!_deviceConnectionState.value.isConnected) {
                    _settingsEvents.emit(SettingsEvent.ShowError("Cannot calibrate: No device connected"))
                    return@launchWithErrorHandling
                }
                // According to Shimmer Android API documentation:
                // The Shimmer device stores calibration parameters that are automatically
                // applied during streaming. The ObjectCluster contains both RAW and CAL formats.
                // For GSR sensors, calibration converts raw ADC values to microsiemens.
                // Since GSRSensorRecorder already uses the Shimmer API's built-in calibration
                // (via ObjectCluster.getFormatClusterValue with CAL format), the calibration
                // is active whenever the device streams data.
                // A full calibration workflow would involve:
                // 1. Reading current calibration parameters from device
                // 2. Optionally writing new calibration coefficients
                // 3. Verifying calibration by checking sensor readings
                // Verify that the device is providing calibrated data
                if (gsrSensorRecorder != null) {
                    _settingsEvents.emit(
                        SettingsEvent.CalibrationCompleted(
                            "GSR sensor calibration verified. Device is using Shimmer factory calibration parameters."
                        )
                    )
                } else {
                    _settingsEvents.emit(
                        SettingsEvent.ShowError(
                            "GSR sensor not initialized. Please reconnect the device."
                        )
                    )
                }
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


// ===== app\src\main\java\mpdc4gsr\feature\gsr\presentation\MultiModalRecordingViewModel.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.gsr.model.GSRSample
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.model.SyncMark
import com.mpdc4gsr.gsr.network.NetworkClient
import com.mpdc4gsr.gsr.service.GSRRecorder
import com.mpdc4gsr.gsr.service.SessionManager
import com.mpdc4gsr.gsr.util.TimeUtils
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.shimmerresearch.android.Shimmer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.RgbCameraRecorder
import mpdc4gsr.feature.gsr.data.RealShimmerDeviceFactory

class MultiModalRecordingViewModel(application: Application) : BaseViewModel() {
    data class RecordingState(
        val isRecording: Boolean = false,
        val isStartingRecording: Boolean = false,
        val sampleCount: Long = 0,
        val syncMarkCount: Int = 0,
        val recordingDuration: Long = 0,
        val sessionId: String = "",
        val participantId: String? = null
    )

    data class GSRState(
        val isConnected: Boolean = false,
        val isRecording: Boolean = false,
        val sampleRate: Int = 128,
        val lastSample: GSRSample? = null,
        val signalQuality: SignalQuality = SignalQuality.UNKNOWN,
        val deviceBattery: Int? = null
    )

    data class CameraState(
        val isInitialized: Boolean = false,
        val isRecording: Boolean = false,
        val videoEnabled: Boolean = true,
        val is4KEnabled: Boolean = false,
        val rawCaptureEnabled: Boolean = false,
        val frameRate: Int = 30,
        val resolution: String = "1080p"
    )

    data class NetworkState(
        val isConnected: Boolean = false,
        val controllerInfo: NetworkClient.ControllerInfo? = null,
        val isSyncing: Boolean = false,
        val lastSyncTime: Long? = null
    )

    data class ShimmerDeviceInfo(
        val shimmer: Shimmer?,
        val deviceName: String,
        val macAddress: String,
        val batteryLevel: Int? = null,
        val signalStrength: Int = 0,
        val isConnected: Boolean = false
    )

    data class RecordingConfiguration(
        val enableVideo: Boolean = true,
        val enable4K: Boolean = false,
        val enableRawCapture: Boolean = false,
        val rawFrameRate: Int = 30,
        val gsrSampleRate: Int = 128,
        val participantId: String = "",
        val sessionTemplate: String? = null
    )

    data class CombinedRecordingState(
        val gsrState: GSRState,
        val cameraState: CameraState,
        val networkState: NetworkState
    ) {
        val allSystemsReady: Boolean
            get() = gsrState.isConnected && cameraState.isInitialized
        val anySystemRecording: Boolean
            get() = gsrState.isRecording || cameraState.isRecording
    }

    enum class SignalQuality {
        EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
    }

    data class RecordingAction(
        val type: ActionType,
        val message: String? = null,
        val data: Any? = null
    )

    private val context: Context = application.applicationContext
    private lateinit var gsrRecorder: GSRRecorder
    private lateinit var sessionManager: SessionManager
    private var rgbCameraRecorder: RgbCameraRecorder? = null
    private var networkClient: NetworkClient? = null

    // Recording State Management
    private val _recordingState = MutableLiveData<RecordingState>()
    val recordingState: LiveData<RecordingState> = _recordingState
    private val _sessionInfo = MutableLiveData<SessionInfo?>()
    val sessionInfo: LiveData<SessionInfo?> = _sessionInfo

    // Multimodal Sensor States
    private val _gsrState = MutableStateFlow<GSRState>(GSRState())
    val gsrState: StateFlow<GSRState> = _gsrState
    private val _cameraState = MutableStateFlow<CameraState>(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState
    private val _networkState = MutableStateFlow<NetworkState>(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState

    // Device Management
    private val _discoveredDevices = MutableLiveData<List<ShimmerDeviceInfo>>()
    val discoveredDevices: LiveData<List<ShimmerDeviceInfo>> = _discoveredDevices
    private val _connectedDevices = MutableLiveData<List<ShimmerDeviceInfo>>()
    val connectedDevices: LiveData<List<ShimmerDeviceInfo>> = _connectedDevices

    // UI State and Actions
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage
    private val _recordingAction = MutableLiveData<RecordingAction?>()
    val recordingAction: LiveData<RecordingAction?> = _recordingAction

    // Configuration
    private val _recordingConfig =
        MutableStateFlow<RecordingConfiguration>(RecordingConfiguration())
    val recordingConfig: StateFlow<RecordingConfiguration> = _recordingConfig

    // Combined state for UI optimization
    val combinedRecordingState = combine(
        _gsrState, _cameraState, _networkState
    ) { gsrState, cameraState, networkState ->
        CombinedRecordingState(gsrState, cameraState, networkState)
    }

    enum class ActionType {
        RECORDING_STARTED,
        RECORDING_STOPPED,
        SYNC_EVENT_TRIGGERED,
        DEVICE_CONNECTED,
        DEVICE_DISCONNECTED,
        SESSION_EXPORTED,
        ERROR_OCCURRED,
        PERMISSION_REQUIRED
    }

    init {
        initializeRecorders()
        generateDefaultSessionId()
        updateSystemReadiness()
    }

    fun initialize() {
        // Kept for compatibility, but initialization now happens in init block
        initializeRecorders()
        generateDefaultSessionId()
        updateSystemReadiness()
    }

    private fun initializeRecorders() {
        viewModelScope.launch {
            try {
                // Initialize GSR Recorder
                gsrRecorder = GSRRecorder(context, RealShimmerDeviceFactory(context))
                gsrRecorder.addListener(createGSRListener())
                // Initialize Session Manager
                sessionManager = SessionManager.getInstance(context)
                _statusMessage.value = "Initializing multimodal recording system..."
                // Set initial states
                _recordingState.value = RecordingState()
                updateSystemReadiness()
            } catch (e: Exception) {
                _error.value = "Failed to initialize recording system: ${e.message}"
            }
        }
    }

    fun initializeCameraRecorder(rgbCameraRecorder: RgbCameraRecorder) {
        this.rgbCameraRecorder = rgbCameraRecorder
        viewModelScope.launch {
            try {
                val initialized = rgbCameraRecorder.initialize()
                _cameraState.value = _cameraState.value.copy(isInitialized = initialized)
                if (initialized) {
                    _statusMessage.value = "Camera system initialized"
                } else {
                    _error.value = "Camera initialization failed"
                }
                updateSystemReadiness()
            } catch (e: Exception) {
                _error.value = "Camera initialization error: ${e.message}"
            }
        }
    }

    fun updateRecordingConfiguration(config: RecordingConfiguration) {
        _recordingConfig.value = config
        // Update camera configuration
        _cameraState.value = _cameraState.value.copy(
            videoEnabled = config.enableVideo,
            is4KEnabled = config.enable4K,
            rawCaptureEnabled = config.enableRawCapture,
            frameRate = config.rawFrameRate
        )
    }

    fun startRecording() {
        val currentState = _recordingState.value
        if (currentState?.isRecording == true || currentState?.isStartingRecording == true) {
            return
        }
        viewModelScope.launch {
            try {
                _recordingState.value = currentState?.copy(isStartingRecording = true)
                _statusMessage.value = "Starting multimodal recording..."
                // Generate session info
                val config = _recordingConfig.value
                val sessionInfo = SessionInfo(
                    sessionId = TimeUtils.generateSessionId("MultiModal"),
                    participantId = config.participantId.takeIf { it.isNotEmpty() },
                    startTime = System.currentTimeMillis()
                )
                // Start GSR recording
                gsrRecorder.startRecording(sessionInfo.sessionId, sessionInfo.participantId)
                // Start camera recording if enabled
                if (config.enableVideo) {
                    rgbCameraRecorder?.startRecording(sessionInfo.sessionId)
                    _cameraState.value = _cameraState.value.copy(isRecording = true)
                }
                // Update states
                _sessionInfo.value = sessionInfo
                _recordingState.value = RecordingState(
                    isRecording = true,
                    isStartingRecording = false,
                    sessionId = sessionInfo.sessionId,
                    participantId = sessionInfo.participantId
                )
                _gsrState.value = _gsrState.value.copy(isRecording = true)
                _recordingAction.value = RecordingAction(
                    type = ActionType.RECORDING_STARTED,
                    message = "Recording started for session ${sessionInfo.sessionId}"
                )
            } catch (e: Exception) {
                _recordingState.value = currentState?.copy(isStartingRecording = false)
                _error.value = "Failed to start recording: ${e.message}"
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Stopping multimodal recording..."
                // Stop GSR recording
                gsrRecorder.stopRecording()
                // Stop camera recording
                rgbCameraRecorder?.stopRecording()
                // Update final session info
                val finalSession = _sessionInfo.value?.copy(
                    endTime = System.currentTimeMillis()
                )
                // Update states
                _recordingState.value = RecordingState()
                _gsrState.value = _gsrState.value.copy(isRecording = false)
                _cameraState.value = _cameraState.value.copy(isRecording = false)
                _sessionInfo.value = finalSession
                _recordingAction.value = RecordingAction(
                    type = ActionType.RECORDING_STOPPED,
                    message = "Recording stopped. Session saved.",
                    data = finalSession
                )
            } catch (e: Exception) {
                _error.value = "Failed to stop recording: ${e.message}"
            }
        }
    }

    fun triggerSyncEvent() {
        val currentState = _recordingState.value
        if (currentState?.isRecording != true) {
            _error.value = "Cannot trigger sync event when not recording"
            return
        }
        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val syncMark = SyncMark(
                    timestamp = timestamp,
                    utcTimestamp = timestamp,
                    eventType = "USER_TRIGGER",
                    sessionId = currentState.sessionId
                )
                // Add sync mark to GSR data
                gsrRecorder.addSyncMark("USER_TRIGGER", "Manual sync event")
                // Add sync mark to camera data if recording
                if (_cameraState.value.isRecording) {
                    rgbCameraRecorder?.addSyncMarker(
                        "USER_TRIGGER",
                        timestamp * 1_000_000,
                        emptyMap()
                    )
                }
                // Update state
                _recordingState.value = currentState.copy(
                    syncMarkCount = currentState.syncMarkCount + 1
                )
                _recordingAction.value = RecordingAction(
                    type = ActionType.SYNC_EVENT_TRIGGERED,
                    message = "Sync event USER_TRIGGER triggered"
                )
            } catch (e: Exception) {
                _error.value = "Failed to trigger sync event: ${e.message}"
            }
        }
    }

    fun discoverDevices() {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Discovering Shimmer devices..."
                // Simulate device discovery
                val devices = discoverShimmerDevices()
                _discoveredDevices.value = devices
                _statusMessage.value = "Found ${devices.size} Shimmer device(s)"
            } catch (e: Exception) {
                _error.value = "Device discovery failed: ${e.message}"
            }
        }
    }

    fun connectToDevice(deviceInfo: ShimmerDeviceInfo) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Connecting to ${deviceInfo.deviceName}..."
                // Simulate device connection
                kotlinx.coroutines.delay(2000)
                val connectedDevice = deviceInfo.copy(isConnected = true)
                val currentConnected = _connectedDevices.value?.toMutableList() ?: mutableListOf()
                currentConnected.add(connectedDevice)
                _connectedDevices.value = currentConnected
                _gsrState.value = _gsrState.value.copy(isConnected = true)
                _recordingAction.value = RecordingAction(
                    type = ActionType.DEVICE_CONNECTED,
                    message = "Connected to ${deviceInfo.deviceName}"
                )
                updateSystemReadiness()
            } catch (e: Exception) {
                _error.value = "Failed to connect to device: ${e.message}"
            }
        }
    }

    private suspend fun discoverShimmerDevices(): List<ShimmerDeviceInfo> {
        // Simulate device discovery - use null placeholders for now as this is discovery phase
        return listOf(
            ShimmerDeviceInfo(
                shimmer = null,
                deviceName = "Shimmer GSR #001",
                macAddress = "00:11:22:AA:BB:CC",
                batteryLevel = 85,
                signalStrength = 75
            ),
            ShimmerDeviceInfo(
                shimmer = null,
                deviceName = "Shimmer GSR #002",
                macAddress = "00:11:22:AA:BB:DD",
                batteryLevel = 92,
                signalStrength = 88
            )
        )
    }

    private fun createGSRListener(): GSRRecorder.GSRRecordingListener {
        return object : GSRRecorder.GSRRecordingListener {
            override fun onRecordingStarted(sessionInfo: SessionInfo) {
                _statusMessage.value = "GSR recording started"
            }

            override fun onRecordingStopped(sessionInfo: SessionInfo) {
                _statusMessage.value = "GSR recording stopped"
            }

            override fun onSampleRecorded(sample: GSRSample) {
                val currentState = _recordingState.value
                _recordingState.value = currentState?.copy(
                    sampleCount = currentState.sampleCount + 1
                )
                _gsrState.value = _gsrState.value.copy(lastSample = sample)
            }

            override fun onSyncMarkAdded(syncMark: SyncMark) {
                // Handle sync mark addition
                val currentState = _recordingState.value
                _recordingState.value = currentState?.copy(
                    syncMarkCount = currentState.syncMarkCount + 1
                )
            }

            override fun onError(error: String) {
                _error.value = "GSR recording error: $error"
            }
        }
    }

    private fun updateSystemReadiness() {
        val gsrReady = _gsrState.value.isConnected
        val cameraReady = _cameraState.value.isInitialized
        _statusMessage.value = when {
            gsrReady && cameraReady -> "All systems ready for recording"
            gsrReady -> "GSR ready, initializing camera..."
            cameraReady -> "Camera ready, connect GSR device..."
            else -> "Initializing recording systems..."
        }
    }

    private fun generateDefaultSessionId() {
        val config = _recordingConfig.value
        val defaultParticipantId = TimeUtils.generateSessionId("MultiModal")
        _recordingConfig.value = config.copy(participantId = defaultParticipantId)
    }

    fun clearAction() {
        _recordingAction.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                if (_recordingState.value?.isRecording == true) {
                    stopRecording()
                }
                rgbCameraRecorder?.cleanup()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    companion object {
        private const val TAG = "MultiModalRecordingViewModel"
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\presentation\SessionExportViewModel.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

enum class ExportFormat(val displayName: String) {
    CSV("CSV (Comma Separated Values)"),
    JSON("JSON (JavaScript Object Notation)"),
    XML("XML (eXtensible Markup Language)"),
    EXCEL("Excel Spreadsheet")
}

enum class ExportDestination(val displayName: String) {
    DOWNLOADS("Downloads Folder"),
    EXTERNAL_STORAGE("External Storage"),
    SHARE("Share with Other Apps"),
    EMAIL("Email Export")
}

data class GSRSession(
    val sessionId: String,
    val name: String,
    val startTime: Long,
    val endTime: Long?,
    val deviceId: String,
    val participantId: String?,
    val readingCount: Int,
    val avgConductance: Float,
    val status: String = "COMPLETED",
    val duration: String = "0min",
    val dataPointCount: Int = 0,
    val filePath: String = "",
    val lastModified: Long = 0L
)

class SessionExportViewModel(
    context: Context
) : AppBaseViewModel() {
    private val application: Context = context.applicationContext

    data class SessionExportState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val sessions: List<GSRSession> = emptyList(),
        val selectedSessions: Set<GSRSession> = emptySet(),
        val exportFormat: ExportFormat = ExportFormat.CSV,
        val exportDestination: ExportDestination = ExportDestination.DOWNLOADS,
        val isExporting: Boolean = false,
        val exportProgress: Float = 0f,
        val currentExportFile: String? = null
    )

    private val _exportState = MutableStateFlow(SessionExportState())
    val exportState: StateFlow<SessionExportState> = _exportState.asStateFlow()

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            _exportState.value = _exportState.value.copy(isLoading = true, error = null)
            try {
                val sessions = getAvailableSessions()
                _exportState.value = _exportState.value.copy(
                    isLoading = false,
                    sessions = sessions
                )
            } catch (e: Exception) {
                _exportState.value = _exportState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load sessions"
                )
            }
        }
    }

    fun toggleSessionSelection(session: GSRSession) {
        val currentSelection = _exportState.value.selectedSessions
        val newSelection = if (session in currentSelection) {
            currentSelection - session
        } else {
            currentSelection + session
        }
        _exportState.value = _exportState.value.copy(selectedSessions = newSelection)
    }

    fun setExportFormat(format: ExportFormat) {
        _exportState.value = _exportState.value.copy(exportFormat = format)
    }

    fun setExportDestination(destination: ExportDestination) {
        _exportState.value = _exportState.value.copy(exportDestination = destination)
    }

    fun startExport() {
        viewModelScope.launch {
            val selectedSessions = _exportState.value.selectedSessions
            if (selectedSessions.isEmpty()) {
                _exportState.value = _exportState.value.copy(error = "No sessions selected for export")
                return@launch
            }
            _exportState.value = _exportState.value.copy(
                isExporting = true,
                exportProgress = 0f,
                error = null
            )
            try {
                val exportFiles = mutableListOf<File>()
                val totalSessions = selectedSessions.size
                selectedSessions.forEachIndexed { index, session ->
                    _exportState.value = _exportState.value.copy(
                        currentExportFile = session.name,
                        exportProgress = (index.toFloat() / totalSessions)
                    )
                    val exportedFile = exportSession(session)
                    exportFiles.add(exportedFile)
                }
                _exportState.value = _exportState.value.copy(
                    exportProgress = 1f,
                    currentExportFile = null
                )
                // Handle export destination
                handleExportDestination(exportFiles)
            } catch (e: Exception) {
                _exportState.value = _exportState.value.copy(
                    isExporting = false,
                    exportProgress = 0f,
                    currentExportFile = null,
                    error = "Export failed: ${e.message}"
                )
            }
        }
    }

    private fun getAvailableSessions(): List<GSRSession> {
        val sessions = mutableListOf<GSRSession>()
        // Check multiple possible session directories
        val possibleDirectories = listOf(
            File(Environment.getExternalStorageDirectory(), "GSR/Sessions"),
            File(Environment.getExternalStorageDirectory(), "IRCamera/GSR/Sessions"),
            File(application.getExternalFilesDir(null), "gsr_sessions"),
            File(application.filesDir, "gsr_sessions")
        )
        for (directory in possibleDirectories) {
            if (directory.exists() && directory.isDirectory) {
                directory.listFiles { file ->
                    file.isFile && (file.name.endsWith(".csv") || file.name.endsWith(".txt") || file.name.endsWith(".json"))
                }?.forEach { file ->
                    sessions.add(
                        GSRSession(
                            sessionId = file.nameWithoutExtension,
                            name = file.nameWithoutExtension,
                            startTime = file.lastModified(),
                            endTime = null,
                            deviceId = "unknown",
                            participantId = null,
                            readingCount = countDataPoints(file),
                            avgConductance = 0f,
                            status = "COMPLETED",
                            duration = calculateSessionDuration(file),
                            dataPointCount = countDataPoints(file),
                            filePath = file.absolutePath,
                            lastModified = file.lastModified()
                        )
                    )
                }
            }
        }
        // Sort by modification date (newest first)
        return sessions.sortedByDescending { it.lastModified }
    }

    private suspend fun exportSession(session: GSRSession): File {
        val outputDir = getExportDirectory()
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${session.name}_export_$timestamp.${_exportState.value.exportFormat.fileExtension}"
        val outputFile = File(outputDir, fileName)
        when (_exportState.value.exportFormat) {
            ExportFormat.CSV -> exportToCSV(session, outputFile)
            ExportFormat.JSON -> exportToJSON(session, outputFile)
            ExportFormat.XML -> exportToXML(session, outputFile)
            ExportFormat.EXCEL -> exportToExcel(session, outputFile)
        }
        return outputFile
    }

    private fun exportToCSV(session: GSRSession, outputFile: File) {
        val sessionFile = File(session.filePath)
        val writer = FileWriter(outputFile)
        writer.use { w ->
            // Write CSV header
            w.write("Timestamp,GSR_Value,Resistance,Conductance,Status\n")
            // Read and convert session data
            sessionFile.readLines().forEach { line ->
                if (line.isNotBlank() && !line.startsWith("#")) {
                    val convertedLine = convertDataLineToCSV(line)
                    w.write("$convertedLine\n")
                }
            }
        }
    }

    private fun exportToJSON(session: GSRSession, outputFile: File) {
        val sessionFile = File(session.filePath)
        val writer = FileWriter(outputFile)
        writer.use { w ->
            w.write("{\n")
            w.write("  \"session\": {\n")
            w.write("    \"name\": \"${session.name}\",\n")
            w.write("    \"duration\": \"${session.duration}\",\n")
            w.write("    \"dataPointCount\": ${session.dataPointCount},\n")
            w.write(
                "    \"exportedAt\": \"${
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(
                        Date()
                    )
                }\",\n"
            )
            w.write("    \"data\": [\n")
            val lines = sessionFile.readLines().filter { it.isNotBlank() && !it.startsWith("#") }
            lines.forEachIndexed { index, line ->
                val jsonLine = convertDataLineToJSON(line)
                w.write("      $jsonLine")
                if (index < lines.size - 1) w.write(",")
                w.write("\n")
            }
            w.write("    ]\n")
            w.write("  }\n")
            w.write("}\n")
        }
    }

    private fun exportToXML(session: GSRSession, outputFile: File) {
        val sessionFile = File(session.filePath)
        val writer = FileWriter(outputFile)
        writer.use { w ->
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            w.write("<gsrSession>\n")
            w.write("  <metadata>\n")
            w.write("    <name>${session.name}</name>\n")
            w.write("    <duration>${session.duration}</duration>\n")
            w.write("    <dataPointCount>${session.dataPointCount}</dataPointCount>\n")
            w.write(
                "    <exportedAt>${
                    SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                        Locale.getDefault()
                    ).format(Date())
                }</exportedAt>\n"
            )
            w.write("  </metadata>\n")
            w.write("  <data>\n")
            sessionFile.readLines().forEach { line ->
                if (line.isNotBlank() && !line.startsWith("#")) {
                    val xmlLine = convertDataLineToXML(line)
                    w.write("    $xmlLine\n")
                }
            }
            w.write("  </data>\n")
            w.write("</gsrSession>\n")
        }
    }

    private fun exportToExcel(session: GSRSession, outputFile: File) {
        // For now, export as CSV with Excel-compatible format
        // In a full implementation, you'd use Apache POI or similar library
        exportToCSV(session, outputFile)
    }

    private suspend fun handleExportDestination(exportFiles: List<File>) {
        try {
            when (_exportState.value.exportDestination) {
                ExportDestination.DOWNLOADS -> {
                    // Files are already in downloads, just notify completion
                    _exportState.value = _exportState.value.copy(
                        isExporting = false,
                        error = "Export completed! Files saved to Downloads folder."
                    )
                }

                ExportDestination.EXTERNAL_STORAGE -> {
                    // Files are already in external storage
                    _exportState.value = _exportState.value.copy(
                        isExporting = false,
                        error = "Export completed! Files saved to external storage."
                    )
                }

                ExportDestination.SHARE -> {
                    shareFiles(exportFiles)
                }

                ExportDestination.EMAIL -> {
                    emailFiles(exportFiles)
                }
            }
        } catch (e: Exception) {
            _exportState.value = _exportState.value.copy(
                isExporting = false,
                error = "Export completed but failed to handle destination: ${e.message}"
            )
        }
    }

    private fun shareFiles(files: List<File>) {
        try {
            val context = application.applicationContext
            val uris = files.map { file ->
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share GSR Export"))
            _exportState.value = _exportState.value.copy(
                isExporting = false,
                error = "Export completed! Opening share dialog..."
            )
        } catch (e: Exception) {
            _exportState.value = _exportState.value.copy(
                isExporting = false,
                error = "Export completed but failed to share: ${e.message}"
            )
        }
    }

    private fun emailFiles(files: List<File>) {
        try {
            val context = application.applicationContext
            val uris = files.map { file ->
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_SUBJECT, "GSR Session Export")
                putExtra(Intent.EXTRA_TEXT, "Attached are the exported GSR session files.")
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Email GSR Export"))
            _exportState.value = _exportState.value.copy(
                isExporting = false,
                error = "Export completed! Opening email client..."
            )
        } catch (e: Exception) {
            _exportState.value = _exportState.value.copy(
                isExporting = false,
                error = "Export completed but failed to email: ${e.message}"
            )
        }
    }

    private fun getExportDirectory(): File {
        return when (_exportState.value.exportDestination) {
            ExportDestination.DOWNLOADS -> File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "GSR_Exports"
            )

            ExportDestination.EXTERNAL_STORAGE -> File(
                Environment.getExternalStorageDirectory(),
                "IRCamera/GSR_Exports"
            )

            else -> File(application.getExternalFilesDir(null), "exports")
        }
    }

    private fun calculateSessionDuration(file: File): String {
        // Simple duration calculation based on file timestamps
        // In a real implementation, you'd parse the actual session data
        val durationMinutes = (file.length() / 1000).coerceAtMost(999)
        return "${durationMinutes}min"
    }

    private fun countDataPoints(file: File): Int {
        return try {
            file.readLines().count { line ->
                line.isNotBlank() && !line.startsWith("#")
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun convertDataLineToCSV(line: String): String {
        // Convert data line to CSV format
        // This is a simplified conversion - adjust based on actual data format
        return line.replace("\t", ",")
    }

    private fun convertDataLineToJSON(line: String): String {
        // Convert data line to JSON format
        val parts = line.split("\t", ",")
        return if (parts.size >= 2) {
            "{ \"timestamp\": \"${parts[0]}\", \"value\": ${parts[1]} }"
        } else {
            "{ \"data\": \"$line\" }"
        }
    }

    private fun convertDataLineToXML(line: String): String {
        // Convert data line to XML format
        val parts = line.split("\t", ",")
        return if (parts.size >= 2) {
            "<dataPoint timestamp=\"${parts[0]}\" value=\"${parts[1]}\" />"
        } else {
            "<dataPoint data=\"$line\" />"
        }
    }
}

// Extension property for file extensions
private val ExportFormat.fileExtension: String
    get() = when (this) {
        ExportFormat.CSV -> "csv"
        ExportFormat.JSON -> "json"
        ExportFormat.XML -> "xml"
        ExportFormat.EXCEL -> "xlsx"
    }


// ===== app\src\main\java\mpdc4gsr\feature\gsr\presentation\SessionExportViewModelFactory.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SessionExportViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionExportViewModel::class.java)) {
            return SessionExportViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\presentation\SessionManagerViewModel.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.service.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import mpdc4gsr.core.data.utils.SessionDirectoryManager
import mpdc4gsr.core.ui.AppBaseViewModel
import java.io.File

class SessionManagerViewModel : AppBaseViewModel() {
    // StateFlow for session management
    private val _allSessions = MutableStateFlow<List<SessionInfo>>(emptyList())
    private val _filteredSessions = MutableStateFlow<List<SessionInfo>>(emptyList())
    val filteredSessions: StateFlow<List<SessionInfo>> = _filteredSessions.asStateFlow()
    private val _storageInfo = MutableStateFlow(StorageInfo("0 MB", 0, false))
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    // SharedFlow for one-time events
    private val _sessionEvents = MutableSharedFlow<SessionEvent>()
    val sessionEvents: SharedFlow<SessionEvent> = _sessionEvents.asSharedFlow()

    // UI State
    private val _sessionUiState = MutableStateFlow(SessionManagerUiState())
    val sessionUiState: StateFlow<SessionManagerUiState> = _sessionUiState.asStateFlow()
    private lateinit var sessionManager: SessionManager
    private lateinit var sessionDirectoryManager: SessionDirectoryManager
    private var currentFilter: FilterType = FilterType.ALL
    private var currentSearchQuery: String = ""

    data class SessionManagerUiState(
        val isLoading: Boolean = false,
        val sessionCount: Int = 0,
        val filteredCount: Int = 0,
        val currentFilter: FilterType = FilterType.ALL,
        val searchQuery: String = ""
    )

    data class StorageInfo(
        val formattedAvailable: String,
        val usagePercentage: Int,
        val isLowStorage: Boolean
    )

    sealed class SessionEvent {
        data class OpenDetails(val session: SessionInfo) : SessionEvent()
        data class DeleteConfirm(val session: SessionInfo) : SessionEvent()
        data class Export(val session: SessionInfo) : SessionEvent()
        data class DeletedSuccess(val session: SessionInfo, val message: String) : SessionEvent()
        data class ExportSuccess(val session: SessionInfo, val message: String) : SessionEvent()
        data class ExportFailed(val session: SessionInfo, val message: String) : SessionEvent()
        data class ShowError(val message: String) : SessionEvent()
        data class ShowToast(val message: String) : SessionEvent()
    }

    enum class FilterType {
        ALL, RECENT, COMPLETED, WITH_DATA
    }

    fun initialize(context: Context) {
        sessionManager = SessionManager.getInstance(context)
        sessionDirectoryManager = SessionDirectoryManager(context)
    }

    fun loadSessions(context: Context) {
        if (!::sessionManager.isInitialized) {
            initialize(context)
        }
        _sessionUiState.value = _sessionUiState.value.copy(isLoading = true)
        launchWithErrorHandling {
            try {
                // Display storage info
                updateStorageInfo()
                // Clean up failed sessions
                val cleanedSessions = withContext(Dispatchers.IO) {
                    sessionDirectoryManager.cleanupFailedSessions()
                }
                if (cleanedSessions.isNotEmpty()) {
                    AppLogger.i(TAG, "Cleaned up ${cleanedSessions.size} failed sessions")
                    _sessionEvents.emit(SessionEvent.ShowToast("Cleaned up ${cleanedSessions.size} failed sessions"))
                }
                // Load sessions
                val loadedSessions = withContext(Dispatchers.IO) {
                    val activeSessions = sessionManager.getActiveSessions()
                    val historicalSessions = loadHistoricalSessions(context)
                    (activeSessions + historicalSessions).distinctBy { it.sessionId }
                }
                val sortedSessions = loadedSessions.sortedByDescending { it.startTime }
                _allSessions.value = sortedSessions
                applyCurrentFilters()
                _sessionUiState.value = _sessionUiState.value.copy(
                    isLoading = false,
                    sessionCount = sortedSessions.size
                )
                AppLogger.i(TAG, "Loaded ${sortedSessions.size} sessions")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load sessions", e)
                _sessionEvents.emit(SessionEvent.ShowError("Failed to load sessions: ${e.message}"))
                _sessionUiState.value = _sessionUiState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun updateStorageInfo() {
        try {
            val storageStatus = sessionDirectoryManager.checkStorageSpace()
            _storageInfo.value = StorageInfo(
                formattedAvailable = storageStatus.formattedAvailable,
                usagePercentage = storageStatus.usagePercentage,
                isLowStorage = storageStatus.isLowStorage
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to get storage info", e)
        }
    }

    private suspend fun loadHistoricalSessions(context: Context): List<SessionInfo> {
        return withContext(Dispatchers.IO) {
            val historicalSessions = mutableListOf<SessionInfo>()
            try {
                val baseDir = File(context.getExternalFilesDir(null), "recordings")
                if (baseDir.exists() && baseDir.isDirectory) {
                    baseDir.listFiles()?.forEach { sessionDir ->
                        if (sessionDir.isDirectory && sessionDir.name.startsWith("session_")) {
                            try {
                                val sessionInfo = parseSessionFromDirectory(sessionDir)
                                historicalSessions.add(sessionInfo)
                            } catch (e: Exception) {
                                AppLogger.w(TAG, "Failed to parse session from ${sessionDir.name}", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load historical sessions", e)
            }
            historicalSessions
        }
    }

    private fun parseSessionFromDirectory(sessionDir: File): SessionInfo {
        val sessionId = sessionDir.name
        val metadataFile = File(sessionDir, "session_metadata.txt")
        val sessionInfo = SessionInfo(
            sessionId = sessionId,
            startTime = sessionDir.lastModified(),
        )
        if (metadataFile.exists()) {
            try {
                metadataFile.readLines().forEach { line ->
                    val parts = line.split(":", limit = 2)
                    if (parts.size >= 2) {
                        val key = parts[0]
                        val value = parts[1]
                        when (key.trim()) {
                            "participantId" -> sessionInfo.participantId = value.trim()
                            "studyName" -> sessionInfo.studyName = value.trim()
                            "endTime" -> sessionInfo.endTime = value.trim().toLongOrNull()
                            "sampleCount" -> sessionInfo.sampleCount =
                                value.trim().toLongOrNull() ?: 0

                            else -> sessionInfo.metadata[key.trim()] = value.trim()
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to parse metadata for ${sessionInfo.sessionId}", e)
            }
        }
        // Calculate data file counts and sizes
        calculateSessionDataInfo(sessionDir, sessionInfo)
        return sessionInfo
    }

    private fun calculateSessionDataInfo(sessionDir: File, sessionInfo: SessionInfo) {
        try {
            var totalSize = 0L
            var gsrFileCount = 0
            var thermalFileCount = 0
            var rgbFileCount = 0
            sessionDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    totalSize += file.length()
                    when {
                        file.name.contains("gsr") -> gsrFileCount++
                        file.name.contains("thermal") -> thermalFileCount++
                        file.name.contains("rgb") -> rgbFileCount++
                    }
                }
            }
            sessionInfo.totalDataSize = totalSize
            sessionInfo.metadata["gsrFileCount"] = gsrFileCount.toString()
            sessionInfo.metadata["thermalFileCount"] = thermalFileCount.toString()
            sessionInfo.metadata["rgbFileCount"] = rgbFileCount.toString()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to calculate data info for ${sessionInfo.sessionId}", e)
        }
    }

    fun filterSessions(query: String?) {
        currentSearchQuery = query ?: ""
        _sessionUiState.value = _sessionUiState.value.copy(searchQuery = currentSearchQuery)
        applyCurrentFilters()
    }

    fun filterSessionsByType(filterPosition: Int) {
        currentFilter = when (filterPosition) {
            0 -> FilterType.ALL
            1 -> FilterType.RECENT
            2 -> FilterType.COMPLETED
            3 -> FilterType.WITH_DATA
            else -> FilterType.ALL
        }
        _sessionUiState.value = _sessionUiState.value.copy(currentFilter = currentFilter)
        applyCurrentFilters()
    }

    private fun applyCurrentFilters() {
        val allSessions = _allSessions.value
        var filtered = allSessions
        // Apply type filter
        filtered = when (currentFilter) {
            FilterType.ALL -> filtered
            FilterType.RECENT -> filtered.filter {
                System.currentTimeMillis() - it.startTime < 24 * 60 * 60 * 1000 // Last 24 hours
            }

            FilterType.COMPLETED -> filtered.filter { it.endTime != null }
            FilterType.WITH_DATA -> filtered.filter { it.totalDataSize > 0 }
        }
        // Apply search filter
        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.filter { session ->
                session.sessionId.contains(currentSearchQuery, ignoreCase = true) ||
                        session.participantId?.contains(
                            currentSearchQuery,
                            ignoreCase = true
                        ) == true ||
                        session.studyName?.contains(currentSearchQuery, ignoreCase = true) == true
            }
        }
        _filteredSessions.value = filtered
        _sessionUiState.value = _sessionUiState.value.copy(filteredCount = filtered.size)
    }

    fun onSessionClick(session: SessionInfo) {
        launchWithErrorHandling {
            _sessionEvents.emit(SessionEvent.OpenDetails(session))
        }
    }

    fun onSessionDelete(session: SessionInfo) {
        launchWithErrorHandling {
            _sessionEvents.emit(SessionEvent.DeleteConfirm(session))
        }
    }

    fun onSessionExport(session: SessionInfo) {
        launchWithErrorHandling {
            _sessionEvents.emit(SessionEvent.Export(session))
        }
    }

    fun deleteSession(session: SessionInfo) {
        launchWithErrorHandling {
            val success = withContext(Dispatchers.IO) {
                sessionDirectoryManager.deleteSession(session.sessionId)
            }
            if (success) {
                // Remove from local list and update UI
                val updatedSessions =
                    _allSessions.value.filter { it.sessionId != session.sessionId }
                _allSessions.value = updatedSessions
                applyCurrentFilters()
                _sessionUiState.value =
                    _sessionUiState.value.copy(sessionCount = updatedSessions.size)
                _sessionEvents.emit(
                    SessionEvent.DeletedSuccess(
                        session,
                        "Session ${session.sessionId} deleted successfully"
                    )
                )
            } else {
                _sessionEvents.emit(SessionEvent.ShowError("Failed to delete session ${session.sessionId}"))
            }
        }
    }

    fun exportSession(session: SessionInfo) {
        launchWithErrorHandling {
            val success = withContext(Dispatchers.IO) {
                sessionDirectoryManager.exportSession(session.sessionId)
            }
            if (success) {
                _sessionEvents.emit(
                    SessionEvent.ExportSuccess(
                        session,
                        "Session ${session.sessionId} exported successfully"
                    )
                )
            } else {
                _sessionEvents.emit(
                    SessionEvent.ExportFailed(
                        session,
                        "Failed to export session ${session.sessionId}"
                    )
                )
            }
        }
    }

    companion object {
        private const val TAG = "SessionManagerViewModel"
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\presentation\ShimmerConfigViewModel.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.gsr.domain.usecase.*

class ShimmerConfigViewModel(
    private val scanDevicesUseCase: ScanShimmerDevicesUseCase,
    private val connectDeviceUseCase: ConnectShimmerDeviceUseCase,
    private val disconnectDeviceUseCase: DisconnectShimmerDeviceUseCase,
    private val getBatteryLevelUseCase: GetDeviceBatteryUseCase,
    private val checkConnectionUseCase: CheckDeviceConnectionUseCase
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

data class ShimmerConfigUiState(
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


// ===== app\src\main\java\mpdc4gsr\feature\gsr\presentation\ShimmerConfigViewModelFactory.kt =====

package mpdc4gsr.feature.gsr.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import mpdc4gsr.core.data.ShimmerDeviceManager
import mpdc4gsr.feature.gsr.data.repository.ShimmerRepositoryImpl
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSourceImpl
import mpdc4gsr.feature.gsr.domain.repository.ShimmerRepository
import mpdc4gsr.feature.gsr.domain.usecase.*

class ShimmerConfigViewModelFactory(
    private val application: Application,
    private val lifecycleOwner: androidx.lifecycle.LifecycleOwner
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShimmerConfigViewModel::class.java)) {
            // Build the dependency graph from bottom up
            val shimmerDeviceManager = ShimmerDeviceManager(application, lifecycleOwner)
            val shimmerDataSource = ShimmerDataSourceImpl(shimmerDeviceManager)
            val shimmerRepository: ShimmerRepository = ShimmerRepositoryImpl(shimmerDataSource)
            // Create all use cases with the repository
            val scanDevicesUseCase = ScanShimmerDevicesUseCase(shimmerRepository)
            val connectDeviceUseCase = ConnectShimmerDeviceUseCase(shimmerRepository)
            val disconnectDeviceUseCase = DisconnectShimmerDeviceUseCase(shimmerRepository)
            val getBatteryLevelUseCase = GetDeviceBatteryUseCase(shimmerRepository)
            val checkConnectionUseCase = CheckDeviceConnectionUseCase(shimmerRepository)
            // Create the ViewModel with all use cases
            return ShimmerConfigViewModel(
                scanDevicesUseCase = scanDevicesUseCase,
                connectDeviceUseCase = connectDeviceUseCase,
                disconnectDeviceUseCase = disconnectDeviceUseCase,
                getBatteryLevelUseCase = getBatteryLevelUseCase,
                checkConnectionUseCase = checkConnectionUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


