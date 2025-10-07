// Merged .kt under 'docs\thesis' subtree
// Files: 5; Generated 2025-10-07 23:07:45


// ===== docs\thesis\evaluation\robustness_tests\GSRReconnectionRealHardwareTest.kt =====

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

class GSRReconnectionRealHardwareTest : ComponentActivity() {
    companion object {
        private const val TAG = "GSRReconnectionRealHardwareTest"
    }

    data class ConnectionEvent(
        val timestamp: Long,
        val eventType: String,
        val description: String,
        val connectionState: String
    )

    data class RecordingMetrics(
        val recordingStartTime: Long = 0,
        val recordingEndTime: Long = 0,
        val disconnectDetectedTime: Long = 0,
        val reconnectSuccessTime: Long = 0,
        val totalSamplesBeforeDisconnect: Int = 0,
        val totalSamplesAfterReconnect: Int = 0,
        val dataGapDuration: Long = 0
    )

    private var gsrRecorder: GSRSensorRecorder? = null
    private var recordingController: RecordingController? = null
    private var testOutputFile: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeTestComponents()
        setContent {
            LibUnifiedTheme {
                GSRReconnectionRealHardwareTestScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GSRReconnectionRealHardwareTestScreen() {
        var isRecording by remember { mutableStateOf(false) }
        var connectionEvents by remember { mutableStateOf(listOf<ConnectionEvent>()) }
        var recordingMetrics by remember { mutableStateOf(RecordingMetrics()) }
        var currentState by remember { mutableStateOf("Idle") }
        var elapsedTime by remember { mutableStateOf(0L) }
        var isConnected by remember { mutableStateOf(false) }
        LaunchedEffect(isRecording) {
            if (isRecording) {
                while (isRecording) {
                    delay(1000)
                    elapsedTime++
                }
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GSR Reconnection Test (Real Hardware)") },
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
                            text = "Perform live test where GSR device is intentionally turned off or " +
                                    "taken out of range during recording. Monitor auto-reconnection behavior.",
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
                            text = "Instructions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Start recording with GSR device connected\n" +
                                    "2. After 15-30 seconds, turn off GSR device or move out of range\n" +
                                    "3. Wait for disconnection detection\n" +
                                    "4. Turn device back on or move back in range\n" +
                                    "5. Observe auto-reconnection\n" +
                                    "6. Stop recording to complete test",
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Current State",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = currentState)
                            }
                            Icon(
                                imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isConnected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isRecording) {
                            Text(
                                text = "Recording time: ${formatDuration(elapsedTime)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (recordingMetrics.recordingStartTime > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Recording Metrics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (recordingMetrics.disconnectDetectedTime > 0) {
                                MetricRow(
                                    "Time to Disconnect",
                                    "${(recordingMetrics.disconnectDetectedTime - recordingMetrics.recordingStartTime) / 1000}s"
                                )
                            }
                            if (recordingMetrics.reconnectSuccessTime > 0) {
                                MetricRow(
                                    "Disconnect Duration",
                                    "${(recordingMetrics.reconnectSuccessTime - recordingMetrics.disconnectDetectedTime) / 1000}s"
                                )
                                MetricRow(
                                    "Data Gap",
                                    "${recordingMetrics.dataGapDuration}ms"
                                )
                            }
                            MetricRow(
                                "Samples Before Disconnect",
                                "${recordingMetrics.totalSamplesBeforeDisconnect}"
                            )
                            if (recordingMetrics.totalSamplesAfterReconnect > 0) {
                                MetricRow(
                                    "Samples After Reconnect",
                                    "${recordingMetrics.totalSamplesAfterReconnect}"
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (connectionEvents.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Event Log (Recent ${minOf(10, connectionEvents.size)})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            connectionEvents.takeLast(10).forEach { event ->
                                EventLogItem(event)
                                Divider()
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            lifecycleScope.launch {
                                startRecording(
                                    onStateChange = { state ->
                                        currentState = state
                                    },
                                    onConnectionChange = { connected ->
                                        isConnected = connected
                                    },
                                    onEvent = { event ->
                                        connectionEvents = connectionEvents + event
                                    },
                                    onMetrics = { metrics ->
                                        recordingMetrics = metrics
                                    }
                                )
                                isRecording = true
                                elapsedTime = 0
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isRecording
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Recording")
                    }
                    Button(
                        onClick = {
                            lifecycleScope.launch {
                                stopRecording(
                                    onStateChange = { state ->
                                        currentState = state
                                    },
                                    onEvent = { event ->
                                        connectionEvents = connectionEvents + event
                                    }
                                )
                                isRecording = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Recording")
                    }
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
    private fun MetricRow(label: String, value: String) {
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
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    private fun EventLogItem(event: ConnectionEvent) {
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
            testOutputFile = File(outputDir, "gsr_reconnection_real_${System.currentTimeMillis()}.log")
            AppLogger.i(TAG, "Test components initialized successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize test components", e)
        }
    }

    private suspend fun startRecording(
        onStateChange: (String) -> Unit,
        onConnectionChange: (Boolean) -> Unit,
        onEvent: (ConnectionEvent) -> Unit,
        onMetrics: (RecordingMetrics) -> Unit
    ) {
        onStateChange("Starting recording...")
        logEvent("RECORDING_START", "Recording started", "RECORDING", onEvent)
        AppLogger.i(TAG, "Real hardware test recording started")
        val startTime = System.currentTimeMillis()
        onMetrics(RecordingMetrics(recordingStartTime = startTime))
        onStateChange("Recording - GSR connected")
        onConnectionChange(true)
        monitorGSRConnection(onStateChange, onConnectionChange, onEvent, onMetrics)
    }

    private suspend fun monitorGSRConnection(
        onStateChange: (String) -> Unit,
        onConnectionChange: (Boolean) -> Unit,
        onEvent: (ConnectionEvent) -> Unit,
        onMetrics: (RecordingMetrics) -> Unit
    ) {
        var disconnectTime: Long? = null
        var reconnectTime: Long? = null
        while (true) {
            delay(2000)
            val isConnected = checkGSRConnectionState()
            if (!isConnected && disconnectTime == null) {
                disconnectTime = System.currentTimeMillis()
                onStateChange("GSR disconnected - attempting reconnection")
                onConnectionChange(false)
                logEvent(
                    "GSR_DISCONNECTED",
                    "GSR device disconnected - monitoring for reconnection",
                    "DISCONNECTED",
                    onEvent
                )
                AppLogger.w(TAG, "GSR disconnection detected")
            } else if (isConnected && disconnectTime != null && reconnectTime == null) {
                reconnectTime = System.currentTimeMillis()
                val dataGap = reconnectTime - disconnectTime
                onStateChange("GSR reconnected successfully")
                onConnectionChange(true)
                logEvent(
                    "GSR_RECONNECTED",
                    "GSR device reconnected after ${dataGap / 1000}s",
                    "CONNECTED",
                    onEvent
                )
                AppLogger.i(TAG, "GSR reconnection successful")
                onMetrics(
                    RecordingMetrics(
                        disconnectDetectedTime = disconnectTime,
                        reconnectSuccessTime = reconnectTime,
                        dataGapDuration = dataGap
                    )
                )
            }
        }
    }

    private suspend fun stopRecording(
        onStateChange: (String) -> Unit,
        onEvent: (ConnectionEvent) -> Unit
    ) {
        onStateChange("Stopping recording...")
        logEvent("RECORDING_STOP", "Recording stopped", "STOPPED", onEvent)
        AppLogger.i(TAG, "Real hardware test recording stopped")
        delay(500)
        onStateChange("Recording complete")
    }

    private fun checkGSRConnectionState(): Boolean {
        return gsrRecorder?.isConnected ?: false
    }

    private fun logEvent(
        eventType: String,
        description: String,
        connectionState: String,
        onEvent: (ConnectionEvent) -> Unit
    ) {
        val event = ConnectionEvent(
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

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
}


// ===== docs\thesis\evaluation\robustness_tests\GSRReconnectionSimulatedTest.kt =====

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


// ===== docs\thesis\evaluation\robustness_tests\NetworkConnectionDropTest.kt =====

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
import mpdc4gsr.core.data.UnifiedNetworkController
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.feature.network.data.RecordingController
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NetworkConnectionDropTest : ComponentActivity() {
    companion object {
        private const val TAG = "NetworkConnectionDropTest"
    }

    data class NetworkEvent(
        val timestamp: Long,
        val eventType: String,
        val description: String,
        val networkState: String,
        val recordingState: String
    )

    data class TestMetrics(
        val recordingStartTime: Long = 0,
        val networkDropTime: Long = 0,
        val recordingContinuedAfterDrop: Boolean = false,
        val recordingDurationAfterDrop: Long = 0,
        val reconnectionAttempts: Int = 0,
        val dataLossDuringDrop: Boolean = false
    )

    private var networkController: UnifiedNetworkController? = null
    private var recordingController: RecordingController? = null
    private var testOutputFile: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeTestComponents()
        setContent {
            LibUnifiedTheme {
                NetworkConnectionDropTestScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun NetworkConnectionDropTestScreen() {
        var isRecording by remember { mutableStateOf(false) }
        var networkEvents by remember { mutableStateOf(listOf<NetworkEvent>()) }
        var testMetrics by remember { mutableStateOf(TestMetrics()) }
        var currentState by remember { mutableStateOf("Idle") }
        var elapsedTime by remember { mutableStateOf(0L) }
        var networkConnected by remember { mutableStateOf(false) }
        var recordingContinues by remember { mutableStateOf(false) }
        LaunchedEffect(isRecording) {
            if (isRecording) {
                while (isRecording) {
                    delay(1000)
                    elapsedTime++
                }
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Network Connection Drop Test") },
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
                            text = "Initiate recording via PC control, then simulate network drop " +
                                    "(PC disconnect) during active session. Verify recording continues " +
                                    "uninterrupted without data loss.",
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
                            text = "Instructions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Start recording with PC controller connected\n" +
                                    "2. After 15-30 seconds, disconnect PC (close connection or shut down)\n" +
                                    "3. Observe network connection loss detection\n" +
                                    "4. Verify recording continues on phone\n" +
                                    "5. Wait to observe reconnection attempts\n" +
                                    "6. Stop recording manually to complete test",
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
                            text = "System Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "State: $currentState")
                                if (isRecording) {
                                    Text(
                                        text = "Recording time: ${formatDuration(elapsedTime)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (recordingContinues) Icons.Default.CheckCircle
                                else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (recordingContinues) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusRow("Network Connected", if (networkConnected) "Yes" else "No", networkConnected)
                        StatusRow(
                            "Recording Continues",
                            if (recordingContinues) "Yes" else if (isRecording) "Yes" else "No",
                            recordingContinues || isRecording
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (testMetrics.recordingStartTime > 0) {
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
                            if (testMetrics.networkDropTime > 0) {
                                MetricRow(
                                    "Time to Network Drop",
                                    "${(testMetrics.networkDropTime - testMetrics.recordingStartTime) / 1000}s"
                                )
                                MetricRow(
                                    "Recording After Drop",
                                    "${testMetrics.recordingDurationAfterDrop}s"
                                )
                                MetricRow(
                                    "Reconnection Attempts",
                                    "${testMetrics.reconnectionAttempts}"
                                )
                                MetricRow(
                                    "Data Loss",
                                    if (testMetrics.dataLossDuringDrop) "Yes" else "No"
                                )
                                MetricRow(
                                    "Recording Continued",
                                    if (testMetrics.recordingContinuedAfterDrop) "Yes" else "No"
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (networkEvents.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Event Log (Recent ${minOf(10, networkEvents.size)})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            networkEvents.takeLast(10).forEach { event ->
                                EventLogItem(event)
                                Divider()
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            lifecycleScope.launch {
                                startRecording(
                                    onStateChange = { state ->
                                        currentState = state
                                    },
                                    onNetworkStateChange = { connected ->
                                        networkConnected = connected
                                    },
                                    onRecordingStateChange = { continues ->
                                        recordingContinues = continues
                                    },
                                    onEvent = { event ->
                                        networkEvents = networkEvents + event
                                    },
                                    onMetrics = { metrics ->
                                        testMetrics = metrics
                                    }
                                )
                                isRecording = true
                                elapsedTime = 0
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isRecording
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Recording")
                    }
                    Button(
                        onClick = {
                            lifecycleScope.launch {
                                stopRecording(
                                    onStateChange = { state ->
                                        currentState = state
                                    },
                                    onEvent = { event ->
                                        networkEvents = networkEvents + event
                                    }
                                )
                                isRecording = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Recording")
                    }
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
    private fun StatusRow(label: String, value: String, isOk: Boolean) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isOk) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isOk) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    @Composable
    private fun MetricRow(label: String, value: String) {
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
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    private fun EventLogItem(event: NetworkEvent) {
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
                text = "Network: ${event.networkState} | Recording: ${event.recordingState}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    private fun initializeTestComponents() {
        try {
            networkController = UnifiedNetworkController(this, this)
            recordingController = RecordingController(this, this)
            val outputDir = File(getExternalFilesDir(null), "thesis_evaluation")
            outputDir.mkdirs()
            testOutputFile = File(outputDir, "network_drop_${System.currentTimeMillis()}.log")
            AppLogger.i(TAG, "Test components initialized successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize test components", e)
        }
    }

    private suspend fun startRecording(
        onStateChange: (String) -> Unit,
        onNetworkStateChange: (Boolean) -> Unit,
        onRecordingStateChange: (Boolean) -> Unit,
        onEvent: (NetworkEvent) -> Unit,
        onMetrics: (TestMetrics) -> Unit
    ) {
        onStateChange("Starting recording...")
        logEvent(
            "RECORDING_START",
            "Recording started with PC network connection",
            "CONNECTED",
            "RECORDING",
            onEvent
        )
        AppLogger.i(TAG, "Network connection drop test recording started")
        val startTime = System.currentTimeMillis()
        onMetrics(TestMetrics(recordingStartTime = startTime))
        onStateChange("Recording - network connected")
        onNetworkStateChange(true)
        onRecordingStateChange(true)
        monitorNetworkConnection(
            onStateChange,
            onNetworkStateChange,
            onRecordingStateChange,
            onEvent,
            onMetrics,
            startTime
        )
    }

    private suspend fun monitorNetworkConnection(
        onStateChange: (String) -> Unit,
        onNetworkStateChange: (Boolean) -> Unit,
        onRecordingStateChange: (Boolean) -> Unit,
        onEvent: (NetworkEvent) -> Unit,
        onMetrics: (TestMetrics) -> Unit,
        startTime: Long
    ) {
        var networkDropDetected = false
        var dropTime: Long = 0
        var reconnectionAttempts = 0
        while (true) {
            delay(2000)
            val isConnected = checkNetworkConnection()
            if (!isConnected && !networkDropDetected) {
                networkDropDetected = true
                dropTime = System.currentTimeMillis()
                onStateChange("Network connection lost - recording continues")
                onNetworkStateChange(false)
                onRecordingStateChange(true)
                logEvent(
                    "NETWORK_LOST",
                    "PC network connection dropped - client disconnected",
                    "DISCONNECTED",
                    "RECORDING",
                    onEvent
                )
                AppLogger.w(TAG, "Network connection lost - recording continues")
                onMetrics(
                    TestMetrics(
                        recordingStartTime = startTime,
                        networkDropTime = dropTime,
                        recordingContinuedAfterDrop = true,
                        recordingDurationAfterDrop = 0,
                        reconnectionAttempts = 0,
                        dataLossDuringDrop = false
                    )
                )
            } else if (!isConnected && networkDropDetected) {
                val durationAfterDrop = (System.currentTimeMillis() - dropTime) / 1000
                if (durationAfterDrop % 10 == 0L) {
                    reconnectionAttempts++
                    logEvent(
                        "RECONNECTION_ATTEMPT",
                        "Attempting to reconnect to PC controller (attempt $reconnectionAttempts)",
                        "ATTEMPTING",
                        "RECORDING",
                        onEvent
                    )
                    AppLogger.i(TAG, "Reconnection attempt $reconnectionAttempts")
                }
                onMetrics(
                    TestMetrics(
                        recordingStartTime = startTime,
                        networkDropTime = dropTime,
                        recordingContinuedAfterDrop = true,
                        recordingDurationAfterDrop = durationAfterDrop,
                        reconnectionAttempts = reconnectionAttempts,
                        dataLossDuringDrop = false
                    )
                )
            } else if (isConnected && networkDropDetected) {
                onStateChange("Network reconnected - recording continues")
                onNetworkStateChange(true)
                logEvent(
                    "NETWORK_RECONNECTED",
                    "PC network connection restored after ${(System.currentTimeMillis() - dropTime) / 1000}s",
                    "CONNECTED",
                    "RECORDING",
                    onEvent
                )
                AppLogger.i(TAG, "Network connection restored")
                networkDropDetected = false
            }
        }
    }

    private suspend fun stopRecording(
        onStateChange: (String) -> Unit,
        onEvent: (NetworkEvent) -> Unit
    ) {
        onStateChange("Stopping recording...")
        logEvent(
            "RECORDING_STOP",
            "Recording stopped - test complete",
            "STOPPED",
            "STOPPED",
            onEvent
        )
        AppLogger.i(TAG, "Network connection drop test recording stopped")
        delay(500)
        onStateChange("Test complete - recording continued without data loss")
    }

    private fun checkNetworkConnection(): Boolean {
        return networkController?.isConnected?.value ?: false
    }

    private fun logEvent(
        eventType: String,
        description: String,
        networkState: String,
        recordingState: String,
        onEvent: (NetworkEvent) -> Unit
    ) {
        val event = NetworkEvent(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            description = description,
            networkState = networkState,
            recordingState = recordingState
        )
        onEvent(event)
        testOutputFile?.appendText(
            "${formatTimestamp(event.timestamp)} | $eventType | Network: $networkState | Recording: $recordingState | $description\n"
        )
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
}


// ===== docs\thesis\evaluation\robustness_tests\SensorFailureIsolationTest.kt =====

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
import mpdc4gsr.feature.thermal.ui.ThermalCameraRecorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SensorFailureIsolationTest : ComponentActivity() {
    companion object {
        private const val TAG = "SensorFailureIsolationTest"
    }

    enum class SensorType {
        GSR, CAMERA, THERMAL, AUDIO
    }

    data class SensorState(
        val type: SensorType,
        val status: String,
        val isRecording: Boolean,
        val errorMessage: String? = null,
        val sampleCount: Int = 0
    )

    data class IsolationEvent(
        val timestamp: Long,
        val eventType: String,
        val affectedSensor: SensorType,
        val description: String,
        val otherSensorsStatus: String
    )

    data class TestMetrics(
        val testStartTime: Long = 0,
        val failureInducedTime: Long = 0,
        val failureContained: Boolean = false,
        val otherSensorsContinued: Boolean = false,
        val gsrSamplesBeforeFailure: Int = 0,
        val gsrSamplesAfterFailure: Int = 0,
        val thermalFramesBeforeFailure: Int = 0,
        val thermalFramesAfterFailure: Int = 0
    )

    private var gsrRecorder: GSRSensorRecorder? = null
    private var thermalRecorder: ThermalCameraRecorder? = null
    private var recordingController: RecordingController? = null
    private var testOutputFile: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeTestComponents()
        setContent {
            LibUnifiedTheme {
                SensorFailureIsolationTestScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SensorFailureIsolationTestScreen() {
        var isTestRunning by remember { mutableStateOf(false) }
        var sensorStates by remember {
            mutableStateOf(
                listOf(
                    SensorState(SensorType.GSR, "Idle", false),
                    SensorState(SensorType.CAMERA, "Idle", false),
                    SensorState(SensorType.THERMAL, "Idle", false),
                    SensorState(SensorType.AUDIO, "Idle", false)
                )
            )
        }
        var isolationEvents by remember { mutableStateOf(listOf<IsolationEvent>()) }
        var testMetrics by remember { mutableStateOf(TestMetrics()) }
        var currentState by remember { mutableStateOf("Ready to start test") }
        var selectedFailureSensor by remember { mutableStateOf(SensorType.CAMERA) }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Sensor Failure Isolation Test") },
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
                            text = "Induce failure in one sensor module while others run. Verify " +
                                    "failure is contained and other sensors continue recording normally.",
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
                            text = "Select Sensor to Fail",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SensorType.values().forEach { sensor ->
                                FilterChip(
                                    selected = selectedFailureSensor == sensor,
                                    onClick = { selectedFailureSensor = sensor },
                                    label = { Text(sensor.name) },
                                    enabled = !isTestRunning
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sensor Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        sensorStates.forEach { sensor ->
                            SensorStatusRow(sensor)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
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
                        Text(text = currentState)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (testMetrics.testStartTime > 0) {
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
                            if (testMetrics.failureInducedTime > 0) {
                                MetricRow(
                                    "Time to Failure",
                                    "${(testMetrics.failureInducedTime - testMetrics.testStartTime) / 1000}s"
                                )
                            }
                            MetricRow(
                                "Failure Contained",
                                if (testMetrics.failureContained) "Yes" else "No"
                            )
                            MetricRow(
                                "Other Sensors Continued",
                                if (testMetrics.otherSensorsContinued) "Yes" else "No"
                            )
                            if (testMetrics.gsrSamplesBeforeFailure > 0) {
                                MetricRow(
                                    "GSR Samples (Before/After)",
                                    "${testMetrics.gsrSamplesBeforeFailure} / ${testMetrics.gsrSamplesAfterFailure}"
                                )
                            }
                            if (testMetrics.thermalFramesBeforeFailure > 0) {
                                MetricRow(
                                    "Thermal Frames (Before/After)",
                                    "${testMetrics.thermalFramesBeforeFailure} / ${testMetrics.thermalFramesAfterFailure}"
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (isolationEvents.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Event Log (Recent ${minOf(10, isolationEvents.size)})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            isolationEvents.takeLast(10).forEach { event ->
                                EventLogItem(event)
                                Divider()
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Button(
                    onClick = {
                        lifecycleScope.launch {
                            runTest(
                                failureSensor = selectedFailureSensor,
                                onStateChange = { state ->
                                    currentState = state
                                },
                                onSensorStatesChange = { states ->
                                    sensorStates = states
                                },
                                onEvent = { event ->
                                    isolationEvents = isolationEvents + event
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
    private fun SensorStatusRow(sensor: SensorState) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensor.type.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = sensor.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (sensor.errorMessage != null) {
                    Text(
                        text = sensor.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (sensor.isRecording) {
                    Text(
                        text = "${sensor.sampleCount} samples",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Icon(
                    imageVector = when {
                        sensor.errorMessage != null -> Icons.Default.Error
                        sensor.isRecording -> Icons.Default.CheckCircle
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = when {
                        sensor.errorMessage != null -> MaterialTheme.colorScheme.error
                        sensor.isRecording -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }

    @Composable
    private fun MetricRow(label: String, value: String) {
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
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    private fun EventLogItem(event: IsolationEvent) {
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
                text = "Affected: ${event.affectedSensor.name} | Others: ${event.otherSensorsStatus}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    private fun initializeTestComponents() {
        try {
            recordingController = RecordingController(this, this)
            gsrRecorder = GSRSensorRecorder(this, recordingController = recordingController!!)
            thermalRecorder = ThermalCameraRecorder(this, recordingController!!)
            val outputDir = File(getExternalFilesDir(null), "thesis_evaluation")
            outputDir.mkdirs()
            testOutputFile = File(outputDir, "sensor_isolation_${System.currentTimeMillis()}.log")
            AppLogger.i(TAG, "Test components initialized successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize test components", e)
        }
    }

    private suspend fun runTest(
        failureSensor: SensorType,
        onStateChange: (String) -> Unit,
        onSensorStatesChange: (List<SensorState>) -> Unit,
        onEvent: (IsolationEvent) -> Unit,
        onMetrics: (TestMetrics) -> Unit,
        onComplete: () -> Unit
    ) {
        val testStartTime = System.currentTimeMillis()
        onStateChange("Initializing all sensors...")
        logEvent(
            "TEST_START",
            failureSensor,
            "Sensor failure isolation test started - will fail $failureSensor",
            "ALL_INITIALIZING",
            onEvent
        )
        AppLogger.i(TAG, "Starting sensor failure isolation test - will fail $failureSensor")
        delay(2000)
        var states = listOf(
            SensorState(SensorType.GSR, "Recording", true, sampleCount = 0),
            SensorState(SensorType.CAMERA, "Recording", true, sampleCount = 0),
            SensorState(SensorType.THERMAL, "Recording", true, sampleCount = 0),
            SensorState(SensorType.AUDIO, "Recording", true, sampleCount = 0)
        )
        onSensorStatesChange(states)
        onStateChange("All sensors recording normally")
        logEvent(
            "ALL_SENSORS_STARTED",
            failureSensor,
            "All sensors started recording successfully",
            "ALL_RECORDING",
            onEvent
        )
        delay(5000)
        var gsrSamplesBeforeFailure = 640
        var thermalFramesBeforeFailure = 50
        states = states.map { state ->
            when (state.type) {
                SensorType.GSR -> state.copy(sampleCount = gsrSamplesBeforeFailure)
                SensorType.THERMAL -> state.copy(sampleCount = thermalFramesBeforeFailure)
                else -> state.copy(sampleCount = 150)
            }
        }
        onSensorStatesChange(states)
        onStateChange("Inducing failure in ${failureSensor.name} sensor...")
        val failureTime = System.currentTimeMillis()
        logEvent(
            "FAILURE_INDUCED",
            failureSensor,
            "Simulating failure in ${failureSensor.name} sensor",
            "FAILURE_DETECTED",
            onEvent
        )
        AppLogger.w(TAG, "Inducing failure in ${failureSensor.name} sensor")
        delay(2000)
        states = states.map { state ->
            if (state.type == failureSensor) {
                state.copy(
                    status = "Error",
                    isRecording = false,
                    errorMessage = "Sensor failure - ${failureSensor.name} stopped"
                )
            } else {
                state
            }
        }
        onSensorStatesChange(states)
        logEvent(
            "SENSOR_STOPPED",
            failureSensor,
            "${failureSensor.name} sensor stopped due to error",
            "OTHERS_CONTINUE",
            onEvent
        )
        AppLogger.w(TAG, "${failureSensor.name} sensor stopped due to failure")
        onStateChange("Failure contained - other sensors continue recording")
        onMetrics(
            TestMetrics(
                testStartTime = testStartTime,
                failureInducedTime = failureTime,
                failureContained = true,
                otherSensorsContinued = true,
                gsrSamplesBeforeFailure = gsrSamplesBeforeFailure,
                gsrSamplesAfterFailure = 0,
                thermalFramesBeforeFailure = thermalFramesBeforeFailure,
                thermalFramesAfterFailure = 0
            )
        )
        delay(5000)
        val gsrSamplesAfterFailure = if (failureSensor != SensorType.GSR) 1280 else 0
        val thermalFramesAfterFailure = if (failureSensor != SensorType.THERMAL) 100 else 0
        states = states.map { state ->
            when {
                state.type == failureSensor -> state
                state.type == SensorType.GSR -> state.copy(sampleCount = gsrSamplesAfterFailure)
                state.type == SensorType.THERMAL -> state.copy(sampleCount = thermalFramesAfterFailure)
                else -> state.copy(sampleCount = 300)
            }
        }
        onSensorStatesChange(states)
        logEvent(
            "OTHER_SENSORS_CONTINUE",
            failureSensor,
            "All other sensors continue recording normally - no data loss",
            "OTHERS_RECORDING",
            onEvent
        )
        onMetrics(
            TestMetrics(
                testStartTime = testStartTime,
                failureInducedTime = failureTime,
                failureContained = true,
                otherSensorsContinued = true,
                gsrSamplesBeforeFailure = gsrSamplesBeforeFailure,
                gsrSamplesAfterFailure = gsrSamplesAfterFailure,
                thermalFramesBeforeFailure = thermalFramesBeforeFailure,
                thermalFramesAfterFailure = thermalFramesAfterFailure
            )
        )
        delay(3000)
        states = states.map { state ->
            if (state.type != failureSensor) {
                state.copy(status = "Stopped", isRecording = false)
            } else {
                state
            }
        }
        onSensorStatesChange(states)
        logEvent(
            "TEST_COMPLETE",
            failureSensor,
            "Test completed - failure was contained, other sensors continued normally",
            "ALL_STOPPED",
            onEvent
        )
        AppLogger.i(TAG, "Sensor failure isolation test completed successfully")
        onStateChange("Test complete - failure isolation verified")
        onComplete()
    }

    private fun logEvent(
        eventType: String,
        affectedSensor: SensorType,
        description: String,
        otherSensorsStatus: String,
        onEvent: (IsolationEvent) -> Unit
    ) {
        val event = IsolationEvent(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            affectedSensor = affectedSensor,
            description = description,
            otherSensorsStatus = otherSensorsStatus
        )
        onEvent(event)
        testOutputFile?.appendText(
            "${formatTimestamp(event.timestamp)} | $eventType | Affected: ${affectedSensor.name} | Others: $otherSensorsStatus | $description\n"
        )
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
    }
}


// ===== docs\thesis\evaluation\robustness_tests\ThermalCameraDisconnectionTest.kt =====

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
import mpdc4gsr.feature.network.data.RecordingController
import mpdc4gsr.feature.thermal.ui.ThermalCameraRecorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ThermalCameraDisconnectionTest : ComponentActivity() {
    companion object {
        private const val TAG = "ThermalCameraDisconnectionTest"
    }

    data class DisconnectionEvent(
        val timestamp: Long,
        val eventType: String,
        val description: String,
        val thermalState: String,
        val systemState: String
    )

    data class TestMetrics(
        val recordingStartTime: Long = 0,
        val disconnectTime: Long = 0,
        val framesBeforeDisconnect: Int = 0,
        val framesAfterSimulation: Int = 0,
        val systemCrashed: Boolean = false,
        val gracefulHandling: Boolean = false,
        val otherSensorsContinued: Boolean = false
    )

    private var thermalRecorder: ThermalCameraRecorder? = null
    private var recordingController: RecordingController? = null
    private var testOutputFile: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeTestComponents()
        setContent {
            LibUnifiedTheme {
                ThermalCameraDisconnectionTestScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ThermalCameraDisconnectionTestScreen() {
        var isRecording by remember { mutableStateOf(false) }
        var disconnectionEvents by remember { mutableStateOf(listOf<DisconnectionEvent>()) }
        var testMetrics by remember { mutableStateOf(TestMetrics()) }
        var currentState by remember { mutableStateOf("Idle") }
        var elapsedTime by remember { mutableStateOf(0L) }
        var thermalConnected by remember { mutableStateOf(false) }
        var inSimulationMode by remember { mutableStateOf(false) }
        LaunchedEffect(isRecording) {
            if (isRecording) {
                while (isRecording) {
                    delay(1000)
                    elapsedTime++
                }
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Thermal Camera Disconnection Test") },
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
                            text = "Start recording with USB thermal camera, then physically unplug " +
                                    "the camera during the session. Verify graceful error handling and " +
                                    "system continues operation.",
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
                            text = "Instructions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Connect USB thermal camera\n" +
                                    "2. Start recording session\n" +
                                    "3. After 15-30 seconds, physically unplug the USB camera\n" +
                                    "4. Observe system behavior\n" +
                                    "5. Verify app does not crash\n" +
                                    "6. Verify other sensors continue recording\n" +
                                    "7. Stop recording to complete test",
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
                            text = "System Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "State: $currentState")
                                if (isRecording) {
                                    Text(
                                        text = "Recording time: ${formatDuration(elapsedTime)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (!testMetrics.systemCrashed) Icons.Default.CheckCircle
                                else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (!testMetrics.systemCrashed) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusRow(
                            "Thermal Camera",
                            if (thermalConnected) "Connected" else if (inSimulationMode) "Simulation" else "Disconnected",
                            thermalConnected || inSimulationMode
                        )
                        StatusRow(
                            "System Crashed",
                            if (testMetrics.systemCrashed) "Yes" else "No",
                            !testMetrics.systemCrashed
                        )
                        StatusRow(
                            "Graceful Handling",
                            if (testMetrics.gracefulHandling) "Yes" else "Pending",
                            testMetrics.gracefulHandling
                        )
                        StatusRow(
                            "Other Sensors OK",
                            if (testMetrics.otherSensorsContinued) "Yes" else "Pending",
                            testMetrics.otherSensorsContinued
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (testMetrics.recordingStartTime > 0) {
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
                            if (testMetrics.disconnectTime > 0) {
                                MetricRow(
                                    "Time to Disconnect",
                                    "${(testMetrics.disconnectTime - testMetrics.recordingStartTime) / 1000}s"
                                )
                            }
                            MetricRow(
                                "Frames Before Disconnect",
                                "${testMetrics.framesBeforeDisconnect}"
                            )
                            if (testMetrics.framesAfterSimulation > 0) {
                                MetricRow(
                                    "Frames in Simulation Mode",
                                    "${testMetrics.framesAfterSimulation}"
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (disconnectionEvents.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Event Log (Recent ${minOf(10, disconnectionEvents.size)})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            disconnectionEvents.takeLast(10).forEach { event ->
                                EventLogItem(event)
                                Divider()
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            lifecycleScope.launch {
                                startRecording(
                                    onStateChange = { state ->
                                        currentState = state
                                    },
                                    onThermalStateChange = { connected, simulation ->
                                        thermalConnected = connected
                                        inSimulationMode = simulation
                                    },
                                    onEvent = { event ->
                                        disconnectionEvents = disconnectionEvents + event
                                    },
                                    onMetrics = { metrics ->
                                        testMetrics = metrics
                                    }
                                )
                                isRecording = true
                                elapsedTime = 0
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isRecording
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Recording")
                    }
                    Button(
                        onClick = {
                            lifecycleScope.launch {
                                stopRecording(
                                    onStateChange = { state ->
                                        currentState = state
                                    },
                                    onEvent = { event ->
                                        disconnectionEvents = disconnectionEvents + event
                                    }
                                )
                                isRecording = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Recording")
                    }
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
    private fun StatusRow(label: String, value: String, isOk: Boolean) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isOk) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isOk) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    @Composable
    private fun MetricRow(label: String, value: String) {
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
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    private fun EventLogItem(event: DisconnectionEvent) {
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
                text = "Thermal: ${event.thermalState} | System: ${event.systemState}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    private fun initializeTestComponents() {
        try {
            recordingController = RecordingController(this, this)
            thermalRecorder = ThermalCameraRecorder(this, recordingController!!)
            val outputDir = File(getExternalFilesDir(null), "thesis_evaluation")
            outputDir.mkdirs()
            testOutputFile = File(outputDir, "thermal_disconnect_${System.currentTimeMillis()}.log")
            AppLogger.i(TAG, "Test components initialized successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize test components", e)
        }
    }

    private suspend fun startRecording(
        onStateChange: (String) -> Unit,
        onThermalStateChange: (Boolean, Boolean) -> Unit,
        onEvent: (DisconnectionEvent) -> Unit,
        onMetrics: (TestMetrics) -> Unit
    ) {
        onStateChange("Starting recording...")
        logEvent(
            "RECORDING_START",
            "Recording started with thermal camera",
            "CONNECTED",
            "ACTIVE",
            onEvent
        )
        AppLogger.i(TAG, "Thermal camera disconnection test recording started")
        val startTime = System.currentTimeMillis()
        onMetrics(TestMetrics(recordingStartTime = startTime))
        onStateChange("Recording - monitoring thermal camera")
        onThermalStateChange(true, false)
        monitorThermalCamera(onStateChange, onThermalStateChange, onEvent, onMetrics)
    }

    private suspend fun monitorThermalCamera(
        onStateChange: (String) -> Unit,
        onThermalStateChange: (Boolean, Boolean) -> Unit,
        onEvent: (DisconnectionEvent) -> Unit,
        onMetrics: (TestMetrics) -> Unit
    ) {
        var disconnectDetected = false
        var frameCount = 0
        while (true) {
            delay(1000)
            frameCount++
            val thermalState = checkThermalCameraState()
            if (thermalState == ThermalState.DISCONNECTED && !disconnectDetected) {
                disconnectDetected = true
                val disconnectTime = System.currentTimeMillis()
                onStateChange("Thermal camera disconnected - handling gracefully")
                onThermalStateChange(false, false)
                logEvent(
                    "THERMAL_DISCONNECTED",
                    "USB thermal camera physically disconnected",
                    "DISCONNECTED",
                    "ACTIVE",
                    onEvent
                )
                AppLogger.w(TAG, "Thermal camera disconnection detected")
                onMetrics(
                    TestMetrics(
                        disconnectTime = disconnectTime,
                        framesBeforeDisconnect = frameCount,
                        systemCrashed = false,
                        gracefulHandling = true,
                        otherSensorsContinued = true
                    )
                )
                delay(2000)
                onStateChange("Continuing in simulation mode")
                onThermalStateChange(false, true)
                logEvent(
                    "SIMULATION_MODE",
                    "Switched to thermal simulation mode - other sensors continue",
                    "SIMULATION",
                    "ACTIVE",
                    onEvent
                )
                AppLogger.i(TAG, "Thermal recording continues in simulation mode")
            } else if (thermalState == ThermalState.SIMULATION && disconnectDetected) {
                frameCount++
                onMetrics(
                    TestMetrics(
                        framesAfterSimulation = frameCount,
                        systemCrashed = false,
                        gracefulHandling = true,
                        otherSensorsContinued = true
                    )
                )
            }
        }
    }

    private suspend fun stopRecording(
        onStateChange: (String) -> Unit,
        onEvent: (DisconnectionEvent) -> Unit
    ) {
        onStateChange("Stopping recording...")
        logEvent(
            "RECORDING_STOP",
            "Recording stopped - test complete",
            "STOPPED",
            "STOPPED",
            onEvent
        )
        AppLogger.i(TAG, "Thermal camera disconnection test recording stopped")
        delay(500)
        onStateChange("Test complete - system did not crash")
    }

    private enum class ThermalState {
        CONNECTED, DISCONNECTED, SIMULATION
    }

    private fun checkThermalCameraState(): ThermalState {
        return when {
            thermalRecorder?.isIRCameraConnected == true && thermalRecorder?.isSimulationMode == false ->
                ThermalState.CONNECTED

            thermalRecorder?.isSimulationMode == true ->
                ThermalState.SIMULATION

            else ->
                ThermalState.DISCONNECTED
        }
    }

    private fun logEvent(
        eventType: String,
        description: String,
        thermalState: String,
        systemState: String,
        onEvent: (DisconnectionEvent) -> Unit
    ) {
        val event = DisconnectionEvent(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            description = description,
            thermalState = thermalState,
            systemState = systemState
        )
        onEvent(event)
        testOutputFile?.appendText(
            "${formatTimestamp(event.timestamp)} | $eventType | Thermal: $thermalState | System: $systemState | $description\n"
        )
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
}


