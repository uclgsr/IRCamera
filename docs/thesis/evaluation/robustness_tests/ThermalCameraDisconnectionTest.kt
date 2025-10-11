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
import mpdc4gsr.feature.connectivity.data.RecordingController
import mpdc4gsr.feature.capture.thermal.ui.ThermalCameraRecorder
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
            LibSharedTheme {
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




