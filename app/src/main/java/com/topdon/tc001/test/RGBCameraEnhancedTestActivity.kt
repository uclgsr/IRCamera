package com.topdon.tc001.test

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.topdon.tc001.camera.ui.CameraStatusWidget
import kotlinx.coroutines.launch
import java.io.File
import com.topdon.tc001.permissions.PermissionController
import com.topdon.tc001.permissions.EnhancedPermissionManager

/**
 * Test activity to demonstrate the enhanced RGB camera functionality including:
 * - Front/back camera selection
 * - Enhanced error handling
 * - Permission management
 * - Live preview
 * - Real-time statistics
 * - Resource cleanup
 */
class RGBCameraEnhancedTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RGBCameraEnhancedTest"
        
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private lateinit var cameraWidget: CameraStatusWidget
    private lateinit var frontCameraWidget: CameraStatusWidget
    private lateinit var permissionStatusText: TextView
    private lateinit var logText: TextView
    private lateinit var cameraToggle: Switch
    private lateinit var startRecordingButton: Button
    private lateinit var stopRecordingButton: Button
    private lateinit var clearLogsButton: Button
    
    // Enhanced permission system
    private lateinit var permissionController: PermissionController
    private lateinit var enhancedPermissionManager: EnhancedPermissionManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        updatePermissionStatus(allGranted)
        if (allGranted) {
            initializeCameras()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupUI()
        checkPermissionsAndInitialize()
    }

    private fun setupUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Title
        val title = TextView(this).apply {
            text = "RGB Camera Enhanced Test"
            textSize = 20f
            setPadding(0, 0, 0, 16)
        }
        layout.addView(title)

        // Permission status
        permissionStatusText = TextView(this).apply {
            text = "Checking permissions..."
            textSize = 14f
            setPadding(0, 0, 0, 8)
        }
        layout.addView(permissionStatusText)

        // Camera toggle
        val toggleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        
        toggleLayout.addView(TextView(this).apply {
            text = "Back Camera"
            textSize = 16f
        })
        
        cameraToggle = Switch(this).apply {
            setOnCheckedChangeListener { _, isChecked ->
                switchCamera(isChecked)
            }
        }
        toggleLayout.addView(cameraToggle)
        
        toggleLayout.addView(TextView(this).apply {
            text = "Front Camera"
            textSize = 16f
        })
        
        layout.addView(toggleLayout)

        // Back camera widget
        cameraWidget = CameraStatusWidget(this).apply {
            visibility = LinearLayout.VISIBLE
        }
        layout.addView(cameraWidget)

        // Front camera widget (initially hidden)
        frontCameraWidget = CameraStatusWidget(this).apply {
            visibility = LinearLayout.GONE
        }
        layout.addView(frontCameraWidget)

        // Control buttons
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        startRecordingButton = Button(this).apply {
            text = "Start Recording"
            setOnClickListener { startRecording() }
            isEnabled = false
        }
        buttonLayout.addView(startRecordingButton)

        stopRecordingButton = Button(this).apply {
            text = "Stop Recording"
            setOnClickListener { stopRecording() }
            isEnabled = false
        }
        buttonLayout.addView(stopRecordingButton)

        clearLogsButton = Button(this).apply {
            text = "Clear Logs"
            setOnClickListener { clearLogs() }
        }
        buttonLayout.addView(clearLogsButton)

        layout.addView(buttonLayout)

        // Log display
        logText = TextView(this).apply {
            text = "Enhanced RGB Camera Test started...\n"
            textSize = 12f
            setPadding(0, 16, 0, 0)
            setTextIsSelectable(true)
        }

        val scrollView = ScrollView(this).apply {
            addView(logText)
        }
        layout.addView(scrollView)

        setContentView(layout)
    }

    private fun checkPermissionsAndInitialize() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            updatePermissionStatus(true)
            initializeCameras()
        } else {
            addLog("Missing permissions: ${missingPermissions.joinToString(", ")}")
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun updatePermissionStatus(allGranted: Boolean) {
        permissionStatusText.text = if (allGranted) {
            "✅ All permissions granted"
        } else {
            "❌ Camera permissions required"
        }
        permissionStatusText.setTextColor(
            ContextCompat.getColor(
                this, 
                if (allGranted) android.R.color.holo_green_dark else android.R.color.holo_red_dark
            )
        )
    }

    private fun initializeCameras() {
        // Initialize enhanced permission system
        permissionController = PermissionController(this)
        permissionController.initialize()
        enhancedPermissionManager = EnhancedPermissionManager(this, permissionController)
        
        lifecycleScope.launch {
            try {
                addLog("Initializing cameras with enhanced permission system...")
                
                // Initialize back camera with enhanced permission support
                cameraWidget.initializeWithCamera(
                    lifecycleOwner = this@RGBCameraEnhancedTestActivity,
                    useFrontCamera = false,
                    enhancedPermissionManager = enhancedPermissionManager
                )
                addLog("✅ Back camera initialized with enhanced features:")
                addLog("  - 4K@60fps video recording capability")
                addLog("  - 30fps JPEG frame capture")
                addLog("  - Enhanced error handling and recovery")
                addLog("  - Real-time frame drop detection")
                addLog("  - Live preview display")
                addLog("  - Enhanced permission request system")
                
                // Initialize front camera with enhanced permission support
                frontCameraWidget.initializeWithCamera(
                    lifecycleOwner = this@RGBCameraEnhancedTestActivity,
                    useFrontCamera = true,
                    enhancedPermissionManager = enhancedPermissionManager
                )
                addLog("✅ Front camera initialized with enhanced permission system")
                
                startRecordingButton.isEnabled = true
                addLog("📱 Cameras ready for recording with enhanced permission management!")
                
            } catch (e: Exception) {
                addLog("❌ Camera initialization failed: ${e.message}")
                Log.e(TAG, "Camera initialization failed", e)
            }
        }
    }

    private fun switchCamera(useFrontCamera: Boolean) {
        if (useFrontCamera) {
            cameraWidget.visibility = LinearLayout.GONE
            frontCameraWidget.visibility = LinearLayout.VISIBLE
            addLog("Switched to front camera")
        } else {
            frontCameraWidget.visibility = LinearLayout.GONE
            cameraWidget.visibility = LinearLayout.VISIBLE
            addLog("Switched to back camera")
        }
    }

    private fun startRecording() {
        val activeWidget = if (cameraToggle.isChecked) frontCameraWidget else cameraWidget
        val cameraType = if (cameraToggle.isChecked) "front" else "back"
        
        // Create session directory
        val sessionDir = File(filesDir, "test_sessions/rgb_camera_${System.currentTimeMillis()}")
        sessionDir.mkdirs()
        
        addLog("Starting $cameraType camera recording in: ${sessionDir.absolutePath}")
        startRecordingButton.isEnabled = false
        
        activeWidget.startRecording(sessionDir.absolutePath) { success ->
            if (success) {
                addLog("✅ Recording started successfully")
                stopRecordingButton.isEnabled = true
            } else {
                addLog("❌ Failed to start recording")
                startRecordingButton.isEnabled = true
            }
        }
    }

    private fun stopRecording() {
        val activeWidget = if (cameraToggle.isChecked) frontCameraWidget else cameraWidget
        val cameraType = if (cameraToggle.isChecked) "front" else "back"
        
        addLog("Stopping $cameraType camera recording...")
        stopRecordingButton.isEnabled = false
        
        activeWidget.stopRecording { success ->
            if (success) {
                addLog("✅ Recording stopped successfully")
                
                // Get and display final statistics
                val recorder = activeWidget.getCameraRecorder()
                if (recorder != null) {
                    val stats = recorder.getFrameCaptureStats()
                    addLog("📊 Final Recording Statistics:")
                    addLog("  Frames Captured: ${stats["frames_captured"]}")
                    addLog("  Frames Dropped: ${stats["frames_dropped"]}")
                    addLog("  Camera Type: ${stats["camera_type"]}")
                    addLog("  Video Resolution: ${stats["video_resolution"]}")
                }
            } else {
                addLog("❌ Failed to stop recording")
            }
            startRecordingButton.isEnabled = true
        }
    }

    private fun clearLogs() {
        logText.text = "Logs cleared.\n"
    }

    private fun addLog(message: String) {
        runOnUiThread {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            logText.append("[$timestamp] $message\n")
            
            // Auto-scroll to bottom
            (logText.parent as? ScrollView)?.post {
                (logText.parent as ScrollView).fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
        
        Log.i(TAG, message)
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up camera resources
        try {
            cameraWidget.cleanup()
            frontCameraWidget.cleanup()
            addLog("Camera resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}