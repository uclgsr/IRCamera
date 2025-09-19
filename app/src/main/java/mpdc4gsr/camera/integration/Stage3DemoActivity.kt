package mpdc4gsr.camera.integration

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.TextureView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.constraintlayout.widget.ConstraintLayout
import com.csl.irCamera.R
import mpdc4gsr.camera.Camera2System
import mpdc4gsr.camera.ui.CameraSettingsView
import mpdc4gsr.camera.core.ModeManager
import kotlinx.coroutines.launch

/**
 * Activity that demonstrates Samsung Stage3/Level3 RAW DNG recording
 * Integrates Camera2System with CameraSettingsView for complete functionality
 */
class Stage3DemoActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "Stage3DemoActivity"
    }
    
    private lateinit var textureView: TextureView
    private lateinit var cameraSettingsView: CameraSettingsView
    private lateinit var camera2System: Camera2System
    
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                initializeCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission required for Stage3/Level3 DNG recording",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stage3_demo)
        
        setupViews()
        checkPermissions()
    }
    
    private fun setupViews() {
        textureView = findViewById(R.id.texture_view_stage3)
        cameraSettingsView = findViewById(R.id.camera_settings_stage3)
        
        // Initialize Camera2System
        camera2System = Camera2System(this, textureView)
        setupCamera2SystemCallbacks()
        setupCameraSettingsIntegration()
    }
    
    private fun setupCamera2SystemCallbacks() {
        camera2System.onError = { error ->
            runOnUiThread {
                Toast.makeText(this, "Camera error: $error", Toast.LENGTH_SHORT).show()
            }
        }
        
        camera2System.onProgress = { message ->
            runOnUiThread {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
        
        camera2System.onModeChanged = { mode ->
            runOnUiThread {
                val modeText = when (mode) {
                    ModeManager.CameraMode.RAW_50MP -> "RAW 50MP (Stage3/Level3 DNG)"
                    ModeManager.CameraMode.VIDEO_4K -> "4K Video"
                    ModeManager.CameraMode.PREVIEW_ONLY -> "Preview Only"
                }
                Toast.makeText(this, "Mode: $modeText", Toast.LENGTH_SHORT).show()
            }
        }
        
        camera2System.onRecordingStarted = {
            runOnUiThread {
                val processingMode = if (camera2System.isStage3ProcessingEnabled()) {
                    "Stage3/Level3 DNG"
                } else {
                    "Standard RAW"
                }
                Toast.makeText(this, "Recording started: $processingMode", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupCameraSettingsIntegration() {
        // Connect the Stage3/Level3 toggle to Camera2System
        cameraSettingsView.onStage3ProcessingToggle = { enabled ->
            camera2System.configureStage3Processing(enabled)
            val mode = if (enabled) "Stage3/Level3 DNG" else "Standard RAW"
            Toast.makeText(this, "RAW processing: $mode", Toast.LENGTH_SHORT).show()
        }
        
        // Set up recording toggle
        cameraSettingsView.onRecordingToggle = { shouldRecord ->
            lifecycleScope.launch {
                if (shouldRecord) {
                    val success = camera2System.startRecording()
                    if (!success) {
                        Toast.makeText(this@Stage3DemoActivity, "Failed to start recording", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    camera2System.stopRecording()
                }
            }
        }
    }
    
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            initializeCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    private fun initializeCamera() {
        lifecycleScope.launch {
            try {
                if (camera2System.initialize()) {
                    val caps = camera2System.getDeviceCaps()
                    
                    // Configure Stage3/Level3 processing for Samsung devices
                    val deviceModel = android.os.Build.MODEL
                    val isSamsungDevice = deviceModel.contains("SM-S9") || deviceModel.contains("SM-S22")
                    
                    if (isSamsungDevice && caps?.supportsRaw == true) {
                        // Enable Stage3/Level3 processing
                        camera2System.configureStage3Processing(true)
                        cameraSettingsView.setStage3ProcessingVisible(true)
                        cameraSettingsView.setStage3ProcessingEnabled(true)
                        
                        Toast.makeText(
                            this@Stage3DemoActivity,
                            "Samsung $deviceModel - Stage3/Level3 DNG Ready",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // Hide Stage3/Level3 option for unsupported devices
                        cameraSettingsView.setStage3ProcessingVisible(false)
                        
                        val reason = if (!isSamsungDevice) {
                            "Non-Samsung device"
                        } else {
                            "RAW not supported"
                        }
                        Toast.makeText(
                            this@Stage3DemoActivity,
                            "Standard RAW mode only ($reason)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    
                    // Switch to RAW mode for demonstration
                    camera2System.switchMode(ModeManager.CameraMode.RAW_50MP)
                    
                } else {
                    Toast.makeText(this@Stage3DemoActivity, "Failed to initialize camera", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@Stage3DemoActivity, "Camera initialization error: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            camera2System.release()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Error during camera release in onDestroy", e)
        }
    }
}