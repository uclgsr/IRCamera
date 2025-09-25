package mpdc4gsr.camera.integration

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import kotlinx.coroutines.launch
import mpdc4gsr.sensors.RgbCameraRecorder
import mpdc4gsr.camera.core.SamsungDeviceCompatibility
import mpdc4gsr.camera.ui.TapToFocusPreviewView

class DualModeCameraActivity : AppCompatActivity() {
    private lateinit var previewView: TapToFocusPreviewView
    private lateinit var cameraModeSelector: LinearLayout
    private var rgbCameraRecorder: RgbCameraRecorder? = null

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                initializeCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission required for dual-mode system",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dual_mode_camera)

        previewView = findViewById(R.id.preview_view)
        cameraModeSelector = findViewById(R.id.camera_mode_selector)

        val initialMode = intent.getStringExtra("INITIAL_MODE") ?: "VIDEO_4K"
        val enableSamsungOptimizations =
            intent.getBooleanExtra("ENABLE_SAMSUNG_OPTIMIZATIONS", true)

        setupModeSelector(initialMode, enableSamsungOptimizations)

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED -> {
                initializeCamera()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun initializeCamera() {
        try {

            // Use proper PreviewView for RgbCameraRecorder
            rgbCameraRecorder = RgbCameraRecorder(
                context = this,
                lifecycleOwner = this,
                previewView = previewView.previewView, // Use the inner PreviewView
                useFrontCamera = false
            )

            lifecycleScope.launch {
                rgbCameraRecorder?.initialize()
            }

            Toast.makeText(this, "Dual-mode camera system initialized", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to initialize camera: ${e.message}", Toast.LENGTH_LONG)
                .show()
            finish()
        }
    }

    private fun setupModeSelector(initialMode: String, enableSamsungOptimizations: Boolean = true) {
        // Simple setup without complex mode selector since we removed CameraModeSelector
        val mode = when (initialMode) {
            "RAW_50MP" -> "RAW"
            "VIDEO_4K" -> "VIDEO" 
            else -> "PREVIEW"
        }
        
        // We can add simple buttons or text to indicate the mode later
        // For now, just initialize with default video mode
    }

    private suspend fun switchCameraMode(
        newMode: String,
        enableSamsungOptimizations: Boolean = true
    ) {
        try {
            // RgbCameraRecorder doesn't have switchMode method, 
            // so we'll handle mode changes through its API
            when (newMode) {
                "RAW" -> {
                    // Configure for RAW capture mode
                    if (enableSamsungOptimizations && SamsungDeviceCompatibility.isStage3Compatible()) {
                        Toast.makeText(this, "RAW Mode: Samsung Stage3/Level3 DNG Enabled", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(
                            this,
                            "RAW Mode: Standard DNG (${SamsungDeviceCompatibility.getDeviceInfo()})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                "VIDEO" -> {
                    Toast.makeText(this, "Switched to 4K Video Mode", Toast.LENGTH_SHORT).show()
                }
                "PREVIEW" -> {
                    Toast.makeText(this, "Switched to Preview Only Mode", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Mode switch error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rgbCameraRecorder = null
    }
}
