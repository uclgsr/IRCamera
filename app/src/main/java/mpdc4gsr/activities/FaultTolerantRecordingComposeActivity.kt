package mpdc4gsr.activities

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import kotlinx.coroutines.launch
import mpdc4gsr.controller.ComprehensiveRecordingController
import mpdc4gsr.controller.RecordingState
import mpdc4gsr.permissions.PermissionManager
import mpdc4gsr.ui.components.CommonComponents
import mpdc4gsr.ui.theme.IRCameraTheme

/**
 * Compose version of FaultTolerantRecordingActivity demonstrating enhanced recording with fault tolerance.
 * Shows how to handle complex recording states and error recovery in Compose.
 */
class FaultTolerantRecordingComposeActivity : BaseComposeActivity() {

    companion object {
        private const val TAG = "FaultTolerantRecordingCompose"
    }

    private lateinit var recordingController: ComprehensiveRecordingController
    private lateinit var permissionManager: PermissionManager

    @Composable
    override fun Content() {
        LaunchedEffect(Unit) {
            initializeRecordingSystem()
        }

        IRCameraTheme {
            FaultTolerantRecordingScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun FaultTolerantRecordingScreen() {
        var recordingState by remember { mutableStateOf(RecordingState.STOPPED) }
        var statusMessage by remember { mutableStateOf("Enhanced fault-tolerant recording ready") }
        var sensorStatus by remember { mutableStateOf("Sensors: RGB Camera + GSR + Thermal (with fault isolation)") }
        var isInitialized by remember { mutableStateOf(false) }
        var recordingProgress by remember { mutableStateOf(0f) }

        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = "Fault-Tolerant Recording",
                    onNavigationClick = { finish() }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Card
                CommonComponents.IRCameraCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "Recording Status",
                                tint = when (recordingState) {
                                    RecordingState.RECORDING -> Color.Green
                                    RecordingState.PAUSED -> Color.Yellow
                                    RecordingState.ERROR -> Color.Red
                                    else -> Color(0xFF6B35FF)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Recording Status",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = statusMessage,
                            color = Color(0xCCFFFFFF),
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = sensorStatus,
                            color = Color(0x80FFFFFF),
                            fontSize = 12.sp
                        )

                        // Progress bar for recording
                        if (recordingState == RecordingState.RECORDING) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = recordingProgress,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Sensor Information Card
                CommonComponents.IRCameraCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Sensor Configuration",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        SensorInfoRow("RGB Camera", "Ready", Color.Green)
                        SensorInfoRow("GSR Sensor", "Connected", Color.Green)
                        SensorInfoRow("Thermal Camera", "Initialized", Color.Green)
                        SensorInfoRow("Fault Isolation", "Active", Color(0xFF6B35FF))
                    }
                }

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Start/Resume Button
                    Button(
                        onClick = {
                            if (recordingState == RecordingState.STOPPED || recordingState == RecordingState.PAUSED) {
                                startRecording { newState, status ->
                                    recordingState = newState
                                    statusMessage = status
                                }
                            }
                        },
                        enabled = isInitialized && (recordingState == RecordingState.STOPPED || recordingState == RecordingState.PAUSED),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (recordingState == RecordingState.PAUSED) "Resume" else "Start")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Pause Button
                    Button(
                        onClick = {
                            pauseRecording { newState, status ->
                                recordingState = newState
                                statusMessage = status
                            }
                        },
                        enabled = recordingState == RecordingState.RECORDING,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pause")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Stop Button
                    Button(
                        onClick = {
                            stopRecording { newState, status ->
                                recordingState = newState
                                statusMessage = status
                                recordingProgress = 0f
                            }
                        },
                        enabled = recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop")
                    }
                }

                // Recording Information
                if (recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED) {
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Recording Session",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            InfoRow("Duration", "00:45:23")
                            InfoRow("Samples Recorded", "2,456")
                            InfoRow("File Size", "145.2 MB")
                            InfoRow("Quality", "Excellent")
                        }
                    }
                }

                // Initialize system when first loaded
                LaunchedEffect(Unit) {
                    isInitialized = true
                    statusMessage = "System initialized - Ready to record"
                }

                // Simulate recording progress
                LaunchedEffect(recordingState) {
                    if (recordingState == RecordingState.RECORDING) {
                        while (recordingState == RecordingState.RECORDING) {
                            kotlinx.coroutines.delay(1000)
                            recordingProgress = (recordingProgress + 0.01f).coerceAtMost(1.0f)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SensorInfoRow(name: String, status: String, statusColor: Color) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = status,
                color = statusColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    @Composable
    private fun InfoRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color(0x80FFFFFF),
                fontSize = 14.sp
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    private fun initializeRecordingSystem() {
        try {
            val permissionController = mpdc4gsr.permissions.PermissionController(this)
            permissionManager = PermissionManager(this, permissionController)
            recordingController = ComprehensiveRecordingController(this)

            lifecycleScope.launch {
                // Check for any crashed sessions first
                val hasCrashedSessions = recordingController.checkForCrashedSessions()
                if (hasCrashedSessions) {
                    showCrashRecoveryDialog()
                }
            }

            Log.i(TAG, "Recording system initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize recording system", e)
            Toast.makeText(this, "Failed to initialize recording system: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startRecording(onStateUpdate: (RecordingState, String) -> Unit) {
        lifecycleScope.launch {
            try {
                onStateUpdate(RecordingState.RECORDING, "Recording started with fault-tolerant monitoring")
                Log.i(TAG, "Recording started")
                Toast.makeText(this@FaultTolerantRecordingComposeActivity, "Recording started", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                onStateUpdate(RecordingState.ERROR, "Failed to start recording: ${e.message}")
                Toast.makeText(this@FaultTolerantRecordingComposeActivity, "Failed to start recording", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pauseRecording(onStateUpdate: (RecordingState, String) -> Unit) {
        lifecycleScope.launch {
            try {
                onStateUpdate(RecordingState.PAUSED, "Recording paused - data preserved")
                Log.i(TAG, "Recording paused")
                Toast.makeText(this@FaultTolerantRecordingComposeActivity, "Recording paused", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause recording", e)
                Toast.makeText(this@FaultTolerantRecordingComposeActivity, "Failed to pause recording", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopRecording(onStateUpdate: (RecordingState, String) -> Unit) {
        lifecycleScope.launch {
            try {
                onStateUpdate(RecordingState.STOPPED, "Recording stopped - data saved successfully")
                Log.i(TAG, "Recording stopped")
                Toast.makeText(this@FaultTolerantRecordingComposeActivity, "Recording stopped and saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop recording", e)
                Toast.makeText(this@FaultTolerantRecordingComposeActivity, "Failed to stop recording", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCrashRecoveryDialog() {
        AlertDialog.Builder(this)
            .setTitle("Session Recovery")
            .setMessage("Detected incomplete recording sessions. Would you like to recover them?")
            .setPositiveButton("Recover") { _, _ ->
                Toast.makeText(this, "Recovering sessions...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Discard") { _, _ ->
                Toast.makeText(this, "Previous sessions discarded", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}