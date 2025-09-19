package com.topdon.tc001.camera.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.topdon.tc001.sensors.RgbCameraRecorder
import com.topdon.tc001.permissions.PermissionController
import com.topdon.tc001.permissions.PermissionManager
import kotlinx.coroutines.launch

/**
 * Camera status widget that integrates with RgbCameraRecorder to show live camera status,
 * preview, and recording statistics. Demonstrates the enhanced camera functionality.
 */
class CameraStatusWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "CameraStatusWidget"
    }

    private lateinit var statusText: TextView
    private lateinit var statsText: TextView
    private lateinit var previewView: PreviewView
    private var cameraRecorder: RgbCameraRecorder? = null

    init {
        orientation = VERTICAL
        setPadding(16, 16, 16, 16)
        setupUI()
    }

    private fun setupUI() {
        // Status text
        statusText = TextView(context).apply {
            text = "Camera Status: Not Initialized"
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, android.R.color.primary_text_dark))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }
        addView(statusText)

        // Preview view
        previewView = PreviewView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                400 // Fixed height for preview
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        addView(previewView)

        // Statistics text
        statsText = TextView(context).apply {
            text = "Camera Statistics:\nNot Available"
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, android.R.color.secondary_text_dark))
            gravity = Gravity.START
            setPadding(0, 8, 0, 0)
        }
        addView(statsText)
    }

    /**
     * Initialize the widget with a camera recorder and enhanced permission system
     */
    fun initializeWithCamera(
        lifecycleOwner: LifecycleOwner,
        useFrontCamera: Boolean = false,
        permissionManager: PermissionManager? = null
    ) {
        try {
            // Create camera recorder with preview support and permission system
            cameraRecorder = RgbCameraRecorder(
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                useFrontCamera = useFrontCamera,
                permissionManager = permissionManager
            )

            // Start monitoring camera status
            startStatusMonitoring(lifecycleOwner)

            // Initialize camera
            lifecycleOwner.lifecycleScope.launch {
                val success = cameraRecorder?.initialize() ?: false
                if (success) {
                    updateStatusText("Camera initialized successfully", Color.GREEN)
                } else {
                    updateStatusText("Camera initialization failed", Color.RED)
                }
            }

        } catch (e: Exception) {
            updateStatusText("Error initializing camera: ${e.message}", Color.RED)
        }
    }

    private fun startStatusMonitoring(lifecycleOwner: LifecycleOwner) {
        val recorder = cameraRecorder ?: return

        // Monitor camera status
        lifecycleOwner.lifecycleScope.launch {
            recorder.cameraStatus.collect { status ->
                updateStatusText("Camera: $status", getStatusColor(status))
            }
        }

        // Update statistics periodically
        lifecycleOwner.lifecycleScope.launch {
            while (true) {
                updateStatistics()
                kotlinx.coroutines.delay(1000) // Update every second
            }
        }
    }

    private fun updateStatusText(text: String, color: Int) {
        post {
            statusText.text = text
            statusText.setTextColor(color)
        }
    }

    private fun getStatusColor(status: String): Int {
        return when {
            status.contains("Error") || status.contains("Failed") -> Color.RED
            status.contains("Recording") -> Color.BLUE
            status.contains("Ready") || status.contains("Initialized") -> Color.GREEN
            status.contains("Permission") -> Color.MAGENTA
            else -> ContextCompat.getColor(context, android.R.color.primary_text_dark)
        }
    }

    private fun updateStatistics() {
        val recorder = cameraRecorder ?: return
        
        try {
            val stats = recorder.getFrameCaptureStats()
            val statusText = recorder.getStatusText()
            
            val statsDisplay = buildString {
                append("Camera Statistics:\n")
                append("Type: ${stats["camera_type"]}\n")
                append("Resolution: ${stats["video_resolution"]}\n")
                append("Capture FPS: ${stats["capture_fps"]}\n")
                append("Frames Captured: ${stats["frames_captured"]}\n")
                append("Frames Dropped: ${stats["frames_dropped"]}\n")
                append("Consecutive Errors: ${stats["consecutive_errors"]}\n")
                append("Has Preview: ${stats["has_preview"]}\n")
                append("Permission: ${if (recorder.hasCameraPermission()) "Granted" else "Denied"}\n")
                append("Status: $statusText")
            }
            
            post {
                this@CameraStatusWidget.statsText.text = statsDisplay
            }
        } catch (e: Exception) {
            post {
                this@CameraStatusWidget.statsText.text = "Statistics Error: ${e.message}"
            }
        }
    }

    /**
     * Start recording with the camera
     */
    fun startRecording(sessionDirectory: String, callback: (Boolean) -> Unit) {
        cameraRecorder?.let { recorder ->
            Thread {
                try {
                    val success = kotlinx.coroutines.runBlocking {
                        recorder.startRecording(sessionDirectory)
                    }
                    post { callback(success) }
                } catch (e: Exception) {
                    post {
                        updateStatusText("Recording failed: ${e.message}", Color.RED)
                        callback(false)
                    }
                }
            }.start()
        }
    }

    /**
     * Stop recording
     */
    fun stopRecording(callback: (Boolean) -> Unit) {
        cameraRecorder?.let { recorder ->
            Thread {
                try {
                    val success = kotlinx.coroutines.runBlocking {
                        recorder.stopRecording()
                    }
                    post { callback(success) }
                } catch (e: Exception) {
                    post {
                        updateStatusText("Stop recording failed: ${e.message}", Color.RED) 
                        callback(false)
                    }
                }
            }.start()
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        cameraRecorder?.let { recorder ->
            Thread {
                kotlinx.coroutines.runBlocking {
                    recorder.cleanup()
                }
            }.start()
        }
        cameraRecorder = null
    }

    /**
     * Get the camera recorder instance for advanced usage
     */
    fun getCameraRecorder(): RgbCameraRecorder? = cameraRecorder
}