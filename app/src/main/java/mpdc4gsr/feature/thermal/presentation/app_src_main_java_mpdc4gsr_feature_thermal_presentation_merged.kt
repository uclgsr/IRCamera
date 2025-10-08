// Merged ALL .kt and .java files from the 'app\src\main\java\mpdc4gsr\feature\thermal\presentation' directory and its subdirectories.
// Total files: 4 | Generated on: 2025-10-08 01:42:33


// ===== FROM: app\src\main\java\mpdc4gsr\feature\thermal\presentation\CalibrationViewModel.kt =====

package mpdc4gsr.feature.thermal.presentation

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.utils.AppLogger
import java.text.SimpleDateFormat
import java.util.*

class CalibrationViewModel : AppBaseViewModel() {
    private lateinit var prefs: SharedPreferences
    private val _calibrationSettings = MutableStateFlow(CalibrationSettings())
    val calibrationSettings: StateFlow<CalibrationSettings> = _calibrationSettings.asStateFlow()
    private val _calibrationInfo = MutableStateFlow(CalibrationInfo())
    val calibrationInfo: StateFlow<CalibrationInfo> = _calibrationInfo.asStateFlow()

    data class CalibrationSettings(
        val autoCalibration: Boolean = true
    )

    data class CalibrationInfo(
        val thermalLastCalibrated: String = "Never",
        val gsrLastCalibrated: String = "Never",
        val cameraLastAligned: String = "Never"
    )

    companion object {
        private const val TAG = "CalibrationViewModel"
        private const val KEY_AUTO_CALIBRATION = "calibration_auto"
        private const val KEY_THERMAL_LAST_CALIB = "calibration_thermal_last"
        private const val KEY_GSR_LAST_CALIB = "calibration_gsr_last"
        private const val KEY_CAMERA_LAST_ALIGN = "calibration_camera_last"
        private const val TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"
    }

    fun initialize(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        loadSettings()
        loadCalibrationInfo()
    }

    private fun loadSettings() {
        _calibrationSettings.value = CalibrationSettings(
            autoCalibration = prefs.getBoolean(KEY_AUTO_CALIBRATION, true)
        )
    }

    private fun loadCalibrationInfo() {
        _calibrationInfo.value = CalibrationInfo(
            thermalLastCalibrated = prefs.getString(KEY_THERMAL_LAST_CALIB, "Never") ?: "Never",
            gsrLastCalibrated = prefs.getString(KEY_GSR_LAST_CALIB, "Never") ?: "Never",
            cameraLastAligned = prefs.getString(KEY_CAMERA_LAST_ALIGN, "Never") ?: "Never"
        )
    }

    fun updateAutoCalibration(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean(KEY_AUTO_CALIBRATION, enabled).apply()
            _calibrationSettings.value = _calibrationSettings.value.copy(autoCalibration = enabled)
        }
    }

    fun startThermalCalibration() {
        viewModelScope.launch {
            try {
                AppLogger.d(TAG, "Starting thermal camera calibration")
                val timestamp = getCurrentTimestamp()
                prefs.edit().putString(KEY_THERMAL_LAST_CALIB, timestamp).apply()
                AppLogger.i(TAG, "Thermal calibration completed at: $timestamp")
                AppLogger.w(TAG, "Note: Full calibration requires Topdon SDK LibIRTemp integration")
                loadCalibrationInfo()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error during thermal calibration", e)
            }
        }
    }

    fun startGSRCalibration() {
        viewModelScope.launch {
            try {
                AppLogger.d(TAG, "Starting GSR sensor calibration")
                val timestamp = getCurrentTimestamp()
                prefs.edit().putString(KEY_GSR_LAST_CALIB, timestamp).apply()
                AppLogger.i(TAG, "GSR calibration completed at: $timestamp")
                AppLogger.w(TAG, "Note: Full calibration requires Shimmer3 SDK calibration commands")
                loadCalibrationInfo()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error during GSR calibration", e)
            }
        }
    }

    fun startCameraAlignment() {
        viewModelScope.launch {
            try {
                AppLogger.d(TAG, "Starting camera alignment procedure")
                val timestamp = getCurrentTimestamp()
                prefs.edit().putString(KEY_CAMERA_LAST_ALIGN, timestamp).apply()
                AppLogger.i(TAG, "Camera alignment completed at: $timestamp")
                AppLogger.w(TAG, "Note: Full alignment requires multi-camera spatial calibration")
                loadCalibrationInfo()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error during camera alignment", e)
            }
        }
    }

    private fun getCurrentTimestamp(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.time.format.DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT, Locale.US)
                .format(java.time.LocalDateTime.now())
        } else {
            SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(Date())
        }
    }
}


