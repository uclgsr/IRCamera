package mpdc4gsr.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.compose.base.BaseComposeActivity
import mpdc4gsr.compose.theme.IRCameraTheme

/**
 * Modern Compose implementation of Simplified Main Activity
 * Provides a streamlined interface with essential functionality
 */
class SimplifiedMainComposeActivity : BaseComposeActivity<SimplifiedMainViewModel>() {

    override fun createViewModel(): SimplifiedMainViewModel =
        viewModels<SimplifiedMainViewModel>().value

    @Composable
    override fun Content(viewModel: SimplifiedMainViewModel) {
        IRCameraTheme {
            SimplifiedMainScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}

@Composable
fun SimplifiedMainScreen(
    viewModel: SimplifiedMainViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.viewState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Material 3 App Bar
        CenterAlignedTopAppBar(
            title = { Text("IRCamera Simplified") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.refreshStatus() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh status"
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // System Status Card
            SystemStatusCard(
                systemStatus = uiState.systemStatus,
                onToggleRecording = { viewModel.toggleRecording() }
            )

            // Quick Actions
            QuickActionsCard(
                onThermalCamera = { viewModel.launchThermalCamera() },
                onGSRSensor = { viewModel.launchGSRSensor() },
                onSettings = { viewModel.launchSettings() }
            )

            // Connection Status
            ConnectionStatusCard(
                connectionStatus = uiState.connectionStatus
            )

            // Recording Status
            if (uiState.isRecording) {
                RecordingStatusCard(
                    recordingDuration = uiState.recordingDuration,
                    onStopRecording = { viewModel.stopRecording() }
                )
            }

            // Recent Sessions (if any)
            if (uiState.recentSessions.isNotEmpty()) {
                RecentSessionsCard(
                    sessions = uiState.recentSessions,
                    onSessionClick = { session -> viewModel.openSession(session) }
                )
            }

            // Error display
            uiState.error?.let { error ->
                ErrorCard(
                    error = error,
                    onDismiss = { viewModel.clearViewError() }
                )
            }
        }
    }
}

@Composable
private fun SystemStatusCard(
    systemStatus: SystemStatus,
    onToggleRecording: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (systemStatus.overallHealth) {
                HealthStatus.HEALTHY -> MaterialTheme.colorScheme.primaryContainer
                HealthStatus.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                HealthStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
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
                        text = "System Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = when (systemStatus.overallHealth) {
                            HealthStatus.HEALTHY -> MaterialTheme.colorScheme.onPrimaryContainer
                            HealthStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                            HealthStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )

                    Text(
                        text = systemStatus.overallHealth.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (systemStatus.overallHealth) {
                            HealthStatus.HEALTHY -> MaterialTheme.colorScheme.onPrimaryContainer
                            HealthStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                            HealthStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }

                Icon(
                    imageVector = when (systemStatus.overallHealth) {
                        HealthStatus.HEALTHY -> Icons.Default.CheckCircle
                        HealthStatus.WARNING -> Icons.Default.Warning
                        HealthStatus.ERROR -> Icons.Default.Error
                    },
                    contentDescription = "System status",
                    modifier = Modifier.size(32.dp),
                    tint = when (systemStatus.overallHealth) {
                        HealthStatus.HEALTHY -> Color.Green
                        HealthStatus.WARNING -> Color.Yellow
                        HealthStatus.ERROR -> Color.Red
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick recording toggle
            Button(
                onClick = onToggleRecording,
                modifier = Modifier.fillMaxWidth(),
                enabled = systemStatus.overallHealth != HealthStatus.ERROR
            ) {
                Icon(
                    imageVector = if (systemStatus.isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (systemStatus.isRecording) "Stop Recording" else "Start Recording")
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    onThermalCamera: () -> Unit,
    onGSRSensor: () -> Unit,
    onSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thermal Camera
                Card(
                    onClick = onThermalCamera,
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Thermal Camera",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Thermal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // GSR Sensor
                Card(
                    onClick = onGSRSensor,
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "GSR Sensor",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "GSR",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Settings
                Card(
                    onClick = onSettings,
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionStatus: ConnectionStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Device Connections",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Thermal Camera Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Thermal Camera",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thermal Camera",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                ConnectionStatusIndicator(isConnected = connectionStatus.thermalCameraConnected)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // GSR Sensor Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "GSR Sensor",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GSR Sensor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                ConnectionStatusIndicator(isConnected = connectionStatus.gsrSensorConnected)
            }
        }
    }
}

@Composable
private fun ConnectionStatusIndicator(isConnected: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isConnected) Color.Green else Color.Red,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isConnected) "Connected" else "Disconnected",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecordingStatusCard(
    recordingDuration: String,
    onStopRecording: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = "Recording",
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Recording Active",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = recordingDuration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Button(
                onClick = onStopRecording,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stop")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentSessionsCard(
    sessions: List<RecentSession>,
    onSessionClick: (RecentSession) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recent Sessions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            sessions.take(3).forEach { session ->
                Card(
                    onClick = { onSessionClick(session) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Session",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = session.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = session.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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

// Data classes
data class SystemStatus(
    val overallHealth: HealthStatus,
    val isRecording: Boolean
)

data class ConnectionStatus(
    val thermalCameraConnected: Boolean,
    val gsrSensorConnected: Boolean
)

data class RecentSession(
    val id: String,
    val name: String,
    val date: String
)

enum class HealthStatus(val displayName: String) {
    HEALTHY("Healthy"),
    WARNING("Warning"),
    ERROR("Error")
}