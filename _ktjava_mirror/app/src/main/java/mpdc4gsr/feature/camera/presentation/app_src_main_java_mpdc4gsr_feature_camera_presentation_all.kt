// Merged .kt under 'app\src\main\java\mpdc4gsr\feature\camera\presentation' subtree
// Files: 4; Generated 2025-10-07 23:07:39


// ===== app\src\main\java\mpdc4gsr\feature\camera\presentation\DualModeCameraViewModel.kt =====

package mpdc4gsr.feature.camera.presentation

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.RgbCameraRecorder
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.camera.data.SamsungDeviceCompatibility

class DualModeCameraViewModel : AppBaseViewModel() {
    // Enhanced data classes
    data class CameraState(
        val isInitialized: Boolean = false,
        val isRecording: Boolean = false,
        val deviceInfo: String = "",
        val supportedModes: List<CameraMode> = emptyList(),
        val currentResolution: String = "",
        val frameRate: Int = 0
    )

    data class RecordingState(
        val isRecording: Boolean = false,
        val recordingDuration: Long = 0L,
        val recordedFileCount: Int = 0,
        val currentFileSize: Long = 0L,
        val totalRecordedSize: Long = 0L
    )

    data class CameraScreenState(
        val canRecord: Boolean = false,
        val canSwitchMode: Boolean = false,
        val showPermissionRequest: Boolean = false,
        val showProgress: Boolean = false,
        val displayMessage: String = ""
    )

    // StateFlow for reactive state management
    private val _permissionState = MutableStateFlow(PermissionState.UNKNOWN)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()
    private val _cameraMode = MutableStateFlow(CameraMode.PREVIEW)
    val cameraMode: StateFlow<CameraMode> = _cameraMode.asStateFlow()
    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    // SharedFlow for one-time events
    private val _events = MutableSharedFlow<CameraEvent>()
    val events: SharedFlow<CameraEvent> = _events.asSharedFlow()

    // Combined state for complex UI scenarios
    private val _cameraScreenState = MutableStateFlow(CameraScreenState())
    val cameraScreenState: StateFlow<CameraScreenState> = _cameraScreenState.asStateFlow()
    private var rgbCameraRecorder: RgbCameraRecorder? = null
    private var enableSamsungOptimizations: Boolean = true
    private var appContext: Context? = null

    enum class PermissionState {
        UNKNOWN,
        GRANTED,
        DENIED,
        REQUESTING,
        PERMANENTLY_DENIED
    }

    enum class CameraMode {
        PREVIEW,
        RAW,
        VIDEO_4K,
        VIDEO_1080P,
        PHOTO_BURST,
        NIGHT_MODE
    }

    sealed class CameraEvent {
        data class ShowError(val message: String) : CameraEvent()
        data class ShowSuccess(val message: String) : CameraEvent()
        data class RequestPermission(val permissions: List<String>) : CameraEvent()
        data class RecordingStarted(val fileName: String) : CameraEvent()
        data class RecordingStopped(val filePath: String, val duration: Long) : CameraEvent()
        data class ModeChanged(val newMode: CameraMode) : CameraEvent()
        object NavigateToGallery : CameraEvent()
    }

    init {
        // Setup combined state management
        viewModelScope.launch {
            combine(
                _permissionState,
                _cameraState,
                _cameraMode,
                _recordingState
            ) { permission, camera, mode, recording ->
                CameraScreenState(
                    canRecord = permission == PermissionState.GRANTED && camera.isInitialized && !recording.isRecording,
                    canSwitchMode = permission == PermissionState.GRANTED && camera.isInitialized && !recording.isRecording,
                    showPermissionRequest = permission == PermissionState.UNKNOWN || permission == PermissionState.DENIED,
                    showProgress = recording.isRecording,
                    displayMessage = generateDisplayMessage(permission, camera, mode, recording)
                )
            }.collect { newState ->
                _cameraScreenState.value = newState
            }
        }
    }

