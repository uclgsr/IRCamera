package mpdc4gsr.feature.testing.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Speed
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
import mpdc4gsr.feature.gsr.data.GSRCalculationUtils
import mpdc4gsr.feature.gsr.data.GSRSensorRecorder
import mpdc4gsr.feature.network.data.RecordingController
import kotlin.system.measureTimeMillis

class GSRBenchTestComposeActivity : ComponentActivity() {
    companion object {
        private const val TEST_DURATION_SECONDS = 10
        private const val SAMPLE_TOLERANCE = 50
    }

    private var gsrRecorder: GSRSensorRecorder? = null
    private var recordingController: RecordingController? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeComponents()
        setContent {
            LibUnifiedTheme {
                GSRBenchTestScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GSRBenchTestScreen() {
        var testResults by remember { mutableStateOf(listOf<TestCase>()) }
        var isTestRunning by remember { mutableStateOf(false) }
        var benchmarkMetrics by remember { mutableStateOf(mapOf<String, Any>()) }
        var testLogs by remember { mutableStateOf(listOf<String>()) }
        // Initialize test cases
        LaunchedEffect(Unit) {
            testResults = listOf(
                TestCase(
                    id = "connection",
                    name = "GSR Connection Test",
                    description = "Test Shimmer GSR device connection"
                ),
                TestCase(
                    id = "calibration",
                    name = "Calibration Test",
                    description = "Test GSR sensor calibration process"
                ),
                TestCase(
                    id = "data_quality",
                    name = "Data Quality Test",
                    description = "Validate GSR data quality and consistency"
                ),
                TestCase(
                    id = "performance",
                    name = "Performance Benchmark",
                    description = "Test GSR data processing performance"
                ),
                TestCase(
                    id = "stress_test",
                    name = "Stress Test",
                    description = "Extended GSR data streaming test"
                )
            )
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "GSR Bench Test",
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
                // Test Progress
                TestProgressIndicator(
                    totalTests = testResults.size,
                    completedTests = testResults.count { it.status != TestStatus.PENDING },
                    passedTests = testResults.count { it.status == TestStatus.PASSED },
                    failedTests = testResults.count { it.status == TestStatus.FAILED }
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Benchmark Metrics
                if (benchmarkMetrics.isNotEmpty()) {
                    TestMetricsDisplay(
                        metrics = benchmarkMetrics,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Quick Test Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            lifecycleScope.launch { runComprehensiveBenchTest() }
                        },
                        enabled = !isTestRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Full Bench")
                    }
                    OutlinedButton(
                        onClick = {
                            lifecycleScope.launch { runConnectionTest() }
                        },
                        enabled = !isTestRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quick Test")
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
                // Test Logs
                if (testLogs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Test Logs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                TextButton(
                                    onClick = { testLogs = emptyList() }
                                ) {
                                    Text("Clear")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            testLogs.takeLast(10).forEach { message ->
                                Text(
                                    text = message,
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
            val controller = RecordingController(this, this)
            recordingController = controller
            gsrRecorder = GSRSensorRecorder(this, recordingController = controller)
        }
    }

    private suspend fun runComprehensiveBenchTest() {
        val overallStartTime = System.currentTimeMillis()
        val testMetrics = mutableMapOf<String, Any>()
            // Connection Test
            val connectionTime = measureTimeMillis {
                runConnectionTest()
            }
            testMetrics["Connection Time"] = "${connectionTime}ms"
            delay(1000)
            // Calibration Test
            val calibrationTime = measureTimeMillis {
                runCalibrationTest()
            }
            testMetrics["Calibration Time"] = "${calibrationTime}ms"
            delay(1000)
            // Data Quality Test
            val qualityResults = runDataQualityTest()
            testMetrics.putAll(qualityResults)
            delay(1000)
            // Performance Benchmark
            val performanceResults = runPerformanceBenchmark()
            testMetrics.putAll(performanceResults)
            // Update metrics
            val totalTime = System.currentTimeMillis() - overallStartTime
            testMetrics["Total Test Time"] = "${totalTime}ms"
            // Update state (this would be done with proper state management in real implementation)
        }
    }

    private suspend fun runConnectionTest() {
            // Simulate connection test logic
            delay(2000) // Simulate connection time
        }
    }

    private suspend fun runCalibrationTest() {
            // Simulate calibration test
            delay(3000) // Simulate calibration time
        }
    }

    private suspend fun runDataQualityTest(): Map<String, Any> {
        val qualityMetrics = mutableMapOf<String, Any>()
            // Simulate data quality analysis
            delay(2000)
            qualityMetrics["Sample Rate"] = "128 Hz"
            qualityMetrics["Data Integrity"] = "99.8%"
            qualityMetrics["Signal Quality"] = "Excellent"
        }
        return qualityMetrics
    }

    private suspend fun runPerformanceBenchmark(): Map<String, Any> {
        val performanceMetrics = mutableMapOf<String, Any>()
            // Simulate performance testing
            val processingTime = measureTimeMillis {
                // Simulate GSR data processing
                delay(1000)
                repeat(1000) {
                    GSRCalculationUtils.calculateGSRMicrosiemens(500)
                }
            }
            performanceMetrics["Processing Speed"] = "${processingTime}ms/1000 samples"
            performanceMetrics["Memory Usage"] = "~15MB"
            performanceMetrics["CPU Usage"] = "~12%"
        }
        return performanceMetrics
    }

    private fun runIndividualTest(testId: String) {
        lifecycleScope.launch {
            when (testId) {
                "connection" -> runConnectionTest()
                "calibration" -> runCalibrationTest()
                "data_quality" -> runDataQualityTest()
                "performance" -> runPerformanceBenchmark()
                "stress_test" -> runStressTest()
            }
        }
    }

    private suspend fun runStressTest() {
            // Extended testing for 30 seconds
            delay(30000)
        }
    }
}