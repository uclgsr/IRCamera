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

/**
 * Focus mode options for camera
 */
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

/**
 * White balance options for camera
 */
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

/**
 * ViewModel for RGB Camera Screen
 * Manages RgbCameraRecorder lifecycle and camera controls
 */
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
        val error: String? = null
    )

    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private var cameraRecorder: RgbCameraRecorder? = null

    /**
     * Initialize camera recorder with lifecycle owner
     */
    fun initializeCamera(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        viewModelScope.launch {
            try {
                cameraRecorder = RgbCameraRecorder(
                    context = application,
                    lifecycleOwner = lifecycleOwner
                )

                val initialized = cameraRecorder?.initialize() ?: false
                if (initialized) {
                    _cameraState.update {
                        it.copy(
                            isPreviewActive = true,
                            resolution = cameraRecorder?.getResolution() ?: "1920×1080",
                            frameRate = cameraRecorder?.getCurrentFps() ?: 30,
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

    /**
     * Get camera recorder instance for preview binding
     */
    fun getCameraRecorder(): RgbCameraRecorder? = cameraRecorder

    /**
     * Start recording
     */
    fun startRecording() {
        viewModelScope.launch {
            try {
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

                cameraRecorder?.startRecording(sessionDir, metadata)
                _cameraState.update { it.copy(isRecording = true, recordingDuration = 0, error = null) }

                // Start duration tracking
                trackRecordingDuration()
            } catch (e: Exception) {
                _cameraState.update { it.copy(error = "Recording start failed: ${e.message}") }
            }
        }
    }

    /**
     * Stop recording
     */
    fun stopRecording() {
        viewModelScope.launch {
            try {
                cameraRecorder?.stopRecording()
                _cameraState.update { it.copy(isRecording = false, error = null) }
            } catch (e: Exception) {
                _cameraState.update { it.copy(error = "Recording stop failed: ${e.message}") }
            }
        }
    }

    /**
     * Toggle preview
     */
    fun togglePreview() {
        _cameraState.update { it.copy(isPreviewActive = !it.isPreviewActive) }
    }

    /**
     * Capture photo
     */
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

    /**
     * Switch between front and back camera
     */
    fun switchCamera() {
        viewModelScope.launch {
            try {
                val cameraInfo = cameraRecorder?.getCurrentCameraInfo()
                if (cameraInfo == null) {
                    _cameraState.update { it.copy(error = "Camera not initialized") }
                    return@launch
                }

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
                    cameraRecorder?.switchToBackCamera() ?: false
                } else {
                    cameraRecorder?.switchToFrontCamera() ?: false
                }

                if (success) {
                    _cameraState.update { it.copy(error = null) }
                } else {
                    _cameraState.update { it.copy(error = "Failed to switch camera") }
                }
            } catch (e: Exception) {
                _cameraState.update { it.copy(error = "An unexpected error occurred while switching cameras.") }
            }
        }
    }

    /**
     * Update camera settings
     */
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

    /**
     * Dismiss error message
     */
    fun dismissError() {
        _cameraState.update { it.copy(error = null) }
    }

    /**
     * Track recording duration
     */
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
                cameraRecorder?.stopRecording()
                cameraRecorder?.cleanup()
            } catch (e: Exception) {
                android.util.Log.e("RGBCameraViewModel", "Error during cleanup", e)
            }
        }
    }
}
