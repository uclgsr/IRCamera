package com.topdon.tc001.camera

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.TextureView
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.topdon.tc001.camera.core.DeviceCaps
import com.topdon.tc001.camera.core.ModeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

class RGBCameraRecorder(
    private val context: Context,
    private val textureView: TextureView,
    private val activity: Activity? = null,
) {
    companion object {
        private const val TAG = "RGBCameraRecorder"
    }

    private val camera2System = Camera2System(context, textureView)

    enum class CameraMode(val displayName: String, val description: String) {
        RAW_50MP("RAW 50MP", "High-resolution RAW capture at ~15fps"),
        VIDEO_4K("4K Video", "4K video recording at 30/60fps"),
        PREVIEW_ONLY("Preview", "Preview mode only"),
    }

    enum class VideoResolution(val width: Int, val height: Int, val displayName: String) {
        UHD_4K(3840, 2160, "4K UHD (3840×2160)"),
        HD_1080P(1920, 1080, "Full HD (1920×1080)"),
        HD_720P(1280, 720, "HD (1280×720)"),
        SD_480P(720, 480, "SD (720×480)"),
    }

    enum class CameraFacing(val displayName: String) {
        BACK("Back Camera"),
        FRONT("Front Camera"),
    }

    data class RecordingSettings(
        val mode: CameraMode = CameraMode.VIDEO_4K,
        val resolution: VideoResolution = VideoResolution.UHD_4K,
        val frameRate: Int = 30,
        val bitRate: Int = 10_000_000,
        val enableStabilization: Boolean = true,
        val enableFlash: Boolean = false,
        val audioEnabled: Boolean = true,
        val rawCaptureFrameRate: Int = 15,
        val enableHighSpeedVideo: Boolean = false,
    )

    data class CameraInfo(
        val cameraId: String,
        val facing: CameraFacing,
        val supportsRaw: Boolean,
        val supports4K: Boolean,
        val displayName: String,
    )

    private var currentCameraFacing = CameraFacing.BACK
    private var recordingSettings = RecordingSettings()
    private var sessionId: String = ""

    var onError: ((String) -> Unit)? = null
    var onCameraSwitched: ((CameraFacing, String) -> Unit)? = null
    var onRawImageCaptured: ((File) -> Unit)? = null
    var onVideoRecordingStarted: (() -> Unit)? = null
    var onVideoRecordingCompleted: ((File) -> Unit)? = null

    init {
        setupCallbacks()
    }

    private fun setupCallbacks() {
        camera2System.onError = { error -> onError?.invoke(error) }
        camera2System.onProgress = { message -> Log.d(TAG, "Progress: $message") }
        camera2System.onModeChanged = { mode -> Log.i(TAG, "Mode changed to: $mode") }
        camera2System.onRecordingStarted = { onVideoRecordingStarted?.invoke() }
        camera2System.onRecordingStopped = { /* Handle stopped */ }
    }

    suspend fun initializeCamera(cameraId: String = "0"): Boolean =
        withContext(Dispatchers.Main) {
            try {

                if (!checkCameraPermission()) {
                    Log.w(TAG, "Camera permission not granted")
                    return@withContext false
                }

                return@withContext camera2System.initialize(cameraId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize camera", e)
                onError?.invoke("Failed to initialize camera: ${e.message}")
                return@withContext false
            }
        }

    suspend fun switchMode(mode: CameraMode): Boolean {
        val systemMode =
            when (mode) {
                CameraMode.RAW_50MP -> ModeManager.CameraMode.RAW_50MP
                CameraMode.VIDEO_4K -> ModeManager.CameraMode.VIDEO_4K
                CameraMode.PREVIEW_ONLY -> ModeManager.CameraMode.PREVIEW_ONLY
            }
        return camera2System.switchMode(systemMode)
    }

    suspend fun startRecording(
        outputDir: File,
        sessionId: String,
    ): Boolean {
        this.sessionId = sessionId


        return camera2System.startRecording(sessionId)
    }

    suspend fun stopRecording(): Boolean {
        return camera2System.stopRecording()
    }

    private fun checkCameraPermission(): Boolean {
        return XXPermissions.isGranted(context, Permission.CAMERA)
    }

    fun requestCameraPermission(callback: (Boolean) -> Unit) {
        if (activity == null) {
            Log.e(TAG, "Activity context required for permission request")
            callback(false)
            return
        }

        XXPermissions.with(activity)
            .permission(Permission.CAMERA)
            .request(
                object : OnPermissionCallback {
                    override fun onGranted(
                        permissions: MutableList<String>,
                        allGranted: Boolean,
                    ) {
                        callback(allGranted)
                    }

                    override fun onDenied(
                        permissions: MutableList<String>,
                        doNotAskAgain: Boolean,
                    ) {
                        Log.w(TAG, "Camera permission denied")
                        callback(false)
                    }
                },
            )
    }

    suspend fun switchCamera(facing: CameraFacing): Boolean {
        val cameraId = getFirstCameraIdForFacing(facing) ?: return false
        currentCameraFacing = facing
        val success = camera2System.initialize(cameraId)
        if (success) {
            onCameraSwitched?.invoke(facing, cameraId)
        }
        return success
    }

    suspend fun switchCamera(cameraId: String): Boolean {
        val success = camera2System.initialize(cameraId)
        if (success) {

            val facing = if (cameraId == "1") CameraFacing.FRONT else CameraFacing.BACK
            currentCameraFacing = facing
            onCameraSwitched?.invoke(facing, cameraId)
        }
        return success
    }

    fun getAvailableCameras(): List<CameraInfo> {

        return emptyList() // Simplified for now
    }

    fun getCurrentMode(): CameraMode {
        val systemMode = camera2System.getCurrentMode()
        return when (systemMode) {
            ModeManager.CameraMode.RAW_50MP -> CameraMode.RAW_50MP
            ModeManager.CameraMode.VIDEO_4K -> CameraMode.VIDEO_4K
            ModeManager.CameraMode.PREVIEW_ONLY -> CameraMode.PREVIEW_ONLY
        }
    }

    fun isRecording(): Boolean = camera2System.isRecording()

    fun getDeviceCaps(): DeviceCaps? = camera2System.getDeviceCaps()

    fun getCurrentCameraFacing(): CameraFacing = currentCameraFacing

    fun getCurrentSessionId(): String = sessionId

    fun updateRecordingSettings(settings: RecordingSettings) {
        recordingSettings = settings
    }

    fun getRecordingSettings(): RecordingSettings = recordingSettings

    suspend fun release() {
        camera2System.release()
    }

    private fun getFirstCameraIdForFacing(facing: CameraFacing): String? {

        return when (facing) {
            CameraFacing.BACK -> "0"
            CameraFacing.FRONT -> "1"
        }
    }

    fun isModeSupported(mode: CameraMode): Boolean {
        val caps = getDeviceCaps()
        return when (mode) {
            CameraMode.RAW_50MP -> caps?.supportsRaw ?: false
            CameraMode.VIDEO_4K -> caps?.supports4k60 ?: true // Video is generally supported
            CameraMode.PREVIEW_ONLY -> true // Always supported
        }
    }

    fun getAvailableModes(): List<CameraMode> {
        val modes = mutableListOf<CameraMode>()
        modes.add(CameraMode.PREVIEW_ONLY) // Always available
        if (isModeSupported(CameraMode.VIDEO_4K)) modes.add(CameraMode.VIDEO_4K)
        if (isModeSupported(CameraMode.RAW_50MP)) modes.add(CameraMode.RAW_50MP)
        return modes
    }

    fun supportsRawCapture(): Boolean = isModeSupported(CameraMode.RAW_50MP)

    fun supportsVideoRecording(): Boolean = isModeSupported(CameraMode.VIDEO_4K)

    fun supportsHighSpeed60fps(): Boolean = getDeviceCaps()?.supports4k60 ?: false

    fun getMaxRawResolution(): VideoResolution? {

        return VideoResolution.UHD_4K // Simplified
    }

    fun getCurrentVideoResolution(): VideoResolution = recordingSettings.resolution

    fun updateSettings(settings: RecordingSettings) = updateRecordingSettings(settings)

    fun getCurrentSettings(): RecordingSettings = getRecordingSettings()

    fun cleanup() = runBlocking { release() }

    suspend fun setFlashEnabled(enabled: Boolean): Boolean {

        Log.w(TAG, "Flash control not yet implemented in clean architecture")
        return false
    }

    suspend fun pauseRecording(): Boolean {

        Log.w(TAG, "Pause/resume not yet implemented in clean architecture")
        return false
    }

    suspend fun resumeRecording(): Boolean {

        Log.w(TAG, "Pause/resume not yet implemented in clean architecture")
        return false
    }

    suspend fun startRecording(sessionId: String): Boolean {
        this.sessionId = sessionId
        return camera2System.startRecording(sessionId)
    }

    fun getAvailableCameraFacing(): List<CameraFacing> =
        listOf(CameraFacing.BACK, CameraFacing.FRONT)

    fun getSupportedResolutions(): List<VideoResolution> = VideoResolution.values().toList()

    fun isRawCaptureActive(): Boolean = isRecording() && getCurrentMode() == CameraMode.RAW_50MP

    fun isVideoRecordingActive(): Boolean = isRecording() && getCurrentMode() == CameraMode.VIDEO_4K

    fun getRawCaptureCount(): Int = camera2System.getDeviceCaps()?.let { 0 } ?: 0

    fun getCurrentVideoFile(): File? = null // Not exposed in clean architecture

    fun getRawImagesDirectory(): File? = null // Not exposed in clean architecture

    fun isSessionSwitching(): Boolean = false // Clean architecture handles this internally
}
