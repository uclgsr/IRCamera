package com.topdon.tc001.camera.integration

import android.content.Context
import android.util.Log
import android.view.TextureView
import com.topdon.tc001.camera.RGBCameraRecorder
import com.topdon.tc001.camera.ui.CameraModeSelector
import kotlinx.coroutines.*

/**
 * Integration example demonstrating dual RAW/Video mode usage
 * 
 * Shows how to integrate the enhanced RGBCameraRecorder with dual-mode
 * capabilities into existing applications, particularly for Samsung S22 devices.
 */
class DualModeIntegrationExample(
    private val context: Context,
    private val textureView: TextureView,
    private val cameraModeSelector: CameraModeSelector
) {
    companion object {
        private const val TAG = "DualModeIntegration"
    }

    private var cameraRecorder: RGBCameraRecorder? = null
    private val integrationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Initialize the dual-mode camera system
     */
    fun initializeDualModeCamera() {
        try {
            // Create enhanced camera recorder with dual-mode support
            cameraRecorder = RGBCameraRecorder(context, textureView).apply {
                
                // Setup callbacks for mode changes and recording events
                onModeChanged = { mode ->
                    Log.i(TAG, "Camera mode changed to: ${mode.displayName}")
                    handleModeChange(mode)
                }
                
                onRecordingStarted = {
                    Log.i(TAG, "Recording started in ${getCurrentMode().displayName} mode")
                }
                
                onRecordingStopped = { file ->
                    when (getCurrentMode()) {
                        RGBCameraRecorder.CameraMode.RAW_50MP -> {
                            Log.i(TAG, "RAW recording completed: ${getRawImagesDirectory()?.absolutePath}")
                        }
                        RGBCameraRecorder.CameraMode.VIDEO_4K -> {
                            Log.i(TAG, "4K video completed: ${file?.absolutePath}")
                        }
                        else -> { /* Preview mode - no recording */ }
                    }
                }
                
                onRawImageCaptured = { dngFile ->
                    Log.d(TAG, "RAW image captured: ${dngFile.name}")
                }
                
                onError = { error ->
                    Log.e(TAG, "Camera error: $error")
                    handleCameraError(error)
                }
                
                // Initialize the camera system
                initialize()
            }
            
            // Setup mode selector UI integration
            cameraModeSelector.apply {
                setCameraRecorder(cameraRecorder!!)
                setOnModeChangeListener { mode ->
                    integrationScope.launch {
                        handleUserModeSwitch(mode)
                    }
                }
            }
            
            Log.i(TAG, "Dual-mode camera system initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize dual-mode camera", e)
        }
    }

    /**
     * Handle user-initiated mode switching from UI
     */
    private suspend fun handleUserModeSwitch(mode: RGBCameraRecorder.CameraMode) {
        val recorder = cameraRecorder ?: return
        
        try {
            Log.i(TAG, "User switching to ${mode.displayName}")
            
            // Ensure not recording before switching
            if (recorder.isRecording()) {
                Log.w(TAG, "Cannot switch modes while recording")
                return
            }
            
            // Check device support for requested mode
            if (!recorder.isModeSupported(mode)) {
                Log.w(TAG, "${mode.displayName} not supported on this device")
                return
            }
            
            // Perform the mode switch
            val success = recorder.switchMode(mode)
            if (success) {
                Log.i(TAG, "Successfully switched to ${mode.displayName}")
                
                // Log device-specific capabilities
                logModeCapabilities(mode)
            } else {
                Log.e(TAG, "Failed to switch to ${mode.displayName}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during mode switch", e)
        }
    }

    /**
     * Handle camera mode changes (internal callback)
     */
    private fun handleModeChange(mode: RGBCameraRecorder.CameraMode) {
        when (mode) {
            RGBCameraRecorder.CameraMode.RAW_50MP -> {
                Log.i(TAG, "Entered RAW 50MP mode - continuous capture ready")
            }
            RGBCameraRecorder.CameraMode.VIDEO_4K -> {
                Log.i(TAG, "Entered 4K Video mode - recording ready")
            }
            RGBCameraRecorder.CameraMode.PREVIEW_ONLY -> {
                Log.i(TAG, "Entered Preview mode - low power consumption")
            }
        }
    }

    /**
     * Log device-specific capabilities for debugging
     */
    private fun logModeCapabilities(mode: RGBCameraRecorder.CameraMode) {
        val recorder = cameraRecorder ?: return
        
        when (mode) {
            RGBCameraRecorder.CameraMode.RAW_50MP -> {
                val maxRes = recorder.getMaxRawResolution()
                val megapixels = if (maxRes != null) {
                    (maxRes.width * maxRes.height) / 1_000_000
                } else 0
                Log.i(TAG, "RAW Mode - Max resolution: ${maxRes?.width}×${maxRes?.height} (~${megapixels}MP)")
            }
            
            RGBCameraRecorder.CameraMode.VIDEO_4K -> {
                val videoRes = recorder.getCurrentVideoResolution()
                val supports60fps = recorder.supportsHighSpeed60fps()
                val maxFps = if (supports60fps) 60 else 30
                Log.i(TAG, "Video Mode - Resolution: ${videoRes.width}×${videoRes.height} @${maxFps}fps")
            }
            
            RGBCameraRecorder.CameraMode.PREVIEW_ONLY -> {
                Log.i(TAG, "Preview Mode - Basic preview functionality")
            }
        }
    }

    /**
     * Start recording in current mode
     */
    fun startRecording(sessionId: String? = null): Boolean {
        val recorder = cameraRecorder ?: return false
        
        return try {
            val currentMode = recorder.getCurrentMode()
            Log.i(TAG, "Starting recording in ${currentMode.displayName} mode")
            
            recorder.startRecording(sessionId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            false
        }
    }

    /**
     * Stop recording in current mode
     */
    fun stopRecording(): Boolean {
        val recorder = cameraRecorder ?: return false
        
        return try {
            val currentMode = recorder.getCurrentMode()
            Log.i(TAG, "Stopping recording in ${currentMode.displayName} mode")
            
            recorder.stopRecording()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            false
        }
    }

    /**
     * Handle camera errors with user-friendly messages
     */
    private fun handleCameraError(error: String) {
        when {
            error.contains("Samsung", ignoreCase = true) -> {
                Log.e(TAG, "Samsung device specific error - check compatibility mode")
            }
            error.contains("permission", ignoreCase = true) -> {
                Log.e(TAG, "Camera permission error - request permissions")
            }
            error.contains("busy", ignoreCase = true) -> {
                Log.e(TAG, "Camera busy - close other camera apps")
            }
            else -> {
                Log.e(TAG, "General camera error: $error")
            }
        }
    }

    /**
     * Get available camera modes for this device
     */
    fun getAvailableModes(): List<RGBCameraRecorder.CameraMode> {
        return cameraRecorder?.getAvailableModes() ?: emptyList()
    }

    /**
     * Check if device supports high-end features
     */
    fun checkDeviceCapabilities(): DeviceCapabilities {
        val recorder = cameraRecorder ?: return DeviceCapabilities()
        
        return DeviceCapabilities(
            supportsRaw50MP = recorder.supportsRawCapture(),
            supports4KVideo = recorder.supportsVideoRecording(),
            supports60fps = recorder.supportsHighSpeed60fps(),
            maxRawResolution = recorder.getMaxRawResolution(),
            availableModes = recorder.getAvailableModes()
        )
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        integrationScope.cancel()
        cameraRecorder?.cleanup()
        cameraRecorder = null
    }

    /**
     * Device capability information
     */
    data class DeviceCapabilities(
        val supportsRaw50MP: Boolean = false,
        val supports4KVideo: Boolean = false,
        val supports60fps: Boolean = false,
        val maxRawResolution: android.util.Size? = null,
        val availableModes: List<RGBCameraRecorder.CameraMode> = emptyList()
    )
}