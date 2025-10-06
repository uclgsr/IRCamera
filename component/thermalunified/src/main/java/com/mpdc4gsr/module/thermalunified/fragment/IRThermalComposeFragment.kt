package com.mpdc4gsr.module.thermalunified.fragment

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.module.thermalunified.activity.IRThermalPlusComposeActivity
import com.mpdc4gsr.module.thermalunified.activity.ThermalIrNightComposeActivity
import com.mpdc4gsr.module.thermalunified.viewmodel.IRThermalFragmentViewModel

class IRThermalComposeFragment : BaseComposeFragment<IRThermalFragmentViewModel>() {
    override fun createViewModel(): IRThermalFragmentViewModel {
        return viewModels<IRThermalFragmentViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRThermalFragmentViewModel) {
        val context = LocalContext.current
        // Observe ViewModel state
        val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
        val isTC007 by viewModel.isTC007.collectAsStateWithLifecycle()
        val deviceInfo by viewModel.deviceInfo.collectAsStateWithLifecycle()
        LibUnifiedTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header section
                Text(
                    text = "IR Thermal Imaging",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Access advanced thermal imaging capabilities",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Connection status card
                ConnectionStatusCard(
                    connectionStatus = connectionStatus,
                    isTC007 = isTC007,
                    deviceInfo = deviceInfo,
                    onRetryConnection = { viewModel.retryConnection() }
                )
                // Main thermal entry point
                if (connectionStatus == IRThermalFragmentViewModel.ConnectionStatus.CONNECTED) {
                    ThermalEntryCard(
                        onOpenThermal = { viewModel.openMainThermal() },
                        onOpenNightVision = {
                            val intent = Intent(context, ThermalIrNightComposeActivity::class.java)
                            context.startActivity(intent)
                        },
                        onOpenThermalPlus = {
                            val intent = Intent(context, IRThermalPlusComposeActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                } else {
                    ConnectionGuideCard(
                        connectionStatus = connectionStatus,
                        isTC007 = isTC007,
                        onConnectDevice = { viewModel.connectDevice() },
                        onOpenSettings = { viewModel.openDeviceSettings() }
                    )
                }
                // Advanced features section
                AdvancedFeaturesSection(
                    onNavigateToFeature = { route ->
                        NavigationManager.getInstance()
                            .build(route)
                            .navigation(context)
                    }
                )
            }
        }
    }

    @Composable
    private fun ConnectionStatusCard(
        connectionStatus: IRThermalFragmentViewModel.ConnectionStatus,
        isTC007: Boolean,
        deviceInfo: String?,
        onRetryConnection: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (connectionStatus) {
                    IRThermalFragmentViewModel.ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                    IRThermalFragmentViewModel.ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Device Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isTC007) "TC007 Device" else "Standard Device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = when (connectionStatus) {
                                    IRThermalFragmentViewModel.ConnectionStatus.CONNECTED -> Color.Green
                                    IRThermalFragmentViewModel.ConnectionStatus.CONNECTING -> Color(0xFFFFA500)
                                    else -> Color.Red
                                },
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
                Text(
                    text = getStatusText(connectionStatus),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (connectionStatus) {
                        IRThermalFragmentViewModel.ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                        IRThermalFragmentViewModel.ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                deviceInfo?.let { info ->
                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (connectionStatus != IRThermalFragmentViewModel.ConnectionStatus.CONNECTED) {
                    Button(
                        onClick = onRetryConnection,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry Connection")
                    }
                }
            }
        }
    }

    @Composable
    private fun ThermalEntryCard(
        onOpenThermal: () -> Unit,
        onOpenNightVision: () -> Unit,
        onOpenThermalPlus: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Thermal Imaging Modes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                // Main thermal button
                Button(
                    onClick = onOpenThermal,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Thermal Camera")
                }
                // Additional mode buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenNightVision,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NightsStay, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Night Vision")
                    }
                    OutlinedButton(
                        onClick = onOpenThermalPlus,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thermal Plus")
                    }
                }
            }
        }
    }

    @Composable
    private fun ConnectionGuideCard(
        connectionStatus: IRThermalFragmentViewModel.ConnectionStatus,
        isTC007: Boolean,
        onConnectDevice: () -> Unit,
        onOpenSettings: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Connection Required",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (connectionStatus) {
                        IRThermalFragmentViewModel.ConnectionStatus.CONNECTING -> "Connecting to Device..."
                        else -> "Device Not Connected"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isTC007) {
                        "Connect your TC007 thermal imaging device to access advanced thermal features"
                    } else {
                        "Connect your thermal imaging device to start capturing thermal data"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (connectionStatus != IRThermalFragmentViewModel.ConnectionStatus.CONNECTING) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onConnectDevice,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Cable, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect Device")
                        }
                        OutlinedButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Device Settings")
                        }
                    }
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }

    @Composable
    private fun AdvancedFeaturesSection(
        onNavigateToFeature: (String) -> Unit
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Advanced Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureCard(
                    title = "Gallery",
                    description = "View thermal images",
                    icon = Icons.Default.PhotoLibrary,
                    onClick = { onNavigateToFeature(RouterConfig.IR_GALLERY_HOME) },
                    modifier = Modifier.weight(1f)
                )
                FeatureCard(
                    title = "Settings",
                    description = "Configure thermal",
                    icon = Icons.Default.Settings,
                    onClick = { onNavigateToFeature(RouterConfig.THERMAL_SETTINGS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    @Composable
    private fun FeatureCard(
        title: String,
        description: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    private fun getStatusText(status: IRThermalFragmentViewModel.ConnectionStatus): String = when (status) {
        IRThermalFragmentViewModel.ConnectionStatus.CONNECTED -> "Device connected and ready"
        IRThermalFragmentViewModel.ConnectionStatus.CONNECTING -> "Connecting to device..."
        IRThermalFragmentViewModel.ConnectionStatus.DISCONNECTED -> "Device not connected"
        IRThermalFragmentViewModel.ConnectionStatus.ERROR -> "Connection error - check device"
    }
}