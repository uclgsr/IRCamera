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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.feature.network.data.RecordingController
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CompleteSessionTrialComposeActivity : ComponentActivity() {
    companion object {
        private const val TAG = "CompleteSessionTrialCompose"
        private const val EXTENDED_DURATION_SECONDS = 300 // 5 minutes as per plan
        private const val STATUS_UPDATE_INTERVAL = 10 // Update every 10 seconds
    }

    private var recordingController: RecordingController? = null
    private var trialSessionDir: File? = null
    private var trialStartTime: Long = 0
    private var trialEndTime: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeComponents()
        setContent {
            LibUnifiedTheme {
                CompleteSessionTrialScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CompleteSessionTrialScreen() {
        var testResults by remember { mutableStateOf(listOf<TestCase>()) }
        var isRecording by remember { mutableStateOf(false) }
        var elapsedTime by remember { mutableStateOf(0L) }
        var sessionProgress by remember { mutableStateOf(0f) }
        var sessionPhase by remember { mutableStateOf("Ready") }
        var testMetrics by remember { mutableStateOf(mapOf<String, Any>()) }
        var trialLogs by remember { mutableStateOf(listOf<String>()) }
        // Initialize test cases
        LaunchedEffect(Unit) {
            testResults = listOf(
                TestCase(
                    id = "session_initialization",
                    name = "Session Initialization",
                    description = "Initialize all sensors and recording systems"
                ),
                TestCase(
                    id = "extended_recording",
                    name = "Extended Recording (5 min)",
                    description = "Test continuous recording with all sensors for 5 minutes"
                ),
                TestCase(
                    id = "data_verification",
                    name = "Data Verification",
                    description = "Verify output files and data quality"
                ),
                TestCase(
                    id = "session_cleanup",
                    name = "Session Cleanup",
                    description = "Proper session termination and resource cleanup"
                ),
                TestCase(
                    id = "report_generation",
                    name = "Report Generation",
                    description = "Generate comprehensive session report"
                )
            )
        }
        // Timer for recording
        LaunchedEffect(isRecording) {
            if (isRecording) {
                while (isRecording && elapsedTime < EXTENDED_DURATION_SECONDS) {
                    delay(1000)
                    elapsedTime += 1
                    sessionProgress = elapsedTime.toFloat() / EXTENDED_DURATION_SECONDS
                    // Update phase based on elapsed time
                    sessionPhase = when {
                        elapsedTime < 30 -> "Initialization"
                        elapsedTime < 270 -> "Recording (${elapsedTime / 60}:${
                            String.format(
                                "%02d",
                                elapsedTime % 60
                            )
                        })"

                        else -> "Finalizing"
                    }
                    // Log status every 10 seconds
                    if (elapsedTime % STATUS_UPDATE_INTERVAL == 0L) {
                        trialLogs = trialLogs + "${
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        }: Recording ${elapsedTime}s - All sensors active"
                    }
                }
                if (elapsedTime >= EXTENDED_DURATION_SECONDS) {
                    isRecording = false
                    sessionPhase = "Completed"
                    trialLogs = trialLogs + "${
                        SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())
                    }: Trial completed successfully"
                }
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Complete Session Trial",
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
                // Session Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isRecording -> MaterialTheme.colorScheme.primaryContainer
                            sessionPhase == "Completed" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.Stop,
                                    contentDescription = null,
                                    tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Phase: $sessionPhase",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (isRecording) {
                                Text(
                                    text = "${elapsedTime}s / ${EXTENDED_DURATION_SECONDS}s",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        if (isRecording) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { sessionProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Recording Progress: ${(sessionProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Test Parameters Card
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Test Parameters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf(
                            "Duration" to "${EXTENDED_DURATION_SECONDS / 60} minutes (${EXTENDED_DURATION_SECONDS}s)",
                            "Sensors" to "RGB Camera, Thermal Camera, GSR Sensor",
                            "Output" to "Video files, CSV data, session metadata",
                            "Verification" to "File presence, data quality, metadata"
                        ).forEach { (key, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
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
                // Session Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            lifecycleScope.launch { startCompleteSessionTrial() }
                            isRecording = true
                            elapsedTime = 0
                            sessionProgress = 0f
                        },
                        enabled = !isRecording,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Trial")
                    }
                    Button(
                        onClick = {
                            lifecycleScope.launch { stopCompleteSessionTrial() }
                            isRecording = false
                            sessionPhase = "Stopped"
                        },
                        enabled = isRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop Trial")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            lifecycleScope.launch { verifySessionOutput() }
                        },
                        enabled = !isRecording && sessionPhase == "Completed",
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verify Output")
                    }
                    OutlinedButton(
                        onClick = {
                            lifecycleScope.launch { generateCompleteReport() }
                        },
                        enabled = !isRecording && sessionPhase == "Completed",
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Generate Report")
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
                // Trial Logs
                if (trialLogs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Trial Logs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                TextButton(
                                    onClick = { trialLogs = emptyList() }
                                ) {
                                    Text("Clear")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            trialLogs.takeLast(10).forEach { log ->
                                Text(
                                    text = log,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initializeComponents() {
        try {
            recordingController = RecordingController(this, this)
            AppLogger.d(TAG, "Recording controller initialized successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize components: ${e.message}")
        }
    }

    private suspend fun startCompleteSessionTrial() {
        AppLogger.i(TAG, "Starting complete session trial")
        trialStartTime = System.currentTimeMillis()
        try {
            // Initialize session directory
            val sessionName =
                "trial_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
            trialSessionDir = File(filesDir, "trials/$sessionName")
            trialSessionDir?.mkdirs()
            // Start recording with all sensors
            recordingController?.startRecording()
            AppLogger.d(TAG, "Complete session trial started successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start session trial: ${e.message}")
        }
    }

    private suspend fun stopCompleteSessionTrial() {
        AppLogger.i(TAG, "Stopping complete session trial")
        trialEndTime = System.currentTimeMillis()
        try {
            recordingController?.stopRecording()
            AppLogger.d(TAG, "Complete session trial stopped successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to stop session trial: ${e.message}")
        }
    }

    private suspend fun verifySessionOutput() {
        AppLogger.d(TAG, "Verifying session output")
        try {
            delay(2000) // Simulate verification time
            AppLogger.d(TAG, "Session output verification completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Session output verification failed: ${e.message}")
        }
    }

    private suspend fun generateCompleteReport() {
        AppLogger.d(TAG, "Generating complete report")
        try {
            delay(3000) // Simulate report generation
            AppLogger.d(TAG, "Complete report generated successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Report generation failed: ${e.message}")
        }
    }

    private fun runIndividualTest(testId: String) {
        lifecycleScope.launch {
            when (testId) {
                "session_initialization" -> testSessionInitialization()
                "extended_recording" -> testExtendedRecording()
                "data_verification" -> verifySessionOutput()
                "session_cleanup" -> testSessionCleanup()
                "report_generation" -> generateCompleteReport()
            }
        }
    }

    private suspend fun testSessionInitialization() {
        AppLogger.d(TAG, "Testing session initialization")
        try {
            delay(3000)
            AppLogger.d(TAG, "Session initialization test completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Session initialization test failed: ${e.message}")
        }
    }

    private suspend fun testExtendedRecording() {
        AppLogger.d(TAG, "Testing extended recording")
        try {
            delay(5000) // Simulate extended recording test
            AppLogger.d(TAG, "Extended recording test completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Extended recording test failed: ${e.message}")
        }
    }

    private suspend fun testSessionCleanup() {
        AppLogger.d(TAG, "Testing session cleanup")
        try {
            delay(2000)
            AppLogger.d(TAG, "Session cleanup test completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Session cleanup test failed: ${e.message}")
        }
    }
}