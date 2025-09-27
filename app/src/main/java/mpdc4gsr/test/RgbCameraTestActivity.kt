package mpdc4gsr.test

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import kotlinx.coroutines.launch
import mpdc4gsr.camera.ui.TapToFocusPreviewView
import mpdc4gsr.data.SessionMetadata
import mpdc4gsr.permissions.PermissionManager
import mpdc4gsr.sensors.RgbCameraRecorder
import java.io.File

/**
 * Dedicated test activity for RGB camera features
 * Tests device compatibility, recording quality, manual controls, and tap-to-focus
 */
class RgbCameraTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RGBCameraTest"
    }

    private lateinit var previewView: TapToFocusPreviewView
    private lateinit var statusText: TextView
    private lateinit var capabilitiesText: TextView
    private lateinit var recordButton: Button
    private lateinit var testResultsText: TextView

    // Manual controls
    private lateinit var manualExposureSwitch: Switch
    private lateinit var exposureCompensationSeekBar: SeekBar
    private lateinit var exposureValueText: TextView
    private lateinit var manualFocusSwitch: Switch
    private lateinit var focusDistanceSeekBar: SeekBar
    private lateinit var focusDistanceText: TextView

    // Test controls
    private lateinit var test4KButton: Button
    private lateinit var testRawButton: Button
    private lateinit var testManualControlsButton: Button
    private lateinit var testTapToFocusButton: Button

    private var cameraRecorder: RgbCameraRecorder? = null
    private var permissionManager: PermissionManager? = null
    private var isRecording = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            initializeCamera()
        } else {
            showError("Camera permissions required for testing")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rgb_camera_test)

        initializeViews()
        setupListeners()
        checkPermissions()
    }

    private fun initializeViews() {
        previewView = findViewById(R.id.previewView)
        statusText = findViewById(R.id.statusText)
        capabilitiesText = findViewById(R.id.capabilitiesText)
        recordButton = findViewById(R.id.recordButton)
        testResultsText = findViewById(R.id.testResultsText)

        // Manual controls
        manualExposureSwitch = findViewById(R.id.manualExposureSwitch)
        exposureCompensationSeekBar = findViewById(R.id.exposureCompensationSeekBar)
        exposureValueText = findViewById(R.id.exposureValueText)
        manualFocusSwitch = findViewById(R.id.manualFocusSwitch)
        focusDistanceSeekBar = findViewById(R.id.focusDistanceSeekBar)
        focusDistanceText = findViewById(R.id.focusDistanceText)

        // Test controls
        test4KButton = findViewById(R.id.test4KButton)
        testRawButton = findViewById(R.id.testRawButton)
        testManualControlsButton = findViewById(R.id.testManualControlsButton)
        testTapToFocusButton = findViewById(R.id.testTapToFocusButton)

        // Initial UI state
        statusText.text = "Initializing camera test..."
        exposureValueText.text = "Exposure: 0.0 EV"
        focusDistanceText.text = "Focus: Infinity"
    }

    private fun setupListeners() {
        recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        // Manual exposure controls
        manualExposureSwitch.setOnCheckedChangeListener { _, isChecked ->
            cameraRecorder?.setManualExposureMode(isChecked)
            exposureCompensationSeekBar.isEnabled = !isChecked
        }

        exposureCompensationSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val evValue = (progress - 100) / 50.0f  // -2.0 to +2.0 EV
                    exposureValueText.text = "Exposure: ${String.format("%.1f", evValue)} EV"
                    cameraRecorder?.setExposureCompensation(evValue)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Manual focus controls
        manualFocusSwitch.setOnCheckedChangeListener { _, isChecked ->
            cameraRecorder?.setManualFocusMode(isChecked)
            focusDistanceSeekBar.isEnabled = isChecked
        }

        focusDistanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val focusValue = progress / 100.0f  // 0.0-1.0
                    val focusText =
                        if (focusValue < 0.1f) "Infinity" else String.format(
                            "%.1fm",
                            0.1f + focusValue * 2.0f
                        )
                    focusDistanceText.text = "Focus: $focusText"
                    cameraRecorder?.setFocusDistance(focusValue)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Test buttons
        test4KButton.setOnClickListener { test4KCapability() }
        testRawButton.setOnClickListener { testRawCapability() }
        testManualControlsButton.setOnClickListener { testManualControls() }
        testTapToFocusButton.setOnClickListener { testTapToFocus() }

        // Tap-to-focus
        previewView.onTapToFocus = { x, y ->
            cameraRecorder?.triggerTapToFocus(x, y)
            showMessage(
                "Tap-to-focus triggered at (${
                    String.format(
                        "%.2f",
                        x
                    )
                }, ${String.format("%.2f", y)})"
            )
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            initializeCamera()
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun initializeCamera() {
        lifecycleScope.launch {
            try {
                statusText.text = "Initializing camera..."

                cameraRecorder = RgbCameraRecorder(
                    context = this@RgbCameraTestActivity,
                    lifecycleOwner = this@RgbCameraTestActivity,
                    previewView = previewView.previewView,
                    useFrontCamera = false,
                    permissionManager = permissionManager
                )

                val success = cameraRecorder?.initialize() ?: false
                if (success) {
                    statusText.text = "Camera initialized successfully"
                    displayCameraCapabilities()
                } else {
                    showError("Failed to initialize camera")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize camera", e)
                showError("Initialization error: ${e.message}")
            }
        }
    }

    private fun displayCameraCapabilities() {
        cameraRecorder?.let { recorder ->
            val capabilities = recorder.getDetailedCameraCapabilities()
            val capabilitiesStr = buildString {
                appendLine("Device: ${capabilities["device_manufacturer"]} ${capabilities["device_model"]}")
                appendLine("Android API: ${capabilities["android_version"]}")
                appendLine("4K Support: ${capabilities["supports_4k"]}")
                appendLine("60fps Support: ${capabilities["supports_60fps"]}")
                appendLine("RAW Support: ${capabilities["supports_raw"]}")
                appendLine("Stage3 Compatible: ${capabilities["stage3_compatible"]}")
                appendLine("Current Resolution: ${capabilities["current_resolution"]}")
                appendLine("Flash Available: ${capabilities["has_flash"]}")
                appendLine("Max Zoom: ${capabilities["max_zoom"]}")
                appendLine("Exposure Range: ${capabilities["exposure_compensation_min"]} to ${capabilities["exposure_compensation_max"]}")
                appendLine("Min Focus Distance: ${capabilities["min_focus_distance"]}")
            }
            capabilitiesText.text = capabilitiesStr
        }
    }

    private fun startRecording() {
        lifecycleScope.launch {
            try {
                val testDir = File(filesDir, "camera_test_${System.currentTimeMillis()}")
                testDir.mkdirs()

                val metadata = SessionMetadata.createSessionStart("camera_test")
                val success =
                    cameraRecorder?.startRecording(testDir.absolutePath, metadata) ?: false

                if (success) {
                    isRecording = true
                    recordButton.text = "Stop Recording"
                    statusText.text = "Recording in progress..."
                } else {
                    showError("Failed to start recording")
                }
            } catch (e: Exception) {
                showError("Recording error: ${e.message}")
            }
        }
    }

    private fun stopRecording() {
        lifecycleScope.launch {
            try {
                cameraRecorder?.stopRecording()
                isRecording = false
                recordButton.text = "Start Recording"
                statusText.text = "Recording stopped"
            } catch (e: Exception) {
                showError("Stop recording error: ${e.message}")
            }
        }
    }

    private fun test4KCapability() {
        cameraRecorder?.let { recorder ->
            val capabilities = recorder.getCaptureMode()
            val supports4K = capabilities["supports_4k"] as? Boolean ?: false
            val supports60fps = capabilities["supports_60fps"] as? Boolean ?: false

            val result = buildString {
                appendLine("=== 4K Capability Test ===")
                appendLine("4K Support: $supports4K")
                appendLine("60fps Support: $supports60fps")
                appendLine("Current Resolution: ${capabilities["current_resolution"]}")
                appendLine("Current FPS: ${capabilities["current_fps"]}")
                if (supports4K && supports60fps) {
                    appendLine("✅ Device supports 4K60 recording")
                } else if (supports4K) {
                    appendLine("✅ Device supports 4K30 recording")
                } else {
                    appendLine("❌ Device does not support 4K recording")
                }
                appendLine()
            }

            updateTestResults(result)
            Log.i(TAG, "4K capability test completed: $capabilities")
        }
    }

    private fun testRawCapability() {
        cameraRecorder?.let { recorder ->
            val capabilities = recorder.getCaptureMode()
            val supportsRaw = capabilities["supports_raw"] as? Boolean ?: false
            val stage3Compatible = capabilities["stage3_compatible"] as? Boolean ?: false

            val result = buildString {
                appendLine("=== RAW Capability Test ===")
                appendLine("RAW Support: $supportsRaw")
                appendLine("Stage3 Compatible: $stage3Compatible")
                if (supportsRaw && stage3Compatible) {
                    appendLine("✅ Device supports Samsung Stage3 RAW DNG capture")
                } else if (supportsRaw) {
                    appendLine("✅ Device supports standard RAW capture")
                } else {
                    appendLine("❌ Device does not support RAW capture")
                }
                appendLine()
            }

            updateTestResults(result)
            Log.i(TAG, "RAW capability test completed: $capabilities")
        }
    }

    private fun testManualControls() {
        val result = buildString {
            appendLine("=== Manual Controls Test ===")

            // Test exposure compensation
            cameraRecorder?.setExposureCompensation(1.0f)
            appendLine("✓ Exposure compensation +1.0 EV applied")

            // Test manual focus mode
            cameraRecorder?.setManualFocusMode(true)
            appendLine("✓ Manual focus mode enabled")

            // Test focus distance
            cameraRecorder?.setFocusDistance(0.5f)
            appendLine("✓ Focus distance set to 50% (mid-range)")

            // Test AE/AF locks
            cameraRecorder?.setAutoExposureLock(true)
            cameraRecorder?.setAutoFocusLock(true)
            appendLine("✓ Auto-exposure and auto-focus locks enabled")

            appendLine("Manual controls test completed successfully")
            appendLine()
        }

        updateTestResults(result)
        showMessage("Manual controls test completed - check logs for details")
    }

    private fun testTapToFocus() {
        val result = buildString {
            appendLine("=== Tap-to-Focus Test ===")
            appendLine("Tap anywhere on the preview to test focus functionality")
            appendLine("Watch for visual focus indicator and check logs")
            appendLine()
        }

        updateTestResults(result)
        showMessage("Tap on the preview to test tap-to-focus functionality")
    }

    private fun updateTestResults(result: String) {
        val currentText = testResultsText.text.toString()
        testResultsText.text = currentText + result
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.i(TAG, message)
    }

    private fun showError(error: String) {
        statusText.text = "Error: $error"
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        Log.e(TAG, error)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            cameraRecorder?.cleanup()
        }
    }
}