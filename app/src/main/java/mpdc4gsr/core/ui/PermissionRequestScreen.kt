package mpdc4gsr.core.ui

import android.widget.Toast
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionRequestScreen(
    viewModel: PermissionRequestViewModel,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val permissionStates by viewModel.permissionStates.collectAsStateWithLifecycle()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val logMessages by viewModel.logMessages.collectAsStateWithLifecycle()
    // Handle one-time events
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PermissionRequestViewModel.PermissionEvent.ShowError -> {
                    // Handle error display
                }

                is PermissionRequestViewModel.PermissionEvent.ShowSuccess -> {
                    // Handle success display
                }

                PermissionRequestViewModel.PermissionEvent.NavigateToRecording -> {
                    // Navigate to recording activity
                }
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission Manager") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.updatePermissionStatus() },
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Status Overview
            StatusOverviewCard(
                screenState = screenState,
                permissionStates = permissionStates,
            )
            // Permission Cards
            PermissionCardsSection(
                permissionStates = permissionStates,
                onRequestCamera = { viewModel.requestCameraPermissions() },
                onRequestBluetooth = { viewModel.requestBluetoothPermissions() },
                onRequestLocation = { viewModel.requestLocationPermissions() },
                onRequestStorage = { viewModel.requestStoragePermissions() },
                isRequestingPermissions = screenState.isRequestingPermissions,
            )
            // Action Buttons
            ActionButtonsSection(
                screenState = screenState,
                onRequestAll = { viewModel.requestAllPermissions() },
                onTestCapabilities = { viewModel.testRecordingCapabilities() },
                onStartRecording = { viewModel.startRecordingSession() },
            )
            // Log Section
            LogSection(
                logMessages = logMessages,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusOverviewCard(
    screenState: PermissionRequestViewModel.ScreenState,
    permissionStates: PermissionRequestViewModel.PermissionStates,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (screenState.canStartRecording) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    if (screenState.canStartRecording) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint =
                        if (screenState.canStartRecording) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
                Text(
                    text = "Permission Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (screenState.isRequestingPermissions) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
            Text(
                text = screenState.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
            )
            // Permission quick status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PermissionStatusChip("Camera", permissionStates.camera)
                PermissionStatusChip("Bluetooth", permissionStates.bluetooth)
                PermissionStatusChip("Location", permissionStates.location)
                PermissionStatusChip("Storage", permissionStates.storage)
            }
        }
    }
}

@Composable
private fun PermissionStatusChip(
    name: String,
    status: PermissionRequestViewModel.PermissionStatus,
) {
    val context = LocalContext.current
    val color =
        when (status) {
            PermissionRequestViewModel.PermissionStatus.GRANTED -> MaterialTheme.colorScheme.primary
            PermissionRequestViewModel.PermissionStatus.DENIED -> MaterialTheme.colorScheme.error
            PermissionRequestViewModel.PermissionStatus.NOT_AVAILABLE -> MaterialTheme.colorScheme.outline
            PermissionRequestViewModel.PermissionStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
        }
    val text =
        when (status) {
            PermissionRequestViewModel.PermissionStatus.GRANTED -> "OK"
            PermissionRequestViewModel.PermissionStatus.DENIED -> "Need"
            PermissionRequestViewModel.PermissionStatus.NOT_AVAILABLE -> "N/A"
            PermissionRequestViewModel.PermissionStatus.UNKNOWN -> "?"
        }
    val statusDescription =
        when (status) {
            PermissionRequestViewModel.PermissionStatus.GRANTED -> "Permission granted"
            PermissionRequestViewModel.PermissionStatus.DENIED -> "Permission required"
            PermissionRequestViewModel.PermissionStatus.NOT_AVAILABLE -> "Not available on this device"
            PermissionRequestViewModel.PermissionStatus.UNKNOWN -> "Status unknown"
        }
    AssistChip(
        onClick = {
            Toast
                .makeText(
                    context,
                    "$name: $statusDescription",
                    Toast.LENGTH_SHORT,
                ).show()
        },
        label = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(name, style = MaterialTheme.typography.labelSmall)
                Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        },
        colors =
            AssistChipDefaults.assistChipColors(
                labelColor = color,
                leadingIconContentColor = color,
            ),
    )
}

@Composable
private fun PermissionCardsSection(
    permissionStates: PermissionRequestViewModel.PermissionStates,
    onRequestCamera: () -> Unit,
    onRequestBluetooth: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestStorage: () -> Unit,
    isRequestingPermissions: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Permission Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PermissionCard(
                title = "Camera",
                icon = Icons.Default.Camera,
                status = permissionStates.camera,
                onClick = onRequestCamera,
                enabled = !isRequestingPermissions,
                modifier = Modifier.weight(1f),
            )
            PermissionCard(
                title = "Bluetooth",
                icon = Icons.Default.Bluetooth,
                status = permissionStates.bluetooth,
                onClick = onRequestBluetooth,
                enabled = !isRequestingPermissions,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PermissionCard(
                title = "Location",
                icon = Icons.Default.LocationOn,
                status = permissionStates.location,
                onClick = onRequestLocation,
                enabled = !isRequestingPermissions,
                modifier = Modifier.weight(1f),
            )
            PermissionCard(
                title = "Storage",
                icon = Icons.Default.Storage,
                status = permissionStates.storage,
                onClick = onRequestStorage,
                enabled = !isRequestingPermissions,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionCard(
    title: String,
    icon: ImageVector,
    status: PermissionRequestViewModel.PermissionStatus,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        when (status) {
            PermissionRequestViewModel.PermissionStatus.GRANTED -> MaterialTheme.colorScheme.primaryContainer
            PermissionRequestViewModel.PermissionStatus.DENIED -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && status != PermissionRequestViewModel.PermissionStatus.GRANTED,
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint =
                    when (status) {
                        PermissionRequestViewModel.PermissionStatus.GRANTED -> MaterialTheme.colorScheme.primary
                        PermissionRequestViewModel.PermissionStatus.DENIED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text =
                    when (status) {
                        PermissionRequestViewModel.PermissionStatus.GRANTED -> "Granted"
                        PermissionRequestViewModel.PermissionStatus.DENIED -> "Request"
                        PermissionRequestViewModel.PermissionStatus.NOT_AVAILABLE -> "N/A"
                        PermissionRequestViewModel.PermissionStatus.UNKNOWN -> "Unknown"
                    },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ActionButtonsSection(
    screenState: PermissionRequestViewModel.ScreenState,
    onRequestAll: () -> Unit,
    onTestCapabilities: () -> Unit,
    onStartRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onRequestAll,
                enabled = !screenState.isRequestingPermissions,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Request All")
            }
            OutlinedButton(
                onClick = onTestCapabilities,
                enabled = !screenState.isRequestingPermissions,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test")
            }
        }
        Button(
            onClick = onStartRecording,
            enabled = screenState.canStartRecording && !screenState.isRequestingPermissions,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.FiberManualRecord, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Recording Session")
        }
    }
}

@Composable
private fun LogSection(
    logMessages: List<PermissionRequestViewModel.LogMessage>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Activity Log",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
        ) {
            val listState = rememberLazyListState()
            // Auto-scroll to bottom when new messages arrive
            LaunchedEffect(logMessages.size) {
                if (logMessages.isNotEmpty()) {
                    listState.animateScrollToItem(logMessages.size - 1)
                }
            }
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(logMessages, key = { it.id }) { logMessage ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "[${logMessage.timestamp}]",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            text = logMessage.message,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
