package mpdc4gsr.test

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.databinding.ActivityRawCaptureTestBinding
import kotlinx.coroutines.launch
import mpdc4gsr.camera.core.SamsungDeviceCompatibility
import mpdc4gsr.data.SessionMetadata
import mpdc4gsr.sensors.RgbCameraRecorder

class RAWCaptureTestActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "RAWCaptureTest"
    }
    
    private lateinit var binding: ActivityRawCaptureTestBinding
    private var rgbCameraRecorder: RgbCameraRecorder? = null
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRawCaptureTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        initializeCamera()
        updateDeviceCompatibilityInfo()
    }

    private fun setupUI() {
        setupSpinner()
        setupSwitchListeners()
        setupRecordingButton()
        
        // Update status with Stage 3 capability
        val isStage3Compatible = SamsungDeviceCompatibility.isStage3Compatible()
        val deviceInfo = SamsungDeviceCompatibility.getDeviceInfo()
        
        val statusText = if (isStage3Compatible) {
            "Ready: Stage 3/Level 3 DNG capture available on $deviceInfo"
        } else {
            "Device: $deviceInfo (Stage 3/Level 3 not supported - using standard RAW)"
        }
        
        binding.statusText.text = statusText
    }

    private fun setupSpinner() {
        binding.rawFrameRateSpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                listOf("12 fps (Optimized)", "15 fps", "10 fps", "5 fps"),
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        binding.rawFrameRateSpinner.setSelection(0)
    }

    private fun setupSwitchListeners() {
        binding.enableRawCaptureSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.rawFrameRateSpinner.isEnabled = isChecked
            binding.rawFrameRateSpinner.alpha = if (isChecked) 1.0f else 0.5f
            
            val mode = if (isChecked && SamsungDeviceCompatibility.isStage3Compatible()) {
                "Stage 3/Level 3 DNG"
            } else if (isChecked) {
                "Standard RAW"
            } else {
                "Video Only"
            }
            
            Log.i(TAG, "RAW capture mode changed to: $mode")
        }
        
        // Enable RAW capture by default for testing
        binding.enableRawCaptureSwitch.isChecked = true
    }
    
    private fun setupRecordingButton() {
        binding.startStopButton.setOnClickListener {
            if (!isRecording) {
                startRecording()
            } else {
                stopRecording()
            }
        }
    }

    private fun initializeCamera() {
        try {
            // Initialize camera with PreviewView for live camera preview
            val previewView = binding.previewView
            
            rgbCameraRecorder = RgbCameraRecorder(
                context = this,
                lifecycleOwner = this,
                previewView = previewView,
                useFrontCamera = false
            )
            
            lifecycleScope.launch {
                val initialized = rgbCameraRecorder?.initialize() ?: false
                if (initialized) {
                    Log.i(TAG, "RGB camera initialized for Stage 3 DNG testing")
                    observeCameraStatus()
                    runOnUiThread {
                        binding.statusText.text = binding.statusText.text.toString() + " - Camera Ready"
                    }
                } else {
                    Log.w(TAG, "Camera initialization failed")
                    runOnUiThread {
                        Toast.makeText(this@RAWCaptureTestActivity, "Camera initialization failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing camera", e)
            Toast.makeText(this, "Camera setup error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeCameraStatus() {
        lifecycleScope.launch {
            rgbCameraRecorder?.statusFlow?.collect { status ->
                runOnUiThread {
                    binding.cameraStatusText.text = "Camera: ${status.displayText}"
                }
            }
        }
    }
        val isStage3Compatible = SamsungDeviceCompatibility.isStage3Compatible()
        val deviceInfo = SamsungDeviceCompatibility.getDeviceInfo()
        
        val features = mutableListOf<String>()
        if (binding.enableVideoSwitch.isChecked) {
            features.add(if (binding.enable4kSwitch.isChecked) "4K Video" else "1080p Video")
        }
        if (binding.enableRawCaptureSwitch.isChecked) {
            val rawType = if (isStage3Compatible) "Stage 3/Level 3 DNG" else "Standard RAW"
            features.add("$rawType (${binding.rawFrameRateSpinner.selectedItem})")
        }
        
        val newFeaturesText = "🎯 Test Mode: ${features.joinToString(" + ")}"
        binding.tvFeaturesTitle.text = newFeaturesText
        
        // Update features list with Stage 3 information
        val featuresList = buildString {
            append("• Stage 3/Level 3 DNG Recording: ${if (isStage3Compatible) "SUPPORTED ✓" else "Not Available (fallback to standard)"}\n")
            append("• Device: $deviceInfo\n")
            append("• Frame throttling at ~12fps for optimal I/O performance\n")
            append("• Proper DNG metadata with Stage 3/Level 3 processing pipeline\n")
            append("• Camera2 interop for advanced sensor configuration\n")
            append("• Synchronized timestamps across all modalities")
        }
        
        binding.tvFeaturesList.text = featuresList
    }

    private fun startRecording() {
        lifecycleScope.launch {
            try {
                isRecording = true
                binding.startStopButton.text = "⏹️ Stop Recording"
                binding.startStopButton.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark))
                
                val sessionId = "stage3_test_${System.currentTimeMillis()}"
                val sessionDir = "stage3_dng_test"
                
                val metadata = SessionMetadata(
                    sessionId = sessionId,
                    participantId = "stage3_test_user",
                    startTime = System.currentTimeMillis(),
                    deviceInfo = SamsungDeviceCompatibility.getDeviceInfo()
                )
                
                val success = rgbCameraRecorder?.startRecording(sessionDir, metadata) ?: false
                
                if (success) {
                    val mode = if (binding.enableRawCaptureSwitch.isChecked) {
                        if (SamsungDeviceCompatibility.isStage3Compatible()) "Stage 3/Level 3 DNG" else "Standard RAW"
                    } else "Video Only"
                    
                    binding.statusText.text = "🎬 Recording: $mode | Session: $sessionId"
                    Log.i(TAG, "Recording started with $mode processing")
                } else {
                    throw Exception("Failed to start recording")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e)
                isRecording = false
                binding.startStopButton.text = "▶️ Start Multi-Modal Recording"
                binding.startStopButton.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
                Toast.makeText(this@RAWCaptureTestActivity, "Recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun stopRecording() {
        lifecycleScope.launch {
            try {
                val success = rgbCameraRecorder?.stopRecording() ?: false
                
                isRecording = false
                binding.startStopButton.text = "▶️ Start Multi-Modal Recording"
                binding.startStopButton.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
                
                if (success) {
                    binding.statusText.text = "✅ Recording stopped successfully - check files for Stage 3/Level 3 DNG output"
                    Log.i(TAG, "Recording stopped successfully")
                } else {
                    binding.statusText.text = "⚠️ Recording stopped with warnings"
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording", e)
                binding.statusText.text = "❌ Error stopping recording: ${e.message}"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            try {
                rgbCameraRecorder?.cleanup()
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }
}
