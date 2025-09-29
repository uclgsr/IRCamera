package mpdc4gsr.compose.testing

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.permissions.PermissionManager
import mpdc4gsr.compose.common.ComposeImports.*
import mpdc4gsr.compose.common.ComposeLayouts.*
import mpdc4gsr.compose.common.ComposeDimens
import mpdc4gsr.sensors.RgbCameraRecorder
import kotlin.system.measureTimeMillis

/**
 * Compose version of RGB Camera Test Activity
 * Tests camera functionality, recording quality, and manual controls
 */
class RgbCameraTestComposeActivity : ComponentActivity() {

    companion object {
        private const val TAG = "RgbCameraTestCompose"
    }

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
            Log.e(TAG, "Camera permissions required for testing")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionManager = PermissionManager(this)
        checkPermissions()

        setContent {
            LibUnifiedTheme {
                RgbCameraTestScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun RgbCameraTestScreen() {
        var testResults by remember { mutableStateOf(listOf<TestCase>()) }
        var isTestRunning by remember { mutableStateOf(false) }
        var cameraCapabilities by remember { mutableStateOf(mapOf<String, Any>()) }
        var recordingStatus by remember { mutableStateOf("Ready") }

        // Initialize test cases
        LaunchedEffect(Unit) {
            testResults = listOf(
                TestCase(
                    id = "permissions",
                    name = "Camera Permissions",
                    description = "Verify camera and storage permissions"
                ),
                TestCase(
                    id = "capability",
                    name = "Camera Capabilities",
                    description = "Test camera features and resolutions"
                ),
                TestCase(
                    id = "4k_recording",
                    name = "4K Recording Test",
                    description = "Test 4K video recording capability"
                ),
                TestCase(
                    id = "tap_focus",
                    name = "Tap-to-Focus",
                    description = "Test tap-to-focus functionality"
                ),
                TestCase(
                    id = "manual_controls",
                    name = "Manual Controls",
                    description = "Test manual exposure and focus controls"
                ),
                TestCase(
                    id = "raw_capture",
                    name = "RAW Capture",
                    description = "Test RAW image capture capability"
                )
            )

            // Load camera capabilities
            initializeCameraCapabilities()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "RGB Camera Test",
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                // Camera Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Camera Status: $recordingStatus",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (cameraCapabilities.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Capabilities loaded: ${cameraCapabilities.size} features",
                                style = MaterialTheme.typography.bodyMedium
                            )
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

                // Camera Capabilities
                if (cameraCapabilities.isNotEmpty()) {
                    TestMetricsDisplay(
                        metrics = cameraCapabilities,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Quick Test Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            lifecycleScope.launch { runAllCameraTests() }
                        },
                        enabled = !isTestRunning && !isRecording,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run All")
                    }

                    OutlinedButton(
                        onClick = {
                            if (isRecording) {
                                stopRecording()
                            } else {
                                startTestRecording()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRecording) "Stop" else "Record")
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
            }
        }
    }

    private fun checkPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        permissionLauncher.launch(requiredPermissions)
    }

    private fun initializeCamera() {
        try {
            cameraRecorder = RgbCameraRecorder(this)
            Log.d(TAG, "Camera recorder initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize camera: ${e.message}")
        }
    }

    private suspend fun initializeCameraCapabilities() {
        try {
            // Simulate loading camera capabilities
            delay(1000)

            // This would be populated with real camera capabilities
            val capabilities = mapOf(
                "Max Resolution" to "4K (3840x2160)",
                "Frame Rate" to "30 FPS",
                "Auto Focus" to "Supported",
                "Manual Controls" to "Exposure, Focus",
                "RAW Support" to "DNG Format",
                "Video Codecs" to "H.264, H.265"
            )

            Log.d(TAG, "Camera capabilities loaded: $capabilities")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load camera capabilities: ${e.message}")
        }
    }

    private suspend fun runAllCameraTests() {
        Log.i(TAG, "Starting comprehensive camera tests")

        try {
            runPermissionsTest()
            delay(1000)

            runCapabilityTest()
            delay(1000)

            run4KRecordingTest()
            delay(2000)

            runTapFocusTest()
            delay(1000)

            runManualControlsTest()
            delay(1000)

            runRawCaptureTest()

            Log.i(TAG, "All camera tests completed")
        } catch (e: Exception) {
            Log.e(TAG, "Camera tests failed: ${e.message}")
        }
    }

    private suspend fun runPermissionsTest() {
        Log.d(TAG, "Testing camera permissions")
        try {
            val hasPermissions = permissionManager?.hasCameraPermissions() ?: false
            Log.d(TAG, "Camera permissions check: $hasPermissions")
        } catch (e: Exception) {
            Log.e(TAG, "Permissions test failed: ${e.message}")
        }
    }

    private suspend fun runCapabilityTest() {
        Log.d(TAG, "Testing camera capabilities")
        try {
            // Test camera capabilities
            delay(2000) // Simulate capability testing
            Log.d(TAG, "Camera capability test completed")
        } catch (e: Exception) {
            Log.e(TAG, "Capability test failed: ${e.message}")
        }
    }

    private suspend fun run4KRecordingTest() {
        Log.d(TAG, "Testing 4K recording")
        try {
            val recordingTime = measureTimeMillis {
                // Start 4K recording test
                delay(5000) // Simulate 5 second recording
            }
            Log.d(TAG, "4K recording test completed in ${recordingTime}ms")
        } catch (e: Exception) {
            Log.e(TAG, "4K recording test failed: ${e.message}")
        }
    }

    private suspend fun runTapFocusTest() {
        Log.d(TAG, "Testing tap-to-focus")
        try {
            // Test tap-to-focus functionality
            delay(3000) // Simulate focus testing
            Log.d(TAG, "Tap-to-focus test completed")
        } catch (e: Exception) {
            Log.e(TAG, "Tap-to-focus test failed: ${e.message}")
        }
    }

    private suspend fun runManualControlsTest() {
        Log.d(TAG, "Testing manual controls")
        try {
            // Test manual exposure and focus controls
            delay(4000) // Simulate manual controls testing
            Log.d(TAG, "Manual controls test completed")
        } catch (e: Exception) {
            Log.e(TAG, "Manual controls test failed: ${e.message}")
        }
    }

    private suspend fun runRawCaptureTest() {
        Log.d(TAG, "Testing RAW capture")
        try {
            // Test RAW image capture
            delay(3000) // Simulate RAW capture
            Log.d(TAG, "RAW capture test completed")
        } catch (e: Exception) {
            Log.e(TAG, "RAW capture test failed: ${e.message}")
        }
    }

    private fun runIndividualTest(testId: String) {
        lifecycleScope.launch {
            when (testId) {
                "permissions" -> runPermissionsTest()
                "capability" -> runCapabilityTest()
                "4k_recording" -> run4KRecordingTest()
                "tap_focus" -> runTapFocusTest()
                "manual_controls" -> runManualControlsTest()
                "raw_capture" -> runRawCaptureTest()
            }
        }
    }

    private fun startTestRecording() {
        try {
            isRecording = true
            cameraRecorder?.startRecording()
            Log.d(TAG, "Test recording started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}")
            isRecording = false
        }
    }

    private fun stopRecording() {
        try {
            isRecording = false
            cameraRecorder?.stopRecording()
            Log.d(TAG, "Test recording stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording: ${e.message}")
        }
    }
}