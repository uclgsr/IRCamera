package mpdc4gsr.feature.capture.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.component.shared.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import dagger.hilt.android.AndroidEntryPoint
import mpdc4gsr.core.designsystem.AppBaseViewModel

@AndroidEntryPoint
class GSRDeviceManagementComposeActivity : BaseComposeActivity<AppBaseViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, GSRDeviceManagementComposeActivity::class.java))
        }
    }

    override fun createViewModel(): AppBaseViewModel = viewModels<AppBaseViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: AppBaseViewModel) {
        val localContext = this@GSRDeviceManagementComposeActivity
        var isScanning by remember { mutableStateOf(false) }
        var selectedDevice by remember { mutableStateOf<GSRDeviceInfo?>(null) }
        var showDeviceDetails by remember { mutableStateOf(false) }
        var showBulkActions by remember { mutableStateOf(false) }
        LibSharedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "GSR Device Management",
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { isScanning = !isScanning }) {
                                Icon(
                                    if (isScanning) Icons.Default.Stop else Icons.Default.Refresh,
                                    contentDescription = if (isScanning) "Stop Scan" else "Scan",
                                )
                            }
                            IconButton(onClick = { showBulkActions = true }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Bulk Actions")
                            }
                            IconButton(onClick = {
                                // TODO: Open device help documentation
                                android.widget.Toast
                                    .makeText(
                                        this@GSRDeviceManagementComposeActivity,
                                        "Opening device help...",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
                            }
                        },
                    )
                },
            ) { paddingValues ->
                GSRDeviceManagementContent(
                    isScanning = isScanning,
                    selectedDevice = selectedDevice,
                    onDeviceSelect = {
                        selectedDevice = it
                        showDeviceDetails = true
                    },
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
        if (showDeviceDetails && selectedDevice != null) {
            DeviceDetailsDialog(
                device = selectedDevice!!,
                onDismiss = { showDeviceDetails = false },
                onConfigure = {
                    GSRDeviceConfigComposeActivity.startActivity(this@GSRDeviceManagementComposeActivity)
                    showDeviceDetails = false
                },
            )
        }
        if (showBulkActions) {
            BulkActionsDialog(
                onDismiss = { showBulkActions = false },
                onPerformAction = { action ->
                    // Perform bulk action
                    showBulkActions = false
                },
            )
        }
    }
}

