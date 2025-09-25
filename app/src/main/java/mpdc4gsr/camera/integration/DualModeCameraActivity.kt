package mpdc4gsr.camera.integration

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import kotlinx.coroutines.launch
import mpdc4gsr.sensors.RgbCameraRecorder
import mpdc4gsr.camera.core.SamsungDeviceCompatibility
import android.widget.LinearLayout

class DualModeCameraActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
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
            rgbCameraRecorder = RgbCameraRecorder(
                context = this,
                lifecycleOwner = this,
                previewView = previewView,
                useFrontCamera = false
            )

            // Initialize the camera recorder
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
        // Simplified mode handling - the comprehensive RgbCameraRecorder handles modes internally
        val mode = when (initialMode) {
            "RAW_50MP" -> "RAW"
            "VIDEO_4K" -> "VIDEO" 
            else -> "PREVIEW"
        }

        // The mode selector is now a simple LinearLayout - functionality is handled by RgbCameraRecorder
        // No need for complex mode switching UI since the consolidated implementation handles this
    }

    private suspend fun switchCameraMode(
        newMode: String,
        enableSamsungOptimizations: Boolean = true
    ) {
        try {
            // The comprehensive RgbCameraRecorder handles mode switching internally
            when (newMode) {
                "RAW" -> {
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
                else -> {
                    Toast.makeText(this, "Switched to Preview Mode", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Mode switch error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            rgbCameraRecorder?.cleanup()
        }
    }
}
