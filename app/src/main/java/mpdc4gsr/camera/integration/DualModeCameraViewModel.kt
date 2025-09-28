package mpdc4gsr.camera.integration

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.launch
import mpdc4gsr.camera.core.SamsungDeviceCompatibility
import mpdc4gsr.sensors.RgbCameraRecorder

class DualModeCameraViewModel : BaseViewModel() {

    private val _permissionState = MutableLiveData<PermissionState>()
    val permissionState: LiveData<PermissionState> = _permissionState

    private val _cameraState = MutableLiveData<CameraState>()
    val cameraState: LiveData<CameraState> = _cameraState

    private val _cameraMode = MutableLiveData<CameraMode>()
    val cameraMode: LiveData<CameraMode> = _cameraMode

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _statusMessage = MutableLiveData<String?>()
    val statusMessage: LiveData<String?> = _statusMessage

    private var rgbCameraRecorder: RgbCameraRecorder? = null
    private var enableSamsungOptimizations: Boolean = true

    data class CameraState(
        val isInitialized: Boolean,
        val isRecording: Boolean,
        val deviceInfo: String? = null
    )

    enum class PermissionState {
        UNKNOWN,
        GRANTED,
        DENIED,
        REQUESTING
    }

    enum class CameraMode {
        PREVIEW,
        RAW,
        VIDEO_4K
    }

    fun initialize(initialMode: String, enableOptimizations: Boolean) {
        enableSamsungOptimizations = enableOptimizations

        val mode = when (initialMode) {
            "RAW_50MP" -> CameraMode.RAW
            "VIDEO_4K" -> CameraMode.VIDEO_4K
            else -> CameraMode.PREVIEW
        }

        _cameraMode.value = mode
        _permissionState.value = PermissionState.UNKNOWN
        _cameraState.value = CameraState(
            isInitialized = false,
            isRecording = false,
            deviceInfo = SamsungDeviceCompatibility.getDeviceInfo()
        )
    }

    fun onPermissionGranted() {
        _permissionState.value = PermissionState.GRANTED
    }

    fun onPermissionDenied() {
        _permissionState.value = PermissionState.DENIED
        _error.value = "Camera permission required for dual-mode system"
    }

    fun requestPermission() {
        _permissionState.value = PermissionState.REQUESTING
    }

    fun initializeCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        viewModelScope.launch {
            try {
                rgbCameraRecorder = RgbCameraRecorder(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    useFrontCamera = false
                )

                rgbCameraRecorder?.initialize()

                _cameraState.value = _cameraState.value?.copy(isInitialized = true)
                _statusMessage.value = "Dual-mode camera system initialized"

            } catch (e: Exception) {
                _error.value = "Failed to initialize camera: ${e.message}"
            }
        }
    }

    fun switchCameraMode(newMode: CameraMode) {
        if (_cameraState.value?.isInitialized != true) {
            _error.value = "Camera not initialized"
            return
        }

        viewModelScope.launch {
            try {
                _cameraMode.value = newMode

                when (newMode) {
                    CameraMode.RAW -> {
                        handleRawModeSwitch()
                    }

                    CameraMode.VIDEO_4K -> {
                        _statusMessage.value = "Switched to 4K Video Mode"
                    }

                    CameraMode.PREVIEW -> {
                        _statusMessage.value = "Switched to Preview Mode"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Mode switch error: ${e.message}"
            }
        }
    }

    private fun handleRawModeSwitch() {
        if (enableSamsungOptimizations && SamsungDeviceCompatibility.isStage3Compatible()) {
            _statusMessage.value = "RAW Mode: Samsung Stage3/Level3 DNG Enabled"
        } else {
            val deviceInfo = SamsungDeviceCompatibility.getDeviceInfo()
            _statusMessage.value = "RAW Mode: Standard DNG ($deviceInfo)"
        }
    }

    fun startRecording() {
        if (_cameraState.value?.isInitialized != true) {
            _error.value = "Camera not initialized"
            return
        }

        viewModelScope.launch {
            try {
                // Implement recording logic through RgbCameraRecorder
                _cameraState.value = _cameraState.value?.copy(isRecording = true)
                _statusMessage.value = "Recording started"
            } catch (e: Exception) {
                _error.value = "Failed to start recording: ${e.message}"
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                // Implement stop recording logic
                _cameraState.value = _cameraState.value?.copy(isRecording = false)
                _statusMessage.value = "Recording stopped"
            } catch (e: Exception) {
                _error.value = "Failed to stop recording: ${e.message}"
            }
        }
    }

    fun cleanup() {
        viewModelScope.launch {
            try {
                rgbCameraRecorder?.cleanup()
                _cameraState.value = CameraState(
                    isInitialized = false,
                    isRecording = false
                )
            } catch (e: Exception) {
                _error.value = "Cleanup error: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun getSamsungOptimizationStatus(): String {
        return if (enableSamsungOptimizations) {
            if (SamsungDeviceCompatibility.isStage3Compatible()) {
                "Samsung Stage3/Level3 optimizations enabled"
            } else {
                "Samsung optimizations enabled (${SamsungDeviceCompatibility.getDeviceInfo()})"
            }
        } else {
            "Samsung optimizations disabled"
        }
    }

    companion object {
        private const val TAG = "DualModeCameraViewModel"
    }
}