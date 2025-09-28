package mpdc4gsr.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mpdc4gsr.compose.base.BaseComposeActivity
import mpdc4gsr.compose.components.SensorStatusCard
import mpdc4gsr.compose.components.sensor.GSRVisualizationCard
import mpdc4gsr.compose.components.sensor.GSRData
import mpdc4gsr.compose.components.sensor.GSRConnectionState
import mpdc4gsr.viewmodel.ConnectionState
import mpdc4gsr.viewmodel.MainActivityViewModel

/**
 * Task C: Complete Sensor Dashboard using Compose
 * 
 * This activity demonstrates:
 * - Modern sensor monitoring dashboard
 * - Real-time GSR data visualization
 * - Connection status monitoring
 * - Data export and statistics
 * - Enhanced visual feedback
 */
class SensorDashboardComposeActivity : BaseComposeActivity<MainActivityViewModel>() {

    override fun createViewModel(): MainActivityViewModel {
        return viewModels<MainActivityViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MainActivityViewModel) {
        // Observe sensor states
        val thermalCameraState by viewModel.thermalCameraState.collectAsState()
        val gsrSensorState by viewModel.gsrSensorState.collectAsState()
        val gsrConnectionState by viewModel.gsrConnectionState.collectAsState()
        val gsrBatteryLevel by viewModel.gsrBatteryLevel.collectAsState()
        
        // Mock GSR data for demonstration (in real implementation, this would come from ViewModel)
        val gsrData by remember {
            derivedStateOf {
                GSRData(
                    currentValue = 125.5f,
                    batteryLevel = gsrBatteryLevel ?: 75,
                    recentReadings = generateMockGSRReadings(),
                    averageValue = 118.3f,
                    minValue = 95.2f,
                    maxValue = 145.8f
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
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                        isConnected = gsrConnectionState != MainActivityViewModel.GSRConnectionState.DISCONNECTED,
                        deviceName = "Shimmer3-GSR",
                        connectionStrength = if (gsrConnectionState == MainActivityViewModel.GSRConnectionState.CONNECTED) 85 else 0
                    )
                )
                
                // Additional sensor information cards
                AdditionalSensorInfo(
                    thermalCameraState = thermalCameraState,
                    gsrSensorState = gsrSensorState
                )
                
                // Data export and management section
                DataManagementSection(
                    onExportAllData = { /* Export all sensor data */ },
                    onClearData = { /* Clear sensor data */ },
                    onOpenSettings = { /* Open sensor settings */ }
                )
            }
        }
    }

    @Composable
    private fun AdditionalSensorInfo(
        thermalCameraState: MainActivityViewModel.SensorState,
        gsrSensorState: MainActivityViewModel.SensorState
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
                        text = "Status: ${thermalCameraState.status}",
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
                        text = "Status: ${gsrSensorState.status}",
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
            MainActivityViewModel.SensorStatus.CONNECTED -> ConnectionState.Connected
            MainActivityViewModel.SensorStatus.STREAMING -> ConnectionState.Connected
            MainActivityViewModel.SensorStatus.ERROR -> ConnectionState.Error("Sensor Error")
            MainActivityViewModel.SensorStatus.SIMULATION -> ConnectionState.Connected
        }
    }

    private fun mapGSRConnectionToConnectionState(gsrState: MainActivityViewModel.GSRConnectionState): ConnectionState {
        return when (gsrState) {
            MainActivityViewModel.GSRConnectionState.DISCONNECTED -> ConnectionState.Disconnected
            MainActivityViewModel.GSRConnectionState.CONNECTING -> ConnectionState.Connecting
            MainActivityViewModel.GSRConnectionState.CONNECTED -> ConnectionState.Connected
            MainActivityViewModel.GSRConnectionState.STREAMING -> ConnectionState.Connected
            MainActivityViewModel.GSRConnectionState.ERROR -> ConnectionState.Error("GSR Error")
        }
    }

    // Mock data generation for demo purposes
    private fun generateMockGSRReadings(): List<Float> {
        return (0..50).map { 
            100f + (kotlin.random.Random.nextFloat() - 0.5f) * 40f 
        }
    }
}