    fun initialize(initialMode: String, enableOptimizations: Boolean) {
        launchWithErrorHandling {
            enableSamsungOptimizations = enableOptimizations
            val mode = when (initialMode) {
                "RAW_50MP" -> CameraMode.RAW
                "VIDEO_4K" -> CameraMode.VIDEO_4K
                "VIDEO_1080P" -> CameraMode.VIDEO_1080P
                "PHOTO_BURST" -> CameraMode.PHOTO_BURST
                "NIGHT_MODE" -> CameraMode.NIGHT_MODE
                else -> CameraMode.PREVIEW
            }
            _cameraMode.value = mode
            _permissionState.value = PermissionState.UNKNOWN
            val deviceInfo = SamsungDeviceCompatibility.getDeviceInfo()
            val supportedModes = getSupportedModes()
            _cameraState.value = CameraState(
                isInitialized = false,
                isRecording = false,
                deviceInfo = deviceInfo,
                supportedModes = supportedModes
            )
            _events.emit(CameraEvent.ShowSuccess("Camera system initialized with mode: ${mode.name}"))
        }
    }

    fun onPermissionGranted() {
        _permissionState.value = PermissionState.GRANTED
        viewModelScope.launch {
            _events.emit(CameraEvent.ShowSuccess("Camera permission granted"))
        }
    }

    fun onPermissionDenied(isPermanent: Boolean = false) {
        _permissionState.value =
            if (isPermanent) PermissionState.PERMANENTLY_DENIED else PermissionState.DENIED
        viewModelScope.launch {
            val message = if (isPermanent) {
                "Camera permission permanently denied. Please enable in settings."
            } else {
                "Camera permission required for dual-mode system"
            }
            _events.emit(CameraEvent.ShowError(message))
        }
    }

    fun requestPermission() {
        _permissionState.value = PermissionState.REQUESTING
        viewModelScope.launch {
            _events.emit(
                CameraEvent.RequestPermission(
                    listOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            )
        }
    }

    fun initializeCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        launchWithLoading {
            try {
                appContext = context.applicationContext
                rgbCameraRecorder = RgbCameraRecorder(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    useFrontCamera = false
                )
                rgbCameraRecorder?.initialize()
                _cameraState.value = _cameraState.value.copy(
                    isInitialized = true,
                    currentResolution = "1920x1080", // Default resolution
                    frameRate = 30
                )
                _events.emit(CameraEvent.ShowSuccess("Dual-mode camera system initialized"))
            } catch (e: Exception) {
                _events.emit(CameraEvent.ShowError("Failed to initialize camera: ${e.message}"))
            }
        }
    }

    fun switchCameraMode(newMode: CameraMode) {
        launchWithErrorHandling {
            if (!_cameraState.value.isInitialized) {
                _events.emit(CameraEvent.ShowError("Camera not initialized"))
                return@launchWithErrorHandling
            }
            if (_recordingState.value.isRecording) {
                _events.emit(CameraEvent.ShowError("Cannot switch mode while recording"))
                return@launchWithErrorHandling
            }
            val previousMode = _cameraMode.value
            _cameraMode.value = newMode
            when (newMode) {
                CameraMode.RAW -> {
                    handleRawModeSwitch()
                }

                CameraMode.VIDEO_4K -> {
                    _cameraState.value = _cameraState.value.copy(
                        currentResolution = "3840x2160",
                        frameRate = 30
                    )
                }

                CameraMode.VIDEO_1080P -> {
                    _cameraState.value = _cameraState.value.copy(
                        currentResolution = "1920x1080",
                        frameRate = 60
                    )
                }

                CameraMode.PREVIEW -> {
                    _cameraState.value = _cameraState.value.copy(
                        currentResolution = "1920x1080",
                        frameRate = 30
                    )
                }

                CameraMode.PHOTO_BURST -> {
                    _cameraState.value = _cameraState.value.copy(
                        currentResolution = "4000x3000",
                        frameRate = 0
                    )
                }

                CameraMode.NIGHT_MODE -> {
                    _cameraState.value = _cameraState.value.copy(
                        currentResolution = "1920x1080",
                        frameRate = 24
                    )
                }
            }
            _events.emit(CameraEvent.ModeChanged(newMode))
            _events.emit(CameraEvent.ShowSuccess("Switched from ${previousMode.name} to ${newMode.name}"))
        }
    }

