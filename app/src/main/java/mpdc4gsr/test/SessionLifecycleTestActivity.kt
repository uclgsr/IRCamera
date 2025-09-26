package mpdc4gsr.test

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import mpdc4gsr.controller.ComprehensiveRecordingController
import mpdc4gsr.data.SessionMetadata
import mpdc4gsr.permissions.PermissionController
import mpdc4gsr.permissions.PermissionManager
import mpdc4gsr.sensors.RecordingStats
import mpdc4gsr.sensors.RecordingStatus
import mpdc4gsr.sensors.SensorError
import mpdc4gsr.sensors.SensorRecorder

/**
 * Comprehensive test activity for session lifecycle and recording coordination
 * Tests all enhanced functionality including fault tolerance, crash recovery, and foreground service
 */
class SessionLifecycleTestActivity : FragmentActivity() {

    companion object {
        private const val TAG = "SessionLifecycleTest"
    }

    private lateinit var recordingController: ComprehensiveRecordingController
    private lateinit var permissionController: PermissionController
    private lateinit var permissionManager: PermissionManager

    private var testSessionCounter = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)        // Initialize components
        permissionController = PermissionController(this)
        permissionManager = PermissionManager(this, permissionController)
        recordingController = ComprehensiveRecordingController(this)

        // Add test sensors
        setupTestSensors()

        // Check for crashed sessions on startup
        checkForCrashedSessions()

        // Create simple UI for testing
        createTestUI()
    }

    private fun setupTestSensors() {
        // Add mock sensors for testing
        recordingController.addSensorRecorder("RGB", MockRgbSensor())
        recordingController.addSensorRecorder("Thermal", MockThermalSensor())
        recordingController.addSensorRecorder("GSR", MockGSRSensor())    }

    private fun checkForCrashedSessions() {
        lifecycleScope.launch {
            try {
                val hasCrashedSession = recordingController.checkForCrashedSessions()
                if (hasCrashedSession) {
                    showAlert("Crash Recovery", "Detected and recovered from crashed session. Check logs for details.")
                } else {                }
            } catch (e: Exception) {            }
        }
    }

    private fun createTestUI() {
        // Create simple test UI programmatically
        setContentView(android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)

            // Test 1: Normal recording
            addView(android.widget.Button(this@SessionLifecycleTestActivity).apply {
                text = "Test 1: Normal Recording (All Sensors)"
                setOnClickListener { testNormalRecording() }
            })

            // Test 2: Partial recording (one sensor fails)
            addView(android.widget.Button(this@SessionLifecycleTestActivity).apply {
                text = "Test 2: Partial Recording (Thermal Fails)"
                setOnClickListener { testPartialRecording() }
            })

            // Test 3: All sensors fail
            addView(android.widget.Button(this@SessionLifecycleTestActivity).apply {
                text = "Test 3: All Sensors Fail"
                setOnClickListener { testAllSensorsFail() }
            })

            // Test 4: Sensor exception during start
            addView(android.widget.Button(this@SessionLifecycleTestActivity).apply {
                text = "Test 4: Sensor Exception Isolation"
                setOnClickListener { testSensorExceptionIsolation() }
            })

            // Test 5: Insufficient storage
            addView(android.widget.Button(this@SessionLifecycleTestActivity).apply {
                text = "Test 5: Insufficient Storage Test"
                setOnClickListener { testInsufficientStorage() }
            })

            // Test 6: Stop recording
            addView(android.widget.Button(this@SessionLifecycleTestActivity).apply {
                text = "Stop Current Recording"
                setOnClickListener { stopCurrentRecording() }
            })

            // Status display
            addView(android.widget.TextView(this@SessionLifecycleTestActivity).apply {
                text = "Check logcat output for detailed test results"
                setPadding(0, 32, 0, 0)
            })
        })
    }

    private fun testNormalRecording() {
        lifecycleScope.launch {
            try {                // Reset all sensors to success mode
                (recordingController as? ComprehensiveRecordingController)?.let { controller ->
                    // Configure sensors for success
                    MockRgbSensor.shouldSucceed = true
                    MockThermalSensor.shouldSucceed = true
                    MockGSRSensor.shouldSucceed = true
                    MockGSRSensor.shouldThrowException = false
                }

                val sessionId = "normal_test_${testSessionCounter++}"
                val result = recordingController.startRecording(
                    sessionId = sessionId,
                    enabledSensors = listOf("RGB", "Thermal", "GSR"),
                    estimatedDurationMinutes = 5
                )

                if (result) {
                    showToast("✅ Normal recording started successfully")                } else {
                    showToast("❌ Normal recording failed to start")                }

            } catch (e: Exception) {                showToast("❌ Test 1 exception: ${e.message}")
            }
        }
    }

    private fun testPartialRecording() {
        lifecycleScope.launch {
            try {===")

                // Configure thermal sensor to fail, others succeed
                MockRgbSensor.shouldSucceed = true
                MockThermalSensor.shouldSucceed = false
                MockGSRSensor.shouldSucceed = true
                MockGSRSensor.shouldThrowException = false

                val sessionId = "partial_test_${testSessionCounter++}"
                val result = recordingController.startRecording(
                    sessionId = sessionId,
                    enabledSensors = listOf("RGB", "Thermal", "GSR"),
                    estimatedDurationMinutes = 5
                )

                if (result) {
                    showToast("✅ Partial recording started (2/3 sensors)")                } else {
                    showToast("❌ Partial recording failed")                }

            } catch (e: Exception) {                showToast("❌ Test 2 exception: ${e.message}")
            }
        }
    }

    private fun testAllSensorsFail() {
        lifecycleScope.launch {
            try {                // Configure all sensors to fail
                MockRgbSensor.shouldSucceed = false
                MockThermalSensor.shouldSucceed = false
                MockGSRSensor.shouldSucceed = false
                MockGSRSensor.shouldThrowException = false

                val sessionId = "all_fail_test_${testSessionCounter++}"
                val result = recordingController.startRecording(
                    sessionId = sessionId,
                    enabledSensors = listOf("RGB", "Thermal", "GSR"),
                    estimatedDurationMinutes = 5
                )

                if (!result) {
                    showToast("✅ Recording correctly failed (no sensors started)")                } else {
                    showToast("❌ Recording should have failed but succeeded")                }

            } catch (e: Exception) {                showToast("❌ Test 3 exception: ${e.message}")
            }
        }
    }

    private fun testSensorExceptionIsolation() {
        lifecycleScope.launch {
            try {                // Configure GSR sensor to throw exception, others succeed
                MockRgbSensor.shouldSucceed = true
                MockThermalSensor.shouldSucceed = true
                MockGSRSensor.shouldSucceed = true
                MockGSRSensor.shouldThrowException = true

                val sessionId = "exception_test_${testSessionCounter++}"
                val result = recordingController.startRecording(
                    sessionId = sessionId,
                    enabledSensors = listOf("RGB", "Thermal", "GSR"),
                    estimatedDurationMinutes = 5
                )

                if (result) {
                    showToast("✅ Recording continued despite sensor exception")                } else {
                    showToast("❌ Recording failed due to sensor exception")                }

            } catch (e: Exception) {                showToast("❌ Test 4 exception: ${e.message}")
            }
        }
    }

    private fun testInsufficientStorage() {
        lifecycleScope.launch {
            try {                // Reset sensors to succeed
                MockRgbSensor.shouldSucceed = true
                MockThermalSensor.shouldSucceed = true
                MockGSRSensor.shouldSucceed = true
                MockGSRSensor.shouldThrowException = false

                val sessionId = "storage_test_${testSessionCounter++}"
                val result = recordingController.startRecording(
                    sessionId = sessionId,
                    enabledSensors = listOf("RGB", "Thermal", "GSR"),
                    estimatedDurationMinutes = 999 // Very long recording to trigger storage check
                )

                if (!result) {
                    showToast("✅ Recording correctly failed (insufficient storage)")                } else {
                    showToast("⚠️ Storage check may have passed (device has lots of space)")                }

            } catch (e: Exception) {                showToast("❌ Test 5 exception: ${e.message}")
            }
        }
    }

    private fun stopCurrentRecording() {
        lifecycleScope.launch {
            try {                val result = recordingController.stopRecording()

                if (result) {
                    showToast("✅ Recording stopped successfully")                } else {
                    showToast("❌ Failed to stop recording")                }

            } catch (e: Exception) {                showToast("❌ Stop exception: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showAlert(title: String, message: String) {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    // Mock sensor implementations for testing
    class MockRgbSensor : SensorRecorder {
        companion object {
            var shouldSucceed = true
        }

        override val sensorId: String = "RGB"
        override val sensorType: String = "camera"
        override val isRecording: Boolean = shouldSucceed
        override val samplingRate: Double = 30.0

        override suspend fun initialize(): Boolean {called - returning $shouldSucceed")
            return shouldSucceed
        }

        override suspend fun startRecording(sessionDirectory: String): Boolean {called - returning $shouldSucceed")
            return shouldSucceed
        }

        override suspend fun startRecording(sessionDirectory: String, sessionMetadata: SessionMetadata): Boolean {called - returning $shouldSucceed")
            return shouldSucceed
        }

        override suspend fun stopRecording(): Boolean {called")
            return true
        }

        override suspend fun addSyncMarker(markerType: String, timestampNs: Long, metadata: Map<String, String>) {called")
        }

        override suspend fun cleanup() {called")
        }

        private fun mockRecordingStatus(): RecordingStatus {
            return RecordingStatus(sensorId, sensorType, isRecording, 0, 0.0, 0.0, 0L)
        }

        override fun getStatusFlow(): Flow<RecordingStatus> {
            return flowOf(mockRecordingStatus())
        }

        override fun getErrorFlow(): Flow<SensorError> {
            return flowOf()
        }

        override fun getRecordingStats(): RecordingStats {
            return RecordingStats(sensorId, sensorType, 0L, 0L, 0.0, 0L, 0.0, 0, 0L)
        }
    }

    class MockThermalSensor : SensorRecorder {
        companion object {
            var shouldSucceed = true
        }

        override val sensorId: String = "Thermal"
        override val sensorType: String = "thermal"
        override val isRecording: Boolean = shouldSucceed
        override val samplingRate: Double = 10.0

        override suspend fun initialize(): Boolean {called - returning $shouldSucceed")
            return shouldSucceed
        }

        override suspend fun startRecording(sessionDirectory: String): Boolean {called - returning $shouldSucceed")
            return shouldSucceed
        }

        override suspend fun startRecording(sessionDirectory: String, sessionMetadata: SessionMetadata): Boolean {called - returning $shouldSucceed")
            return shouldSucceed
        }

        override suspend fun stopRecording(): Boolean {called")
            return true
        }

        override suspend fun addSyncMarker(markerType: String, timestampNs: Long, metadata: Map<String, String>) {called")
        }

        override suspend fun cleanup() {called")
        }

        private fun createMockRecordingStatus(): RecordingStatus {
            return RecordingStatus(sensorId, sensorType, isRecording, 0, 0.0, 0.0, 0L)
        }

        override fun getStatusFlow(): Flow<RecordingStatus> {
            return flowOf(createMockRecordingStatus())
        }

        override fun getErrorFlow(): Flow<SensorError> {
            return flowOf()
        }

        override fun getRecordingStats(): RecordingStats {
            return RecordingStats(sensorId, sensorType, 0L, 0L, 0.0, 0L, 0.0, 0, 0L)
        }
    }

    class MockGSRSensor : SensorRecorder {
        companion object {
            var shouldSucceed = true
            var shouldThrowException = false
        }

        override val sensorId: String = "GSR"
        override val sensorType: String = "gsr"
        override val isRecording: Boolean = shouldSucceed
        override val samplingRate: Double = 128.0

        override suspend fun initialize(): Boolean {called - returning $shouldSucceed")
            return shouldSucceed
        }

        override suspend fun startRecording(sessionDirectory: String): Boolean {
            if (shouldThrowException) {throwing exception")
                throw RuntimeException("Mock GSR sensor connection failed")
            }called - returning $shouldSucceed")
            return shouldSucceed
        }

        override suspend fun startRecording(sessionDirectory: String, sessionMetadata: SessionMetadata): Boolean {
            if (shouldThrowException) {throwing exception")
                throw RuntimeException("Mock GSR sensor connection failed")
            }called - returning $shouldSucceed")
            return shouldSucceed
        }

        override suspend fun stopRecording(): Boolean {called")
            return true
        }

        override suspend fun addSyncMarker(markerType: String, timestampNs: Long, metadata: Map<String, String>) {called")
        }

        override suspend fun cleanup() {called")
        }

        private fun createMockRecordingStatus(): RecordingStatus {
            return RecordingStatus(sensorId, sensorType, isRecording, 0, 0.0, 0.0, 0L)
        }

        override fun getStatusFlow(): Flow<RecordingStatus> {
            return flowOf(createMockRecordingStatus())
        }

        override fun getErrorFlow(): Flow<SensorError> {
            return flowOf()
        }

        override fun getRecordingStats(): RecordingStats {
            return createMockRecordingStats()
        }

        private fun createMockRecordingStats(): RecordingStats {
            // All mock values are zero/default for testing purposes
            val mockStartTimeNs = 0L
            val mockEndTimeNs = 0L
            val mockDurationSec = 0.0
            val mockNumSamples = 0L
            val mockSamplingRate = 0.0
            val mockNumDropped = 0
            val mockNumErrors = 0L
            return RecordingStats(
                sensorId,
                sensorType,
                mockStartTimeNs,
                mockEndTimeNs,
                mockDurationSec,
                mockNumSamples,
                mockSamplingRate,
                mockNumDropped,
                mockNumErrors
            )
        }
    }
}