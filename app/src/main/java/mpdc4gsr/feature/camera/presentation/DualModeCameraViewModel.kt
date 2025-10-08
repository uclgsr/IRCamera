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
            }
        }
    }

    fun stopRecording() {
        launchWithErrorHandling {
            if (!_recordingState.value.isRecording) {
                _events.emit(CameraEvent.ShowError("Not currently recording"))
                return@launchWithErrorHandling
            }
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
    }
}