    private suspend fun handleRawModeSwitch() {
        val message =
            if (enableSamsungOptimizations && SamsungDeviceCompatibility.isStage3Compatible()) {
                "RAW Mode: Samsung Stage3/Level3 DNG Enabled"
            } else {
                val deviceInfo = SamsungDeviceCompatibility.getDeviceInfo()
                "RAW Mode: Standard DNG ($deviceInfo)"
            }
        _cameraState.value = _cameraState.value.copy(
            currentResolution = "4000x3000",
            frameRate = 0
        )
        _events.emit(CameraEvent.ShowSuccess(message))
    }

    fun startRecording() {
        launchWithErrorHandling {
            if (!_cameraState.value.isInitialized) {
                _events.emit(CameraEvent.ShowError("Camera not initialized"))
                return@launchWithErrorHandling
            }
            if (_recordingState.value.isRecording) {
                _events.emit(CameraEvent.ShowError("Already recording"))
                return@launchWithErrorHandling
            }
            try {
                val fileName = "recording_${System.currentTimeMillis()}"
                val sessionDir = appContext?.getExternalFilesDir("recordings")?.absolutePath ?: ""
                rgbCameraRecorder?.startRecording(sessionDir)
                _recordingState.value = _recordingState.value.copy(
                    isRecording = true,
                    recordingDuration = 0L
                )
                _cameraState.value = _cameraState.value.copy(isRecording = true)
                _events.emit(CameraEvent.RecordingStarted(fileName))
                _events.emit(CameraEvent.ShowSuccess("Recording started"))
            } catch (e: Exception) {
                _events.emit(CameraEvent.ShowError("Failed to start recording: ${e.message}"))
            }
        }
    }

    fun stopRecording() {
        launchWithErrorHandling {
            if (!_recordingState.value.isRecording) {
                _events.emit(CameraEvent.ShowError("Not currently recording"))
                return@launchWithErrorHandling
            }
            try {
                val duration = _recordingState.value.recordingDuration
                rgbCameraRecorder?.stopRecording()
                _recordingState.value = _recordingState.value.copy(
                    isRecording = false,
                    recordedFileCount = _recordingState.value.recordedFileCount + 1
                )
                _cameraState.value = _cameraState.value.copy(isRecording = false)
                val filePath = "recording_path" // Would be actual path in real implementation
                _events.emit(CameraEvent.RecordingStopped(filePath, duration))
                _events.emit(CameraEvent.ShowSuccess("Recording stopped"))
            } catch (e: Exception) {
                _events.emit(CameraEvent.ShowError("Failed to stop recording: ${e.message}"))
            }
        }
    }

    fun navigateToGallery() {
        viewModelScope.launch {
            _events.emit(CameraEvent.NavigateToGallery)
        }
    }

    private fun getSupportedModes(): List<CameraMode> {
        return if (SamsungDeviceCompatibility.isStage3Compatible()) {
            CameraMode.values().toList()
        } else {
            listOf(CameraMode.PREVIEW, CameraMode.VIDEO_1080P, CameraMode.PHOTO_BURST)
        }
    }

    private fun generateDisplayMessage(
        permission: PermissionState,
        camera: CameraState,
        mode: CameraMode,
        recording: RecordingState
    ): String {
        return when {
            permission != PermissionState.GRANTED -> "Camera permission required"
            !camera.isInitialized -> "Initializing camera..."
            recording.isRecording -> "Recording... ${recording.recordingDuration}s"
            else -> "Mode: ${mode.name} | ${camera.currentResolution}@${camera.frameRate}fps"
        }
    }

