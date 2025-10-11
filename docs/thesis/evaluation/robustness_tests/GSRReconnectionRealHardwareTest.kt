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
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.common.AppLogger
import mpdc4gsr.feature.capture.gsr.data.GSRSensorRecorder
import mpdc4gsr.feature.connectivity.data.RecordingController
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
            LibSharedTheme {
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