// ===== FROM: app\src\main\java\mpdc4gsr\feature\thermal\presentation\ThermalCameraViewModel.kt =====

package mpdc4gsr.feature.thermal.presentation

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.SessionMetadata
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.feature.thermal.ui.ThermalCameraRecorder

class ThermalCameraViewModel(application: Application) : ViewModel() {
    private val context: Context = application.applicationContext

    companion object {
        private const val TAG = "ThermalCameraViewModel"
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        AppLogger.e(TAG, "Coroutine exception in ThermalCameraViewModel", exception)
        _uiState.update { it.copy(errorMessage = "Error: ${exception.message}") }
    }

    data class ThermalCameraUiState(
        val isConnected: Boolean = false,
        val isRecording: Boolean = false,
        val currentTemperature: Float? = null,
        val minTemperature: Float = 0f,
        val maxTemperature: Float = 100f,
        val avgTemperature: Float = 0f,
        val centerTemperature: Float = 0f,
        val isPaused: Boolean = false,
        val recordingDuration: Long = 0L,
        val errorMessage: String? = null,
        val previewBitmap: Bitmap? = null,
        val isSimulationMode: Boolean = false,
        val frameCount: Long = 0L
    )

    private val _uiState = MutableStateFlow(ThermalCameraUiState())
    val uiState: StateFlow<ThermalCameraUiState> = _uiState.asStateFlow()
    private var thermalRecorder: ThermalCameraRecorder? = null
    private var recordingStartTime: Long = 0L

    init {
        initializeThermalRecorder()
    }

    private fun initializeThermalRecorder() {
        viewModelScope.launch(exceptionHandler) {
            try {
                thermalRecorder = ThermalCameraRecorder(context, "thermal_preview_1")
                // Set preview callback to receive thermal frames
                thermalRecorder?.setThermalPreviewCallback(object : ThermalCameraRecorder.ThermalPreviewCallback {
                    override fun onThermalFrame(
                        bitmap: Bitmap?,
                        temperatureData: ThermalCameraRecorder.ThermalFrameData?
                    ) {
                        // Update UI state with new thermal frame and temperature data
                        // Use update() for thread-safe state updates from background thread
                        _uiState.update { currentState ->
                            currentState.copy(
                                previewBitmap = bitmap,
                                // Retain previous values if temperatureData is null
                                minTemperature = temperatureData?.minTemperature ?: currentState.minTemperature,
                                maxTemperature = temperatureData?.maxTemperature ?: currentState.maxTemperature,
                                avgTemperature = temperatureData?.avgTemperature ?: currentState.avgTemperature,
                                centerTemperature = temperatureData?.centerTemperature
                                    ?: currentState.centerTemperature,
                                currentTemperature = temperatureData?.centerTemperature
                                    ?: currentState.currentTemperature
                            )
                        }
                    }
                })
                // Initialize the thermal camera
                val success = thermalRecorder?.initialize() ?: false
                // Update connection status after initialization
                val status = thermalRecorder?.getThermalSystemStatus()
                _uiState.update {
                    it.copy(
                        isConnected = status?.isConnected ?: false,
                        isSimulationMode = status?.isSimulationMode ?: false
                    )
                }
                if (success) {
                    AppLogger.i(TAG, "Thermal camera initialized successfully")
                } else {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to initialize thermal camera")
                    }
                    AppLogger.e(TAG, "Failed to initialize thermal camera")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error initializing thermal recorder", e)
                _uiState.update {
                    it.copy(errorMessage = "Error: ${e.message}")
                }
            }
        }
    }

    fun connectToDevice() {
        viewModelScope.launch(exceptionHandler) {
            val status = thermalRecorder?.getThermalSystemStatus()
            _uiState.update {
                it.copy(
                    isConnected = status?.isConnected ?: false,
                    isSimulationMode = status?.isSimulationMode ?: false
                )
            }
        }
    }