    override fun onCleared() {
        viewModelScope.launch {
            rgbCameraRecorder?.cleanup()
        }
        super.onCleared()
    }

    companion object {
        private const val TAG = "DualModeCameraViewModel"
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\presentation\RGBCameraViewModel.kt =====

package mpdc4gsr.feature.camera.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.RgbCameraRecorder
import mpdc4gsr.core.ui.AppBaseViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class FocusMode(val displayName: String) {
    AUTO("Auto"),
    MANUAL("Manual"),
    CONTINUOUS("Continuous");

    fun getNext(): FocusMode {
        return when (this) {
            AUTO -> MANUAL
            MANUAL -> CONTINUOUS
            CONTINUOUS -> AUTO
        }
    }
}

enum class WhiteBalance(val displayName: String) {
    AUTO("Auto"),
    DAYLIGHT("Daylight"),
    CLOUDY("Cloudy"),
    TUNGSTEN("Tungsten");

    fun getNext(): WhiteBalance {
        return when (this) {
            AUTO -> DAYLIGHT
            DAYLIGHT -> CLOUDY
            CLOUDY -> TUNGSTEN
            TUNGSTEN -> AUTO
        }
    }
}

class RGBCameraViewModel(
    context: Context
) : AppBaseViewModel() {
    private val application: Context = context.applicationContext

    companion object {
        // Reuse SimpleDateFormat instance for better performance
        private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    data class CameraState(
        val isPreviewActive: Boolean = false,
        val isRecording: Boolean = false,
        val resolution: String = "1920Ã—1080",
        val frameRate: Int = 30,
        val exposureTime: String = "1/60",
        val iso: Int = 200,
        val focusMode: FocusMode = FocusMode.AUTO,
        val whiteBalance: WhiteBalance = WhiteBalance.AUTO,
        val recordingDuration: Int = 0,
        val capturedFrames: Int = 0,
        val error: String? = null,
        val cameraChangeCounter: Int = 0
    )

    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()
    private val _cameraRecorder = MutableStateFlow<RgbCameraRecorder?>(null)
    val cameraRecorder: StateFlow<RgbCameraRecorder?> = _cameraRecorder.asStateFlow()

    fun initializeCamera(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        viewModelScope.launch {
            try {
                val recorder = RgbCameraRecorder(
                    context = application,
                    lifecycleOwner = lifecycleOwner
                )
                val initialized = recorder.initialize()
                if (initialized) {
                    _cameraRecorder.value = recorder
                    _cameraState.update {
                        it.copy(
                            isPreviewActive = true,
                            resolution = recorder.getResolution(),
                            frameRate = recorder.getCurrentFps(),
                            error = null
                        )
                    }
                } else {
                    _cameraState.update { it.copy(error = "Failed to initialize camera") }
                }
            } catch (e: Exception) {
                _cameraState.update { it.copy(error = "Camera initialization error: ${e.message}") }
            }
        }
    }

    @Deprecated(
        message = "Use cameraRecorder StateFlow for reactive updates",
        replaceWith = ReplaceWith("cameraRecorder.value")
    )
    fun getCameraRecorder(): RgbCameraRecorder? = _cameraRecorder.value

    fun startRecording() {
        viewModelScope.launch {
            try {
                val recorder = _cameraRecorder.value
                if (recorder == null) {
                    _cameraState.update { it.copy(error = "Camera not initialized") }
                    return@launch
                }
                val sessionDir = application.getExternalFilesDir("camera_recordings")?.absolutePath
                    ?: application.filesDir.absolutePath
                val currentTimeMs = System.currentTimeMillis()
                val currentMonotonicNs = System.nanoTime()
                val metadata = mpdc4gsr.core.data.SessionMetadata(
                    sessionId = "camera_${currentTimeMs}",
                    sessionStartTimestampMs = currentTimeMs,
                    sessionStartMonotonicNs = currentMonotonicNs,
                    sessionStartIso = ISO_DATE_FORMAT.format(java.util.Date(currentTimeMs))
                )
                recorder.startRecording(sessionDir, metadata)
                _cameraState.update { it.copy(isRecording = true, recordingDuration = 0, error = null) }
                // Start duration tracking
                trackRecordingDuration()
            } catch (e: Exception) {
                _cameraState.update { it.copy(error = "Recording start failed: ${e.message}") }
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                _cameraRecorder.value?.stopRecording()
                _cameraState.update { it.copy(isRecording = false, error = null) }
            } catch (e: Exception) {
                _cameraState.update { it.copy(error = "Recording stop failed: ${e.message}") }
            }
        }
    }

    fun togglePreview() {
        _cameraState.update { it.copy(isPreviewActive = !it.isPreviewActive) }
    }

    fun capturePhoto() {
        viewModelScope.launch {
            try {
                // Photo capture functionality would be implemented here
                android.util.Log.d("RGBCameraViewModel", "Photo capture requested")
            } catch (e: Exception) {
                _cameraState.update { it.copy(error = "Photo capture failed: ${e.message}") }
            }
        }
    }

    fun switchCamera() {
        viewModelScope.launch {
            try {
                val recorder = _cameraRecorder.value
                if (recorder == null) {
                    _cameraState.update { it.copy(error = "Camera not initialized") }
                    return@launch
                }
                val cameraInfo = recorder.getCurrentCameraInfo()
                if (!cameraInfo.canSwitch) {
                    val reason = when {
                        _cameraState.value.isRecording -> "Cannot switch camera during recording"
                        !cameraInfo.frontAvailable && !cameraInfo.backAvailable -> "No cameras available"
                        cameraInfo.isUsingFrontCamera && !cameraInfo.backAvailable -> "Back camera not available"
                        !cameraInfo.isUsingFrontCamera && !cameraInfo.frontAvailable -> "Front camera not available"
                        else -> "Camera switch not available"
                    }
                    _cameraState.update { it.copy(error = reason) }
                    return@launch
                }
                val success = if (cameraInfo.isUsingFrontCamera) {
                    recorder.switchToBackCamera()
                } else {
                    recorder.switchToFrontCamera()
                }
                if (success) {
                    // Increment counter to trigger preview rebind in UI
                    _cameraState.update {
                        it.copy(
                            error = null,
                            cameraChangeCounter = it.cameraChangeCounter + 1
                        )
                    }
                } else {
                    _cameraState.update { it.copy(error = "Failed to switch camera") }
                }
            } catch (e: Exception) {
                _cameraState.update { it.copy(error = "An unexpected error occurred while switching cameras.") }
            }
        }
    }

    fun updateResolution(resolution: String) {
        _cameraState.update { it.copy(resolution = resolution) }
    }

    fun updateFrameRate(frameRate: Int) {
        _cameraState.update { it.copy(frameRate = frameRate) }
    }

    fun updateFocusMode(focusMode: FocusMode) {
        _cameraState.update { it.copy(focusMode = focusMode) }
    }

    fun updateWhiteBalance(whiteBalance: WhiteBalance) {
        _cameraState.update { it.copy(whiteBalance = whiteBalance) }
    }

    fun updateExposureTime(exposureTime: String) {
        _cameraState.update { it.copy(exposureTime = exposureTime) }
    }

    fun updateISO(iso: Int) {
        _cameraState.update { it.copy(iso = iso) }
    }

    fun dismissError() {
        _cameraState.update { it.copy(error = null) }
    }

    fun reinitializeCamera(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        viewModelScope.launch {
            try {
                // Clean up existing camera first
                val recorder = _cameraRecorder.value
                if (recorder != null) {
                    recorder.cleanup()
                    _cameraRecorder.value = null
                }
                // Reinitialize - cleanup() is a suspend function that completes before continuing
                initializeCamera(lifecycleOwner)
                // Increment counter to trigger UI updates
                _cameraState.update {
                    it.copy(cameraChangeCounter = it.cameraChangeCounter + 1)
                }
            } catch (e: Exception) {
                _cameraState.update { it.copy(error = "Failed to reinitialize camera: ${e.message}") }
            }
        }
    }

    private fun trackRecordingDuration() {
        viewModelScope.launch {
            while (_cameraState.value.isRecording) {
                kotlinx.coroutines.delay(1000)
                _cameraState.update {
                    it.copy(
                        recordingDuration = it.recordingDuration + 1,
                        capturedFrames = it.capturedFrames + it.frameRate
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                _cameraRecorder.value?.stopRecording()
                _cameraRecorder.value?.cleanup()
            } catch (e: Exception) {
                android.util.Log.e("RGBCameraViewModel", "Error during cleanup", e)
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\presentation\RGBCameraViewModelFactory.kt =====

package mpdc4gsr.feature.camera.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RGBCameraViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RGBCameraViewModel::class.java)) {
            return RGBCameraViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\camera\presentation\TimeLapseCameraViewModel.kt =====

package mpdc4gsr.feature.camera.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel

enum class TimeLapseMode(val displayName: String) {
    MANUAL("Manual Interval"),
    AUTO("Auto Optimize"),
    PRESET_FAST("Fast (1s)"),
    PRESET_MEDIUM("Medium (5s)"),
    PRESET_SLOW("Slow (10s)")
}

class TimeLapseCameraViewModel(
    @Suppress("UNUSED_PARAMETER") context: Context
) : AppBaseViewModel() {
    companion object {
        private const val DEFAULT_PLAYBACK_FPS = 30
    }

    data class TimeLapseState(
        val isRecording: Boolean = false,
        val capturedFrames: Int = 0,
        val intervalSeconds: Int = 5,
        val mode: TimeLapseMode = TimeLapseMode.PRESET_MEDIUM,
        val totalDuration: Int = 0,
        val estimatedVideoLength: Int = 0,
        val lastCaptureTime: Long = 0L,
        val error: String? = null,
        val resolution: String = "1920Ã—1080",
        val quality: Int = 90
    )

    private val _timeLapseState = MutableStateFlow(TimeLapseState())
    val timeLapseState: StateFlow<TimeLapseState> = _timeLapseState.asStateFlow()
    fun startTimeLapse() {
        launchWithErrorHandling {
            _timeLapseState.value = _timeLapseState.value.copy(
                isRecording = true,
                capturedFrames = 0,
                totalDuration = 0,
                error = null
            )
        }
    }

    fun stopTimeLapse() {
        launchWithErrorHandling {
            _timeLapseState.value = _timeLapseState.value.copy(
                isRecording = false
            )
        }
    }

    fun updateInterval(seconds: Int) {
        _timeLapseState.value = _timeLapseState.value.copy(
            intervalSeconds = seconds.coerceIn(1, 60)
        )
    }

    fun setMode(mode: TimeLapseMode) {
        val interval = when (mode) {
            TimeLapseMode.PRESET_FAST -> 1
            TimeLapseMode.PRESET_MEDIUM -> 5
            TimeLapseMode.PRESET_SLOW -> 10
            else -> _timeLapseState.value.intervalSeconds
        }
        _timeLapseState.value = _timeLapseState.value.copy(
            mode = mode,
            intervalSeconds = interval
        )
    }

    fun captureFrame() {
        launchWithErrorHandling {
            val current = _timeLapseState.value
            _timeLapseState.value = current.copy(
                capturedFrames = current.capturedFrames + 1,
                lastCaptureTime = System.currentTimeMillis(),
                estimatedVideoLength = (current.capturedFrames + 1) / DEFAULT_PLAYBACK_FPS
            )
        }
    }
}

class TimeLapseCameraViewModelFactory(
    private val context: Context
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimeLapseCameraViewModel::class.java)) {
            return TimeLapseCameraViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


