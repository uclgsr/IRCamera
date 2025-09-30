package mpdc4gsr.ui_components

import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.csl.irCamera.R
import mpdc4gsr.activities.DeviceTypeActivityCompose
import mpdc4gsr.compose.base.BaseComposeFragment
import mpdc4gsr.ui_components.MainFragmentViewModel.*

/**
 * Compose migration of MainFragment
 *
 * This fragment demonstrates:
 * - Complete migration of MainFragment UI to Compose
 * - StateFlow integration for reactive UI updates
 * - Material 3 design system implementation
 * - GSR sensor integration and dual-mode camera controls
 * - Seamless integration with existing navigation system
 */
class MainFragmentCompose : BaseComposeFragment<MainFragmentViewModel>() {

    override fun createViewModel(): MainFragmentViewModel {
        return viewModels<MainFragmentViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MainFragmentViewModel) {
        val context = LocalContext.current
        val deviceState by viewModel.deviceState.collectAsStateWithLifecycle()
        val batteryInfo by viewModel.batteryInfo.collectAsStateWithLifecycle()

        // Handle navigation events
        LaunchedEffect(Unit) {
            viewModel.navigationEvents.collect { event ->
                when (event) {
                    is NavigationEvent.Navigate -> {
                        // Handle navigation through existing NavigationManager
                        viewModel.handleNavigation(context, event.route, event.isTC007, event.isTS004)
                    }

                    is NavigationEvent.ShowError -> {
                        // Show error snackbar or dialog
                    }

                    is NavigationEvent.ShowDeviceSelection -> {
                        // Show device selection dialog
                    }

                    is NavigationEvent.ShowDeviceAddDialog -> {
                        // Navigate to device add screen
                        context.startActivity(Intent(context, DeviceTypeActivityCompose::class.java))
                    }

                    is NavigationEvent.ShowDeviceDeleteDialog -> {
                        // Show delete confirmation dialog
                    }

                    is NavigationEvent.ShowGSROptions -> {
                        // Show GSR options dialog
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header section with device status
            DeviceStatusHeader(
                hasDevices = deviceState?.hasAnyDevice ?: false,
                onAddDevice = {
                    context.startActivity(Intent(context, DeviceTypeActivityCompose::class.java))
                }
            )

            // Device list or empty state
            if (deviceState?.hasAnyDevice == true) {
                DeviceList(
                    deviceState = deviceState!!,
                    batteryInfo = batteryInfo,
                    onDeviceClick = { connectType ->
                        viewModel.onDeviceItemClick(connectType)
                    },
                    onDeviceLongClick = { connectType ->
                        viewModel.showDeviceDeleteDialog(connectType)
                    }
                )
            } else {
                EmptyDeviceState(
                    onAddDevice = {
                        context.startActivity(Intent(context, DeviceTypeActivityCompose::class.java))
                    },
                    onGsrOptionsClick = {
                        viewModel.showGSROptions()
                    }
                )
            }

            // GSR Multi-modal Recording FAB
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomEnd
            ) {
                FloatingActionButton(
                    onClick = { viewModel.showGSROptions() },
                    containerColor = MaterialTheme.colorScheme.tertiary
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_gsr_pulse),
                        contentDescription = "GSR Recording"
                    )
                }
            }
        }
    }

    @Composable
    private fun DeviceStatusHeader(
        hasDevices: Boolean,
        onAddDevice: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasDevices)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
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
                        text = if (hasDevices) "Devices Connected" else "No Devices Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasDevices)
                            "Tap device to connect or long-press to manage"
                        else
                            "Add a device to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onAddDevice) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Device",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    @Composable
    private fun DeviceList(
        deviceState: MainFragmentViewModel.DeviceState,
        batteryInfo: BatteryInfo?,
        onDeviceClick: (ConnectType) -> Unit,
        onDeviceLongClick: (ConnectType) -> Unit
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Only show LINE device if enabled
            if (deviceState.hasConnectLine) {
                item {
                    DeviceCard(
                        connectType = ConnectType.LINE,
                        isConnected = deviceState.hasConnectLine,
                        batteryInfo = null,
                        onClick = onDeviceClick,
                        onLongClick = onDeviceLongClick
                    )
                }
            }

            // TS004 and TC007 functionality commented out per original code
            /*
            if (deviceState.hasConnectTS004) {
                item {
                    DeviceCard(
                        connectType = ConnectType.TS004,
                        isConnected = deviceState.hasConnectTS004,
                        batteryInfo = null,
                        onClick = onDeviceClick,
                        onLongClick = onDeviceLongClick
                    )
                }
            }
            
            if (deviceState.hasConnectTC007) {
                item {
                    DeviceCard(
                        connectType = ConnectType.TC007,
                        isConnected = deviceState.hasConnectTC007,
                        batteryInfo = batteryInfo,
                        onClick = onDeviceClick,
                        onLongClick = onDeviceLongClick
                    )
                }
            }
            */
        }
    }

    @Composable
    private fun DeviceCard(
        connectType: ConnectType,
        isConnected: Boolean,
        batteryInfo: BatteryInfo?,
        onClick: (ConnectType) -> Unit,
        onLongClick: (ConnectType) -> Unit
    ) {
        Card(
            onClick = { onClick(connectType) },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isConnected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device icon
                Icon(
                    painter = painterResource(
                        id = when (connectType) {
                            ConnectType.LINE -> if (isConnected)
                                R.drawable.ic_main_device_line_connect
                            else
                                R.drawable.ic_main_device_line_disconnect

                            ConnectType.TS004 -> if (isConnected)
                                R.drawable.ic_main_device_ts004_connect
                            else
                                R.drawable.ic_main_device_ts004_disconnect

                            ConnectType.TC007 -> if (isConnected)
                                R.drawable.ic_main_device_tc007_connect
                            else
                                R.drawable.ic_main_device_tc007_disconnect
                        }
                    ),
                    contentDescription = connectType.name,
                    tint = if (isConnected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Device info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getDeviceName(connectType),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isConnected) "Online" else "Offline",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isConnected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Show battery info for TC007
                    if (connectType == ConnectType.TC007 && batteryInfo != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (batteryInfo.isCharging())
                                    Icons.Default.BatteryChargingFull
                                else
                                    Icons.Default.BatteryFull,
                                contentDescription = "Battery",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${batteryInfo.getBattery()}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Connection status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = if (isConnected) Color.Green else Color.Gray,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }
        }
    }

    @Composable
    private fun EmptyDeviceState(
        onAddDevice: () -> Unit,
        onGsrOptionsClick: () -> Unit
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "No devices",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "No Devices Connected",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Connect a thermal camera or add a new device to get started",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAddDevice,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Device")
                    }

                    OutlinedButton(
                        onClick = onGsrOptionsClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("GSR Recording")
                    }
                }
            }
        }
    }

    private fun getDeviceName(connectType: ConnectType): String = when (connectType) {
        ConnectType.LINE -> "TC Line Device"
        ConnectType.TS004 -> "TS004"
        ConnectType.TC007 -> "TC007"
    }
}