package mpdc4gsr.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
 * Modern Compose implementation of Camera Network Demo
 * Demonstrates network camera functionality with Material 3 UI
 */
class CameraNetworkDemoComposeActivity : BaseComposeActivity<CameraNetworkDemoViewModel>() {
    
    override fun createViewModel(): CameraNetworkDemoViewModel = 
        viewModels<CameraNetworkDemoViewModel>().value
    
    @Composable
    override fun Content(viewModel: CameraNetworkDemoViewModel) {
        IRCameraTheme {
            CameraNetworkDemoScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}

@Composable
fun CameraNetworkDemoScreen(
    viewModel: CameraNetworkDemoViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Material 3 App Bar
        CenterAlignedTopAppBar(
            title = { Text("Camera Network Demo") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.scanForCameras() }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Scan for cameras"
                    )
                }
                
                IconButton(onClick = { viewModel.toggleStreaming() }) {
                    Icon(
                        imageVector = if (uiState.isStreaming) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isStreaming) "Stop streaming" else "Start streaming",
                        tint = if (uiState.isStreaming) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
            // Network Status Card
            item {
                NetworkStatusCard(
                    networkInfo = uiState.networkInfo,
                    onRefreshNetwork = { viewModel.refreshNetworkInfo() }
                )
            }
            
            // Streaming Status Card
            if (uiState.isStreaming) {
                item {
                    StreamingStatusCard(
                        streamingInfo = uiState.streamingInfo,
                        onStopStreaming = { viewModel.stopStreaming() }
                    )
                }
            }
            
            // Available Cameras
            if (uiState.availableCameras.isNotEmpty()) {
                item {
                    Text(
                        text = "Available Network Cameras (${uiState.availableCameras.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                items(uiState.availableCameras) { camera ->
                    CameraCard(
                        camera = camera,
                        onConnect = { viewModel.connectToCamera(camera) },
                        onDisconnect = { viewModel.disconnectFromCamera(camera) },
                        onStartStream = { viewModel.startStreamFromCamera(camera) }
                    )
                }
            }
            
            // Connection History
            if (uiState.connectionHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "Connection History",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                items(uiState.connectionHistory.take(5)) { history ->
                    ConnectionHistoryCard(history = history)
                }
            }
            
            // Demo Controls
            item {
                DemoControlsCard(
                    onStartDemo = { viewModel.startDemo() },
                    onStopDemo = { viewModel.stopDemo() },
                    isDemoRunning = uiState.isDemoRunning
                )
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
        }
    }
}

@Composable
private fun NetworkStatusCard(
    networkInfo: NetworkInfo,
    onRefreshNetwork: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (networkInfo.isConnected) {
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
                        text = "Network Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (networkInfo.isConnected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    
                    Text(
                        text = if (networkInfo.isConnected) "Connected" else "Disconnected",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (networkInfo.isConnected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
                
                IconButton(onClick = onRefreshNetwork) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh network",
                        tint = if (networkInfo.isConnected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }
            
            if (networkInfo.isConnected) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "IP Address: ${networkInfo.ipAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = "Subnet: ${networkInfo.subnet}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun StreamingStatusCard(
    streamingInfo: StreamingInfo,
    onStopStreaming: () -> Unit
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
                        text = "Live Streaming",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    
                    Text(
                        text = "From: ${streamingInfo.sourceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                
                Button(
                    onClick = onStopStreaming,
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
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Resolution: ${streamingInfo.resolution}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                
                Text(
                    text = "FPS: ${streamingInfo.fps}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = streamingInfo.signalStrength / 100f,
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = "Signal Strength: ${streamingInfo.signalStrength}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraCard(
    camera: NetworkCamera,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onStartStream: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (camera.status) {
                CameraStatus.CONNECTED -> MaterialTheme.colorScheme.secondaryContainer
                CameraStatus.STREAMING -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (camera.type) {
                            CameraType.THERMAL -> Icons.Default.Videocam
                            CameraType.RGB -> Icons.Default.Camera
                            CameraType.IP_CAMERA -> Icons.Default.VideocamOff
                        },
                        contentDescription = "Camera type",
                        modifier = Modifier.size(24.dp),
                        tint = when (camera.status) {
                            CameraStatus.CONNECTED -> MaterialTheme.colorScheme.onSecondaryContainer
                            CameraStatus.STREAMING -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = camera.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = when (camera.status) {
                                CameraStatus.CONNECTED -> MaterialTheme.colorScheme.onSecondaryContainer
                                CameraStatus.STREAMING -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        Text(
                            text = camera.ipAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (camera.status) {
                                CameraStatus.CONNECTED -> MaterialTheme.colorScheme.onSecondaryContainer
                                CameraStatus.STREAMING -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = when (camera.status) {
                                CameraStatus.CONNECTED -> Color.Green
                                CameraStatus.STREAMING -> Color.Blue
                                CameraStatus.DISCONNECTED -> Color.Red
                                CameraStatus.CONNECTING -> Color.Yellow
                            },
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Camera details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Type: ${camera.type.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (camera.status) {
                        CameraStatus.CONNECTED -> MaterialTheme.colorScheme.onSecondaryContainer
                        CameraStatus.STREAMING -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Text(
                    text = "Port: ${camera.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (camera.status) {
                        CameraStatus.CONNECTED -> MaterialTheme.colorScheme.onSecondaryContainer
                        CameraStatus.STREAMING -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (camera.status) {
                    CameraStatus.DISCONNECTED -> {
                        Button(
                            onClick = onConnect,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Connect")
                        }
                    }
                    
                    CameraStatus.CONNECTED -> {
                        Button(
                            onClick = onStartStream,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stream")
                        }
                        
                        OutlinedButton(
                            onClick = onDisconnect,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LinkOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disconnect")
                        }
                    }
                    
                    CameraStatus.STREAMING -> {
                        Button(
                            onClick = onDisconnect,
                            modifier = Modifier.fillMaxWidth(),
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
                            Text("Stop Stream")
                        }
                    }
                    
                    CameraStatus.CONNECTING -> {
                        Button(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionHistoryCard(
    history: ConnectionHistory
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                imageVector = when (history.wasSuccessful) {
                    true -> Icons.Default.CheckCircle
                    false -> Icons.Default.Error
                },
                contentDescription = "Connection result",
                modifier = Modifier.size(20.dp),
                tint = if (history.wasSuccessful) Color.Green else Color.Red
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = history.cameraName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "${history.timestamp} - ${if (history.wasSuccessful) "Connected" else "Failed"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DemoControlsCard(
    onStartDemo: () -> Unit,
    onStopDemo: () -> Unit,
    isDemoRunning: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Demo Controls",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isDemoRunning) {
                Button(
                    onClick = onStopDemo,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Demo")
                }
            } else {
                Button(
                    onClick = onStartDemo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Network Camera Demo")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isDemoRunning) {
                    "Demo is running - simulating network camera discovery and streaming"
                } else {
                    "Start demo to simulate network camera functionality"
                },
                style = MaterialTheme.typography.bodySmall,
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

// Data classes
data class NetworkInfo(
    val isConnected: Boolean,
    val ipAddress: String = "",
    val subnet: String = ""
)

data class StreamingInfo(
    val sourceName: String,
    val resolution: String,
    val fps: Int,
    val signalStrength: Int
)

data class NetworkCamera(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val type: CameraType,
    val status: CameraStatus
)

data class ConnectionHistory(
    val cameraName: String,
    val timestamp: String,
    val wasSuccessful: Boolean
)

enum class CameraType {
    THERMAL, RGB, IP_CAMERA
}

enum class CameraStatus {
    DISCONNECTED, CONNECTING, CONNECTED, STREAMING
}