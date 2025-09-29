package mpdc4gsr.sensors

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.compose.base.BaseComposeActivity
import mpdc4gsr.compose.theme.IRCameraTheme

/**
 * Modern Compose implementation of Hub-Spoke Integration
 * Manages network integration between multiple sensors and devices
 */
class HubSpokeIntegrationComposeActivity : BaseComposeActivity<HubSpokeIntegrationViewModel>() {
    
    override fun createViewModel(): HubSpokeIntegrationViewModel = 
        viewModels<HubSpokeIntegrationViewModel>().value
    
    @Composable
    override fun Content(viewModel: HubSpokeIntegrationViewModel) {
        IRCameraTheme {
            HubSpokeIntegrationScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}

@Composable
fun HubSpokeIntegrationScreen(
    viewModel: HubSpokeIntegrationViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Material 3 App Bar
        CenterAlignedTopAppBar(
            title = { Text("Hub-Spoke Integration") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.refreshConnections() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh connections"
                    )
                }
                
                IconButton(
                    onClick = { viewModel.toggleHubMode() }
                ) {
                    Icon(
                        imageVector = if (uiState.isHubMode) Icons.Default.Hub else Icons.Default.DeviceHub,
                        contentDescription = if (uiState.isHubMode) "Disable hub mode" else "Enable hub mode",
                        tint = if (uiState.isHubMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        
        // Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hub Status Card
            item {
                HubStatusCard(
                    isHubMode = uiState.isHubMode,
                    hubAddress = uiState.hubAddress,
                    connectedDevices = uiState.connectedDevices.size,
                    onToggleHub = { viewModel.toggleHubMode() }
                )
            }
            
            // Network Configuration Card
            item {
                NetworkConfigurationCard(
                    networkConfig = uiState.networkConfig,
                    onConfigChange = { config -> viewModel.updateNetworkConfig(config) }
                )
            }
            
            // Connected Devices
            if (uiState.connectedDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "Connected Devices (${uiState.connectedDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                items(uiState.connectedDevices) { device ->
                    DeviceConnectionCard(
                        device = device,
                        onDisconnect = { viewModel.disconnectDevice(device) },
                        onViewDetails = { viewModel.showDeviceDetails(device) }
                    )
                }
            }
            
            // Available Devices for Connection
            if (uiState.availableDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "Available Devices (${uiState.availableDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                items(uiState.availableDevices) { device ->
                    AvailableDeviceCard(
                        device = device,
                        onConnect = { viewModel.connectDevice(device) }
                    )
                }
            }
            
            // Integration Status
            item {
                IntegrationStatusCard(
                    integrationStatus = uiState.integrationStatus
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HubStatusCard(
    isHubMode: Boolean,
    hubAddress: String?,
    connectedDevices: Int,
    onToggleHub: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHubMode) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
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
                Column {
                    Text(
                        text = "Hub Mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isHubMode) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    Text(
                        text = if (isHubMode) "Active" else "Inactive",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isHubMode) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                Switch(
                    checked = isHubMode,
                    onCheckedChange = { onToggleHub() }
                )
            }
            
            if (isHubMode && hubAddress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkWifi,
                        contentDescription = "Network address",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hub Address: $hubAddress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = "Connected devices",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connected Devices: $connectedDevices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkConfigurationCard(
    networkConfig: NetworkConfig,
    onConfigChange: (NetworkConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = "Network Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Network settings would go here
                Text(
                    text = "Port: ${networkConfig.port}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "Protocol: ${networkConfig.protocol}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "Max Connections: ${networkConfig.maxConnections}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceConnectionCard(
    device: ConnectedDevice,
    onDisconnect: () -> Unit,
    onViewDetails: () -> Unit
) {
    Card(
        onClick = onViewDetails,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device Type Icon
            Icon(
                imageVector = when (device.type) {
                    DeviceType.THERMAL_CAMERA -> Icons.Default.Videocam
                    DeviceType.GSR_SENSOR -> Icons.Default.Sensors
                    DeviceType.RGB_CAMERA -> Icons.Default.Camera
                    DeviceType.UNKNOWN -> Icons.Default.Device
                },
                contentDescription = "Device type",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Device Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                // Connection Status
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = when (device.connectionStatus) {
                                    ConnectionStatus.CONNECTED -> Color.Green
                                    ConnectionStatus.CONNECTING -> Color.Yellow
                                    ConnectionStatus.DISCONNECTED -> Color.Red
                                },
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = device.connectionStatus.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Disconnect Button
            IconButton(onClick = onDisconnect) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Disconnect",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvailableDeviceCard(
    device: AvailableDevice,
    onConnect: () -> Unit
) {
    Card(
        onClick = onConnect,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (device.type) {
                    DeviceType.THERMAL_CAMERA -> Icons.Default.Videocam
                    DeviceType.GSR_SENSOR -> Icons.Default.Sensors
                    DeviceType.RGB_CAMERA -> Icons.Default.Camera
                    DeviceType.UNKNOWN -> Icons.Default.Device
                },
                contentDescription = "Device type",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Connect",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun IntegrationStatusCard(
    integrationStatus: List<IntegrationStatus>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Integration Status",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            integrationStatus.forEach { status ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = status.component,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (status.isHealthy) Color.Green else Color.Red,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (status.isHealthy) "Healthy" else "Error",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// Data classes for hub-spoke integration
data class NetworkConfig(
    val port: Int = 8080,
    val protocol: String = "TCP",
    val maxConnections: Int = 10
)

data class ConnectedDevice(
    val id: String,
    val name: String,
    val address: String,
    val type: DeviceType,
    val connectionStatus: ConnectionStatus
)

data class AvailableDevice(
    val id: String,
    val name: String,
    val address: String,
    val type: DeviceType
)

data class IntegrationStatus(
    val component: String,
    val isHealthy: Boolean,
    val lastUpdate: Long = System.currentTimeMillis()
)

enum class DeviceType {
    THERMAL_CAMERA,
    GSR_SENSOR,
    RGB_CAMERA,
    UNKNOWN
}

enum class ConnectionStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED
}