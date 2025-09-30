package mpdc4gsr.network

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.gsr.model.SessionInfo
import kotlinx.coroutines.flow.collectLatest
import mpdc4gsr.sensors.gsr.MultiModalRecordingComposeActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePairingScreen(
    viewModel: DevicePairingViewModel,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pairingState by viewModel.pairingScreenState.collectAsStateWithLifecycle()
    val discoveredControllers by viewModel.discoveredControllers.collectAsStateWithLifecycle()
    val connectedController by viewModel.connectedController.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    // Handle one-time events
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is DevicePairingViewModel.PairingEvent.ShowError -> {
                    // Show error toast/snackbar
                }

                is DevicePairingViewModel.PairingEvent.ShowSuccess -> {
                    // Show success toast/snackbar
                }

                is DevicePairingViewModel.PairingEvent.NavigateToSession -> {
                    // Navigate to recording activity
                    MultiModalRecordingComposeActivity.startRecording(context, event.sessionInfo)
                }

                is DevicePairingViewModel.PairingEvent.ShowConnectionDialog -> {
                    // Handle connection dialog
                }

                DevicePairingViewModel.PairingEvent.NavigateBack -> {
                    onBackClick()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Pairing") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startControllerScan(forceRefresh = true) },
                        enabled = pairingState.canScan
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Section
            StatusCard(
                connectionState = connectionState,
                statusMessage = statusMessage,
                connectedController = connectedController,
                isScanning = pairingState.showProgress
            )

            // Action Buttons
            ActionButtonsRow(
                pairingState = pairingState,
                onScanClick = { viewModel.startControllerScan() },
                onDisconnectClick = { viewModel.disconnectFromController() },
                connectionState = connectionState
            )

            // Controllers List
            ControllersSection(
                controllers = discoveredControllers,
                onControllerClick = { controller ->
                    viewModel.connectToController(controller)
                },
                canConnect = pairingState.canConnect,
                connectedController = connectedController
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusCard(
    connectionState: DevicePairingViewModel.ConnectionState,
    statusMessage: String,
    connectedController: NetworkClient.ControllerInfo?,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                DevicePairingViewModel.ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                DevicePairingViewModel.ConnectionState.CONNECTION_FAILED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Computer,
                    contentDescription = null,
                    tint = when (connectionState) {
                        DevicePairingViewModel.ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                        DevicePairingViewModel.ConnectionState.CONNECTION_FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = "Connection Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium
            )

            connectedController?.let { controller ->
                Text(
                    text = "Connected to: ${controller.deviceName} (${controller.ipAddress}:${controller.port})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    pairingState: DevicePairingViewModel.PairingScreenState,
    onScanClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    connectionState: DevicePairingViewModel.ConnectionState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onScanClick,
            enabled = pairingState.canScan,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (pairingState.showProgress) "Scanning..." else "Scan Devices")
        }

        OutlinedButton(
            onClick = onDisconnectClick,
            enabled = connectionState == DevicePairingViewModel.ConnectionState.CONNECTED,
            modifier = Modifier.weight(1f)
        ) {
            Text("Disconnect")
        }
    }
}

@Composable
private fun ControllersSection(
    controllers: List<NetworkClient.ControllerInfo>,
    onControllerClick: (NetworkClient.ControllerInfo) -> Unit,
    canConnect: Boolean,
    connectedController: NetworkClient.ControllerInfo?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Available Controllers (${controllers.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (controllers.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No controllers found.\nMake sure you're on the same network and tap 'Scan Devices'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(controllers) { controller ->
                    ControllerItem(
                        controller = controller,
                        onClick = { onControllerClick(controller) },
                        enabled = canConnect,
                        isConnected = connectedController == controller
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControllerItem(
    controller: NetworkClient.ControllerInfo,
    onClick: () -> Unit,
    enabled: Boolean,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = if (enabled) onClick else { {} },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Computer,
                contentDescription = null,
                tint = if (isConnected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = controller.deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isConnected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "${controller.ipAddress}:${controller.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isConnected) {
                Badge {
                    Text("Connected")
                }
            }
        }
    }
}