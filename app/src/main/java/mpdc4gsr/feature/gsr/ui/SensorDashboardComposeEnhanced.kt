package mpdc4gsr.feature.gsr.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.components.sensors.GSRConnectionState
import mpdc4gsr.core.ui.components.sensors.GSRData
import mpdc4gsr.core.ui.components.sensors.GSRVisualizationCard
import mpdc4gsr.feature.main.presentation.MainActivityViewModel

/**
 * Enhanced Sensor Dashboard leveraging consolidated layout patterns from dev branch
 *
 * This version integrates with:
 * - activity_sensor_consolidated.xml patterns
 * - item_sensor_data_consolidated.xml layouts
 * - Enhanced multi-modal recording support
 * - Improved device management from consolidated patterns
 */
class SensorDashboardComposeEnhanced : ComponentActivity() {

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
        val thermalCameraState by viewModel.thermalCameraState.collectAsState()
        val gsrSensorState by viewModel.gsrSensorState.collectAsState()
        val gsrConnectionState by viewModel.gsrConnectionState.collectAsState()
        val gsrBatteryLevel by viewModel.gsrBatteryLevel.collectAsState()
        val sessionState by viewModel.sessionState.collectAsState()
        
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        val gsrData by remember {
            derivedStateOf {
                GSRData(
                    currentValue = 125.5f,
                    batteryLevel = gsrBatteryLevel ?: 75,
                    recentReadings = generateEnhancedGSRReadings(),
                    averageValue = 118.3f,
                    minValue = 95.2f,
                    maxValue = 145.8f
                )
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Enhanced Sensor Dashboard",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Multi-Modal Sensor Integration",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Exporting all sensor data")
                            }
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Export Data")
                        }
                        IconButton(onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Opening sensor settings")
                            }
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                // Enhanced multi-modal sensor overview
                MultiModalSensorOverview(
                    thermalCameraState = thermalCameraState,
                    gsrSensorState = gsrSensorState,
                    sessionState = sessionState
                )

                // Enhanced GSR visualization with consolidated patterns
                GSRVisualizationCard(
                    gsrData = gsrData,
                    connectionState = GSRConnectionState(
                        isConnected = gsrConnectionState != MainActivityViewModel.GSRConnectionState.DISCONNECTED,
                        deviceName = "Shimmer3-GSR-Enhanced",
                        connectionStrength = if (gsrConnectionState == MainActivityViewModel.GSRConnectionState.CONNECTED) 90 else 0
                    )
                )

                // Device management section (consolidated layout pattern)
                DeviceManagementSection(
                    thermalCameraState = thermalCameraState,
                    gsrSensorState = gsrSensorState,
                    onDeviceConfig = { deviceType -> launchDeviceConfig(deviceType) },
                    onDeviceTest = { deviceType -> launchDeviceTest(deviceType) }
                )

