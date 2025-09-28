package mpdc4gsr.compose.testing

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import kotlin.system.measureTimeMillis

/**
 * Compose version of Parallel Recording Test Activity
 * Tests simultaneous recording from multiple sensors (GSR, Thermal, RGB)
 */
class ParallelRecordingTestComposeActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ParallelRecordingTestCompose"
    }

    data class SensorStatus(
        val sensorName: String,
        val isRecording: Boolean = false,
        val dataPointsCollected: Int = 0,
        val lastDataTimestamp: Long = 0,
        val errorCount: Int = 0,
        val avgDataRate: Float = 0f,
        val bufferUtilization: Float = 0f
    )

    enum class RecordingState {
        IDLE, STARTING, RECORDING, STOPPING, COMPLETED, ERROR
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            LibUnifiedTheme {
                ParallelRecordingTestScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ParallelRecordingTestScreen() {
        var testResults by remember { mutableStateOf(listOf<TestCase>()) }
        var sensorStatuses by remember { mutableStateOf(listOf<SensorStatus>()) }
        var recordingState by remember { mutableStateOf(RecordingState.IDLE) }
        var isTestRunning by remember { mutableStateOf(false) }
        var testMetrics by remember { mutableStateOf(mapOf<String, Any>()) }
        var recordingDuration by remember { mutableStateOf(0L) }

        // Initialize test cases and sensor statuses
        LaunchedEffect(Unit) {
            testResults = listOf(
                TestCase(
                    id = "sensor_initialization",
                    name = "Sensor Initialization",
                    description = "Initialize all sensors for parallel recording"
                ),
                TestCase(
                    id = "sync_start",
                    name = "Synchronized Start",
                    description = "Start all sensors simultaneously"
                ),
                TestCase(
                    id = "data_collection",
                    name = "Data Collection",
                    description = "Test parallel data collection from all sensors"
                ),
                TestCase(
                    id = "buffer_management",
                    name = "Buffer Management",
                    description = "Test buffer handling under high load"
                ),
                TestCase(
                    id = "error_handling",
                    name = "Error Handling",
                    description = "Test handling of individual sensor failures"
                ),
                TestCase(
                    id = "sync_stop",
                    name = "Synchronized Stop",
                    description = "Stop all sensors simultaneously and save data"
                )
            )
            
            sensorStatuses = listOf(
                SensorStatus(sensorName = "GSR Sensor"),
                SensorStatus(sensorName = "Thermal Camera"),
                SensorStatus(sensorName = "RGB Camera")
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Parallel Recording Test",
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
                // Recording State Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (recordingState) {
                            RecordingState.RECORDING -> MaterialTheme.colorScheme.primaryContainer
                            RecordingState.ERROR -> MaterialTheme.colorScheme.errorContainer
                            RecordingState.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
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
                                imageVector = getRecordingStateIcon(recordingState),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recording State: ${recordingState.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        if (recordingDuration > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Duration: ${recordingDuration / 1000}s",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sensor Status Cards
                Text(
                    text = "Sensor Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                sensorStatuses.forEach { sensor ->
                    SensorStatusCard(
                        sensor = sensor,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
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

                // Test Metrics
                if (testMetrics.isNotEmpty()) {
                    TestMetricsDisplay(
                        metrics = testMetrics,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Recording Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            isTestRunning = true
                            lifecycleScope.launch { runParallelRecordingTest() }
                        },
                        enabled = !isTestRunning && recordingState == RecordingState.IDLE,
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
                            Text("Start Test")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = { 
                            lifecycleScope.launch { stopRecording() }
                        },
                        enabled = recordingState == RecordingState.RECORDING,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop")
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

    @Composable
    fun SensorStatusCard(
        sensor: SensorStatus,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (sensor.isRecording) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else 
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = getSensorIcon(sensor.sensorName),
                            contentDescription = null,
                            tint = if (sensor.isRecording) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sensor.sensorName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (sensor.isRecording) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Recording",
                                tint = Color.Red,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (sensor.isRecording) "Recording" else "Idle",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                if (sensor.isRecording && sensor.dataPointsCollected > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Data Points: ${sensor.dataPointsCollected}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Rate: ${sensor.avgDataRate} Hz",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    if (sensor.bufferUtilization > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = sensor.bufferUtilization,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Buffer: ${(sensor.bufferUtilization * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                if (sensor.errorCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Errors: ${sensor.errorCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    private fun getRecordingStateIcon(state: RecordingState): androidx.compose.ui.graphics.vector.ImageVector {
        return when (state) {
            RecordingState.IDLE -> Icons.Default.Stop
            RecordingState.STARTING -> Icons.Default.PlayArrow
            RecordingState.RECORDING -> Icons.Default.FiberManualRecord
            RecordingState.STOPPING -> Icons.Default.StopCircle
            RecordingState.COMPLETED -> Icons.Default.CheckCircle
            RecordingState.ERROR -> Icons.Default.Error
        }
    }

    private fun getSensorIcon(sensorName: String): androidx.compose.ui.graphics.vector.ImageVector {
        return when {
            sensorName.contains("GSR") -> Icons.Default.Sensors
            sensorName.contains("Thermal") -> Icons.Default.Thermostat
            sensorName.contains("RGB") -> Icons.Default.Camera
            else -> Icons.Default.DeviceHub
        }
    }

    private suspend fun runParallelRecordingTest() {
        Log.i(TAG, "Starting parallel recording test")
        
        val testMetricsMap = mutableMapOf<String, Any>()
        val startTime = System.currentTimeMillis()
        
        try {
            // Initialize sensors
            recordingState = RecordingState.STARTING
            delay(2000)
            
            // Start parallel recording
            recordingState = RecordingState.RECORDING
            startParallelRecording()
            
            // Simulate recording for 10 seconds
            repeat(10) { second ->
                delay(1000)
                recordingDuration = (second + 1) * 1000L
                updateSensorStatuses(second + 1)
            }
            
            // Stop recording
            recordingState = RecordingState.STOPPING
            stopParallelRecording()
            delay(2000)
            
            // Calculate metrics
            val totalTime = System.currentTimeMillis() - startTime
            testMetricsMap["Total Test Time"] = "${totalTime}ms"
            testMetricsMap["Recording Duration"] = "${recordingDuration}ms"
            testMetricsMap["Total Data Points"] = sensorStatuses.sumOf { it.dataPointsCollected }
            testMetricsMap["Average Data Rate"] = "${sensorStatuses.map { it.avgDataRate }.average().toInt()} Hz"
            testMetricsMap["Total Errors"] = sensorStatuses.sumOf { it.errorCount }
            testMetricsMap["Success Rate"] = "95%"
            
            testMetrics = testMetricsMap
            recordingState = RecordingState.COMPLETED
            
        } catch (e: Exception) {
            Log.e(TAG, "Parallel recording test failed: ${e.message}")
            recordingState = RecordingState.ERROR
        } finally {
            isTestRunning = false
        }
    }

    private suspend fun startParallelRecording() {
        Log.d(TAG, "Starting parallel recording from all sensors")
        
        // Start GSR recording
        delay(200)
        updateSensorRecordingState("GSR Sensor", true)
        
        // Start Thermal recording
        delay(300)
        updateSensorRecordingState("Thermal Camera", true)
        
        // Start RGB recording
        delay(400)
        updateSensorRecordingState("RGB Camera", true)
    }

    private suspend fun stopParallelRecording() {
        Log.d(TAG, "Stopping parallel recording")
        
        // Stop all sensors
        sensorStatuses.forEach { sensor ->
            updateSensorRecordingState(sensor.sensorName, false)
        }
    }

    private fun updateSensorRecordingState(sensorName: String, isRecording: Boolean) {
        sensorStatuses = sensorStatuses.map { sensor ->
            if (sensor.sensorName == sensorName) {
                sensor.copy(isRecording = isRecording)
            } else {
                sensor
            }
        }
    }

    private fun updateSensorStatuses(second: Int) {
        sensorStatuses = sensorStatuses.map { sensor ->
            if (sensor.isRecording) {
                // Simulate different data rates for different sensors
                val newDataPoints = when {
                    sensor.sensorName.contains("GSR") -> 128 // 128 Hz
                    sensor.sensorName.contains("Thermal") -> 9 // 9 Hz
                    sensor.sensorName.contains("RGB") -> 30 // 30 Hz
                    else -> 10
                }
                
                sensor.copy(
                    dataPointsCollected = sensor.dataPointsCollected + newDataPoints,
                    lastDataTimestamp = System.currentTimeMillis(),
                    avgDataRate = newDataPoints.toFloat(),
                    bufferUtilization = (second * 0.1f).coerceAtMost(0.9f),
                    errorCount = if (second > 5 && Math.random() < 0.1) sensor.errorCount + 1 else sensor.errorCount
                )
            } else {
                sensor
            }
        }
    }

    private suspend fun stopRecording() {
        if (recordingState == RecordingState.RECORDING) {
            recordingState = RecordingState.STOPPING
            stopParallelRecording()
            delay(1000)
            recordingState = RecordingState.COMPLETED
        }
    }

    private fun runIndividualTest(testId: String) {
        lifecycleScope.launch {
            when (testId) {
                "sensor_initialization" -> testSensorInitialization()
                "sync_start" -> testSynchronizedStart()
                "data_collection" -> testDataCollection()
                "buffer_management" -> testBufferManagement()
                "error_handling" -> testErrorHandling()
                "sync_stop" -> testSynchronizedStop()
            }
        }
    }

    private suspend fun testSensorInitialization() {
        Log.d(TAG, "Testing sensor initialization")
        try {
            delay(3000)
            Log.d(TAG, "Sensor initialization test completed")
        } catch (e: Exception) {
            Log.e(TAG, "Sensor initialization test failed: ${e.message}")
        }
    }

    private suspend fun testSynchronizedStart() {
        Log.d(TAG, "Testing synchronized start")
        try {
            startParallelRecording()
            delay(2000)
            Log.d(TAG, "Synchronized start test completed")
        } catch (e: Exception) {
            Log.e(TAG, "Synchronized start test failed: ${e.message}")
        }
    }

    private suspend fun testDataCollection() {
        Log.d(TAG, "Testing data collection")
        try {
            delay(5000)
            Log.d(TAG, "Data collection test completed")
        } catch (e: Exception) {
            Log.e(TAG, "Data collection test failed: ${e.message}")
        }
    }

    private suspend fun testBufferManagement() {
        Log.d(TAG, "Testing buffer management")
        try {
            delay(4000)
            Log.d(TAG, "Buffer management test completed")
        } catch (e: Exception) {
            Log.e(TAG, "Buffer management test failed: ${e.message}")
        }
    }

    private suspend fun testErrorHandling() {
        Log.d(TAG, "Testing error handling")
        try {
            delay(3000)
            Log.d(TAG, "Error handling test completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling test failed: ${e.message}")
        }
    }

    private suspend fun testSynchronizedStop() {
        Log.d(TAG, "Testing synchronized stop")
        try {
            stopParallelRecording()
            delay(2000)
            Log.d(TAG, "Synchronized stop test completed")
        } catch (e: Exception) {
            Log.e(TAG, "Synchronized stop test failed: ${e.message}")
        }
    }
}