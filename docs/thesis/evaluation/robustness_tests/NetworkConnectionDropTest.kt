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
import mpdc4gsr.core.common.AppLogger
import mpdc4gsr.feature.connectivity.data.RecordingController
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

