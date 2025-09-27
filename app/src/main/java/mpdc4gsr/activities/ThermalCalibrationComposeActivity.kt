package mpdc4gsr.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import mpdc4gsr.ui.components.IRCameraTopAppBar
import mpdc4gsr.ui.theme.IRCameraTheme
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

data class CalibrationProfile(
    val name: String,
    val minTemp: Float,
    val maxTemp: Float,
    val offset: Float,
    val gain: Float,
    val isActive: Boolean = false
)

class ThermalCalibrationComposeActivity : BaseComposeActivity() {

    @Composable
    override fun Content() {
        IRCameraTheme {
            ThermalCalibrationScreen()
        }
    }

    @Composable
    private fun ThermalCalibrationScreen() {
        var currentTemp by remember { mutableStateOf(25.4f) }
        var targetTemp by remember { mutableStateOf(37.0f) }
        var calibrationOffset by remember { mutableStateOf(0.0f) }
        var calibrationGain by remember { mutableStateOf(1.0f) }
        var isCalibrating by remember { mutableStateOf(false) }
        var calibrationProgress by remember { mutableStateOf(0f) }
        var selectedProfile by remember { mutableStateOf(0) }
        var temperatureHistory by remember { mutableStateOf(listOf<Float>()) }

        val profiles = remember {
            listOf(
                CalibrationProfile("Default", 0f, 100f, 0f, 1f, true),
                CalibrationProfile("Body Temperature", 30f, 45f, -2.1f, 1.05f),
                CalibrationProfile("Ambient", 15f, 35f, 0.5f, 0.98f),
                CalibrationProfile("High Range", 50f, 150f, -5.2f, 1.12f)
            )
        }

        // Simulate temperature readings
        LaunchedEffect(isCalibrating) {
            while (true) {
                delay(500)
                currentTemp = if (isCalibrating) {
                    targetTemp + Random.nextFloat() * 2 - 1 + calibrationOffset
                } else {
                    25.4f + Random.nextFloat() * 10 + sin(System.currentTimeMillis() / 1000.0).toFloat() * 2
                }
                
                temperatureHistory = (temperatureHistory + currentTemp).takeLast(50)
                
                if (isCalibrating && calibrationProgress < 1f) {
                    calibrationProgress += 0.02f
                    if (calibrationProgress >= 1f) {
                        isCalibrating = false
                        calibrationProgress = 0f
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                IRCameraTopAppBar(
                    title = "Thermal Calibration",
                    onNavigationClick = { finish() },
                    actions = {
                        IconButton(onClick = { /* Save calibration */ }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Current temperature display
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current Temperature",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${String.format("%.1f", currentTemp)}°C",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        if (isCalibrating) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = calibrationProgress,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Calibrating... ${(calibrationProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Temperature history graph
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(200.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Temperature History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            drawTemperatureGraph(temperatureHistory, currentTemp)
                        }
                    }
                }

                // Calibration controls
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Manual Calibration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Target temperature
                        Text(
                            text = "Target Temperature: ${String.format("%.1f", targetTemp)}°C",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = targetTemp,
                            onValueChange = { targetTemp = it },
                            valueRange = 0f..100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Calibration offset
                        Text(
                            text = "Offset: ${String.format("%.1f", calibrationOffset)}°C",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = calibrationOffset,
                            onValueChange = { calibrationOffset = it },
                            valueRange = -10f..10f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Calibration gain
                        Text(
                            text = "Gain: ${String.format("%.2f", calibrationGain)}x",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = calibrationGain,
                            onValueChange = { calibrationGain = it },
                            valueRange = 0.5f..2.0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Calibration buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { 
                                    isCalibrating = true
                                    calibrationProgress = 0f
                                },
                                enabled = !isCalibrating,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Calibration")
                            }
                            
                            OutlinedButton(
                                onClick = { 
                                    calibrationOffset = 0f
                                    calibrationGain = 1f
                                    targetTemp = 37f
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reset")
                            }
                        }
                    }
                }

                // Calibration profiles
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Calibration Profiles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        profiles.forEachIndexed { index, profile ->
                            ProfileCard(
                                profile = profile,
                                isSelected = selectedProfile == index,
                                onClick = { selectedProfile = index }
                            )
                            if (index < profiles.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                // Calibration status
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Calibration Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatusItem("Accuracy", if (kotlin.math.abs(currentTemp - targetTemp) < 1f) "Good" else "Needs Calibration")
                            StatusItem("Stability", if (temperatureHistory.size > 10) "Stable" else "Stabilizing")
                            StatusItem("Range", "${profiles[selectedProfile].minTemp}°C - ${profiles[selectedProfile].maxTemp}°C")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    @Composable
    private fun ProfileCard(
        profile: CalibrationProfile,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                               else MaterialTheme.colorScheme.surface
            ),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Range: ${profile.minTemp}°C - ${profile.maxTemp}°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Offset: ${profile.offset}°C, Gain: ${profile.gain}x",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    @Composable
    private fun StatusItem(label: String, value: String) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    private fun DrawScope.drawTemperatureGraph(history: List<Float>, currentTemp: Float) {
        if (history.isEmpty()) return
        
        val width = size.width
        val height = size.height
        val padding = 20f
        
        val minTemp = history.minOrNull() ?: currentTemp
        val maxTemp = history.maxOrNull() ?: currentTemp
        val tempRange = maxTemp - minTemp
        
        if (tempRange <= 0) return
        
        val path = Path()
        
        history.forEachIndexed { index, temp ->
            val x = padding + (index.toFloat() / (history.size - 1).coerceAtLeast(1)) * (width - 2 * padding)
            val y = height - padding - ((temp - minTemp) / tempRange) * (height - 2 * padding)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = Color.Blue,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
        
        // Draw current temperature point
        if (history.isNotEmpty()) {
            val lastX = padding + (width - 2 * padding)
            val lastY = height - padding - ((currentTemp - minTemp) / tempRange) * (height - 2 * padding)
            
            drawCircle(
                color = Color.Red,
                radius = 6f,
                center = Offset(lastX, lastY)
            )
        }
    }
}