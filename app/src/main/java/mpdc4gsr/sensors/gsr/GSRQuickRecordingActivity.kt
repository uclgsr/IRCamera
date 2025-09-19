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
import com.mpdc4gsr.lib.core.ktbase.BaseBindingActivity
import mpdc4gsr.controller.RecordingController
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        
        recordingController = RecordingController(this, this)

        
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
                
                val storageStatus = recordingController.getStorageStatus()
                if (storageStatus.isLowStorage) {
                    runOnUiThread {
                        Toast.makeText(
                            this@GSRQuickRecordingActivity,
                            "Insufficient storage: ${storageStatus.formattedAvailable} available. Need at least 500MB.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                if (storageStatus.shouldWarn) {
                    runOnUiThread {
                        Toast.makeText(
                            this@GSRQuickRecordingActivity,
                            "Storage warning: Only ${storageStatus.formattedAvailable} available",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                Log.i(TAG, "Starting recording with improved session management")

                
                val success = recordingController.startRecording(
                    participantId = "QuickRecording",
                    studyName = "GSR Quick Test"
                )

                runOnUiThread {
                    if (success) {
                        val sessionDir = recordingController.getCurrentSessionDirectory()
                        currentSessionDirectory = sessionDir?.rootDir?.absolutePath

                        Toast.makeText(
                            this@GSRQuickRecordingActivity,
                            "Recording started",
                            Toast.LENGTH_SHORT
                        ).show()

                        
                        currentSessionDirectory?.let { sessionDirPath ->
                            binding.sessionInfoText.text = "Session saved to:\n$sessionDirPath"
                        }
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
