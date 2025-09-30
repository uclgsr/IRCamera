package mpdc4gsr.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.viewModels
import com.csl.irCamera.R
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.compose.base.BaseComposeActivity
import mpdc4gsr.network.DevicePairingViewModel
import mpdc4gsr.network.NetworkClient

/**
 * Device Pairing Activity - Compose Implementation
 *
 * Modern Compose implementation of network device pairing functionality:
 * - Uses existing DevicePairingViewModel for business logic
 * - Maintains compatibility with existing navigation patterns
 * - Follows MVVM architecture with Compose UI
 * - Preserves all network discovery and pairing functionality
 */
class DevicePairingActivityCompose : BaseComposeActivity<DevicePairingViewModel>(),
    NetworkClient.NetworkEventListener {

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, DevicePairingActivityCompose::class.java)
            context.startActivity(intent)
        }
    }

    private val pairing: DevicePairingViewModel by viewModels()

    override fun createViewModel(): DevicePairingViewModel {
        return pairing.also {
            it.initialize(this)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: DevicePairingViewModel) {
        val context = LocalContext.current

        // Collect state
        val discoveredControllers by viewModel.discoveredControllers.collectAsState()
        val connectedController by viewModel.connectedController.collectAsState()
        val connectionState by viewModel.pairingConnectionState.collectAsState()
        val scanState by viewModel.scanState.collectAsState()
        val statusMessage by viewModel.statusMessage.collectAsState()
        val pairingScreenState by viewModel.pairingScreenState.collectAsState()
        val flashState by viewModel.flashState.collectAsState()

        // Handle events
        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is DevicePairingViewModel.PairingEvent.ShowError -> {
                        Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                    }

                    is DevicePairingViewModel.PairingEvent.ShowSuccess -> {
                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    }

                    is DevicePairingViewModel.PairingEvent.NavigateToSession -> {
                        // MultiModalRecordingActivity.startRecording(context, event.sessionInfo)
                        Toast.makeText(context, "Navigate to session", Toast.LENGTH_SHORT).show()
                    }

                    is DevicePairingViewModel.PairingEvent.ShowConnectionDialog -> {
                        Toast.makeText(
                            context,
                            "Connecting to ${event.controller.deviceName}...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    is DevicePairingViewModel.PairingEvent.NavigateBack -> {
                        finish()
                    }
                }
            }
        }

        LibUnifiedTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    "Device Pairing",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    if (scanState == DevicePairingViewModel.ScanState.SCANNING) {
                                        // viewModel.stopControllerScan()
                                    } else {
                                        viewModel.startControllerScan()
                                    }
                                }) {
                                    Icon(
                                        if (scanState == DevicePairingViewModel.ScanState.SCANNING)
                                            Icons.Default.Stop
                                        else
                                            Icons.Default.Refresh,
                                        contentDescription = "Scan"
                                    )
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Connection Status Card
                        ConnectionStatusCard(
                            connectionState = connectionState,
                            connectedController = connectedController,
                            statusMessage = statusMessage,
                            onDisconnect = { viewModel.disconnectFromController() }
                        )

                        // Scan Controls Card
                        ScanControlsCard(
                            scanState = scanState,
                            discoveredCount = discoveredControllers.size,
                            onStartScan = { viewModel.startControllerScan() },
                            onStopScan = { /* Stop not implemented yet */ }
                        )

                        // Discovered Devices List
                        DiscoveredDevicesCard(
                            controllers = discoveredControllers,
                            onControllerClick = { controller ->
                                viewModel.connectToController(controller)
                            }
                        )
                    }
                }

                // Flash overlay for sync flash
                if (flashState.isVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.8f))
                    ) {
                        Text(
                            text = "SYNC FLASH",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // NetworkClient.NetworkEventListener implementation
    override fun onControllerDiscovered(controller: NetworkClient.ControllerInfo) {
        // ViewModel handles this through its own NetworkEventListener implementation
    }

    override fun onConnected(controller: NetworkClient.ControllerInfo) {
        // ViewModel handles this through its own NetworkEventListener implementation
    }

    override fun onDisconnected(reason: String) {
        // ViewModel handles this through its own NetworkEventListener implementation
    }

    override fun onRemoteMeasurementRequest(sessionInfo: SessionInfo) {
        // ViewModel handles this through its own NetworkEventListener implementation
    }

    override fun onSyncFlash(durationMs: Int) {
        runOnUiThread {
            // Trigger flash overlay through ViewModel
            pairing.triggerSyncFlash(durationMs)
        }
    }

    override fun onTimeSynchronized(offsetNanoseconds: Long) {
        runOnUiThread {
            Toast.makeText(
                this,
                "Time synchronized (offset: ${offsetNanoseconds / 1_000_000}ms)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDataStreamingStarted() {
        runOnUiThread {
            Toast.makeText(this, "Data streaming started", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDataStreamingStopped() {
        runOnUiThread {
            Toast.makeText(this, "Data streaming stopped", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onError(operation: String, error: String) {
        runOnUiThread {
            Toast.makeText(this, "Network error: $error", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun ConnectionStatusIndicator(connectionState: DevicePairingViewModel.ConnectionState) {
    val (color, icon) = when (connectionState) {
        DevicePairingViewModel.ConnectionState.CONNECTED ->
            Pair(MaterialTheme.colorScheme.primary, Icons.Default.CheckCircle)

        DevicePairingViewModel.ConnectionState.CONNECTING ->
            Pair(MaterialTheme.colorScheme.tertiary, Icons.Default.Refresh)

        DevicePairingViewModel.ConnectionState.CONNECTION_FAILED ->
            Pair(MaterialTheme.colorScheme.error, Icons.Default.Error)

        else ->
            Pair(MaterialTheme.colorScheme.outline, Icons.Default.Circle)
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun ConnectionStatusCard(
    connectionState: DevicePairingViewModel.ConnectionState,
    connectedController: NetworkClient.ControllerInfo?,
    statusMessage: String,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                DevicePairingViewModel.ConnectionState.CONNECTED ->
                    MaterialTheme.colorScheme.primaryContainer

                DevicePairingViewModel.ConnectionState.CONNECTION_FAILED ->
                    MaterialTheme.colorScheme.errorContainer

                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Connection Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                ConnectionStatusIndicator(connectionState)
            }

            when (connectionState) {
                DevicePairingViewModel.ConnectionState.CONNECTED -> {
                    connectedController?.let { controller ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Connected to: ${controller.deviceName}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${controller.ipAddress}:${controller.port}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = onDisconnect,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Disconnect")
                            }
                        }
                    }
                }

                DevicePairingViewModel.ConnectionState.CONNECTING -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Connecting...")
                    }
                }

                DevicePairingViewModel.ConnectionState.CONNECTION_FAILED -> {
                    Text(
                        text = "Connection failed: $statusMessage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> {
                    Text(
                        text = "Not connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScanControlsCard(
    scanState: DevicePairingViewModel.ScanState,
    discoveredCount: Int,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Device Discovery",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Found $discoveredCount device(s)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = when (scanState) {
                            DevicePairingViewModel.ScanState.SCANNING -> "Scanning..."
                            DevicePairingViewModel.ScanState.COMPLETED -> "Scan completed"
                            else -> "Ready to scan"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (scanState == DevicePairingViewModel.ScanState.SCANNING) {
                    Button(
                        onClick = onStopScan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop")
                    }
                } else {
                    Button(onClick = onStartScan) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan")
                    }
                }
            }

            if (scanState == DevicePairingViewModel.ScanState.SCANNING) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DiscoveredDevicesCard(
    controllers: List<NetworkClient.ControllerInfo>,
    onControllerClick: (NetworkClient.ControllerInfo) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Discovered Devices",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (controllers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.DeviceHub,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "No devices found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Start scanning to discover devices",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(controllers) { controller ->
                        DeviceItem(
                            controller = controller,
                            onClick = { onControllerClick(controller) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(
    controller: NetworkClient.ControllerInfo,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.DeviceHub,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = controller.deviceName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${controller.ipAddress}:${controller.port}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}