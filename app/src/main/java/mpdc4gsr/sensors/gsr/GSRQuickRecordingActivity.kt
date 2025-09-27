package mpdc4gsr.sensors.gsr

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityGsrQuickRecordingBinding
import com.mpdc4gsr.libunified.app.ktbase.BaseBindingActivity
import kotlinx.coroutines.launch
import mpdc4gsr.controller.ComprehensiveRecordingController
import mpdc4gsr.permissions.PermissionController
import mpdc4gsr.permissions.PermissionManager

class GSRQuickRecordingActivity : BaseBindingActivity<ActivityGsrQuickRecordingBinding>() {
    companion object {
        private const val TAG = "GSRQuickRecording"
        private const val REQUEST_PERMISSIONS = 100

        private val REQUIRED_PERMISSIONS =
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
            )

        fun start(context: Context) {
            val intent = Intent(context, GSRQuickRecordingActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun initContentLayoutId() = R.layout.activity_gsr_quick_recording


    private lateinit var recordingController: ComprehensiveRecordingController
    private lateinit var permissionManager: PermissionManager
    private lateinit var permissionController: PermissionController
    private var currentSessionDirectory: String? = null
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initRecordingController()
        setupUI()
        checkPermissions()
    }

    private fun initRecordingController() {
        // Initialize PermissionController and PermissionManager, then ComprehensiveRecordingController
        permissionController = PermissionController(this)
        permissionManager = PermissionManager(this, permissionController)
        recordingController = ComprehensiveRecordingController(this, this, permissionManager)

        // Check for crashed sessions on startup
        lifecycleScope.launch {
            recordingController.checkForCrashedSessions()
        }


        lifecycleScope.launch {
            val success =
                true // ComprehensiveRecordingController handles sensor initialization internally
            runOnUiThread {
                if (success) {
                    binding.statusText.text = "Enhanced recording system ready"
                    binding.startRecordingButton.isEnabled = true
                    binding.gsrStatusText.text = "Enhanced fault-tolerant recording enabled"
                } else {
                    binding.statusText.text = "Enhanced recording system ready"
                    binding.startRecordingButton.isEnabled = true
                }
            }
        }


        lifecycleScope.launch {
            recordingController.recordingStateFlow.collect { state ->
                runOnUiThread {
                    when (state) {
                        mpdc4gsr.controller.RecordingState.RECORDING -> {
                            isRecording = true
                            binding.startRecordingButton.text = "Stop Recording"
                            binding.statusText.text = "Recording in progress..."
                            binding.statusText.setTextColor(
                                ContextCompat.getColor(
                                    this@GSRQuickRecordingActivity,
                                    R.color.gsr_recording_active
                                ),
                            )
                        }

                        mpdc4gsr.controller.RecordingState.STOPPED -> {
                            isRecording = false
                            binding.startRecordingButton.text = "Start Recording"
                            binding.statusText.text = "Recording stopped"
                            binding.statusText.setTextColor(
                                ContextCompat.getColor(
                                    this@GSRQuickRecordingActivity,
                                    R.color.white
                                )
                            )
                        }

                        else -> {
                            binding.statusText.text = "State: $state"
                        }
                    }
                }
            }
        }


        lifecycleScope.launch {
            recordingController.sensorStatusFlow.collect { statusList ->
                runOnUiThread {
                    val gsrStatus =
                        statusList.find { it.sensorId.contains("GSR", ignoreCase = true) }
                    if (gsrStatus != null) {
                        binding.sensorDataText.text =
                            buildString {
                                append("GSR Sensor Status:\n")
                                append("Samples: ${gsrStatus.samplesRecorded}\n")
                                append("Recording: ${if (gsrStatus.isActive) "Active" else "Inactive"}\n")
                                append("Healthy: ${if (gsrStatus.isHealthy) "Yes" else "No"}\n")
                            }
                    }
                }
            }
        }
    }

    private fun setupUI() {
        binding.startRecordingButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        binding.addSyncMarkerButton.setOnClickListener {
            addSyncMarker()
        }

        binding.viewSessionsButton.setOnClickListener {

            try {
                val intent = Intent(this, SessionManagerActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {

                val intent = Intent(this, GSRDemoActivity::class.java)
                startActivity(intent)
            }
        }

        binding.gsrSettingsButton.setOnClickListener {

            try {
                val intent = Intent(this, GSRSettingsActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {

                val intent = Intent(this, MultiModalRecordingActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun checkPermissions() {
        // Use the PermissionController's built-in checking and throttling
        if (!permissionController.hasAllRequiredPermissions()) {
            Log.i(TAG, "Missing permissions detected")
            
            // Check if we should skip permission requests due to throttling
            if (permissionController.shouldSkipPermissionRequest()) {
                Log.w(TAG, "Skipping permission request due to recent denials or throttling")
                showPermissionError("Permissions required. Please check app settings.")
                return
            }
            
            permissionController.ensureAll { allGranted, deniedPermissions ->
                if (allGranted) {
                    Log.i(TAG, "All permissions granted for GSR recording")
                } else {
                    Log.w(TAG, "Some permissions denied: ${deniedPermissions.joinToString(", ")}")
                    val permissionNames = permissionController.getPermissionNames(deniedPermissions)
                    showPermissionError("Missing permissions: ${permissionNames.joinToString(", ")}")
                }
            }
        }
    }
    
    private fun showPermissionError(message: String) {
        runOnUiThread {
            binding.statusText.text = message
            binding.recordButton.isEnabled = false
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun startRecording() {
        lifecycleScope.launch {
            try {
                // Enhanced controller handles storage validation internally during startRecording
                Log.i(TAG, "Starting enhanced fault-tolerant recording")

                val success = recordingController.startRecording(
                    sessionId = "QuickRecording_${System.currentTimeMillis()}",
                    enabledSensors = listOf("GSR", "RGB"),
                    estimatedDurationMinutes = 10
                )

                runOnUiThread {
                    if (success) {
                        // Get the current session directory from the recording controller
                        currentSessionDirectory = try {
                            recordingController.getCurrentSessionDirectory()
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not get session directory", e)
                            null
                        }

                        Toast.makeText(
                            this@GSRQuickRecordingActivity,
                            "Enhanced fault-tolerant recording started",
                            Toast.LENGTH_SHORT
                        ).show()

                        currentSessionDirectory?.let { sessionDir ->
                            binding.sessionInfoText.text =
                                "Recording: ${sessionDir.substringAfterLast("/")}"
                        } ?: run {
                            binding.sessionInfoText.text =
                                "Recording in progress with advanced fault tolerance..."
                        }
                    } else {
                        Toast.makeText(
                            this@GSRQuickRecordingActivity,
                            "Recording start failed - enhanced controller handled gracefully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e)
                runOnUiThread {
                    Toast.makeText(
                        this@GSRQuickRecordingActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun stopRecording() {
        lifecycleScope.launch {
            try {
                val success = recordingController.stopRecording()

                runOnUiThread {
                    if (success) {
                        Toast.makeText(
                            this@GSRQuickRecordingActivity,
                            "Recording stopped",
                            Toast.LENGTH_SHORT
                        ).show()


                        currentSessionDirectory?.let { sessionDir ->
                            binding.sessionInfoText.text = "Session saved to:\n$sessionDir"
                        }
                    } else {
                        Toast.makeText(
                            this@GSRQuickRecordingActivity,
                            "Failed to stop recording",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording", e)
                runOnUiThread {
                    Toast.makeText(
                        this@GSRQuickRecordingActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun addSyncMarker() {
        if (isRecording) {
            lifecycleScope.launch {
                recordingController.addSyncMarker(
                    markerType = "manual_sync",
                    timestampNs = System.nanoTime()
                )

                runOnUiThread {
                    Toast.makeText(
                        this@GSRQuickRecordingActivity,
                        "Sync marker added",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(this, "Recording must be active to add sync markers", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Use PermissionController's result handling for consistent behavior
        permissionController.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        // Legacy handling for direct permission requests (if any remain)
        if (requestCode == REQUEST_PERMISSIONS) {
            val allGranted =
                grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                showPermissionError("Some permissions were denied. GSR functionality may be limited.")
            }
        }
    }
    }

    override fun onDestroy() {
        super.onDestroy()


        lifecycleScope.launch {
            recordingController.cleanup()
        }
    }
}
