package mpdc4gsr.feature.main.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(navController: NavController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "System Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            ConnectionStatusCard()
        }
        item {
            QuickActionsCard(navController = navController)
        }
        item {
            RecentSessionsCard()
        }
        item {
            SystemHealthCard()
        }
    }
}

@Composable
fun ThermalCameraScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Thermal Camera",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Thermal camera preview would go here
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Thermal Camera View")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ThermalControlsPanel()
    }
}

@Composable
fun GSRSensorScreen(navController: NavController) {
    mpdc4gsr.feature.gsr.ui.GSRSensorScreen(
        onBackClick = { navController.popBackStack() },
        onSettingsClick = { navController.navigate("gsr_settings") },
        onSaveData = {
            // Navigate to data export screen if available
            // For now, the GSRSensorScreen has a default empty implementation
        }
    )
}

@Composable
private fun GSRSensorScreenDeprecated(navController: NavController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "GSR Sensor",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            GSRConnectionCard()
        }
        item {
            GSRDataVisualizationCard()
        }
        item {
            GSRRecordingControlsCard()
        }
        item {
            GSRCalibrationCard()
        }
    }
}

@Composable
fun SensorDashboardScreen(navController: NavController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Sensor Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            AllSensorsStatusCard()
        }
        item {
            SensorMetricsCard()
        }
        item {
            DataSynchronizationCard()
        }
        item {
            AdvancedAnalyticsCard()
        }
    }
}

@Composable
fun SettingsScreen(navController: NavController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            GeneralSettingsCard()
        }
        item {
            ThermalCameraSettingsCard()
        }
        item {
            GSRSensorSettingsCard()
        }
        item {
            NetworkSettingsCard()
        }
        item {
            AboutCard()
        }
    }
}

// Reusable component cards
@Composable
fun ConnectionStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Connection Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ConnectionStatusItem("Thermal Camera", true)
                ConnectionStatusItem("GSR Sensor", true)
                ConnectionStatusItem("Network", false)
            }
        }
    }
}

@Composable
fun ConnectionStatusItem(name: String, connected: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (connected) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = if (connected) "$name Connected" else "$name Disconnected",
            tint = if (connected) Color.Green else Color.Red
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = if (connected) "Connected" else "Disconnected",
            style = MaterialTheme.typography.bodySmall,
            color = if (connected) Color.Green else Color.Red
        )
    }
}

@Composable
fun QuickActionsCard(navController: NavController) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { navController.navigate("thermal") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Camera, contentDescription = "Thermal Camera")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thermal")
                }
                Button(
                    onClick = { navController.navigate("gsr") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Sensors, contentDescription = "GSR Sensor")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GSR")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Start recording functionality
                        android.widget.Toast.makeText(
                            navController.context,
                            "Start recording",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start Recording")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Record")
                }
            }
        }
    }
}

@Composable
fun RecentSessionsCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recent Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            val sessions = listOf(
                "Session 2024-01-15 14:30",
                "Session 2024-01-15 10:15",
                "Session 2024-01-14 16:45"
            )
            sessions.forEach { session ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = {
                        // TODO: Open session details
                        android.widget.Toast.makeText(
                            context,
                            "Open session: $session",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Open session"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SystemHealthCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "System Health",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HealthMetric("CPU", "45%", Color.Green)
                HealthMetric("Memory", "62%", Color.Yellow)
                HealthMetric("Battery", "89%", Color.Green)
                HealthMetric("Storage", "71%", Color.Yellow)
            }
        }
    }
}

@Composable
fun HealthMetric(name: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ThermalControlsPanel() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Thermal Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // TODO: Capture thermal image
                        android.widget.Toast.makeText(
                            context,
                            "Capture thermal image",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Capture Image")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Capture")
                }
                Button(
                    onClick = {
                        // TODO: Start thermal recording
                        android.widget.Toast.makeText(
                            context,
                            "Start thermal recording",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "Record Video")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Record")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Open thermal settings
                        android.widget.Toast.makeText(
                            context,
                            "Open thermal settings",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Settings")
                }
            }
        }
    }
}

@Composable
fun GSRConnectionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    text = "GSR Connection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Connected",
                    tint = Color.Green
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Shimmer3 GSR - Device ID: SH001",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Battery: 89% | Signal: Strong",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GSRDataVisualizationCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Real-time GSR Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Placeholder for GSR data visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("GSR Waveform Visualization")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current: 2.45 μS",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Sampling: 51.2 Hz",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GSRRecordingControlsCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recording Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // TODO: Start GSR recording
                        android.widget.Toast.makeText(
                            context,
                            "Start GSR recording",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start GSR Recording")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Stop GSR recording
                        android.widget.Toast.makeText(
                            context,
                            "Stop GSR recording",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop GSR Recording")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Pause GSR recording
                        android.widget.Toast.makeText(
                            context,
                            "Pause GSR recording",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause GSR Recording")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pause")
                }
            }
        }
    }
}

@Composable
fun GSRCalibrationCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Sensor Calibration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last Calibration: 2024-01-15",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = {
                        // TODO: Start GSR calibration
                        android.widget.Toast.makeText(
                            context,
                            "Start GSR calibration",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Text("Calibrate")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 0.85f },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Calibration Quality: 85%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Additional placeholder composables for other screens
@Composable
fun AllSensorsStatusCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("All Sensors Status")
        }
    }
}

@Composable
fun SensorMetricsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Sensor Metrics")
        }
    }
}

@Composable
fun DataSynchronizationCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Data Synchronization")
        }
    }
}

@Composable
fun AdvancedAnalyticsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Advanced Analytics")
        }
    }
}

@Composable
fun GeneralSettingsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("General Settings")
        }
    }
}

@Composable
fun ThermalCameraSettingsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Thermal Camera Settings")
        }
    }
}

@Composable
fun GSRSensorSettingsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("GSR Sensor Settings")
        }
    }
}

@Composable
fun NetworkSettingsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Network Settings")
        }
    }
}

@Composable
fun AboutCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "About IRCamera",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Version 2.0.0\nBuild 2024.01.15",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}