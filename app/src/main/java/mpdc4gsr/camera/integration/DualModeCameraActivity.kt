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
import com.csl.irCamera.R
import mpdc4gsr.camera.RGBCameraRecorder
import mpdc4gsr.camera.ui.CameraModeSelector
import mpdc4gsr.camera.core.SamsungDeviceCompatibility
import kotlinx.coroutines.launch

class DualModeCameraActivity : AppCompatActivity() {
    private lateinit var textureView: TextureView
    private lateinit var cameraModeSelector: CameraModeSelector
    private var rgbCameraRecorder: RGBCameraRecorder? = null

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

        textureView = findViewById(R.id.texture_view)
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

            rgbCameraRecorder = RGBCameraRecorder(this, textureView)

            val settings =
                RGBCameraRecorder.RecordingSettings(
                    mode = RGBCameraRecorder.CameraMode.VIDEO_4K,
                    resolution = RGBCameraRecorder.VideoResolution.UHD_4K,
                    frameRate = 30,
                    bitRate = 10_000_000,
                    enableStabilization = true,
                    enableHighSpeedVideo = false,
                )

            rgbCameraRecorder?.updateRecordingSettings(settings)

            Toast.makeText(this, "Dual-mode camera system initialized", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to initialize camera: ${e.message}", Toast.LENGTH_LONG)
                .show()
            finish()
        }
    }

    private fun setupModeSelector(initialMode: String, enableSamsungOptimizations: Boolean = true) {

        val mode =
            when (initialMode) {
                "RAW_50MP" -> RGBCameraRecorder.CameraMode.RAW_50MP
                "VIDEO_4K" -> RGBCameraRecorder.CameraMode.VIDEO_4K
                else -> RGBCameraRecorder.CameraMode.PREVIEW_ONLY
            }

        cameraModeSelector.setOnModeChangeListener { newMode ->
            lifecycleScope.launch {
                switchCameraMode(newMode, enableSamsungOptimizations)
            }
        }

        cameraModeSelector.setMode(mode)
    }

    private suspend fun switchCameraMode(
        newMode: RGBCameraRecorder.CameraMode,
        enableSamsungOptimizations: Boolean = true
    ) {
        try {
            val success = rgbCameraRecorder?.switchMode(newMode) ?: false
            if (success) {
                // Configure Stage3/Level3 processing for RAW mode on Samsung devices
                if (newMode == RGBCameraRecorder.CameraMode.RAW_50MP && enableSamsungOptimizations) {
                    if (SamsungDeviceCompatibility.isStage3Compatible()) {
                        rgbCameraRecorder?.configureStage3Processing(true)
                        Toast.makeText(this, "RAW Mode: Samsung Stage3/Level3 DNG Enabled", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(
                            this,
                            "RAW Mode: Standard DNG (${SamsungDeviceCompatibility.getDeviceInfo()})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(this, "Switched to ${newMode.displayName}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                this,
                "Failed to switch to ${newMode.displayName}",
                Toast.LENGTH_SHORT
            ).show()
        }
    } catch (e: Exception)
    {
        Toast.makeText(this, "Mode switch error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

override fun onDestroy() {
    super.onDestroy()
    rgbCameraRecorder?.cleanup()
}
}
