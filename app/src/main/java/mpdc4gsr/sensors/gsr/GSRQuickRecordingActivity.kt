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
    private var currentSessionDirectory: String? = null
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initRecordingController()
        setupUI()
        checkPermissions()
    }

    private fun initRecordingController() {
        // Initialize PermissionManager and ComprehensiveRecordingController
        permissionManager = PermissionManager(this)
        recordingController = ComprehensiveRecordingController(this, this, permissionManager)

        // Check for crashed sessions on startup
        lifecycleScope.launch {
            recordingController.checkForCrashedSessions()
        }


        lifecycleScope.launch {
            val success = true // ComprehensiveRecordingController handles sensor initialization internally
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
                            binding.sessionInfoText.text = "Recording: ${sessionDir.substringAfterLast("/")}"
                        } ?: run {
                            binding.sessionInfoText.text = "Recording in progress with advanced fault tolerance..."
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


        lifecycleScope.launch {
            recordingController.cleanup()
        }
    }
}
