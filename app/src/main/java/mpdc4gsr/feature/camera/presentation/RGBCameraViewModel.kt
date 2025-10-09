package mpdc4gsr.feature.camera.presentation

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.RgbCameraRecorder
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.camera.data.CameraConfigurationManager
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
        val resolution: String = "1920×1080",
        val frameRate: Int = 30,
        val exposureTime: String = "1/60",
        val iso: Int = 200,
        val focusMode: FocusMode = FocusMode.AUTO,
        val whiteBalance: WhiteBalance = WhiteBalance.AUTO,
        val recordingDuration: Int = 0,
        val capturedFrames: Int = 0,
        val error: String? = null,
        val cameraChangeCounter: Int = 0,
        val supports4K: Boolean = false,
        val supportsRaw: Boolean = false,
        val supports60Fps: Boolean = false,
        val lensFacingDescription: String = "Back Lens",
        val hardwareLevelDescription: String = "Level Unknown",
        val availableResolutions: List<String> = emptyList(),
        val capabilities: CameraConfigurationManager.DeviceCapabilities? = null
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
                    updateStateFromRecorder(recorder) { state ->
                        state.copy(
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
                val started = recorder.startRecording(sessionDir, metadata)
                if (started) {
                    updateStateFromRecorder(recorder) { state ->
                        state.copy(isRecording = true, recordingDuration = 0, capturedFrames = 0, error = null)
                    }
                    trackRecordingDuration()
                } else {
                    _cameraState.update { it.copy(error = "Failed to start recording") }
                }
            } catch (e: Exception) {
                _cameraState.update { it.copy(error = "Recording start failed: ${e.message}") }
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                val recorder = _cameraRecorder.value
                if (recorder == null) {
                    _cameraState.update { it.copy(error = "Camera not initialized") }
                    return@launch
                }
                val stopped = recorder.stopRecording()
                updateStateFromRecorder(recorder) { state ->
                    state.copy(isRecording = false, error = null)
                }
                if (!stopped) {
                    _cameraState.update { it.copy(error = "Failed to stop recording") }
                }
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
                    updateStateFromRecorder(recorder) { state ->
                        state.copy(
                            error = null,
                            cameraChangeCounter = state.cameraChangeCounter + 1,
                            resolution = recorder.getResolution(),
                            frameRate = recorder.getCurrentFps()
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

    private fun updateStateFromRecorder(
        recorder: RgbCameraRecorder,
        transform: (CameraState) -> CameraState
    ) {
        val capabilities = recorder.getCurrentCapabilities()
        _cameraState.update { current ->
            val nextState = transform(current)
            enrichWithCapabilities(nextState, capabilities)
        }
    }

    private fun enrichWithCapabilities(
        state: CameraState,
        capabilities: CameraConfigurationManager.DeviceCapabilities?
    ): CameraState {
        val resolved = capabilities ?: state.capabilities
        val lensDescription = resolved?.let { describeLensFacing(it.lensFacing) } ?: state.lensFacingDescription
        val hardwareDescription = resolved?.let { describeHardwareLevel(it.hardwareLevel) }
            ?: state.hardwareLevelDescription
        val resolutions = resolved?.supportedVideoSizes?.map { size -> "${size.width}x${size.height}" }
            ?: state.availableResolutions
        return state.copy(
            capabilities = resolved,
            supports4K = resolved?.supports4K == true,
            supportsRaw = resolved?.supportsRaw == true,
            supports60Fps = resolved?.supports60Fps == true,
            lensFacingDescription = lensDescription,
            hardwareLevelDescription = hardwareDescription,
            availableResolutions = resolutions
        )
    }

    private fun describeLensFacing(lensFacing: Int?): String {
        return when (lensFacing) {
            CameraCharacteristics.LENS_FACING_FRONT -> "Front Lens"
            CameraCharacteristics.LENS_FACING_BACK -> "Back Lens"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "External Lens"
            else -> "Unknown Lens"
        }
    }

    private fun describeHardwareLevel(level: Int?): String {
        return when (level) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "Level 3"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "Level Full"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "Level Limited"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "Level Legacy"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "Level External"
            else -> "Level Unknown"
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
