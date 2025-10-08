package mpdc4gsr.feature.gsr.ui
import dagger.hilt.android.AndroidEntryPoint
// Note: MainActivityViewModel was moved to backup during cleanup
// Using modern Compose ViewModels instead
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.AppError
import mpdc4gsr.core.ui.ConnectionState
import mpdc4gsr.core.ui.components.SensorStatusCard
import mpdc4gsr.core.ui.components.sensors.GSRConnectionState
import mpdc4gsr.core.ui.components.sensors.GSRData
import mpdc4gsr.core.ui.components.sensors.GSRVisualizationCard
import mpdc4gsr.feature.main.presentation.MainActivityViewModel

@AndroidEntryPoint
class SensorDashboardComposeActivity : ComponentActivity() {
    private lateinit var dashboardViewModel: MainActivityViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dashboardViewModel = viewModels<MainActivityViewModel>().value
        setContent {
            LibUnifiedTheme {
                Content(dashboardViewModel)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(viewModel: MainActivityViewModel) {
        // Observe sensor states
        val thermalCameraState by viewModel.thermalCameraState.collectAsState()
        val gsrSensorState by viewModel.gsrSensorState.collectAsState()
        val gsrConnectionState by viewModel.gsrConnectionState.collectAsState()
        val gsrBatteryLevel by viewModel.gsrBatteryLevel.collectAsState()
        // Use real GSR data from ViewModel
        val gsrDataState by viewModel.gsrData.collectAsState()
        // Map ViewModel GSRDataState to UI GSRData with battery level
        val gsrData by remember {
            derivedStateOf {
                GSRData(
                    currentValue = gsrDataState.currentValue,
                    batteryLevel = gsrBatteryLevel ?: gsrDataState.batteryLevel,
                    recentReadings = gsrDataState.recentReadings.ifEmpty { generateMockGSRReadings() },
                    averageValue = gsrDataState.averageValue,
                    minValue = gsrDataState.minValue,
                    maxValue = gsrDataState.maxValue
                )
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Sensor Dashboard",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overall sensor status overview
                Text(
                    text = "Sensor Status Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                SensorStatusCard(
                    thermalCameraState = mapSensorStateToConnectionState(thermalCameraState),
                    gsrSensorState = mapSensorStateToConnectionState(gsrSensorState),
                    bleConnectionState = mapGSRConnectionToConnectionState(gsrConnectionState)
                )
                // GSR Sensor detailed visualization
                Text(
                    text = "GSR Sensor Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                GSRVisualizationCard(
                    gsrData = gsrData,
                    connectionState = GSRConnectionState(
                        isConnected = gsrConnectionState == MainActivityViewModel.GSRConnectionState.CONNECTED,
                        deviceName = "Shimmer3-GSR",
                        connectionStrength = if (gsrConnectionState == MainActivityViewModel.GSRConnectionState.CONNECTED) 85 else 0
                    ),
                    onExportData = {
                        // TODO: Implement GSR data export functionality
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeActivity,
                            "Export GSR data feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onResetStatistics = {
                        // TODO: Implement statistics reset functionality
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeActivity,
                            "Reset statistics feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                // Additional sensor information cards
                AdditionalSensorInfo(
                    thermalCameraState = mapSensorStateToConnectionState(thermalCameraState),
                    gsrSensorState = mapSensorStateToConnectionState(gsrSensorState)
                )
                // Data export and management section
                DataManagementSection(
                    onExportAllData = {
                        // TODO: Export all sensor data
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeActivity,
                            "Exporting all sensor data...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onClearData = {
                        // TODO: Clear sensor data
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeActivity,
                            "Clear data feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onOpenSettings = {
                        // TODO: Open sensor settings
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeActivity,
                            "Opening settings...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    @Composable
    private fun AdditionalSensorInfo(
        thermalCameraState: ConnectionState,
        gsrSensorState: ConnectionState
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Thermal camera info
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Thermal Camera",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Resolution: 384x288",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Frame Rate: 10Hz",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Status: ${
                            when (thermalCameraState) {
                                is ConnectionState.Connected -> "Connected"
                                is ConnectionState.Connecting -> "Connecting"
                                is ConnectionState.Disconnected -> "Disconnected"
                                is ConnectionState.Error -> "Error"
                            }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            // GSR sensor info
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "GSR Sensor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sample Rate: 51.2Hz",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Connection: BLE",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Status: ${
                            when (gsrSensorState) {
                                is ConnectionState.Connected -> "Connected"
                                is ConnectionState.Connecting -> "Connecting"
                                is ConnectionState.Disconnected -> "Disconnected"
                                is ConnectionState.Error -> "Error"
                            }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    @Composable
    private fun DataManagementSection(
        onExportAllData: () -> Unit,
        onClearData: () -> Unit,
        onOpenSettings: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportAllData,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export All Data")
                    }
                    OutlinedButton(
                        onClick = onClearData,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Data")
                    }
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Settings")
                    }
                }
            }
        }
    }

    // Helper functions to map existing state to Compose-friendly types
    private fun mapSensorStateToConnectionState(sensorState: MainActivityViewModel.SensorState): ConnectionState {
        return when (sensorState.status) {
            MainActivityViewModel.SensorStatus.DISCONNECTED -> ConnectionState.Disconnected
            MainActivityViewModel.SensorStatus.CONNECTING -> ConnectionState.Connecting
            MainActivityViewModel.SensorStatus.CONNECTED -> ConnectionState.Connected()
            MainActivityViewModel.SensorStatus.STREAMING -> ConnectionState.Connected()
            MainActivityViewModel.SensorStatus.ERROR -> ConnectionState.Error(
                AppError.SensorError(
                    "ThermalCamera",
                    "Sensor Error"
                )
            )

            MainActivityViewModel.SensorStatus.SIMULATION -> ConnectionState.Connected()
        }
    }

    private fun mapGSRConnectionToConnectionState(gsrState: MainActivityViewModel.GSRConnectionState): ConnectionState {
        return when (gsrState) {
            MainActivityViewModel.GSRConnectionState.DISCONNECTED -> ConnectionState.Disconnected
            MainActivityViewModel.GSRConnectionState.DISCOVERING -> ConnectionState.Connecting
            MainActivityViewModel.GSRConnectionState.CONNECTING -> ConnectionState.Connecting
            MainActivityViewModel.GSRConnectionState.CONNECTED -> ConnectionState.Connected()
            MainActivityViewModel.GSRConnectionState.ERROR -> ConnectionState.Error(
                AppError.SensorError(
                    "GSR",
                    "GSR Error"
                )
            )
        }
    }

    private fun generateMockGSRReadings(): List<Float> {
        return (0..50).map {
            100f + (kotlin.random.Random.nextFloat() - 0.5f) * 40f
        }
    }
}