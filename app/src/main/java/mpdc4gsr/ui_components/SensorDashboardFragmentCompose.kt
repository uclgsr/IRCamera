package mpdc4gsr.ui_components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.csl.irCamera.R
import kotlinx.coroutines.delay
import mpdc4gsr.compose.base.SimpleComposeFragment

/**
 * Compose migration of SensorDashboardFragment
 * 
 * Enhanced Comprehensive Sensor Status Dashboard Fragment using Compose
 * Implements all TODO requirements:
 * - Clear UI indicators for each sensor's status (connected, streaming, error)
 * - Connection status indicators for each sensor  
 * - Prominent recording indicator with timer
 * - Simulation mode warnings
 * - Real-time sensor error notifications
 * - Collapsible/expandable sensor container with smooth animations
 */
class SensorDashboardFragmentCompose : SimpleComposeFragment() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var isRecording by remember { mutableStateOf(false) }
        var recordingTime by remember { mutableStateOf(0L) }
        var isCollapsed by remember { mutableStateOf(false) }
        var currentSessionId by remember { mutableStateOf<String?>(null) }
        
        // Simulate recording timer
        LaunchedEffect(isRecording) {
            if (isRecording) {
                val startTime = System.currentTimeMillis()
                while (isRecording) {
                    recordingTime = System.currentTimeMillis() - startTime
                    delay(1000)
                }
            }
        }

        // Sample sensor data - in real implementation this would come from ViewModels
        val sensors = remember {
            listOf(
                SensorData("thermal_camera", "TC001 Thermal Camera", SensorType.THERMAL, SensorStatus.CONNECTED),
                SensorData("rgb_camera", "RGB Camera", SensorType.RGB, SensorStatus.STREAMING),
                SensorData("shimmer_gsr", "Shimmer GSR Sensor", SensorType.GSR, SensorStatus.CONNECTED, 
                    multiDeviceInfo = MultiDeviceInfo(2, 1, 4)),
                SensorData("audio_recorder", "Audio Recorder", SensorType.AUDIO, SensorStatus.DISCONNECTED)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dashboard title
            item {
                Text(
                    text = "Sensor Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Overall status and recording indicator
            item {
                OverallStatusCard(
                    isRecording = isRecording,
                    recordingTime = recordingTime,
                    sessionId = currentSessionId,
                    activeSensorCount = sensors.count { it.isConnected() },
                    totalSensorCount = sensors.size,
                    errorSensorCount = sensors.count { it.hasError() },
                    onToggleRecording = { 
                        isRecording = !isRecording
                        currentSessionId = if (isRecording) "SESSION_${System.currentTimeMillis()}" else null
                    }
                )
            }

            // Collapsible sensors section
            item {
                SensorsSection(
                    sensors = sensors,
                    isCollapsed = isCollapsed,
                    onToggleCollapse = { isCollapsed = !isCollapsed }
                )
            }
        }
    }

    @Composable
    private fun OverallStatusCard(
        isRecording: Boolean,
        recordingTime: Long,
        sessionId: String?,
        activeSensorCount: Int,
        totalSensorCount: Int,
        errorSensorCount: Int,
        onToggleRecording: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isRecording -> MaterialTheme.colorScheme.errorContainer
                    errorSensorCount > 0 -> MaterialTheme.colorScheme.errorContainer
                    activeSensorCount == 0 -> MaterialTheme.colorScheme.surfaceVariant
                    activeSensorCount < totalSensorCount -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isRecording -> "RECORDING - Session: ${sessionId ?: "Unknown"}"
                                errorSensorCount > 0 -> "$errorSensorCount Sensor Error(s) Detected"
                                activeSensorCount == 0 -> "No Sensors Connected"
                                activeSensorCount < totalSensorCount -> "$activeSensorCount/$totalSensorCount Sensors Connected"
                                else -> "All Sensors Connected & Ready"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (isRecording) {
                            Text(
                                text = "RECORDING: ${formatElapsedTime(recordingTime)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "Ready to Record",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Recording indicator
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = if (isRecording) Color.Red else Color.Gray,
                                shape = CircleShape
                            )
                    )
                }
                
                // Recording control button
                Button(
                    onClick = onToggleRecording,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isRecording) "Stop Recording" else "Start Recording")
                }
            }
        }
    }

    @Composable
    private fun SensorsSection(
        sensors: List<SensorData>,
        isCollapsed: Boolean,
        onToggleCollapse: () -> Unit
    ) {
        val rotationAngle by animateFloatAsState(
            targetValue = if (isCollapsed) -90f else 0f,
            animationSpec = tween(300),
            label = "collapse_rotation"
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.animateContentSize()
            ) {
                // Header with collapse/expand functionality
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sensors (${sensors.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onToggleCollapse) {
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = if (isCollapsed) "Expand" else "Collapse",
                            modifier = Modifier.rotate(rotationAngle)
                        )
                    }
                }
                
                // Sensors list (only shown when not collapsed)
                if (!isCollapsed) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sensors.forEach { sensor ->
                            SensorStatusCard(sensor = sensor)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    @Composable
    private fun SensorStatusCard(sensor: SensorData) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = sensor.status.getColor(),
                            shape = CircleShape
                        )
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sensor.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = sensor.status.getDisplayText(),
                        style = MaterialTheme.typography.bodySmall,
                        color = sensor.status.getColor()
                    )
                    
                    // Show additional info for specific sensors
                    when {
                        sensor.type == SensorType.GSR && sensor.multiDeviceInfo != null -> {
                            Text(
                                text = "Multi-device: " +
                                    "${sensor.multiDeviceInfo.connectedCount}/${sensor.multiDeviceInfo.maxDevices} connected, " +
                                    "${sensor.multiDeviceInfo.streamingCount} streaming",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        sensor.isSimulation -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Simulation",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFFFFA500) // Orange for simulation warning
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Using simulated data - no hardware detected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFA500)
                                )
                            }
                        }
                        sensor.errorMessage != null -> {
                            Text(
                                text = "Error: ${sensor.errorMessage}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                // Sensor type icon
                Icon(
                    painter = painterResource(id = sensor.type.getIconRes()),
                    contentDescription = sensor.type.name,
                    modifier = Modifier.size(24.dp),
                    tint = sensor.status.getColor()
                )
            }
        }
    }

    private fun formatElapsedTime(elapsedMs: Long): String {
        val seconds = (elapsedMs / 1000) % 60
        val minutes = (elapsedMs / (1000 * 60)) % 60
        val hours = (elapsedMs / (1000 * 60 * 60))

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    // Data classes and enums
    data class SensorData(
        val id: String,
        val displayName: String,
        val type: SensorType,
        val status: SensorStatus,
        val errorMessage: String? = null,
        val isSimulation: Boolean = false,
        val multiDeviceInfo: MultiDeviceInfo? = null
    ) {
        fun isConnected(): Boolean = status in listOf(SensorStatus.CONNECTED, SensorStatus.STREAMING)
        fun hasError(): Boolean = status == SensorStatus.ERROR
    }

    data class MultiDeviceInfo(
        val connectedCount: Int,
        val streamingCount: Int,
        val maxDevices: Int
    )

    enum class SensorType {
        THERMAL, RGB, GSR, AUDIO;
        
        fun getIconRes(): Int = when (this) {
            THERMAL -> R.drawable.ic_thermal_camera
            RGB -> R.drawable.ic_camera
            GSR -> R.drawable.ic_gsr_sensor
            AUDIO -> R.drawable.ic_microphone
        }
    }

    enum class SensorStatus {
        DISCONNECTED, CONNECTING, CONNECTED, STREAMING, ERROR, SIMULATION;
        
        fun getColor(): Color = when (this) {
            DISCONNECTED -> Color.Gray
            CONNECTING -> Color(0xFFFFA500) // Orange
            CONNECTED -> Color.Green
            STREAMING -> Color.Blue
            ERROR -> Color.Red
            SIMULATION -> Color(0xFFFFA500) // Orange
        }
        
        fun getDisplayText(): String = when (this) {
            DISCONNECTED -> "Disconnected"
            CONNECTING -> "Connecting..."
            CONNECTED -> "Connected"
            STREAMING -> "Streaming"
            ERROR -> "Error"
            SIMULATION -> "Simulation Mode"
        }
    }
}