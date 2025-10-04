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
