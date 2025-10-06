package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.feature.gsr.presentation.ShimmerConfigViewModel
import mpdc4gsr.feature.gsr.presentation.ShimmerConfigViewModelFactory

class ShimmerConfigComposeActivity : ComponentActivity() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, ShimmerConfigComposeActivity::class.java))
        }
    }

    private val viewModel: ShimmerConfigViewModel by viewModels {
        ShimmerConfigViewModelFactory(application, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Content(viewModel)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(viewModel: ShimmerConfigViewModel) {
        val localContext = androidx.compose.ui.platform.LocalContext.current
        var isScanning by remember { mutableStateOf(false) }
        var selectedDevice by remember { mutableStateOf<DeviceInfo?>(null) }
        var showConfigDialog by remember { mutableStateOf(false) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Shimmer Configuration",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            IconButton(onClick = { isScanning = !isScanning }) {
                                Icon(
                                    if (isScanning) Icons.Default.Stop else Icons.Default.Refresh,
                                    contentDescription = if (isScanning) "Stop Scan" else "Scan"
                                )
                            }
                            IconButton(onClick = {
                                // TODO: Implement Shimmer configuration help/documentation
                                android.widget.Toast.makeText(
                                    localContext,
                                    "Opening Shimmer configuration help",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                ShimmerConfigContent(
                    isScanning = isScanning,
                    selectedDevice = selectedDevice,
                    onDeviceSelect = { selectedDevice = it },
                    onConfigureDevice = { showConfigDialog = true },
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        if (showConfigDialog && selectedDevice != null) {
            DeviceConfigurationDialog(
                device = selectedDevice!!,
                onDismiss = { showConfigDialog = false },
                onSaveConfiguration = { config ->
                    // Save device configuration
                    showConfigDialog = false
                }
            )
        }
    }
}

@Composable
private fun ShimmerConfigContent(
    isScanning: Boolean,
    selectedDevice: DeviceInfo?,
    onDeviceSelect: (DeviceInfo?) -> Unit,
    onConfigureDevice: () -> Unit,
    viewModel: ShimmerConfigViewModel,
    modifier: Modifier = Modifier
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scanning Status Card
        ScanningStatusCard(
            isScanning = isScanning,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Device List
        Text(
            text = "Available Devices",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            val mockDevices = listOf(
                DeviceInfo("shimmer_001", "Shimmer3 GSR+ #001", "Shimmer3", -45, true),
                DeviceInfo("shimmer_002", "Shimmer3 GSR+ #002", "Shimmer3", -62, true),
                DeviceInfo("shimmer_003", "Shimmer3 GSR+ #003", "Shimmer3", -38, true)
            )
            items(mockDevices) { device ->
                DeviceCard(
                    device = device,
                    isSelected = selectedDevice?.address == device.address,
                    onSelect = { onDeviceSelect(device) },
                    onConnect = {
                        // TODO: Implement Shimmer device connection
                        android.widget.Toast.makeText(
                            localContext,
                            "Connecting to ${device.name}...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onConfigure = {
                        onDeviceSelect(device)
                        onConfigureDevice()
                    }
                )
            }
        }
        // Selected Device Configuration Panel
        selectedDevice?.let { device ->
            SelectedDevicePanel(
                device = device,
                onConfigure = onConfigureDevice,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun ScanningStatusCard(
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isScanning)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isScanning) "Scanning for devices..." else "Scan complete",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isScanning) "Looking for Shimmer devices" else "3 devices found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Scan complete",
                    tint = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: DeviceInfo,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onConnect: () -> Unit,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surface
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "ID: ${device.address}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Status and signal strength
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Connection status indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (device.deviceType) {
                                        "connected" -> Color(0xFF4CAF50)
                                        "available" -> Color(0xFF2196F3)
                                        "configuring" -> Color(0xFFFF9800)
                                        else -> Color(0xFF9E9E9E)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = device.deviceType.replaceFirstChar { it.uppercaseChar() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // Signal strength
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = "Signal strength",
                            modifier = Modifier.size(16.dp),
                            tint = when (device.signalStrength) {
                                DeviceInfo.SignalStrength.EXCELLENT -> Color(0xFF4CAF50)
                                DeviceInfo.SignalStrength.GOOD -> Color(0xFF4CAF50)
                                DeviceInfo.SignalStrength.FAIR -> Color(0xFFFF9800)
                                DeviceInfo.SignalStrength.POOR -> Color(0xFFE53E3E)
                                DeviceInfo.SignalStrength.VERY_POOR -> Color(0xFFE53E3E)
                            }
                        )
                        Text(
                            text = "${device.rssi} dBm",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onSelect) {
                    Icon(
                        if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isSelected) "Collapse" else "Expand"
                    )
                }
            }
            if (isSelected) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                // Device actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onConnect,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Connect",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect")
                    }
                    Button(
                        onClick = onConfigure,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configure",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Configure")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDevicePanel(
    device: DeviceInfo,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Selected Device",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Status: ${device.deviceType.replaceFirstChar { it.uppercaseChar() }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Button(
                onClick = onConfigure,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Advanced Configuration",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Advanced Configuration")
            }
        }
    }
}

@Composable
private fun DeviceConfigurationDialog(
    device: DeviceInfo,
    onDismiss: () -> Unit,
    onSaveConfiguration: (Map<String, Any>) -> Unit
) {
    var samplingRate by remember { mutableStateOf(128f) }
    var gsrRange by remember { mutableStateOf("Auto") }
    var enablePPG by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Configure ${device.name}")
        },
        text = {
            Column {
                Text(
                    text = "Sampling Rate (Hz)",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Slider(
                    value = samplingRate,
                    onValueChange = { samplingRate = it },
                    valueRange = 1f..512f,
                    steps = 8
                )
                Text(
                    text = "${samplingRate.toInt()} Hz",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "GSR Range",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = enablePPG,
                        onCheckedChange = { enablePPG = it }
                    )
                    Text(
                        text = "Enable PPG channels",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSaveConfiguration(
                        mapOf(
                            "samplingRate" to samplingRate.toInt(),
                            "gsrRange" to gsrRange,
                            "enablePPG" to enablePPG
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}