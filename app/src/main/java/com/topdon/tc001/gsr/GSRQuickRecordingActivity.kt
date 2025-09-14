package com.topdon.tc001.gsr

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
import com.topdon.lib.core.ktbase.BaseBindingActivity
import com.topdon.tc001.controller.RecordingController
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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

    // Core recording components - this is the main integration point
    private lateinit var recordingController: RecordingController
    private var currentSessionDirectory: String? = null
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initRecordingController()
        setupUI()
        checkPermissions()
    }

    private fun initRecordingController() {
        // Initialize the main RecordingController with GSR integration
        recordingController = RecordingController(this, this)

        // Initialize sensors including GSR
        lifecycleScope.launch {
            val success = recordingController.initializeSensors()
            runOnUiThread {
                if (success) {
                    binding.statusText.text = "Recording system initialized successfully"
                    binding.startRecordingButton.isEnabled = true

                    val availableSensors = recordingController.getAvailableSensors()
                    val gsrSensor =
                        availableSensors.find { it.sensorType.contains("GSR", ignoreCase = true) }
                    if (gsrSensor != null) {
                        binding.gsrStatusText.text = "GSR Sensor: ${gsrSensor.sensorId} - Ready"
                        binding.gsrStatusText.setTextColor(
                            ContextCompat.getColor(
                                this@GSRQuickRecordingActivity,
                                R.color.gsr_pulse_color
                            )
                        )
                    } else {
                        binding.gsrStatusText.text = "GSR Sensor: Not Available"
                        binding.gsrStatusText.setTextColor(
                            ContextCompat.getColor(
                                this@GSRQuickRecordingActivity,
                                R.color.gsr_recording_active
                            ),
                        )
                    }
                } else {
                    binding.statusText.text = "Failed to initialize recording system"
                    binding.startRecordingButton.isEnabled = false
                }
            }
        }

        // Monitor recording state
        lifecycleScope.launch {
            recordingController.recordingStateFlow.collect { state ->
                runOnUiThread {
                    when (state) {
                        com.topdon.tc001.controller.RecordingState.RECORDING -> {
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

                        com.topdon.tc001.controller.RecordingState.STOPPED -> {
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

        // Monitor sensor status
        lifecycleScope.launch {
            recordingController.sensorStatusFlow.collect { statusList ->
                runOnUiThread {
                    val gsrStatus =
                        statusList.find { it.sensorType.contains("GSR", ignoreCase = true) }
                    if (gsrStatus != null) {
                        binding.sensorDataText.text =
                            buildString {
                                append("GSR Sensor Status:\n")
                                append("Samples: ${gsrStatus.samplesRecorded}\n")
                                append("Data Rate: ${"%.1f".format(gsrStatus.currentDataRate)} Hz\n")
                                append("Storage: ${"%.2f".format(gsrStatus.storageUsedMB)} MB\n")
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
            // Navigate to session manager
            try {
                SessionManagerActivity.startActivity(this)
            } catch (e: Exception) {
                // Fallback to GSR Demo if SessionManagerActivity not available
                GSRDemoActivity.start(this)
            }
        }

        binding.gsrSettingsButton.setOnClickListener {
            // Navigate to GSR settings
            try {
                GSRSettingsActivity.startActivity(this)
            } catch (e: Exception) {
                // Fallback to Multi-modal recording if GSRSettingsActivity not available
                MultiModalRecordingActivity.start(this)
            }
        }
    }

    private fun checkPermissions() {
        // Check if all required permissions are granted
        val missingPermissions =
            REQUIRED_PERMISSIONS.filter {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }

        if (missingPermissions.isNotEmpty()) {
            requestPermissions(missingPermissions.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }

    private fun startRecording() {
        lifecycleScope.launch {
            try {
                // Create session directory
                val timestamp =
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val sessionDir = File(filesDir, "gsr_sessions/session_$timestamp")
                sessionDir.mkdirs()

                currentSessionDirectory = sessionDir.absolutePath
                Log.i(TAG, "Starting recording in: $currentSessionDirectory")

                val success = recordingController.startRecording(currentSessionDirectory!!)

                runOnUiThread {
                    if (success) {
                        Toast.makeText(
                            this@GSRQuickRecordingActivity,
                            "Recording started",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@GSRQuickRecordingActivity,
                            "Failed to start recording",
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

                        // Show session info
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
                    timestampNs = System.nanoTime(),
                    metadata = mapOf("source" to "quick_recording_ui"),
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

        if (requestCode == REQUEST_PERMISSIONS) {
            val allGranted =
                grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                Toast.makeText(
                    this,
                    "Some permissions were denied. GSR functionality may be limited.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up recording controller
        lifecycleScope.launch {
            recordingController.cleanup()
        }
    }
}
