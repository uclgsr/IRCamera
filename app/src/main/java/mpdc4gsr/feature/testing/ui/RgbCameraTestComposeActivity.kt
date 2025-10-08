package mpdc4gsr.feature.testing.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.RgbCameraRecorder
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.core.ui.PermissionController
import mpdc4gsr.core.ui.PermissionManager
import mpdc4gsr.feature.testing.presentation.RgbCameraTestViewModel
import java.io.File
import kotlin.system.measureTimeMillis

class RgbCameraTestComposeActivity : BaseComposeActivity<RgbCameraTestViewModel>() {
    companion object {
    }

    private var cameraRecorder: RgbCameraRecorder? = null
    private var permissionManager: PermissionManager? = null
    private var permissionController: PermissionController? = null
    private var isRecording = false
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            initializeCamera()
        } else {
        }
    }

    override fun createViewModel(): RgbCameraTestViewModel {
        return viewModels<RgbCameraTestViewModel>().value
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionController = PermissionController(this)
        permissionManager = PermissionManager(this, permissionController!!)
        checkPermissions()
    }

    @Composable
    override fun Content(viewModel: RgbCameraTestViewModel) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            viewModel.initializeTestCases()
            viewModel.initializeCameraRecorder(context, this@RgbCameraTestComposeActivity)
        }
        LibUnifiedTheme {
            RgbCameraTestScreen(viewModel)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun RgbCameraTestScreen(viewModel: RgbCameraTestViewModel) {
        val testResults by viewModel.testResults.collectAsState()
        val isTestRunning by viewModel.isTestRunning.collectAsState()
        val cameraCapabilities by viewModel.cameraCapabilities.collectAsState()
        val recordingStatus by viewModel.recordingStatus.collectAsState()
        LaunchedEffect(Unit) {
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
                    completedTests = testResults.count { it.status != RgbCameraTestViewModel.TestStatus.PENDING },
                    passedTests = testResults.count { it.status == RgbCameraTestViewModel.TestStatus.PASSED },
                    failedTests = testResults.count { it.status == RgbCameraTestViewModel.TestStatus.FAILED }
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
            cameraRecorder = RgbCameraRecorder(this, this)
        }
    }

    private suspend fun initializeCameraCapabilities() {
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
        }
    }

    private suspend fun runAllCameraTests() {
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
        }
    }

    private suspend fun runPermissionsTest() {
            // Check camera permission using Android API
            val hasPermissions = checkSelfPermission(Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private suspend fun runCapabilityTest() {
            // Test camera capabilities
            delay(2000) // Simulate capability testing
        }
    }

    private suspend fun run4KRecordingTest() {
            val recordingTime = measureTimeMillis {
                // Start 4K recording test
                delay(5000) // Simulate 5 second recording
            }
        }
    }

    private suspend fun runTapFocusTest() {
            // Test tap-to-focus functionality
            delay(3000) // Simulate focus testing
        }
    }

    private suspend fun runManualControlsTest() {
            // Test manual exposure and focus controls
            delay(4000) // Simulate manual controls testing
        }
    }

    private suspend fun runRawCaptureTest() {
            // Test RAW image capture
            delay(3000) // Simulate RAW capture
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
        lifecycleScope.launch {
                isRecording = true
                val externalFilesDir = getExternalFilesDir(null)
                if (externalFilesDir == null) {
                    isRecording = false
                    return@launch
                }
                val testDir = File(externalFilesDir, "test_recordings").absolutePath
                cameraRecorder?.startRecording(testDir)
                isRecording = false
            }
        }
    }

    private fun stopRecording() {
        lifecycleScope.launch {
                isRecording = false
                cameraRecorder?.stopRecording()
            }
        }
    }
}

@Composable
private fun TestResultCard(
    testCase: RgbCameraTestViewModel.TestCase,
    onRunTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = testCase.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = testCase.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (testCase.result != null) {
                    Text(
                        text = testCase.result,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (testCase.status) {
                            RgbCameraTestViewModel.TestStatus.PASSED -> MaterialTheme.colorScheme.primary
                            RgbCameraTestViewModel.TestStatus.FAILED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            OutlinedButton(
                onClick = onRunTest,
                enabled = testCase.status != RgbCameraTestViewModel.TestStatus.RUNNING
            ) {
                Text(
                    when (testCase.status) {
                        RgbCameraTestViewModel.TestStatus.RUNNING -> "Running"
                        RgbCameraTestViewModel.TestStatus.PASSED -> "Passed"
                        RgbCameraTestViewModel.TestStatus.FAILED -> "Failed"
                        else -> "Run"
                    }
                )
            }
        }
    }
}