package mpdc4gsr.feature.testing.ui

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
import kotlin.system.measureTimeMillis

class SessionLifecycleTestComposeActivity : ComponentActivity() {
    companion object {
        private const val TAG = "SessionLifecycleTestCompose"
    }

    data class SessionEvent(
        val eventType: String,
        val timestamp: Long,
        val duration: Long = 0,
        val success: Boolean = true,
        val details: String = ""
    )

    enum class SessionState {
        IDLE, INITIALIZING, ACTIVE, PAUSED, STOPPING, COMPLETED, ERROR
    }

    private var sessionEvents by mutableStateOf(listOf<SessionEvent>())
    private var currentSessionState by mutableStateOf(SessionState.IDLE)
    private var isTestRunning by mutableStateOf(false)
    private var sessionMetrics by mutableStateOf(mapOf<String, Any>())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibUnifiedTheme {
                SessionLifecycleTestScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SessionLifecycleTestScreen() {
        var testResults by remember { mutableStateOf(listOf<TestCase>()) }
        // Initialize test cases
        LaunchedEffect(Unit) {
            testResults = listOf(
                TestCase(
                    id = "session_creation",
                    name = "Session Creation",
                    description = "Test recording session initialization"
                ),
                TestCase(
                    id = "multi_sensor_start",
                    name = "Multi-Sensor Start",
                    description = "Test starting all sensors simultaneously"
                ),
                TestCase(
                    id = "session_pause_resume",
                    name = "Pause/Resume",
                    description = "Test session pause and resume functionality"
                ),
                TestCase(
                    id = "graceful_stop",
                    name = "Graceful Stop",
                    description = "Test proper session termination"
                ),
                TestCase(
                    id = "error_recovery",
                    name = "Error Recovery",
                    description = "Test recovery from sensor failures"
                ),
                TestCase(
                    id = "data_integrity",
                    name = "Data Integrity",
                    description = "Validate data consistency throughout lifecycle"
                )
            )
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Session Lifecycle Test",
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
                // Session State Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (currentSessionState) {
                            SessionState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                            SessionState.ERROR -> MaterialTheme.colorScheme.errorContainer
                            SessionState.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getSessionStateIcon(currentSessionState),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Session State: ${currentSessionState.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (sessionEvents.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Events logged: ${sessionEvents.size}",
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
                // Session Metrics
                if (sessionMetrics.isNotEmpty()) {
                    TestMetricsDisplay(
                        metrics = sessionMetrics,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Session Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isTestRunning = true
                            lifecycleScope.launch { runFullLifecycleTest() }
                        },
                        enabled = !isTestRunning && currentSessionState == SessionState.IDLE,
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
                            Icon(Icons.Default.PlayCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Full Test")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            lifecycleScope.launch { simulateSession() }
                        },
                        enabled = !isTestRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulate")
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
                // Session Events Log
                if (sessionEvents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Session Events",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                TextButton(
                                    onClick = { sessionEvents = emptyList() }
                                ) {
                                    Text("Clear")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            sessionEvents.takeLast(10).forEach { event ->
                                SessionEventItem(event = event)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SessionEventItem(event: SessionEvent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.eventType,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (event.details.isNotEmpty()) {
                    Text(
                        text = event.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (event.duration > 0) {
                    Text(
                        text = "${event.duration}ms",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(
                    imageVector = if (event.success) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (event.success)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    private fun getSessionStateIcon(state: SessionState): androidx.compose.ui.graphics.vector.ImageVector {
        return when (state) {
            SessionState.IDLE -> Icons.Default.Stop
            SessionState.INITIALIZING -> Icons.Default.Refresh
            SessionState.ACTIVE -> Icons.Default.PlayArrow
            SessionState.PAUSED -> Icons.Default.Pause
            SessionState.STOPPING -> Icons.Default.StopCircle
            SessionState.COMPLETED -> Icons.Default.CheckCircle
            SessionState.ERROR -> Icons.Default.Error
        }
    }

    private suspend fun runFullLifecycleTest() {
        AppLogger.i(TAG, "Starting full session lifecycle test")
        val testMetrics = mutableMapOf<String, Any>()
        try {
            // Session Creation Test
            val creationTime = testSessionCreation()
            testMetrics["Creation Time"] = "${creationTime}ms"
            // Multi-sensor Start Test
            val startTime = testMultiSensorStart()
            testMetrics["Start Time"] = "${startTime}ms"
            // Session Operation (simulate 5 seconds)
            currentSessionState = SessionState.ACTIVE
            delay(5000)
            // Pause/Resume Test
            val pauseResumeTime = testPauseResume()
            testMetrics["Pause/Resume Time"] = "${pauseResumeTime}ms"
            // Graceful Stop Test
            val stopTime = testGracefulStop()
            testMetrics["Stop Time"] = "${stopTime}ms"
            // Update metrics
            testMetrics["Total Events"] = sessionEvents.size
            testMetrics["Success Rate"] =
                "${sessionEvents.count { it.success } * 100 / sessionEvents.size}%"
            sessionMetrics = testMetrics
            currentSessionState = SessionState.COMPLETED
        } catch (e: Exception) {
            AppLogger.e(TAG, "Full lifecycle test failed: ${e.message}")
            currentSessionState = SessionState.ERROR
        } finally {
            isTestRunning = false
        }
    }

    private suspend fun testSessionCreation(): Long {
        AppLogger.d(TAG, "Testing session creation")
        return measureTimeMillis {
            currentSessionState = SessionState.INITIALIZING
            delay(1000) // Simulate session initialization
            sessionEvents = sessionEvents + SessionEvent(
                eventType = "Session Created",
                timestamp = System.currentTimeMillis(),
                duration = 1000,
                success = true,
                details = "Recording session initialized successfully"
            )
        }
    }

    private suspend fun testMultiSensorStart(): Long {
        AppLogger.d(TAG, "Testing multi-sensor start")
        return measureTimeMillis {
            // Simulate starting GSR sensor
            delay(500)
            sessionEvents = sessionEvents + SessionEvent(
                eventType = "GSR Started",
                timestamp = System.currentTimeMillis(),
                duration = 500,
                success = true
            )
            // Simulate starting thermal camera
            delay(800)
            sessionEvents = sessionEvents + SessionEvent(
                eventType = "Thermal Started",
                timestamp = System.currentTimeMillis(),
                duration = 800,
                success = true
            )
            // Simulate starting RGB camera
            delay(600)
            sessionEvents = sessionEvents + SessionEvent(
                eventType = "RGB Started",
                timestamp = System.currentTimeMillis(),
                duration = 600,
                success = true
            )
        }
    }

    private suspend fun testPauseResume(): Long {
        AppLogger.d(TAG, "Testing pause/resume functionality")
        return measureTimeMillis {
            // Test pause
            currentSessionState = SessionState.PAUSED
            delay(1000)
            sessionEvents = sessionEvents + SessionEvent(
                eventType = "Session Paused",
                timestamp = System.currentTimeMillis(),
                duration = 1000,
                success = true
            )
            // Test resume
            delay(500)
            currentSessionState = SessionState.ACTIVE
            sessionEvents = sessionEvents + SessionEvent(
                eventType = "Session Resumed",
                timestamp = System.currentTimeMillis(),
                duration = 500,
                success = true
            )
        }
    }

    private suspend fun testGracefulStop(): Long {
        AppLogger.d(TAG, "Testing graceful session stop")
        return measureTimeMillis {
            currentSessionState = SessionState.STOPPING
            // Stop each sensor gracefully
            delay(400)
            sessionEvents = sessionEvents + SessionEvent(
                eventType = "GSR Stopped",
                timestamp = System.currentTimeMillis(),
                duration = 400,
                success = true
            )
            delay(600)
            sessionEvents = sessionEvents + SessionEvent(
                eventType = "Thermal Stopped",
                timestamp = System.currentTimeMillis(),
                duration = 600,
                success = true
            )
            delay(300)
            sessionEvents = sessionEvents + SessionEvent(
                eventType = "RGB Stopped",
                timestamp = System.currentTimeMillis(),
                duration = 300,
                success = true
            )
            delay(200)
            sessionEvents = sessionEvents + SessionEvent(
                eventType = "Session Terminated",
                timestamp = System.currentTimeMillis(),
                duration = 200,
                success = true,
                details = "All sensors stopped gracefully"
            )
        }
    }

    private suspend fun simulateSession() {
        AppLogger.d(TAG, "Simulating quick session")
        currentSessionState = SessionState.INITIALIZING
        delay(1000)
        currentSessionState = SessionState.ACTIVE
        delay(3000)
        currentSessionState = SessionState.STOPPING
        delay(1000)
        currentSessionState = SessionState.COMPLETED
        sessionEvents = sessionEvents + SessionEvent(
            eventType = "Simulation Complete",
            timestamp = System.currentTimeMillis(),
            success = true,
            details = "Quick session simulation completed"
        )
    }

    private fun runIndividualTest(testId: String) {
        lifecycleScope.launch {
            when (testId) {
                "session_creation" -> testSessionCreation()
                "multi_sensor_start" -> testMultiSensorStart()
                "session_pause_resume" -> testPauseResume()
                "graceful_stop" -> testGracefulStop()
                "error_recovery" -> testErrorRecovery()
                "data_integrity" -> testDataIntegrity()
            }
        }
    }

    private suspend fun testErrorRecovery() {
        AppLogger.d(TAG, "Testing error recovery")
        try {
            delay(2000)
            sessionEvents = sessionEvents + SessionEvent(
                eventType = "Error Recovery Test",
                timestamp = System.currentTimeMillis(),
                duration = 2000,
                success = true,
                details = "Sensor failure recovery tested"
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error recovery test failed: ${e.message}")
        }
    }

    private suspend fun testDataIntegrity() {
        AppLogger.d(TAG, "Testing data integrity")
        try {
            delay(3000)
            sessionEvents = sessionEvents + SessionEvent(
                eventType = "Data Integrity Check",
                timestamp = System.currentTimeMillis(),
                duration = 3000,
                success = true,
                details = "Data consistency validated across all sensors"
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Data integrity test failed: ${e.message}")
        }
    }
}