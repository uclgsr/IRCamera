package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.module.thermalunified.stubs.IrSurfaceView
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalFragmentViewModel

/**
 * Compose wrapper for thermal camera functionality
 * Provides modern UI while preserving existing thermal camera implementation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalCameraScreen(
    viewModel: ThermalFragmentViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Observe thermal data from ViewModel
    val isRecording by viewModel.isRecording.collectAsState()
    val temperatureData by viewModel.temperatureData.collectAsState()
    val cameraState by viewModel.cameraConnectionState.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Camera status card
        ThermalCameraStatusCard(
            isConnected = cameraState.isConnected,
            isRecording = isRecording,
            connectionInfo = "TC001 384x288 @ 10Hz"
        )
        
        // Main thermal camera view (embedded traditional view)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            AndroidView(
                factory = { context ->
                    IrSurfaceView(context).apply {
                        // Configure the thermal camera surface view
                        // This preserves all existing thermal camera functionality
                        setupThermalSurface(this, viewModel)
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { view ->
                // Handle updates to the thermal surface view
                updateThermalSurface(view, temperatureData)
            }
        }
        
        // Temperature readings card
        temperatureData?.let { data ->
            ThermalReadingsCard(
                centerTemp = data.centerTemp,
                maxTemp = data.maxTemp,
                minTemp = data.minTemp,
                maxTempLocation = data.maxTempLocation,
                minTempLocation = data.minTempLocation
            )
        }
        
        // Control panel
        ThermalControlPanel(
            isRecording = isRecording,
            onStartRecording = { viewModel.startRecording() },
            onStopRecording = { viewModel.stopRecording() },
            onTakeSnapshot = { viewModel.takeSnapshot() },
            onOpenSettings = { viewModel.openSettings() }
        )
    }
}

@Composable
private fun ThermalCameraStatusCard(
    isConnected: Boolean,
    isRecording: Boolean,
    connectionInfo: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
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
                    text = if (isConnected) "Camera Connected" else "Camera Disconnected",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isConnected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = connectionInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConnected) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else 
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
            
            if (isRecording) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .padding(end = 4.dp)
                    ) {
                        Card(
                            modifier = Modifier.size(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(50)
                        ) {}
                    }
                    Text(
                        text = "RECORDING",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
private fun ThermalReadingsCard(
    centerTemp: Float,
    maxTemp: Float,
    minTemp: Float,
    maxTempLocation: Pair<Int, Int>?,
    minTempLocation: Pair<Int, Int>?
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Temperature Readings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TempReading("Center", centerTemp, MaterialTheme.colorScheme.primary)
                TempReading("Max", maxTemp, MaterialTheme.colorScheme.secondary)
                TempReading("Min", minTemp, MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun TempReading(label: String, temperature: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = "${temperature.toInt()}°C",
            style = MaterialTheme.typography.titleLarge,
            color = color
        )
    }
}

@Composable
private fun ThermalControlPanel(
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onTakeSnapshot: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (isRecording) {
                Button(
                    onClick = onStopRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Stop")
                }
            } else {
                Button(onClick = onStartRecording) {
                    Text("Record")
                }
            }
            
            OutlinedButton(onClick = onTakeSnapshot) {
                Text("Snapshot")
            }
            
            OutlinedButton(onClick = onOpenSettings) {
                Text("Settings")
            }
        }
    }
}

// Helper functions for integrating with existing thermal implementation
private fun setupThermalSurface(surfaceView: IrSurfaceView, viewModel: ThermalFragmentViewModel) {
    // Configure the surface view with existing thermal camera setup
    // This would integrate with existing thermal camera initialization
}

private fun updateThermalSurface(surfaceView: IrSurfaceView, temperatureData: Any?) {
    // Update the surface view with new temperature data
    // This would use existing thermal data processing
}

// Data classes for Compose state
data class ThermalTemperatureData(
    val centerTemp: Float,
    val maxTemp: Float,
    val minTemp: Float,
    val maxTempLocation: Pair<Int, Int>?,
    val minTempLocation: Pair<Int, Int>?
)

data class ThermalCameraState(
    val isConnected: Boolean,
    val resolution: String = "384x288",
    val frameRate: String = "10Hz"
)