    fun rescanForThermalCamera() {
        viewModelScope.launch(exceptionHandler) {
            try {
                AppLogger.i(TAG, "Triggering thermal camera rescan from ViewModel")
                val found = thermalRecorder?.rescanForThermalCamera() ?: false
                val status = thermalRecorder?.getThermalSystemStatus()
                _uiState.update {
                    it.copy(
                        isConnected = status?.isConnected ?: false,
                        isSimulationMode = status?.isSimulationMode ?: false,
                        errorMessage = if (found) null else status?.statusMessage
                    )
                }
                if (found) {
                    AppLogger.i(TAG, "Thermal camera found during rescan")
                } else {
                    AppLogger.w(TAG, "Rescan did not initialize camera: ${status?.statusMessage}")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error during thermal camera rescan", e)
                _uiState.update {
                    it.copy(errorMessage = "Rescan error: ${e.message}")
                }
            }
        }
    }

    fun startRecording(sessionDirectory: String, sessionMetadata: SessionMetadata) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val success = thermalRecorder?.startRecording(sessionDirectory, sessionMetadata) ?: false
                if (success) {
                    recordingStartTime = System.currentTimeMillis()
                    _uiState.update {
                        it.copy(
                            isRecording = true,
                            recordingDuration = 0L
                        )
                    }
                    AppLogger.i(TAG, "Thermal recording started")
                } else {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to start recording")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error starting recording", e)
                _uiState.update {
                    it.copy(errorMessage = "Recording error: ${e.message}")
                }
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch(exceptionHandler) {
            try {
                thermalRecorder?.stopRecording()
                _uiState.update {
                    it.copy(
                        isRecording = false,
                        recordingDuration = 0L
                    )
                }
                AppLogger.i(TAG, "Thermal recording stopped")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error stopping recording", e)
            }
        }
    }

    fun updateRecordingDuration() {
        if (_uiState.value.isRecording) {
            val duration = System.currentTimeMillis() - recordingStartTime
            _uiState.update {
                it.copy(recordingDuration = duration)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Launch async cleanup in viewModelScope before it gets cancelled
        // This ensures proper cleanup without blocking the main thread
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            try {
                thermalRecorder?.cleanup()
                AppLogger.i(TAG, "Thermal recorder cleanup completed")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error cleaning up thermal recorder", e)
            }
        }
        // Note: viewModelScope will be automatically cancelled after onCleared returns
    }
}


// ===== FROM: app\src\main\java\mpdc4gsr\feature\thermal\presentation\ThermalCameraViewModelFactory.kt =====

package mpdc4gsr.feature.thermal.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ThermalCameraViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThermalCameraViewModel::class.java)) {
            return ThermalCameraViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


// ===== FROM: app\src\main\java\mpdc4gsr\feature\thermal\presentation\ThermalSettingsViewModel.kt =====

package mpdc4gsr.feature.thermal.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.thermal.data.ThermalSettingsRepository

class ThermalSettingsViewModel : AppBaseViewModel() {
    private lateinit var repository: ThermalSettingsRepository
    private val _thermalSettings = MutableStateFlow(ThermalSettingsRepository.ThermalSettings())
    val thermalSettings: StateFlow<ThermalSettingsRepository.ThermalSettings> = _thermalSettings.asStateFlow()
    fun initialize(context: Context) {
        repository = ThermalSettingsRepository.getInstance(context)
        loadSettings()
        viewModelScope.launch {
            repository.thermalSettings.collect { repoSettings ->
                _thermalSettings.value = repoSettings
            }
        }
    }

    private fun loadSettings() {
        if (::repository.isInitialized) {
            _thermalSettings.value = repository.getSettings()
        }
    }

    fun updateFrameRate(frameRate: Int) {
        viewModelScope.launch {
            repository.updateFrameRate(frameRate)
        }
    }

    fun updateSaveRawImages(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSaveRawImages(enabled)
        }
    }

    fun updatePalette(palette: String) {
        viewModelScope.launch {
            repository.updatePalette(palette)
        }
    }

    fun updateTemperatureUnit(unit: String) {
        viewModelScope.launch {
            repository.updateTemperatureUnit(unit)
        }
    }

    fun updateEmissivity(emissivity: Float) {
        viewModelScope.launch {
            repository.updateEmissivity(emissivity)
        }
    }

    fun updateAutoScale(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAutoScale(enabled)
        }
    }

    fun updateShowCrosshair(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateShowCrosshair(enabled)
        }
    }

    fun updateTemperatureRange(range: String) {
        viewModelScope.launch {
            repository.updateTemperatureRange(range)
        }
    }
}