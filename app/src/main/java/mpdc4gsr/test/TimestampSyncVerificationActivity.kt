package mpdc4gsr.test

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mpdc4gsr.sensors.TimeSynchronizationService
import mpdc4gsr.sensors.TimestampManager
import mpdc4gsr.sensors.unified.ShimmerDeviceManager
import java.io.File
import kotlin.math.abs

class TimestampSyncVerificationActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "TimestampSyncVerification"
        private const val SYNC_TOLERANCE_MS = 5L
        
        private val REQUIRED_PERMISSIONS =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
    }

    private lateinit var resultTextView: TextView
    private lateinit var startTestButton: Button
    private lateinit var scanDevicesButton: Button
    private lateinit var timeSyncService: TimeSynchronizationService
    private var shimmerDeviceManager: ShimmerDeviceManager? = null
    private var isScanning = false
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Log.i(TAG, "All required permissions granted")
            initializeShimmerManager()
        } else {
            Log.w(TAG, "Some permissions were denied")
            appendResultText("Bluetooth permissions required for device scanning\n")
            showPermissionError()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timestamp_sync_verification)

        resultTextView = findViewById(R.id.result_text_view)
        startTestButton = findViewById(R.id.start_test_button)
        scanDevicesButton = findViewById(R.id.scan_devices_button)

        timeSyncService = TimeSynchronizationService()

        startTestButton.setOnClickListener {
            lifecycleScope.launch {
                runTimestampSyncVerificationTest()
            }
        }
        
        scanDevicesButton.setOnClickListener {
            if (isScanning) {
                stopDeviceScanning()
            } else {
                startDeviceScanning()
            }
        }

        updateResultText("Timestamp Synchronization Verification\nReady to test...")
        checkPermissionsAndInitialize()
    }

    private suspend fun runTimestampSyncVerificationTest() {
        updateResultText("Starting timestamp synchronization verification test...\n")

        try {
            val tempDir = File(cacheDir, "timestamp_test_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val sessionRef = timeSyncService.initializeSession(tempDir.absolutePath)
            appendResultText("Session initialized with reference: ${sessionRef.sessionStartSystemMs}ms\n")

            appendResultText("Creating SessionSync markers for multi-modal alignment verification...\n")

            val syncEvents = mutableListOf<SyncTestEvent>()

            syncEvents.add(simulateRGBFrameCapture())
            delay(2)
            syncEvents.add(simulateGSRSample())
            delay(1)
            syncEvents.add(simulateThermalFrame())
            delay(3)
            syncEvents.add(simulateSharpEvent("hand_clap"))

            appendResultText("\nAnalyzing timestamp alignment:\n")

            val alignmentResults = analyzeTimestampAlignment(syncEvents)
            appendResultText(alignmentResults)

            val finalResult = if (isTimestampAlignmentValid(syncEvents)) {
                "PASS: All sensor timestamps are synchronized within ${SYNC_TOLERANCE_MS}ms tolerance"
            } else {
                "FAIL: Timestamp synchronization exceeds tolerance"
            }

            appendResultText("\n$finalResult\n")

            tempDir.deleteRecursively()

        } catch (e: Exception) {
            appendResultText("Test error: ${e.message}\n")
            Log.e(TAG, "Timestamp sync verification test failed", e)
        }
    }

    private suspend fun simulateRGBFrameCapture(): SyncTestEvent {
        val timestamp = TimestampManager.createTimestampRecord()
        timeSyncService.logSyncEvent(
            "RGB_FRAME_TEST", mapOf(
                "sensor" to "rgb_camera",
                "test_event" to "frame_capture",
                "expected_sync" to "true"
            )
        )

        appendResultText("RGB: Frame captured at ${timestamp.systemTimeMs}ms\n")
        return SyncTestEvent("RGB_CAMERA", timestamp.systemNanos, timestamp.systemTimeMs)
    }

    private suspend fun simulateGSRSample(): SyncTestEvent {
        val timestamp = TimestampManager.createTimestampRecord()
        timeSyncService.logSyncEvent(
            "GSR_SAMPLE_TEST", mapOf(
                "sensor" to "gsr_shimmer",
                "test_event" to "conductance_reading",
                "expected_sync" to "true"
            )
        )

        appendResultText("GSR: Sample recorded at ${timestamp.systemTimeMs}ms\n")
        return SyncTestEvent("GSR_SHIMMER", timestamp.systemNanos, timestamp.systemTimeMs)
    }

    private suspend fun simulateThermalFrame(): SyncTestEvent {
        val timestamp = TimestampManager.createTimestampRecord()
        timeSyncService.logSyncEvent(
            "THERMAL_FRAME_TEST", mapOf(
                "sensor" to "thermal_topdon",
                "test_event" to "temperature_frame",
                "expected_sync" to "true"
            )
        )

        appendResultText("THERMAL: Frame processed at ${timestamp.systemTimeMs}ms\n")
        return SyncTestEvent("THERMAL_TOPDON", timestamp.systemNanos, timestamp.systemTimeMs)
    }

    private suspend fun simulateSharpEvent(eventType: String): SyncTestEvent {
        val timestamp = TimestampManager.createTimestampRecord()
        timeSyncService.logSyncEvent(
            "SHARP_EVENT_TEST", mapOf(
                "event_type" to eventType,
                "test_event" to "multi_modal_trigger",
                "expected_sync" to "true"
            )
        )

        appendResultText("EVENT: $eventType detected at ${timestamp.systemTimeMs}ms\n")
        return SyncTestEvent("SHARP_EVENT", timestamp.systemNanos, timestamp.systemTimeMs)
    }

    private fun analyzeTimestampAlignment(events: List<SyncTestEvent>): String {
        val result = StringBuilder()
        result.append("Timestamp Alignment Analysis:\n")

        for (i in events.indices) {
            for (j in i + 1 until events.size) {
                val event1 = events[i]
                val event2 = events[j]

                val diffMs = abs(event1.wallClockMs - event2.wallClockMs)
                val diffNs = abs(event1.timestampNs - event2.timestampNs) / 1_000_000

                result.append("${event1.sensorType} ↔ ${event2.sensorType}: ")
                result.append("${diffMs}ms wall-clock, ${diffNs}ms monotonic\n")
            }
        }

        return result.toString()
    }

    private fun isTimestampAlignmentValid(events: List<SyncTestEvent>): Boolean {
        for (i in events.indices) {
            for (j in i + 1 until events.size) {
                val diffMs = abs(events[i].wallClockMs - events[j].wallClockMs)
                if (diffMs > SYNC_TOLERANCE_MS) {
                    return false
                }
            }
        }
        return true
    }

    private fun updateResultText(text: String) {
        runOnUiThread {
            resultTextView.text = text
        }
    }

    private fun appendResultText(text: String) {
        runOnUiThread {
            resultTextView.append(text)
        }
    }
    
    private fun checkPermissionsAndInitialize() {
        val hasPermissions = REQUIRED_PERMISSIONS.all { permission ->
            ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (hasPermissions) {
            initializeShimmerManager()
        } else {
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }
    
    private fun initializeShimmerManager() {
        lifecycleScope.launch {
            try {
                shimmerDeviceManager = ShimmerDeviceManager(this@TimestampSyncVerificationActivity)
                val initialized = shimmerDeviceManager?.initialize() ?: false
                
                if (initialized) {
                    Log.i(TAG, "ShimmerDeviceManager initialized successfully")
                    setupDeviceScanning()
                } else {
                    appendResultText("Failed to initialize device manager\n")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing ShimmerDeviceManager", e)
                appendResultText("Device manager initialization error: ${e.message}\n")
            }
        }
    }
    
    private fun setupDeviceScanning() {
        lifecycleScope.launch {
            shimmerDeviceManager?.scanResults?.collectLatest { devices ->
                Log.d(TAG, "Received ${devices.size} discovered devices")
                
                if (devices.isEmpty() && isScanning) {
                    appendResultText("Scanning for devices... (${devices.size} found)\n")
                } else if (devices.isNotEmpty()) {
                    appendResultText("Found ${devices.size} device(s):\n")
                    devices.forEach { device ->
                        appendResultText("  - ${device.name} (${device.address})\n")
                    }
                }
            }
        }
    }
    
    private fun startDeviceScanning() {
        val manager = shimmerDeviceManager ?: run {
            appendResultText("Device manager not initialized\n")
            return
        }

        if (!hasRequiredPermissions()) {
            appendResultText("Bluetooth permissions required for scanning\n")
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
            return
        }

        lifecycleScope.launch {
            try {
                val success = manager.startDeviceScanning()
                if (success) {
                    isScanning = true
                    updateScanButton(true)
                    appendResultText("Started scanning for devices...\n")
                    Log.i(TAG, "Started device scanning")
                } else {
                    appendResultText("Failed to start device scanning\n")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting device scan", e)
                appendResultText("Scan error: ${e.message}\n")
            }
        }
    }
    
    private fun stopDeviceScanning() {
        val manager = shimmerDeviceManager ?: return

        lifecycleScope.launch {
            try {
                manager.stopDeviceScanning()
                isScanning = false
                updateScanButton(false)
                appendResultText("Scan stopped\n")
                Log.i(TAG, "Stopped device scanning")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping device scan", e)
            }
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun updateScanButton(scanning: Boolean) {
        runOnUiThread {
            scanDevicesButton.text = if (scanning) "Stop Scan" else "Scan for Devices"
        }
    }
    
    private fun showPermissionError() {
        Toast.makeText(
            this,
            "Bluetooth permissions are required for device discovery",
            Toast.LENGTH_LONG
        ).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        lifecycleScope.launch {
            try {
                if (isScanning) {
                    shimmerDeviceManager?.stopDeviceScanning()
                }
                shimmerDeviceManager?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }
}

private data class SyncTestEvent(
    val sensorType: String,
    val timestampNs: Long,
    val wallClockMs: Long
)