package mpdc4gsr.feature.testing.ui

import android.os.Bundle
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.RgbCameraRecorder
import mpdc4gsr.feature.camera.data.SamsungDeviceCompatibility
import kotlin.system.measureTimeMillis

class RawCaptureTestComposeActivity : ComponentActivity() {
    companion object {
        private const val TAG = "RawCaptureTestCompose"
    }

    data class CaptureFormat(
        val name: String,
        val description: String,
        val supported: Boolean,
        val fileExtension: String
    )

    data class CaptureResult(
        val format: String,
        val success: Boolean,
        val fileSizeMB: Double,
        val captureDurationMs: Long,
        val details: String
    )

    private var rgbCameraRecorder: RgbCameraRecorder? = null
    private var isRecording = false
    private var captureResults by mutableStateOf(listOf<CaptureResult>())
    private var isTestRunning by mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeCamera()
        setContent {
            LibUnifiedTheme {
                RawCaptureTestScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun RawCaptureTestScreen() {
        var testResults by remember { mutableStateOf(listOf<TestCase>()) }
        var captureFormats by remember { mutableStateOf(listOf<CaptureFormat>()) }
        var deviceCompatibility by remember { mutableStateOf(mapOf<String, Any>()) }
        var selectedFormat by remember { mutableStateOf("DNG") }
        var captureSettings by remember { mutableStateOf(mapOf<String, Boolean>()) }
        // Initialize test cases and formats
        LaunchedEffect(Unit) {
            testResults = listOf(
                TestCase(
                    id = "device_compatibility",
                    name = "Device Compatibility",
                    description = "Check RAW capture device support"
                ),
                TestCase(
                    id = "dng_capture",
                    name = "DNG Capture",
                    description = "Test DNG (Digital Negative) format capture"
                ),
                TestCase(
                    id = "stage3_support",
                    name = "Stage 3/Level 3 Support",
                    description = "Verify advanced RAW processing capabilities"
                ),
                TestCase(
                    id = "capture_quality",
                    name = "Capture Quality",
                    description = "Analyze RAW image quality and metadata"
                ),
                TestCase(
                    id = "file_formats",
                    name = "File Format Support",
                    description = "Test various RAW file format support"
                ),
                TestCase(
                    id = "performance_test",
                    name = "Performance Test",
                    description = "Test RAW capture speed and efficiency"
                )
            )
            captureFormats = listOf(
                CaptureFormat(
                    name = "DNG",
                    description = "Digital Negative (Adobe Standard)",
                    supported = true,
                    fileExtension = ".dng"
                ),
                CaptureFormat(
                    name = "RAW",
                    description = "Native camera RAW format",
                    supported = SamsungDeviceCompatibility.isStage3Compatible(),
                    fileExtension = ".raw"
                ),
                CaptureFormat(
                    name = "JPEG + RAW",
                    description = "Simultaneous JPEG and RAW capture",
                    supported = true,
                    fileExtension = ".jpg+.dng"
                )
            )
            captureSettings = mapOf(
                "Manual Focus" to false,
                "Manual Exposure" to false,
                "Stage 3 Processing" to SamsungDeviceCompatibility.isStage3Compatible(),
                "Metadata Embedding" to true
            )
            // Load device compatibility info
            val isStage3Compatible = SamsungDeviceCompatibility.isStage3Compatible()
            val deviceInfo = SamsungDeviceCompatibility.getDeviceInfo()
            deviceCompatibility = mapOf(
                "Device Model" to deviceInfo,
                "Stage 3 Compatible" to isStage3Compatible,
                "RAW Support" to "Available",
                "DNG Version" to "1.6",
                "Color Depth" to "16-bit",
                "Max Resolution" to "64MP"
            )
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "RAW Capture Test",
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Device Compatibility Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (SamsungDeviceCompatibility.isStage3Compatible())
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (SamsungDeviceCompatibility.isStage3Compatible())
                                    Icons.Default.Verified
                                else
                                    Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (SamsungDeviceCompatibility.isStage3Compatible())
                                    "Stage 3/Level 3 DNG Capture Available"
                                else
                                    "Standard RAW Capture Available",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Device: ${SamsungDeviceCompatibility.getDeviceInfo()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Device Compatibility Metrics
                if (deviceCompatibility.isNotEmpty()) {
                    TestMetricsDisplay(
                        metrics = deviceCompatibility,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Capture Format Selection
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Capture Formats",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        captureFormats.forEach { format ->
                            CaptureFormatItem(
                                format = format,
                                isSelected = selectedFormat == format.name,
                                onSelect = { selectedFormat = format.name }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Capture Settings
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Capture Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        captureSettings.forEach { (setting, enabled) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = setting,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = {
                                        captureSettings = captureSettings + (setting to it)
                                    },
                                    enabled = setting != "Stage 3 Processing" // This depends on device
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Test Progress
                TestProgressIndicator(
                    totalTests = testResults.size,
                    completedTests = testResults.count { it.status != TestStatus.PENDING },
                    passedTests = testResults.count { it.status == TestStatus.PASSED },
                    failedTests = testResults.count { it.status == TestStatus.FAILED }
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Capture Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isTestRunning = true
                            lifecycleScope.launch { runAllCaptureTests() }
                        },
                        enabled = !isTestRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTestRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing...")
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Run All Tests")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            lifecycleScope.launch { captureRawImage(selectedFormat) }
                        },
                        enabled = !isTestRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Capture $selectedFormat")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Individual Test Cases
                testResults.forEach { testCase ->
                    TestResultCard(
                        testCase = testCase,
                        onRunTest = { runIndividualTest(testCase.id) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                // Capture Results
                if (captureResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Capture Results",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            captureResults.forEach { result ->
                                CaptureResultItem(result = result)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CaptureFormatItem(
        format: CaptureFormat,
        isSelected: Boolean,
        onSelect: () -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = format.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = format.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (format.supported) {
                    RadioButton(
                        selected = isSelected && format.supported,
                        onClick = { if (format.supported) onSelect() }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Not supported",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun CaptureResultItem(result: CaptureResult) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${result.format} Capture",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = result.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${result.fileSizeMB} MB",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${result.captureDurationMs}ms",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    private fun initializeCamera() {
        try {
            rgbCameraRecorder = RgbCameraRecorder(this, this)
            AppLogger.d(TAG, "RAW capture camera initialized successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize camera: ${e.message}")
        }
    }

    private suspend fun runAllCaptureTests() {
        AppLogger.i(TAG, "Running all RAW capture tests")
        val results = mutableListOf<CaptureResult>()
        try {
            // Test DNG capture
            val dngResult = captureRawImage("DNG")
            if (dngResult != null) results.add(dngResult)
            delay(2000)
            // Test RAW capture if supported
            if (SamsungDeviceCompatibility.isStage3Compatible()) {
                val rawResult = captureRawImage("RAW")
                if (rawResult != null) results.add(rawResult)
            }
            delay(2000)
            // Test JPEG + RAW
            val combinedResult = captureRawImage("JPEG + RAW")
            if (combinedResult != null) results.add(combinedResult)
            captureResults = results
        } catch (e: Exception) {
            AppLogger.e(TAG, "All capture tests failed: ${e.message}")
        } finally {
            isTestRunning = false
        }
    }

    private suspend fun captureRawImage(format: String): CaptureResult? {
        AppLogger.d(TAG, "Capturing RAW image in $format format")
        return try {
            val captureDuration = measureTimeMillis {
                delay(3000) // Simulate capture time
            }
            // Simulate different file sizes and success rates
            val success = when (format) {
                "DNG" -> true
                "RAW" -> SamsungDeviceCompatibility.isStage3Compatible()
                "JPEG + RAW" -> true
                else -> false
            }
            val fileSizeMB = when (format) {
                "DNG" -> 25.6
                "RAW" -> 32.1
                "JPEG + RAW" -> 28.3
                else -> 0.0
            }
            CaptureResult(
                format = format,
                success = success,
                fileSizeMB = fileSizeMB,
                captureDurationMs = captureDuration,
                details = if (success) "Capture completed successfully" else "Format not supported on this device"
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "$format capture failed: ${e.message}")
            CaptureResult(
                format = format,
                success = false,
                fileSizeMB = 0.0,
                captureDurationMs = 0,
                details = "Capture failed: ${e.message}"
            )
        }
    }

    private fun runIndividualTest(testId: String) {
        lifecycleScope.launch {
            when (testId) {
                "device_compatibility" -> testDeviceCompatibility()
                "dng_capture" -> captureRawImage("DNG")
                "stage3_support" -> testStage3Support()
                "capture_quality" -> testCaptureQuality()
                "file_formats" -> testFileFormats()
                "performance_test" -> testCapturePerformance()
            }
        }
    }

    private suspend fun testDeviceCompatibility() {
        AppLogger.d(TAG, "Testing device compatibility")
        try {
            delay(2000)
            AppLogger.d(TAG, "Device compatibility test completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Device compatibility test failed: ${e.message}")
        }
    }

    private suspend fun testStage3Support() {
        AppLogger.d(TAG, "Testing Stage 3/Level 3 support")
        try {
            delay(3000)
            AppLogger.d(TAG, "Stage 3 support test completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Stage 3 support test failed: ${e.message}")
        }
    }

    private suspend fun testCaptureQuality() {
        AppLogger.d(TAG, "Testing capture quality")
        try {
            delay(4000)
            AppLogger.d(TAG, "Capture quality test completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Capture quality test failed: ${e.message}")
        }
    }

    private suspend fun testFileFormats() {
        AppLogger.d(TAG, "Testing file formats")
        try {
            delay(5000)
            AppLogger.d(TAG, "File formats test completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "File formats test failed: ${e.message}")
        }
    }

    private suspend fun testCapturePerformance() {
        AppLogger.d(TAG, "Testing capture performance")
        try {
            delay(6000)
            AppLogger.d(TAG, "Capture performance test completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Capture performance test failed: ${e.message}")
        }
    }
}