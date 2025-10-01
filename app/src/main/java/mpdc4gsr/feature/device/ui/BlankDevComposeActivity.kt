package mpdc4gsr.feature.device.ui

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.compose.base.BaseComposeActivity
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.activities.BlankDevViewModel
import mpdc4gsr.activities.UsbDeviceInfo
import mpdc4gsr.activities.UsbDeviceType
import mpdc4gsr.activities.MainActivity

/**
 * Modern Compose implementation of USB Device Handler
 * Manages USB device connections and permissions with Material 3 UI
 */
class BlankDevComposeActivity : BaseComposeActivity<BlankDevViewModel>() {

    override fun createViewModel(): BlankDevViewModel =
        viewModels<BlankDevViewModel>().value

    @Composable
    override fun Content(viewModel: BlankDevViewModel) {
        IRCameraTheme {
            BlankDevScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() },
                onNavigateToMain = {
                    val intent = Intent(this@BlankDevComposeActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle USB device intent
        intent?.let { intent ->
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    }
                    device?.let { createViewModel().handleUsbDeviceAttached(it) }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    }
                    device?.let { createViewModel().handleUsbDeviceDetached(it) }
                }

                BlankDevViewModel.ACTION_USB_PERMISSION -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    }
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    device?.let { createViewModel().handleUsbPermissionResult(it, granted) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlankDevScreen(
    viewModel: BlankDevViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToMain: () -> Unit = {}
) {
    val uiState by viewModel.blankDevUiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Material 3 App Bar
        CenterAlignedTopAppBar(
            title = { Text("USB Device Manager") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.refreshUsbDevices() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh devices"
                    )
                }

                IconButton(onClick = onNavigateToMain) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Go to main"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
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
            // USB Status Card
            item {
                UsbStatusCard(
                    hasPermission = uiState.hasUsbPermission,
                    connectedDeviceCount = uiState.connectedDevices.size,
                    onRequestPermission = { viewModel.requestUsbPermissions() }
                )
            }

            // Connected Devices
            if (uiState.connectedDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "Connected USB Devices (${uiState.connectedDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(uiState.connectedDevices) { device ->
                    UsbDeviceCard(
                        device = device,
                        onDisconnect = { viewModel.disconnectDevice(device) },
                        onRequestPermission = { viewModel.requestPermissionForDevice(device) }
                    )
                }
            }

            // Available Devices
            if (uiState.availableDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "Available USB Devices (${uiState.availableDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(uiState.availableDevices) { device ->
                    AvailableUsbDeviceCard(
                        device = device,
                        onConnect = { viewModel.connectDevice(device) }
                    )
                }
            }

            // Empty State
            if (uiState.connectedDevices.isEmpty() && uiState.availableDevices.isEmpty()) {
                item {
                    EmptyDevicesCard()
                }
            }

            // Error Display
            uiState.error?.let { error ->
                item {
                    ErrorCard(
                        error = error,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }

            // Auto-navigation section
            item {
                AutoNavigationCard(
                    autoNavigateEnabled = uiState.autoNavigateToMain,
                    countdown = uiState.navigationCountdown,
                    onToggleAutoNavigate = { viewModel.toggleAutoNavigation() },
                    onNavigateNow = onNavigateToMain
                )
            }
        }
    }
}

@Composable
private fun UsbStatusCard(
    hasPermission: Boolean,
    connectedDeviceCount: Int,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermission) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
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
                        text = "USB Permission Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (hasPermission) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )

                    Text(
                        text = if (hasPermission) "Granted" else "Not Granted",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasPermission) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }

                Icon(
                    imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = if (hasPermission) "Permission granted" else "Permission needed",
                    tint = if (hasPermission) Color.Green else Color.Red,
                    modifier = Modifier.size(32.dp)
                )
            }

            if (!hasPermission) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Request USB Permission")
                }
            }

            if (hasPermission) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Connected Devices: $connectedDeviceCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsbDeviceCard(
    device: UsbDeviceInfo,
    onDisconnect: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Card(
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
            // Device Icon
            Icon(
                imageVector = when (device.deviceType) {
                    UsbDeviceType.CAMERA -> Icons.Default.Videocam
                    UsbDeviceType.SENSOR -> Icons.Default.Sensors
                    else -> Icons.Default.Usb
                },
                contentDescription = "USB device",
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
                    text = "PID: ${device.productId} VID: ${device.vendorId}",
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
                                color = if (device.hasPermission) Color.Green else Color.Yellow,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (device.hasPermission) "Connected" else "Permission needed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Actions
            if (!device.hasPermission) {
                IconButton(onClick = onRequestPermission) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Request permission",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

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
private fun AvailableUsbDeviceCard(
    device: UsbDeviceInfo,
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
                imageVector = when (device.deviceType) {
                    UsbDeviceType.CAMERA -> Icons.Default.Videocam
                    UsbDeviceType.SENSOR -> Icons.Default.Sensors
                    else -> Icons.Default.Usb
                },
                contentDescription = "USB device",
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
                    text = "PID: ${device.productId} VID: ${device.vendorId}",
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
private fun EmptyDevicesCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.UsbOff,
                contentDescription = "No USB devices",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No USB Devices Detected",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Connect a USB device to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun AutoNavigationCard(
    autoNavigateEnabled: Boolean,
    countdown: Int?,
    onToggleAutoNavigate: () -> Unit,
    onNavigateNow: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Auto-navigate to Main",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Text(
                        text = if (autoNavigateEnabled) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                Switch(
                    checked = autoNavigateEnabled,
                    onCheckedChange = { onToggleAutoNavigate() }
                )
            }

            if (autoNavigateEnabled && countdown != null) {
                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { (10 - countdown) / 10f },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Navigating to main in ${countdown}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onNavigateNow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to Main Now")
                }
            }
        }
    }
}