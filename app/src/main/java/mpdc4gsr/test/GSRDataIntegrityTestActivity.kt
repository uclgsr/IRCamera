package mpdc4gsr.test

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.controller.RecordingController
import mpdc4gsr.sensors.gsr.GSRSensorRecorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Sampling Rate and Data Integrity Verification Activity
 * Tests data integrity and sampling rate accuracy as per issue #139 plan
 */
class GSRDataIntegrityTestActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "GSRDataIntegrityTest"
        private const val TEST_DURATION_SECONDS = 10
        private const val EXPECTED_SAMPLE_RATE = 128.0
        private const val EXPECTED_SAMPLES = (TEST_DURATION_SECONDS * EXPECTED_SAMPLE_RATE).toInt()
        private const val SAMPLE_TOLERANCE = 50  // Allow 50 sample variance
        private const val EXPECTED_INTERVAL_MS = 1000.0 / EXPECTED_SAMPLE_RATE  // ~7.8ms
        private const val INTERVAL_TOLERANCE_MS = 2.0  // Allow 2ms variance
    }

    private lateinit var resultTextView: TextView
    private lateinit var startTestButton: Button
    private lateinit var quickTestButton: Button

    private var gsrRecorder: GSRSensorRecorder? = null
    private var recordingController: RecordingController? = null
    private var testResults: StringBuilder = StringBuilder()
    private var testSessionDir: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_test_consolidated)

        setupUI()
        initializeComponents()
    }

    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "GSR Data Integrity Testing"

        resultTextView = findViewById(R.id.result_text_view)
        startTestButton = findViewById(R.id.start_test_button)
        quickTestButton = findViewById(R.id.quick_test_button)

        startTestButton.setOnClickListener {
            lifecycleScope.launch {
                runDataIntegrityTest()
            }
        }

        quickTestButton.setOnClickListener {
            lifecycleScope.launch {
                runQuickTest()
            }
        }

        updateResults("GSR Data Integrity Testing\nReady to verify sampling rate and data integrity...\n\n")
        updateResults("Expected: ${EXPECTED_SAMPLES} samples in ${TEST_DURATION_SECONDS}s at ${EXPECTED_SAMPLE_RATE}Hz\n")
        updateResults(
            "Expected interval: ${
                String.format(
                    "%.2f",
                    EXPECTED_INTERVAL_MS
                )
            }ms between samples\n\n"
        )
    }

    private fun initializeComponents() {
        try {
            recordingController = RecordingController(this, this)
            gsrRecorder = GSRSensorRecorder(this, "data_integrity_test", 128, recordingController!!)
            updateResults("✓ GSR components initialized for testing\n\n")
        } catch (e: Exception) {
            updateResults("✗ Failed to initialize GSR components: ${e.message}\n")
            Log.e(TAG, "Failed to initialize components", e)
        }
    }

    /**
     * Run comprehensive data integrity test as outlined in issue #139 plan
     */
    private suspend fun runDataIntegrityTest() {
        testResults.clear()
        testResults.append("=== GSR Data Integrity Test Started ===\n")
        testResults.append("Test Time: ${getCurrentTimeString()}\n")
        testResults.append("Duration: ${TEST_DURATION_SECONDS}s\n")
        testResults.append("Expected Samples: $EXPECTED_SAMPLES\n")
        testResults.append("Sample Rate: ${EXPECTED_SAMPLE_RATE}Hz\n\n")

        updateResults(testResults.toString())

        // Phase 1: Setup recording session
        val setupResult = setupTestRecording()
        if (!setupResult) {
            testResults.append("✗ Failed to setup test recording\n")
            updateResults(testResults.toString())
            return
        }

        testResults.append("✓ Test recording session setup complete\n")
        updateResults(testResults.toString())

        // Phase 2: Record for exact duration
        val recordingResult = performTimedRecording()
        testResults.append("Recording Phase: ${if (recordingResult) "COMPLETED" else "FAILED"}\n")
        updateResults(testResults.toString())

        if (!recordingResult) return

        // Phase 3: Analyze recorded data
        val analysisResult = analyzeRecordedData()
        testResults.append("\n=== Data Analysis Results ===\n")
        testResults.append(analysisResult)
        updateResults(testResults.toString())

        // Phase 4: Generate final report
        generateFinalReport(analysisResult.contains("PASS"))
    }

    private suspend fun runQuickTest(): Boolean {
        updateResults("=== Quick Data Integrity Check ===\n")

        return try {
            val recorder = gsrRecorder ?: return false

            // Quick validation of recorder configuration
            updateResults("Checking recorder configuration...\n")
            updateResults("- Sensor ID: ${recorder.sensorId}\n")
            updateResults("- Sampling Rate: ${recorder.samplingRate}Hz\n")
            updateResults("- Expected Sample Rate: ${EXPECTED_SAMPLE_RATE}Hz\n")

            val rateMatch = recorder.samplingRate == EXPECTED_SAMPLE_RATE
            updateResults("- Rate Match: ${if (rateMatch) "✓ PASS" else "✗ FAIL"}\n\n")

            if (rateMatch) {
                updateResults("✓ Quick test PASSED - recorder properly configured\n")
            } else {
                updateResults("✗ Quick test FAILED - sampling rate mismatch\n")
            }

            rateMatch
        } catch (e: Exception) {
            updateResults("✗ Quick test failed: ${e.message}\n")
            false
        }
    }

    private suspend fun setupTestRecording(): Boolean {
        return try {
            // Create test session directory
            val testDir = File(cacheDir, "gsr_integrity_test_${System.currentTimeMillis()}")
            testDir.mkdirs()
            testSessionDir = testDir

            val recorder = gsrRecorder ?: return false

            // Initialize recorder
            val initialized = recorder.initialize()
            if (!initialized) {
                updateResults("✗ Failed to initialize GSR recorder\n")
                return false
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup test recording", e)
            false
        }
    }

    private suspend fun performTimedRecording(): Boolean {
        return try {
            val recorder = gsrRecorder ?: return false
            val sessionDir = testSessionDir ?: return false

            updateResults("Starting ${TEST_DURATION_SECONDS}s recording...\n")

            // Start recording
            val startTime = System.currentTimeMillis()
            val recordingStarted = recorder.startRecording(sessionDir.absolutePath)

            if (!recordingStarted) {
                updateResults("✗ Failed to start recording\n")
                return false
            }

            updateResults("✓ Recording started...\n")

            // Wait for exact test duration with progress updates
            for (i in 1..TEST_DURATION_SECONDS) {
                delay(1000)
                updateResults("Recording... ${i}s/${TEST_DURATION_SECONDS}s\n")
            }

            // Stop recording
            val recordingStopped = recorder.stopRecording()
            val endTime = System.currentTimeMillis()
            val actualDuration = endTime - startTime

            updateResults("✓ Recording stopped\n")
            updateResults("Actual duration: ${actualDuration}ms\n")

            recordingStopped
        } catch (e: Exception) {
            Log.e(TAG, "Error during timed recording", e)
            false
        }
    }

    private suspend fun analyzeRecordedData(): String {
        val analysis = StringBuilder()

        try {
            val sessionDir = testSessionDir ?: return "Error: No session directory\n"
            val gsrFile = File(sessionDir, "gsr.csv")

            if (!gsrFile.exists()) {
                return "✗ FAIL: gsr.csv file not found\n"
            }

            // Read and analyze CSV data
            val lines = gsrFile.readLines()
            val dataLines = lines.drop(1) // Skip header

            analysis.append("File Analysis:\n")
            analysis.append("- File exists: ✓\n")
            analysis.append("- Total lines: ${lines.size}\n")
            analysis.append("- Data lines: ${dataLines.size}\n")
            analysis.append("- Expected samples: $EXPECTED_SAMPLES\n")

            // Check sample count
            val sampleCountDiff = abs(dataLines.size - EXPECTED_SAMPLES)
            val sampleCountPass = sampleCountDiff <= SAMPLE_TOLERANCE

            analysis.append("- Sample count difference: $sampleCountDiff\n")
            analysis.append("- Sample count test: ${if (sampleCountPass) "✓ PASS" else "✗ FAIL"}\n")

            if (dataLines.isNotEmpty()) {
                // Analyze timestamps
                val timestampAnalysis = analyzeTimestamps(dataLines)
                analysis.append(timestampAnalysis)
            }

            analysis.append("\nOverall Result: ${if (sampleCountPass) "PASS" else "FAIL"}\n")

        } catch (e: Exception) {
            analysis.append("✗ Analysis failed: ${e.message}\n")
            Log.e(TAG, "Error analyzing recorded data", e)
        }

        return analysis.toString()
    }

    private fun analyzeTimestamps(dataLines: List<String>): String {
        val analysis = StringBuilder()

        try {
            analysis.append("\nTimestamp Analysis:\n")

            val timestamps = mutableListOf<Long>()
            var parseErrors = 0

            // Extract timestamps from CSV (assuming first column is timestamp in nanoseconds)
            for (line in dataLines.take(100)) { // Analyze first 100 samples for performance
                try {
                    val columns = line.split(",")
                    if (columns.isNotEmpty()) {
                        val timestampNs = columns[0].toLong()
                        timestamps.add(timestampNs / 1_000_000) // Convert to milliseconds
                    }
                } catch (e: Exception) {
                    parseErrors++
                }
            }

            if (timestamps.size < 2) {
                analysis.append("- Insufficient timestamps for analysis\n")
                return analysis.toString()
            }

            // Calculate intervals
            val intervals = mutableListOf<Double>()
            for (i in 1 until timestamps.size) {
                val interval = timestamps[i] - timestamps[i - 1]
                intervals.add(interval.toDouble())
            }

            val avgInterval = intervals.average()
            val expectedInterval = EXPECTED_INTERVAL_MS
            val intervalDiff = abs(avgInterval - expectedInterval)
            val intervalPass = intervalDiff <= INTERVAL_TOLERANCE_MS

            analysis.append("- Samples analyzed: ${timestamps.size}\n")
            analysis.append("- Average interval: ${String.format("%.2f", avgInterval)}ms\n")
            analysis.append("- Expected interval: ${String.format("%.2f", expectedInterval)}ms\n")
            analysis.append("- Interval difference: ${String.format("%.2f", intervalDiff)}ms\n")
            analysis.append("- Interval test: ${if (intervalPass) "✓ PASS" else "✗ FAIL"}\n")

            // Check for monotonic increasing
            var monotonic = true
            for (i in 1 until timestamps.size) {
                if (timestamps[i] <= timestamps[i - 1]) {
                    monotonic = false
                    break
                }
            }
            analysis.append("- Monotonic timestamps: ${if (monotonic) "✓ PASS" else "✗ FAIL"}\n")

            if (parseErrors > 0) {
                analysis.append("- Parse errors: $parseErrors\n")
            }

        } catch (e: Exception) {
            analysis.append("- Timestamp analysis failed: ${e.message}\n")
        }

        return analysis.toString()
    }

    private fun generateFinalReport(overallPass: Boolean) {
        testResults.append("\n=== FINAL REPORT ===\n")
        testResults.append("Test Date: ${getCurrentTimeString()}\n")
        testResults.append("Test Duration: ${TEST_DURATION_SECONDS}s\n")
        testResults.append("Expected Sample Rate: ${EXPECTED_SAMPLE_RATE}Hz\n")
        testResults.append("Overall Result: ${if (overallPass) "✓ PASS" else "✗ FAIL"}\n\n")

        if (overallPass) {
            testResults.append("Data integrity verified successfully!\n")
            testResults.append("GSR sampling is working as expected.\n")
        } else {
            testResults.append("Data integrity issues detected.\n")
            testResults.append("Review individual test results above.\n")
        }

        updateResults(testResults.toString())
    }

    private fun updateResults(text: String) {
        runOnUiThread {
            resultTextView.text = text
        }
    }

    private fun getCurrentTimeString(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    override fun onDestroy() {
        super.onDestroy()

        // Cleanup test files
        testSessionDir?.let { dir ->
            try {
                dir.deleteRecursively()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cleanup test directory", e)
            }
        }

        gsrRecorder?.let { recorder ->
            lifecycleScope.launch {
                try {
                    recorder.cleanup()
                } catch (e: Exception) {
                    Log.e(TAG, "Error cleaning up GSR recorder", e)
                }
            }
        }
    }
}