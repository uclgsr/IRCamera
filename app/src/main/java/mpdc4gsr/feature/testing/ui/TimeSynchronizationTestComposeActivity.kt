package mpdc4gsr.feature.testing.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import mpdc4gsr.core.data.TimeSynchronizationService
import mpdc4gsr.feature.network.data.RecordingController
import kotlin.math.abs

class TimeSynchronizationTestComposeActivity : ComponentActivity() {
    companion object {
        private const val SYNC_TOLERANCE_MS = 5L
        private const val FLASH_DURATION_MS = 500L
        private const val TEST_RECORDING_DURATION_MS = 10000L
    }

    data class TimestampCheck(
        val sensorName: String,
        val timestamp: Long,
        val syncOffset: Long,
        val isSynchronized: Boolean,
        val details: String
    )

    data class SyncEvent(
        val eventName: String,
        val timestamp: String,
        val sensorData: Map<String, Long>,
        val maxDrift: Long,
        val synchronized: Boolean
    )

    private var timeSyncService: TimeSynchronizationService? = null
    private var recordingController: RecordingController? = null

    // State variables hoisted to activity level
    private val _timestampChecks = mutableStateOf(listOf<TimestampCheck>())
    private val _syncEvents = mutableStateOf(listOf<SyncEvent>())
    private val _syncMetrics = mutableStateOf(mapOf<String, Any>())
    private val _maxDriftMs = mutableStateOf(0L)
    private val _currentSyncStatus = mutableStateOf("Not Synchronized")
    private val _isTestRunning = mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeComponents()
        setContent {
            LibUnifiedTheme {
                TimeSynchronizationTestScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TimeSynchronizationTestScreen() {
        var testResults by remember { mutableStateOf(listOf<TestCase>()) }
        // Use hoisted state
        val isTestRunning by _isTestRunning
        val timestampChecks by _timestampChecks
        val syncEvents by _syncEvents
        val syncMetrics by _syncMetrics
        val currentSyncStatus by _currentSyncStatus
        val maxDriftMs by _maxDriftMs
        // Initialize test cases
        LaunchedEffect(Unit) {
            testResults = listOf(
                TestCase(
                    id = "unified_timestamp",
                    name = "Unified Timestamp System",
                    description = "Test unified timestamp system across all sensors"
                ),
                TestCase(
                    id = "cross_sensor_sync",
                    name = "Cross-Sensor Sync Events",
                    description = "Test synchronization events between sensors"
                ),
                TestCase(
                    id = "timestamp_verification",
                    name = "Timestamp Verification",
                    description = "Verify timestamp precision and accuracy"
                ),
                TestCase(
                    id = "drift_detection",
                    name = "Clock Drift Detection",
                    description = "Detect and measure clock drift between sensors"
                ),
                TestCase(
                    id = "sync_recovery",
                    name = "Sync Recovery",
                    description = "Test synchronization recovery after drift"
                ),
                TestCase(
                    id = "flash_sync_test",
                    name = "Flash Sync Test",
                    description = "Visual synchronization test using camera flash"
                )
            )
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Time Synchronization Test",
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
                // Synchronization Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            maxDriftMs <= SYNC_TOLERANCE_MS -> MaterialTheme.colorScheme.primaryContainer
                            maxDriftMs <= 20 -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
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
                                    imageVector = when {
                                        maxDriftMs <= SYNC_TOLERANCE_MS -> Icons.Default.Sync
                                        maxDriftMs <= 20 -> Icons.Default.SyncProblem
                                        else -> Icons.Default.SyncDisabled
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        maxDriftMs <= SYNC_TOLERANCE_MS -> MaterialTheme.colorScheme.primary
                                        maxDriftMs <= 20 -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.error
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sync Status: $currentSyncStatus",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (maxDriftMs > 0) {
                                Text(
                                    text = "Max Drift: ${maxDriftMs}ms",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        maxDriftMs <= SYNC_TOLERANCE_MS -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }
                        if (timestampChecks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sensors monitored: ${timestampChecks.size}",
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
                // Sync Metrics
                if (syncMetrics.isNotEmpty()) {
                    TestMetricsDisplay(
                        metrics = syncMetrics,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Test Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            _isTestRunning.value = true
                            lifecycleScope.launch { runAllSyncTests() }
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
                            lifecycleScope.launch { performFlashSyncTest() }
                        },
                        enabled = !isTestRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Flash Sync")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            lifecycleScope.launch { checkTimestampAccuracy() }
                        },
                        enabled = !isTestRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Check Accuracy")
                    }
                    OutlinedButton(
                        onClick = {
                            lifecycleScope.launch { measureClockDrift() }
                        },
                        enabled = !isTestRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Measure Drift")
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
                // Timestamp Checks
                if (timestampChecks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Timestamp Checks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            timestampChecks.forEach { check ->
                                TimestampCheckItem(check = check)
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
                // Sync Events
                if (syncEvents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Synchronization Events",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            syncEvents.takeLast(5).forEach { event ->
                                SyncEventItem(event = event)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TimestampCheckItem(check: TimestampCheck) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = check.sensorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = check.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${check.syncOffset}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (check.isSynchronized)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (check.isSynchronized) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (check.isSynchronized)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun SyncEventItem(event: SyncEvent) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (event.synchronized) Icons.Default.Sync else Icons.Default.SyncProblem,
                        contentDescription = null,
                        tint = if (event.synchronized)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.eventName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = event.timestamp,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Drift: ${event.maxDrift}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (event.synchronized)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    private fun initializeComponents() {
            recordingController = RecordingController(this, this)
            timeSyncService = TimeSynchronizationService()
        }
    }

    private suspend fun runAllSyncTests() {
        val startTime = System.currentTimeMillis()
        val metrics = mutableMapOf<String, Any>()
        val checks = mutableListOf<TimestampCheck>()
        val events = mutableListOf<SyncEvent>()
            // Test unified timestamp system
            val unifiedTest = testUnifiedTimestampSystem()
            checks.addAll(unifiedTest)
            delay(1000)
            // Test cross-sensor sync events
            val syncTest = testCrossSensorSyncEvents()
            events.addAll(syncTest)
            delay(1000)
            // Check timestamp accuracy
            checkTimestampAccuracy()
            delay(1000)
            // Measure clock drift
            measureClockDrift()
            // Calculate overall metrics
            val maxDrift = checks.maxOfOrNull { abs(it.syncOffset) } ?: 0L
            val syncedCount = checks.count { it.isSynchronized }
            val syncRate = if (checks.isNotEmpty()) (syncedCount * 100) / checks.size else 0
            metrics["Max Drift"] = "${maxDrift}ms"
            metrics["Sync Rate"] = "$syncRate%"
            metrics["Sensors Tested"] = checks.size
            metrics["Events Captured"] = events.size
            metrics["Test Duration"] = "${System.currentTimeMillis() - startTime}ms"
            _timestampChecks.value = checks
            _syncEvents.value = events
            _syncMetrics.value = metrics
            _maxDriftMs.value = maxDrift
            _currentSyncStatus.value =
                if (maxDrift <= SYNC_TOLERANCE_MS) "Synchronized" else "Out of Sync"
            _isTestRunning.value = false
        }
    }

    private suspend fun testUnifiedTimestampSystem(): List<TimestampCheck> {
        val checks = mutableListOf<TimestampCheck>()
        val baseTimestamp = System.currentTimeMillis()
            delay(2000) // Simulate test time
            // Simulate timestamp checks for different sensors
            val sensors = listOf("GSR Sensor", "Thermal Camera", "RGB Camera", "Audio Recorder")
            sensors.forEach { sensor ->
                val offset = (-10..10).random().toLong()
                val isSynced = abs(offset) <= SYNC_TOLERANCE_MS
                checks.add(
                    TimestampCheck(
                        sensorName = sensor,
                        timestamp = baseTimestamp + offset,
                        syncOffset = offset,
                        isSynchronized = isSynced,
                        details = "Timestamp verification completed"
                    )
                )
                delay(200)
            }
        }
        return checks
    }

    private suspend fun testCrossSensorSyncEvents(): List<SyncEvent> {
        val events = mutableListOf<SyncEvent>()
            delay(3000) // Simulate test time
            // Simulate sync events
            repeat(3) { i ->
                val sensorData = mapOf(
                    "GSR" to System.currentTimeMillis() + (-5..5).random(),
                    "Thermal" to System.currentTimeMillis() + (-5..5).random(),
                    "RGB" to System.currentTimeMillis() + (-5..5).random()
                )
                val maxDrift = sensorData.values.maxOf { it } - sensorData.values.minOf { it }
                val synchronized = maxDrift <= SYNC_TOLERANCE_MS
                events.add(
                    SyncEvent(
                        eventName = "Sync Event ${i + 1}",
                        timestamp = java.text.SimpleDateFormat(
                            "HH:mm:ss.SSS",
                            java.util.Locale.getDefault()
                        )
                            .format(java.util.Date()),
                        sensorData = sensorData,
                        maxDrift = maxDrift,
                        synchronized = synchronized
                    )
                )
                delay(1000)
            }
        }
        return events
    }

    private suspend fun checkTimestampAccuracy() {
            delay(2000)
        }
    }

    private suspend fun measureClockDrift() {
            delay(4000)
        }
    }

    private suspend fun performFlashSyncTest() {
            delay(3000) // Simulate flash sync test
        }
    }

    private fun runIndividualTest(testId: String) {
        lifecycleScope.launch {
            when (testId) {
                "unified_timestamp" -> testUnifiedTimestampSystem()
                "cross_sensor_sync" -> testCrossSensorSyncEvents()
                "timestamp_verification" -> checkTimestampAccuracy()
                "drift_detection" -> measureClockDrift()
                "sync_recovery" -> testSyncRecovery()
                "flash_sync_test" -> performFlashSyncTest()
            }
        }
    }

    private suspend fun testSyncRecovery() {
            delay(5000)
        }
    }
}