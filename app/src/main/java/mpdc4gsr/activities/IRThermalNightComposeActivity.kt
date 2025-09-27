package mpdc4gsr.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import com.mpdc4gsr.libunified.ui.theme.IRCameraTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.ui.components.CommonComponents
import java.text.SimpleDateFormat
import java.util.*

enum class ThermalMode {
    NORMAL, HIGH_CONTRAST, NIGHT_VISION, RAINBOW
}

enum class RecordingState {
    IDLE, RECORDING, PAUSED
}

class IRThermalNightComposeActivity : BaseComposeActivity() {
    
    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on during thermal imaging
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContent {
            IRCameraTheme {
                Content()
            }
        }
        
        checkCameraPermission()
    }
    
    @Composable
    override fun Content() {
        var thermalMode by remember { mutableStateOf(ThermalMode.NORMAL) }
        var recordingState by remember { mutableStateOf(RecordingState.IDLE) }
        var recordingDuration by remember { mutableStateOf(0L) }
        var isConnected by remember { mutableStateOf(false) }
        var currentTemp by remember { mutableStateOf(25.4f) }
        var minTemp by remember { mutableStateOf(18.2f) }
        var maxTemp by remember { mutableStateOf(35.8f) }
        var temperatureScale by remember { mutableStateOf("°C") }
        var showSettings by remember { mutableStateOf(false) }
        
        val context = LocalContext.current
        
        // Simulate real-time temperature updates
        LaunchedEffect(isConnected) {
            if (isConnected) {
                while (true) {
                    delay(1000)
                    currentTemp = (20f + Math.random() * 20f).toFloat()
                    minTemp = currentTemp - (5f + Math.random() * 5f).toFloat()
                    maxTemp = currentTemp + (5f + Math.random() * 10f).toFloat()
                    
                    if (recordingState == RecordingState.RECORDING) {
                        recordingDuration++
                    }
                }
            }
        }
        
        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = "Thermal Night Vision",
                    onNavigateBack = { finish() },
                    actions = {
                        // Connection Status
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = "Connection Status",
                            tint = if (isConnected) Color.Green else Color.Red,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        
                        // Settings
                        IconButton(
                            onClick = { showSettings = !showSettings }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                ThermalControlsBottomBar(
                    thermalMode = thermalMode,
                    onThermalModeChange = { thermalMode = it },
                    recordingState = recordingState,
                    onRecordingStateChange = { newState ->
                        recordingState = newState
                        if (newState == RecordingState.RECORDING) {
                            recordingDuration = 0
                        }
                    },
                    isConnected = isConnected
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (showSettings) {
                    ThermalSettings(
                        temperatureScale = temperatureScale,
                        onTemperatureScaleChange = { temperatureScale = it },
                        onDismiss = { showSettings = false }
                    )
                }
                
                // Connection Status
                if (!isConnected) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Thermal Camera Disconnected",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Please connect your thermal camera device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { isConnected = true }
                            ) {
                                Text("Connect")
                            }
                        }
                    }
                }
                
                // Thermal Camera View
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isConnected) {
                            // Simulate thermal camera view
                            ThermalCameraView(
                                thermalMode = thermalMode,
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Temperature overlay
                            ThermalOverlay(
                                currentTemp = currentTemp,
                                minTemp = minTemp,
                                maxTemp = maxTemp,
                                temperatureScale = temperatureScale,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                            
                            // Recording indicator
                            if (recordingState == RecordingState.RECORDING) {
                                RecordingIndicator(
                                    duration = recordingDuration,
                                    modifier = Modifier.align(Alignment.TopEnd)
                                )
                            }
                        } else {
                            // Placeholder view
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Camera",
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Thermal Camera View",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
                
                // Temperature Information
                if (isConnected) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TemperatureDisplay(
                                label = "Current",
                                temperature = currentTemp,
                                scale = temperatureScale,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TemperatureDisplay(
                                label = "Min",
                                temperature = minTemp,
                                scale = temperatureScale,
                                color = Color.Blue
                            )
                            TemperatureDisplay(
                                label = "Max",
                                temperature = maxTemp,
                                scale = temperatureScale,
                                color = Color.Red
                            )
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun ThermalCameraView(
        thermalMode: ThermalMode,
        modifier: Modifier = Modifier
    ) {
        val backgroundColor = when (thermalMode) {
            ThermalMode.NORMAL -> Color(0xFF1A1A2E)
            ThermalMode.HIGH_CONTRAST -> Color(0xFF0F0F23)
            ThermalMode.NIGHT_VISION -> Color(0xFF0A0A1A)
            ThermalMode.RAINBOW -> Color(0xFF2A1A3E)
        }
        
        Box(
            modifier = modifier
                .background(backgroundColor)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${thermalMode.name} MODE",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
    
    @Composable
    private fun ThermalOverlay(
        currentTemp: Float,
        minTemp: Float,
        maxTemp: Float,
        temperatureScale: String,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "TEMP: ${"%.1f".format(currentTemp)}$temperatureScale",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Range: ${"%.1f".format(minTemp)} - ${"%.1f".format(maxTemp)}$temperatureScale",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    
    @Composable
    private fun RecordingIndicator(
        duration: Long,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Red.copy(alpha = 0.9f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatDuration(duration),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    
    @Composable
    private fun TemperatureDisplay(
        label: String,
        temperature: Float,
        scale: String,
        color: Color
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${"%.1f".format(temperature)}$scale",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
    
    @Composable
    private fun ThermalControlsBottomBar(
        thermalMode: ThermalMode,
        onThermalModeChange: (ThermalMode) -> Unit,
        recordingState: RecordingState,
        onRecordingStateChange: (RecordingState) -> Unit,
        isConnected: Boolean
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Thermal Mode Selection
                Text(
                    text = "Thermal Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ThermalMode.values().forEach { mode ->
                        FilterChip(
                            onClick = { onThermalModeChange(mode) },
                            label = { Text(mode.name) },
                            selected = thermalMode == mode,
                            enabled = isConnected
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Recording Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Record/Stop Button
                    FloatingActionButton(
                        onClick = {
                            val newState = when (recordingState) {
                                RecordingState.IDLE -> RecordingState.RECORDING
                                RecordingState.RECORDING -> RecordingState.IDLE
                                RecordingState.PAUSED -> RecordingState.RECORDING
                            }
                            onRecordingStateChange(newState)
                        },
                        containerColor = when (recordingState) {
                            RecordingState.RECORDING -> Color.Red
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = when (recordingState) {
                                RecordingState.IDLE -> Icons.Default.FiberManualRecord
                                RecordingState.RECORDING -> Icons.Default.Stop
                                RecordingState.PAUSED -> Icons.Default.PlayArrow
                            },
                            contentDescription = when (recordingState) {
                                RecordingState.IDLE -> "Start Recording"
                                RecordingState.RECORDING -> "Stop Recording" 
                                RecordingState.PAUSED -> "Resume Recording"
                            }
                        )
                    }
                    
                    // Pause Button
                    IconButton(
                        onClick = {
                            val newState = if (recordingState == RecordingState.RECORDING) {
                                RecordingState.PAUSED
                            } else {
                                RecordingState.RECORDING
                            }
                            onRecordingStateChange(newState)
                        },
                        enabled = recordingState != RecordingState.IDLE
                    ) {
                        Icon(
                            imageVector = if (recordingState == RecordingState.PAUSED) {
                                Icons.Default.PlayArrow
                            } else {
                                Icons.Default.Pause
                            },
                            contentDescription = if (recordingState == RecordingState.PAUSED) {
                                "Resume"
                            } else {
                                "Pause"
                            }
                        )
                    }
                    
                    // Capture Photo Button
                    IconButton(
                        onClick = {
                            Toast.makeText(this@IRThermalNightComposeActivity, "Photo captured", Toast.LENGTH_SHORT).show()
                        },
                        enabled = isConnected
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture Photo"
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    private fun ThermalSettings(
        temperatureScale: String,
        onTemperatureScaleChange: (String) -> Unit,
        onDismiss: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Thermal Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Settings"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Temperature Scale Selection
                Text(
                    text = "Temperature Scale",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("°C", "°F", "K").forEach { scale ->
                        FilterChip(
                            onClick = { onTemperatureScaleChange(scale) },
                            label = { Text(scale) },
                            selected = temperatureScale == scale
                        )
                    }
                }
            }
        }
    }
    
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }
    }
    
    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Camera permission required for thermal imaging", Toast.LENGTH_LONG).show()
            }
        }
    }
}