@Composable
private fun GSRDeviceManagementContent(
    isScanning: Boolean,
    selectedDevice: GSRDeviceInfo?,
    onDeviceSelect: (GSRDeviceInfo) -> Unit,
    viewModel: AppBaseViewModel,
    modifier: Modifier = Modifier,
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Device Status Overview
        DeviceStatusOverview(
            connectedDevices = 2,
            availableDevices = 3,
            isScanning = isScanning,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        // Connected Devices Section
        Text(
            text = "Connected Devices",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            val connectedDevices = getMockGSRDevices().filter { it.status == "connected" }
            val availableDevices = getMockGSRDevices().filter { it.status != "connected" }
            items(connectedDevices) { device ->
                GSRDeviceCard(
                    device = device,
                    onSelect = { onDeviceSelect(device) },
                    onConnect = {
                        // TODO: Implement device connection logic
                        android.widget.Toast
                            .makeText(
                                localContext,
                                "Connecting to ${device.name}...",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                    },
                    onDisconnect = {
                        // TODO: Implement device disconnection logic
                        android.widget.Toast
                            .makeText(
                                localContext,
                                "Disconnecting from ${device.name}...",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                    },
                )
            }
            if (availableDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "Available Devices",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
                    )
                }
                items(availableDevices) { device ->
                    GSRDeviceCard(
                        device = device,
                        onSelect = { onDeviceSelect(device) },
                        onConnect = {
                            // TODO: Implement device connection logic
                            android.widget.Toast
                                .makeText(
                                    localContext,
                                    "Connecting to ${device.name}...",
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                        },
                        onDisconnect = {
                            // TODO: Implement device disconnection logic
                            android.widget.Toast
                                .makeText(
                                    localContext,
                                    "Disconnecting from ${device.name}...",
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceStatusOverview(
    connectedDevices: Int,
    availableDevices: Int,
    isScanning: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Device Status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DeviceStatusItem(
                    label = "Connected",
                    count = connectedDevices,
                    color = Color(0xFF4CAF50),
                )
                DeviceStatusItem(
                    label = "Available",
                    count = availableDevices,
                    color = Color(0xFF2196F3),
                )
                DeviceStatusItem(
                    label = "Total",
                    count = connectedDevices + availableDevices,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun DeviceStatusItem(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun GSRDeviceCard(
    device: GSRDeviceInfo,
    onSelect: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onSelect() },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when (device.status) {
                        "connected" -> MaterialTheme.colorScheme.tertiaryContainer
                        "connecting" -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "ID: ${device.deviceId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Status indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = getDeviceStatusColor(device.status),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(
                        text = device.status?.uppercase() ?: "N/A",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Device metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DeviceMetricItem(
                    label = "Battery",
                    value = "${device.batteryLevel}%",
                    icon = getBatteryIcon(device.batteryLevel),
                )
                DeviceMetricItem(
                    label = "Signal",
                    value = "${device.signalStrength} dBm",
                    icon = Icons.Default.Wifi,
                )
                DeviceMetricItem(
                    label = "Sample Rate",
                    value = "${device.samplingRate} Hz",
                    icon = Icons.Default.Timeline,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Device actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onSelect,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Details",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Details")
                }
                if (device.status == "connected") {
                    Button(
                        onClick = onDisconnect,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53E3E),
                            ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            Icons.Default.LinkOff,
                            contentDescription = "Disconnect",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disconnect")
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Connect",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceMetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeviceDetailsDialog(
    device: GSRDeviceInfo,
    onDismiss: () -> Unit,
    onConfigure: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(device.name)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                DeviceDetailItem("Device ID", device.deviceId)
                DeviceDetailItem("Status", device.status?.replaceFirstChar { it.uppercaseChar() } ?: "N/A")
                DeviceDetailItem("Battery Level", "${device.batteryLevel}%")
                DeviceDetailItem("Signal Strength", "${device.signalStrength} dBm")
                DeviceDetailItem("Sampling Rate", "${device.samplingRate} Hz")
                DeviceDetailItem("Last Seen", device.lastSeen)
                if (device.status == "connected") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Recent Data",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Text(
                        text = "GSR: 2.45 µS\nPPG: 1024, 1028\nTemperature: 36.2°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfigure) {
                Text("Configure")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun DeviceDetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun BulkActionsDialog(
    onDismiss: () -> Unit,
    onPerformAction: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Actions") },
        text = {
            Column {
                Text("Select an action to perform on all devices:")
                Spacer(modifier = Modifier.height(16.dp))
                listOf(
                    "Disconnect All" to "disconnect_all",
                    "Update Firmware" to "update_firmware",
                    "Reset Configuration" to "reset_config",
                    "Export Device List" to "export_list",
                ).forEach { (label, action) ->
                    TextButton(
                        onClick = { onPerformAction(action) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun getDeviceStatusColor(status: String?) =
    when (status) {
        "connected" -> Color(0xFF4CAF50)
        "connecting" -> Color(0xFFFF9800)
        "available" -> Color(0xFF2196F3)
        "disconnected" -> Color(0xFF9E9E9E)
        null -> Color(0xFF9E9E9E).copy(alpha = 0.6f)
        else -> Color(0xFFE53E3E)
    }

private fun getBatteryIcon(batteryLevel: Int) =
    when {
        batteryLevel > 75 -> Icons.Default.BatteryFull
        batteryLevel > 50 -> Icons.Default.Battery6Bar
        batteryLevel > 25 -> Icons.Default.Battery3Bar
        else -> Icons.Default.Battery1Bar
    }

data class GSRDeviceInfo(
    val name: String,
    val deviceId: String,
    val status: String?,
    val batteryLevel: Int,
    val signalStrength: Int,
    val samplingRate: Int,
    val lastSeen: String,
)

private fun getMockGSRDevices() =
    listOf(
        GSRDeviceInfo("Shimmer3 GSR+ #001", "shimmer_001", "disconnected", 89, -42, 128, "Just now"),
        GSRDeviceInfo("Shimmer3 GSR+ #002", "shimmer_002", "disconnected", 76, -38, 256, "2 min ago"),
        GSRDeviceInfo("Shimmer3 GSR+ #003", "shimmer_003", "available", 92, -55, 128, "5 min ago"),
        GSRDeviceInfo("Shimmer3 GSR+ #004", "shimmer_004", "disconnected", 45, -68, 128, "1 hour ago"),
        GSRDeviceInfo("Shimmer3 GSR+ #005", "shimmer_005", "available", 83, -48, 256, "10 min ago"),
    )



