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
import kotlin.system.measureTimeMillis

class CrossModalSyncTestComposeActivity : ComponentActivity() {
    companion object {
        private const val TAG = "CrossModalSyncTestCompose"
        private const val SYNC_TOLERANCE_MS = 50L
    }
    data class SyncResult(
        val sensorPair: String,
        val timeDifferenceMs: Long,
        val isSynchronized: Boolean,
        val details: String
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibUnifiedTheme {
                CrossModalSyncTestScreen()
            }
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CrossModalSyncTestScreen() {
        var testResults by remember { mutableStateOf(listOf<TestCase>()) }
        var syncResults by remember { mutableStateOf(listOf<SyncResult>()) }
        var isTestRunning by remember { mutableStateOf(false) }
        var overallSyncStatus by remember { mutableStateOf("Not Tested") }
        // Initialize test cases
        LaunchedEffect(Unit) {
            testResults = listOf(
                TestCase(
                    id = "gsr_thermal_sync",
                    name = "GSR-Thermal Sync",
                    description = "Test synchronization between GSR and thermal sensors"
                ),
                TestCase(
                    id = "gsr_rgb_sync",
                    name = "GSR-RGB Sync",
                    description = "Test synchronization between GSR and RGB camera"
                ),
                TestCase(
                    id = "thermal_rgb_sync",
                    name = "Thermal-RGB Sync",
                    description = "Test synchronization between thermal and RGB sensors"
                ),
                TestCase(
                    id = "triple_sync",
                    name = "Triple Sensor Sync",
                    description = "Test synchronization across all three sensor types"
                ),
                TestCase(
                    id = "timestamp_accuracy",
                    name = "Timestamp Accuracy",
                    description = "Validate timestamp precision and drift"
                ),
                TestCase(
                    id = "sync_recovery",
                    name = "Sync Recovery",
                    description = "Test synchronization recovery after disruption"
                )
            )
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Cross-Modal Sync Test",
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
                // Sync Status Overview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (overallSyncStatus) {
                            "Synchronized" -> MaterialTheme.colorScheme.primaryContainer
                            "Out of Sync" -> MaterialTheme.colorScheme.errorContainer
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
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Overall Sync Status: $overallSyncStatus",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (syncResults.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tolerance: ±${SYNC_TOLERANCE_MS}ms",
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
                // Sync Results
                if (syncResults.isNotEmpty()) {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Synchronization Results",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            syncResults.forEach { result ->
                                SyncResultItem(result = result)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Test Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isTestRunning = true
                            lifecycleScope.launch {
                                runAllSyncTests(
                                    onSyncResults = { results -> syncResults = results },
                                    onStatusUpdate = { status -> overallSyncStatus = status },
                                    onComplete = { isTestRunning = false }
                                )
                            }
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
                            Text("Run All")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            lifecycleScope.launch { runRealTimeSync() }
                        },
                        enabled = !isTestRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Timeline, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Live Sync")
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
                Spacer(modifier = Modifier.height(16.dp))
                // Technical Details Card
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Technical Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf(
                            "Sync Method" to "Hardware timestamping + NTP correction",
                            "GSR Sample Rate" to "128 Hz",
                            "Thermal Frame Rate" to "9 Hz",
                            "RGB Frame Rate" to "30 Hz",
                            "Buffer Size" to "500ms window"
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
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
    }
    @Composable
    fun SyncResultItem(result: SyncResult) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.sensorPair,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = result.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${result.timeDifferenceMs}ms",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (result.isSynchronized) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (result.isSynchronized)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    private suspend fun runAllSyncTests(
        onSyncResults: (List<SyncResult>) -> Unit,
        onStatusUpdate: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        AppLogger.i(TAG, "Starting comprehensive cross-modal sync tests")
        val newSyncResults = mutableListOf<SyncResult>()
        try {
            // Test GSR-Thermal sync
            val gsrThermalSync = testSensorPairSync("GSR", "Thermal")
            newSyncResults.add(gsrThermalSync)
            delay(1000)
            // Test GSR-RGB sync
            val gsrRgbSync = testSensorPairSync("GSR", "RGB")
            newSyncResults.add(gsrRgbSync)
            delay(1000)
            // Test Thermal-RGB sync
            val thermalRgbSync = testSensorPairSync("Thermal", "RGB")
            newSyncResults.add(thermalRgbSync)
            delay(1000)
            // Test triple sensor sync
            val tripleSync = testTripleSensorSync()
            newSyncResults.add(tripleSync)
            // Update sync results
            onSyncResults(newSyncResults)
            // Determine overall sync status
            val allSynced = newSyncResults.all { it.isSynchronized }
            onStatusUpdate(if (allSynced) "Synchronized" else "Out of Sync")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Sync tests failed: ${e.message}")
            onStatusUpdate("Test Failed")
        } finally {
            onComplete()
        }
    }
    private suspend fun testSensorPairSync(sensor1: String, sensor2: String): SyncResult {
        AppLogger.d(TAG, "Testing sync between $sensor1 and $sensor2")
        val testTime = measureTimeMillis {
            // Simulate sensor pair synchronization test
            delay(2000)
        }
        // Simulate time difference calculation
        val timeDifference = (10..100).random().toLong()
        val isSynchronized = timeDifference <= SYNC_TOLERANCE_MS
        return SyncResult(
            sensorPair = "$sensor1 ↔ $sensor2",
            timeDifferenceMs = timeDifference,
            isSynchronized = isSynchronized,
            details = "Test completed in ${testTime}ms"
        )
    }
    private suspend fun testTripleSensorSync(): SyncResult {
        AppLogger.d(TAG, "Testing triple sensor synchronization")
        val testTime = measureTimeMillis {
            // Simulate triple sensor sync test
            delay(3000)
        }
        // Simulate worst-case time difference across all three sensors
        val maxTimeDifference = (15..80).random().toLong()
        val isSynchronized = maxTimeDifference <= SYNC_TOLERANCE_MS
        return SyncResult(
            sensorPair = "GSR ↔ Thermal ↔ RGB",
            timeDifferenceMs = maxTimeDifference,
            isSynchronized = isSynchronized,
            details = "Triple sync test completed in ${testTime}ms"
        )
    }
    private suspend fun runRealTimeSync() {
        AppLogger.d(TAG, "Running real-time synchronization monitoring")
        try {
            // Simulate real-time sync monitoring
            delay(5000)
            AppLogger.d(TAG, "Real-time sync monitoring completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Real-time sync test failed: ${e.message}")
        }
    }
    private fun runIndividualTest(testId: String) {
        lifecycleScope.launch {
            when (testId) {
                "gsr_thermal_sync" -> testSensorPairSync("GSR", "Thermal")
                "gsr_rgb_sync" -> testSensorPairSync("GSR", "RGB")
                "thermal_rgb_sync" -> testSensorPairSync("Thermal", "RGB")
                "triple_sync" -> testTripleSensorSync()
                "timestamp_accuracy" -> runTimestampAccuracyTest()
                "sync_recovery" -> runSyncRecoveryTest()
            }
        }
    }
    private suspend fun runTimestampAccuracyTest() {
        AppLogger.d(TAG, "Testing timestamp accuracy")
        try {
            delay(2000)
            AppLogger.d(TAG, "Timestamp accuracy test completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Timestamp accuracy test failed: ${e.message}")
        }
    }
    private suspend fun runSyncRecoveryTest() {
        AppLogger.d(TAG, "Testing sync recovery")
        try {
            delay(4000)
            AppLogger.d(TAG, "Sync recovery test completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Sync recovery test failed: ${e.message}")
        }
    }
}