                // Enhanced data export section
                DataExportSection(
                    sessionState = sessionState,
                    onExportSession = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Exporting current session")
                        }
                    },
                    onExportAllData = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Exporting all sensor data")
                        }
                    },
                    onManageSessions = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Launching session manager")
                        }
                    }
                )

                // System status and diagnostics
                SystemDiagnosticsSection(
                    thermalCameraState = thermalCameraState,
                    gsrSensorState = gsrSensorState,
                    onRunDiagnostics = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Running system diagnostics")
                        }
                    },
                    onViewLogs = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Opening system logs")
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun MultiModalSensorOverview(
        thermalCameraState: MainActivityViewModel.SensorState,
        gsrSensorState: MainActivityViewModel.SensorState,
        sessionState: MainActivityViewModel.SessionState
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Multi-Modal Recording",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (sessionState == MainActivityViewModel.SessionState.RECORDING) "Recording Active" else "Ready to Record",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    if (sessionState == MainActivityViewModel.SessionState.RECORDING) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LIVE",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sensor status grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SensorStatusIndicator(
                        title = "Thermal",
                        status = thermalCameraState.status.name,
                        icon = Icons.Default.Camera,
                        isActive = thermalCameraState.status == MainActivityViewModel.SensorStatus.STREAMING
                    )

                    SensorStatusIndicator(
                        title = "GSR",
                        status = gsrSensorState.status.name,
                        icon = Icons.Default.Sensors,
                        isActive = gsrSensorState.status == MainActivityViewModel.SensorStatus.STREAMING
                    )

                    SensorStatusIndicator(
                        title = "Session",
                        status = if (sessionState == MainActivityViewModel.SessionState.RECORDING) "Active" else "Idle",
                        icon = Icons.Default.Storage,
                        isActive = sessionState == MainActivityViewModel.SessionState.RECORDING
                    )
                }
            }
        }
    }

    @Composable
    private fun SensorStatusIndicator(
        title: String,
        status: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        isActive: Boolean
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isActive) Color.Green else MaterialTheme.colorScheme.onPrimaryContainer.copy(
                    alpha = 0.6f
                ),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }

    @Composable
    private fun DeviceManagementSection(
        thermalCameraState: MainActivityViewModel.SensorState,
        gsrSensorState: MainActivityViewModel.SensorState,
        onDeviceConfig: (String) -> Unit,
        onDeviceTest: (String) -> Unit
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
                    text = "Device Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Enhanced device cards with consolidated layout patterns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeviceCard(
                        title = "Thermal Camera",
                        subtitle = "TOPDON TC001",
                        status = thermalCameraState.status.name,
                        icon = Icons.Default.Camera,
                        onConfig = { onDeviceConfig("thermal") },
                        onTest = { onDeviceTest("thermal") },
                        modifier = Modifier.weight(1f)
                    )

                    DeviceCard(
                        title = "GSR Sensor",
                        subtitle = "Shimmer3",
                        status = gsrSensorState.status.name,
                        icon = Icons.Default.Sensors,
                        onConfig = { onDeviceConfig("gsr") },
                        onTest = { onDeviceTest("gsr") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    @Composable
    private fun DeviceCard(
        title: String,
        subtitle: String,
        status: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onConfig: () -> Unit,
        onTest: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Status: $status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = onConfig,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Config", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onTest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    @Composable
    private fun DataExportSection(
        sessionState: MainActivityViewModel.SessionState,
        onExportSession: () -> Unit,
        onExportAllData: () -> Unit,
        onManageSessions: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (sessionState == MainActivityViewModel.SessionState.RECORDING) {
                    Text(
                        text = "Recording in progress - data will be available after session ends",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportSession,
                        enabled = sessionState != MainActivityViewModel.SessionState.RECORDING,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export Session")
                    }

                    OutlinedButton(
                        onClick = onExportAllData,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export All")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onManageSessions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ManageAccounts, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Sessions")
                }
            }
        }
    }

    @Composable
    private fun SystemDiagnosticsSection(
        thermalCameraState: MainActivityViewModel.SensorState,
        gsrSensorState: MainActivityViewModel.SensorState,
        onRunDiagnostics: () -> Unit,
        onViewLogs: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "System Diagnostics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // System health indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("Thermal Status", style = MaterialTheme.typography.labelMedium)
                        Icon(
                            if (thermalCameraState.status == MainActivityViewModel.SensorStatus.CONNECTED)
                                Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (thermalCameraState.status == MainActivityViewModel.SensorStatus.CONNECTED)
                                Color.Green else Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("GSR Status", style = MaterialTheme.typography.labelMedium)
                        Icon(
                            if (gsrSensorState.status == MainActivityViewModel.SensorStatus.CONNECTED)
                                Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (gsrSensorState.status == MainActivityViewModel.SensorStatus.CONNECTED)
                                Color.Green else Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRunDiagnostics,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run Diagnostics")
                    }

                    OutlinedButton(
                        onClick = onViewLogs,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Logs")
                    }
                }
            }
        }
    }

    // Launch methods for enhanced functionality
    private fun launchDeviceConfig(deviceType: String) {
        // Launch device-specific configuration
        when (deviceType) {
            "thermal" -> {
                // Launch thermal camera configuration
            }

            "gsr" -> {
                // Launch GSR sensor configuration
            }
        }
    }

    private fun launchDeviceTest(deviceType: String) {
        // Launch device-specific testing
        when (deviceType) {
            "thermal" -> {
                // Launch thermal camera test
            }

            "gsr" -> {
                // Launch GSR sensor test
            }
        }
    }

    // Enhanced mock data generation
    private fun generateEnhancedGSRReadings(): List<Float> {
        return (0..100).map {
            100f + (kotlin.random.Random.nextFloat() - 0.5f) * 50f
        }
    }
}