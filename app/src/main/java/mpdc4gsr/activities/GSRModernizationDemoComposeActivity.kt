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
 * Modern Compose implementation of GSR Modernization Demo
 * Showcases the GSR sensor system modernization with Compose
 */
class GSRModernizationDemoComposeActivity : BaseComposeActivity<GSRModernizationDemoViewModel>() {
    
    override fun createViewModel(): GSRModernizationDemoViewModel = 
        viewModels<GSRModernizationDemoViewModel>().value
    
    @Composable
    override fun Content(viewModel: GSRModernizationDemoViewModel) {
        IRCameraTheme {
            GSRModernizationDemoScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}

@Composable
fun GSRModernizationDemoScreen(
    viewModel: GSRModernizationDemoViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Material 3 App Bar
        CenterAlignedTopAppBar(
            title = { Text("GSR Modernization Demo") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.startGSRDemo() }) {
                    Icon(
                        imageVector = if (uiState.isDemoRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isDemoRunning) "Stop demo" else "Start demo",
                        tint = if (uiState.isDemoRunning) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = { viewModel.simulateGSRData() }) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Simulate GSR data"
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
            // GSR System Overview
            item {
                GSRSystemOverviewCard(
                    systemStatus = uiState.gsrSystemStatus,
                    onRefreshStatus = { viewModel.refreshGSRStatus() }
                )
            }
            
            // Live GSR Data
            if (uiState.isSimulatingData) {
                item {
                    LiveGSRDataCard(
                        gsrData = uiState.currentGSRData,
                        onStopSimulation = { viewModel.stopDataSimulation() }
                    )
                }
            }
            
            // Modernization Features
            item {
                Text(
                    text = "Modernization Features",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            items(uiState.modernizationFeatures) { feature ->
                ModernizationFeatureCard(
                    feature = feature,
                    onDemoFeature = { viewModel.demoFeature(feature) }
                )
            }
            
            // GSR Activities Showcase
            if (uiState.gsrActivities.isNotEmpty()) {
                item {
                    Text(
                        text = "Modernized GSR Activities",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                items(uiState.gsrActivities) { activity ->
                    GSRActivityCard(
                        activity = activity,
                        onLaunchActivity = { viewModel.launchGSRActivity(activity) }
                    )
                }
            }
            
            // Performance Metrics
            item {
                GSRPerformanceMetricsCard(
                    metrics = uiState.performanceMetrics
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
private fun GSRSystemOverviewCard(
    systemStatus: GSRSystemStatus,
    onRefreshStatus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (systemStatus.connectionStatus) {
                GSRConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                GSRConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiaryContainer
                GSRConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.errorContainer
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
                        text = "GSR System Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = when (systemStatus.connectionStatus) {
                            GSRConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.onPrimaryContainer
                            GSRConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.onTertiaryContainer
                            GSRConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    
                    Text(
                        text = systemStatus.connectionStatus.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (systemStatus.connectionStatus) {
                            GSRConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.onPrimaryContainer
                            GSRConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.onTertiaryContainer
                            GSRConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
                
                IconButton(onClick = onRefreshStatus) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh status",
                        tint = when (systemStatus.connectionStatus) {
                            GSRConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.onPrimaryContainer
                            GSRConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.onTertiaryContainer
                            GSRConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }
            
            if (systemStatus.connectionStatus == GSRConnectionStatus.CONNECTED) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GSRMetric(
                        label = "Device",
                        value = systemStatus.deviceName,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    GSRMetric(
                        label = "Battery",
                        value = "${systemStatus.batteryLevel}%",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    GSRMetric(
                        label = "Sample Rate",
                        value = systemStatus.sampleRate,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun GSRMetric(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
private fun LiveGSRDataCard(
    gsrData: GSRDataPoint,
    onStopSimulation: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Live GSR Data",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    Text(
                        text = "Real-time sensor simulation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Button(
                    onClick = onStopSimulation,
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // GSR Value Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${gsrData.value} μS",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "GSR Value",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = (gsrData.value / 100f).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Timestamp: ${gsrData.timestamp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Text(
                    text = "Quality: ${gsrData.quality}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernizationFeatureCard(
    feature: ModernizationFeature,
    onDemoFeature: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (feature.isImplemented) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
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
                        imageVector = if (feature.isImplemented) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        contentDescription = "Status",
                        modifier = Modifier.size(24.dp),
                        tint = if (feature.isImplemented) Color.Green else Color.Yellow
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = feature.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (feature.isImplemented) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        
                        Text(
                            text = if (feature.isImplemented) "Implemented" else "In Progress",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (feature.isImplemented) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                
                if (feature.isImplemented) {
                    Button(
                        onClick = onDemoFeature
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Demo")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (feature.isImplemented) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            if (feature.benefits.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                feature.benefits.forEach { benefit ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Benefit",
                            modifier = Modifier.size(12.dp),
                            tint = Color.Green
                        )
                        
                        Spacer(modifier = Modifier.width(6.dp))
                        
                        Text(
                            text = benefit,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (feature.isImplemented) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GSRActivityCard(
    activity: GSRActivity,
    onLaunchActivity: () -> Unit
) {
    Card(
        onClick = onLaunchActivity,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (activity.type) {
                    GSRActivityType.DATA_VIEW -> Icons.Default.Analytics
                    GSRActivityType.SETTINGS -> Icons.Default.Settings
                    GSRActivityType.EXPORT -> Icons.Default.Download
                    GSRActivityType.DEVICE_MANAGEMENT -> Icons.Default.Devices
                    GSRActivityType.RECORDING -> Icons.Default.FiberManualRecord
                },
                contentDescription = "Activity type",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = activity.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (activity.modernizationStatus == ModernizationStatus.COMPLETE) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Modernized",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Green
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Modernized with Compose",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Green
                        )
                    }
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Launch",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GSRPerformanceMetricsCard(
    metrics: GSRPerformanceMetrics
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Improvements",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PerformanceMetric(
                    label = "UI Responsiveness",
                    value = "+${metrics.uiResponsivenessImprovement}%",
                    color = Color.Green
                )
                
                PerformanceMetric(
                    label = "Memory Usage",
                    value = "-${metrics.memoryReduction}%",
                    color = Color.Green
                )
                
                PerformanceMetric(
                    label = "Battery Life",
                    value = "+${metrics.batteryImprovement}%",
                    color = Color.Green
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Data Processing: ${metrics.dataProcessingRate} samples/sec",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PerformanceMetric(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
data class GSRSystemStatus(
    val connectionStatus: GSRConnectionStatus,
    val deviceName: String,
    val batteryLevel: Int,
    val sampleRate: String
)

data class GSRDataPoint(
    val value: Float,
    val timestamp: String,
    val quality: String
)

data class ModernizationFeature(
    val title: String,
    val description: String,
    val isImplemented: Boolean,
    val benefits: List<String>
)

data class GSRActivity(
    val name: String,
    val description: String,
    val type: GSRActivityType,
    val modernizationStatus: ModernizationStatus
)

data class GSRPerformanceMetrics(
    val uiResponsivenessImprovement: Int,
    val memoryReduction: Int,
    val batteryImprovement: Int,
    val dataProcessingRate: Int
)

enum class GSRConnectionStatus(val displayName: String) {
    CONNECTED("Connected"),
    CONNECTING("Connecting"),
    DISCONNECTED("Disconnected")
}

enum class GSRActivityType {
    DATA_VIEW, SETTINGS, EXPORT, DEVICE_MANAGEMENT, RECORDING
}

enum class ModernizationStatus {
    COMPLETE, IN_PROGRESS, PLANNED
}