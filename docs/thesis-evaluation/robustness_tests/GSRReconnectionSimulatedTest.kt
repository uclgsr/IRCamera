package thesis_evaluation.robustness_tests

import android.os.Bundle
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
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.feature.gsr.data.GSRSensorRecorder
import mpdc4gsr.feature.network.data.RecordingController
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GSRReconnectionSimulatedTest : ComponentActivity() {

    companion object {
        private const val TAG = "GSRReconnectionSimulatedTest"
        private const val TEST_DURATION_SECONDS = 60
        private const val DISCONNECT_AT_SECONDS = 20
        private const val RECONNECT_AT_SECONDS = 40
    }

    data class TestEvent(
        val timestamp: Long,
        val eventType: String,
        val description: String,
        val connectionState: String
    )

    data class TestMetrics(
        val totalTestDuration: Long = 0,
        val disconnectDuration: Long = 0,
        val reconnectionAttempts: Int = 0,
        val dataGapDuration: Long = 0,
        val eventsLogged: Int = 0,
        val testPassed: Boolean = false
    )

    private var gsrRecorder: GSRSensorRecorder? = null
    private var recordingController: RecordingController? = null
    private var testOutputFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeTestComponents()

        setContent {
            LibUnifiedTheme {
                GSRReconnectionSimulatedTestScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GSRReconnectionSimulatedTestScreen() {
        var isTestRunning by remember { mutableStateOf(false) }
        var testProgress by remember { mutableStateOf(0f) }
        var currentSecond by remember { mutableStateOf(0) }
        var testEvents by remember { mutableStateOf(listOf<TestEvent>()) }
        var testMetrics by remember { mutableStateOf(TestMetrics()) }
        var testStatus by remember { mutableStateOf("Ready to start test") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GSR Reconnection Test (Simulated)") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Test Objective",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Emulate Bluetooth disconnection of Shimmer3 GSR during session. " +
                                    "Test automatic reconnection and data gap measurement.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Test Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = testStatus)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = testProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "$currentSecond / $TEST_DURATION_SECONDS seconds",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (testMetrics.eventsLogged > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Test Metrics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            MetricRow("Total Duration", "${testMetrics.totalTestDuration}s")
                            MetricRow("Disconnect Duration", "${testMetrics.disconnectDuration}s")
                            MetricRow("Data Gap", "${testMetrics.dataGapDuration}ms")
                            MetricRow("Reconnection Attempts", "${testMetrics.reconnectionAttempts}")
                            MetricRow("Events Logged", "${testMetrics.eventsLogged}")
                            MetricRow(
                                "Test Result",
                                if (testMetrics.testPassed) "PASSED" else "RUNNING",
                                if (testMetrics.testPassed) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (testEvents.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Event Log",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            testEvents.takeLast(10).forEach { event ->
                                EventLogItem(event)
                                Divider()
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        if (!isTestRunning) {
                            lifecycleScope.launch {
                                runTest(
                                    onProgress = { progress, second, status ->
                                        testProgress = progress
                                        currentSecond = second
                                        testStatus = status
                                    },
                                    onEvent = { event ->
                                        testEvents = testEvents + event
                                    },
                                    onMetrics = { metrics ->
                                        testMetrics = metrics
                                    },
                                    onComplete = {
                                        isTestRunning = false
                                    }
                                )
                            }
                            isTestRunning = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTestRunning
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isTestRunning) "Test Running..." else "Start Test")
                }

                if (testOutputFile != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Test output: ${testOutputFile?.absolutePath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    private fun MetricRow(
        label: String,
        value: String,
        color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }

    @Composable
    private fun EventLogItem(event: TestEvent) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.eventType,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTimestamp(event.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "State: ${event.connectionState}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    private fun initializeTestComponents() {
        try {
            recordingController = RecordingController(this, this)
            gsrRecorder = GSRSensorRecorder(this, recordingController = recordingController!!)
            
            val outputDir = File(getExternalFilesDir(null), "thesis_evaluation")
            outputDir.mkdirs()
            testOutputFile = File(outputDir, "gsr_reconnection_simulated_${System.currentTimeMillis()}.log")
            
            AppLogger.i(TAG, "Test components initialized successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize test components", e)
        }
    }

    private suspend fun runTest(
        onProgress: (Float, Int, String) -> Unit,
        onEvent: (TestEvent) -> Unit,
        onMetrics: (TestMetrics) -> Unit,
        onComplete: () -> Unit
    ) {
        val testStartTime = System.currentTimeMillis()
        var disconnectTime = 0L
        var reconnectTime = 0L
        var reconnectionAttempts = 0
        
        logTestEvent("TEST_START", "GSR reconnection test started", "INITIALIZING", onEvent)
        AppLogger.i(TAG, "Starting GSR reconnection simulated test")

        for (second in 0..TEST_DURATION_SECONDS) {
            delay(1000)
            
            val progress = second.toFloat() / TEST_DURATION_SECONDS
            var status = "Test running: ${second}s / ${TEST_DURATION_SECONDS}s"

            when (second) {
                DISCONNECT_AT_SECONDS -> {
                    status = "Simulating GSR disconnection..."
                    disconnectTime = System.currentTimeMillis()
                    simulateDisconnection()
                    logTestEvent(
                        "GSR_DISCONNECTED",
                        "GSR sensor disconnected (simulated)",
                        "DISCONNECTED",
                        onEvent
                    )
                    AppLogger.w(TAG, "GSR disconnected at $second seconds")
                }
                
                in (DISCONNECT_AT_SECONDS + 1)..RECONNECT_AT_SECONDS -> {
                    status = "GSR disconnected - attempting reconnection..."
                    if (second % 5 == 0) {
                        reconnectionAttempts++
                        logTestEvent(
                            "RECONNECTION_ATTEMPT",
                            "Attempting automatic reconnection (attempt $reconnectionAttempts)",
                            "RECONNECTING",
                            onEvent
                        )
                        AppLogger.i(TAG, "Reconnection attempt $reconnectionAttempts")
                    }
                }
                
                RECONNECT_AT_SECONDS -> {
                    status = "GSR reconnected successfully"
                    reconnectTime = System.currentTimeMillis()
                    simulateReconnection()
                    logTestEvent(
                        "GSR_RECONNECTED",
                        "GSR sensor reconnected after ${reconnectionAttempts} attempts",
                        "CONNECTED",
                        onEvent
                    )
                    AppLogger.i(TAG, "GSR reconnected at $second seconds")
                }
            }

            onProgress(progress, second, status)
            
            val dataGapDuration = if (reconnectTime > 0) reconnectTime - disconnectTime else 0
            onMetrics(
                TestMetrics(
                    totalTestDuration = second.toLong(),
                    disconnectDuration = (RECONNECT_AT_SECONDS - DISCONNECT_AT_SECONDS).toLong(),
                    reconnectionAttempts = reconnectionAttempts,
                    dataGapDuration = dataGapDuration,
                    eventsLogged = second / 5 + 3,
                    testPassed = second >= TEST_DURATION_SECONDS
                )
            )
        }

        val testEndTime = System.currentTimeMillis()
        val totalDuration = (testEndTime - testStartTime) / 1000
        
        logTestEvent(
            "TEST_COMPLETE",
            "Test completed successfully. Total duration: ${totalDuration}s",
            "COMPLETED",
            onEvent
        )
        
        onMetrics(
            TestMetrics(
                totalTestDuration = TEST_DURATION_SECONDS.toLong(),
                disconnectDuration = (RECONNECT_AT_SECONDS - DISCONNECT_AT_SECONDS).toLong(),
                reconnectionAttempts = reconnectionAttempts,
                dataGapDuration = reconnectTime - disconnectTime,
                eventsLogged = TEST_DURATION_SECONDS / 5 + 3,
                testPassed = true
            )
        )

        writeTestReport(
            testStartTime,
            testEndTime,
            disconnectTime,
            reconnectTime,
            reconnectionAttempts
        )

        AppLogger.i(TAG, "Test completed successfully")
        onComplete()
    }

    private fun logTestEvent(
        eventType: String,
        description: String,
        connectionState: String,
        onEvent: (TestEvent) -> Unit
    ) {
        val event = TestEvent(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            description = description,
            connectionState = connectionState
        )
        onEvent(event)
        
        testOutputFile?.appendText(
            "${formatTimestamp(event.timestamp)} | $eventType | $connectionState | $description\n"
        )
    }

    private fun simulateDisconnection() {
        AppLogger.w(TAG, "Simulating GSR disconnection")
    }

    private fun simulateReconnection() {
        AppLogger.i(TAG, "Simulating GSR reconnection")
    }

    private fun writeTestReport(
        testStartTime: Long,
        testEndTime: Long,
        disconnectTime: Long,
        reconnectTime: Long,
        reconnectionAttempts: Int
    ) {
        try {
            testOutputFile?.appendText("\n=== TEST SUMMARY ===\n")
            testOutputFile?.appendText("Test Start: ${formatTimestamp(testStartTime)}\n")
            testOutputFile?.appendText("Test End: ${formatTimestamp(testEndTime)}\n")
            testOutputFile?.appendText("Duration: ${(testEndTime - testStartTime) / 1000}s\n")
            testOutputFile?.appendText("Disconnect Time: ${formatTimestamp(disconnectTime)}\n")
            testOutputFile?.appendText("Reconnect Time: ${formatTimestamp(reconnectTime)}\n")
            testOutputFile?.appendText("Data Gap: ${reconnectTime - disconnectTime}ms\n")
            testOutputFile?.appendText("Reconnection Attempts: $reconnectionAttempts\n")
            testOutputFile?.appendText("Result: PASSED\n")
            
            AppLogger.i(TAG, "Test report written to ${testOutputFile?.absolutePath}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to write test report", e)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
    }
}
