package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.feature.gsr.presentation.GSRSettingsViewModel

/**
 * GSRSettingsComposeActivity - Compose Migration Demonstration
 *
 * This demonstrates the next phase of modernization:
 * - Migration from traditional BaseViewModelActivity to shared BaseComposeActivity
 * - Modern Compose UI with Material 3 components
 * - Preserved ViewModel and business logic
 * - Enhanced user experience with consistent theming
 */
class GSRSettingsComposeActivity : BaseComposeActivity<GSRSettingsViewModel>() {

    companion object {
        private const val TAG = "GSRSettingsComposeActivity"

        fun startActivity(context: Context) {
            context.startActivity(Intent(context, GSRSettingsComposeActivity::class.java))
        }
    }

    override fun createViewModel(): GSRSettingsViewModel {
        return viewModels<GSRSettingsViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: GSRSettingsViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "GSR Settings",
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
                GSRSettingsContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun GSRSettingsContent(
    viewModel: GSRSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Initialize ViewModel with context
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
        viewModel.checkPermissions(context)
    }

    // Observe ViewModel states
    val gsrSettings by viewModel.gsrSettings.collectAsState()
    val deviceSettings by viewModel.deviceSettings.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val deviceConnectionState by viewModel.deviceConnectionState.collectAsState()
    val availableDevices by viewModel.availableDevices.collectAsState()
    val scanningState by viewModel.scanningState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bluetooth Settings Card
        BluetoothSettingsCard(
            onScanClick = { viewModel.startDeviceScan() },
            onPermissionRequest = { viewModel.requestPermissions() }
        )

        // Device Connection Card
        DeviceConnectionCard(
            onConnectClick = { deviceId ->
                availableDevices.find { it.id == deviceId }?.let { device ->
                    viewModel.connectToDevice(device)
                }
            },
            onDisconnectClick = { viewModel.disconnectDevice() }
        )

        // Recording Settings Card
        RecordingSettingsCard(
            onSampleRateChange = { viewModel.updateSamplingRate(it) },
            onSessionTimeoutChange = { }
        )

        // Export Settings Card
        ExportSettingsCard(
            onExportFormatChange = { },
            onExportLocationChange = { }
        )
    }
}

@Composable
private fun BluetoothSettingsCard(
    onScanClick: () -> Unit,
    onPermissionRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Bluetooth Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "Configure Bluetooth connection for GSR sensors",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onScanClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Scan Devices")
                }

                OutlinedButton(
                    onClick = onPermissionRequest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Check Permissions")
                }
            }
        }
    }
}

@Composable
private fun DeviceConnectionCard(
    onConnectClick: (String) -> Unit,
    onDisconnectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Device Connection",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Connection status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Text("Disconnected")
                }
                Text(
                    "No GSR device connected",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = { onConnectClick("mock_device") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connect to Device")
            }
        }
    }
}

@Composable
private fun RecordingSettingsCard(
    onSampleRateChange: (Int) -> Unit,
    onSessionTimeoutChange: (Int) -> Unit
) {
    var sampleRate by remember { mutableIntStateOf(128) }
    var sessionTimeout by remember { mutableIntStateOf(30) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Recording Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Sample Rate Setting
            Column {
                Text(
                    "Sample Rate: ${sampleRate} Hz",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = sampleRate.toFloat(),
                    onValueChange = {
                        sampleRate = it.toInt()
                        onSampleRateChange(sampleRate)
                    },
                    valueRange = 64f..512f,
                    steps = 7,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Session Timeout Setting
            Column {
                Text(
                    "Session Timeout: ${sessionTimeout} minutes",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = sessionTimeout.toFloat(),
                    onValueChange = {
                        sessionTimeout = it.toInt()
                        onSessionTimeoutChange(sessionTimeout)
                    },
                    valueRange = 5f..120f,
                    steps = 22,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ExportSettingsCard(
    onExportFormatChange: (String) -> Unit,
    onExportLocationChange: (String) -> Unit
) {
    var selectedFormat by remember { mutableStateOf("CSV") }
    val exportFormats = listOf("CSV", "JSON", "Excel")

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Export Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Export Format Selection
            Text(
                "Export Format",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                exportFormats.forEach { format ->
                    FilterChip(
                        onClick = {
                            selectedFormat = format
                            onExportFormatChange(format)
                        },
                        label = { Text(format) },
                        selected = selectedFormat == format
                    )
                }
            }

            OutlinedButton(
                onClick = { onExportLocationChange("downloads") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose Export Location")
            }
        }
    }
}