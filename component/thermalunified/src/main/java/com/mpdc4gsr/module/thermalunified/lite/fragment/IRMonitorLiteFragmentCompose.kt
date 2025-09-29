package com.mpdc4gsr.module.thermalunified.lite.fragment

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.ir.view.TemperatureView
import com.mpdc4gsr.module.thermalunified.lite.viewmodel.IRMonitorLiteViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Compose migration of IRMonitorLiteFragment
 * 
 * This fragment demonstrates:
 * - Complete migration of lite monitoring interface to Compose
 * - Simplified thermal monitoring for basic use cases
 * - Modern Material 3 UI with essential monitoring features
 * - Efficient resource usage for lite implementation
 * - Integration with USB thermal devices
 */
class IRMonitorLiteFragmentCompose : BaseComposeFragment<IRMonitorLiteViewModel>() {

    override fun createViewModel(): IRMonitorLiteViewModel {
        return viewModels<IRMonitorLiteViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRMonitorLiteViewModel) {
        // Observe ViewModel state
        val monitoringState by viewModel.monitoringState.collectAsStateWithLifecycle()
        val temperatureData by viewModel.temperatureData.collectAsStateWithLifecycle()
        val deviceConnectionState by viewModel.deviceConnectionState.collectAsStateWithLifecycle()
        val monitoringData by viewModel.monitoringData.collectAsStateWithLifecycle()

        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Lite monitoring status bar
                LiteMonitoringStatusBar(
                    monitoringState = monitoringState,
                    deviceConnectionState = deviceConnectionState,
                    onToggleMonitoring = { viewModel.toggleMonitoring() }
                )

                // Main monitoring interface
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Thermal monitoring view (simplified)
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                    ) {
                        LiteThermalView(
                            temperatureData = temperatureData,
                            deviceConnectionState = deviceConnectionState
                        )

                        // Simple temperature overlay
                        TemperatureDisplayOverlay(
                            temperatureData = temperatureData,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )

                        // Device status indicator
                        DeviceStatusIndicator(
                            connectionState = deviceConnectionState,
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    }

                    // Lite controls panel
                    LiteControlsPanel(
                        monitoringState = monitoringState,
                        monitoringData = monitoringData,
                        onStartMonitoring = { viewModel.startMonitoring() },
                        onStopMonitoring = { viewModel.stopMonitoring() },
                        onClearData = { viewModel.clearMonitoringData() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    @Composable
    private fun LiteMonitoringStatusBar(
        monitoringState: MonitoringState,
        deviceConnectionState: DeviceConnectionState,
        onToggleMonitoring: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (monitoringState) {
                    MonitoringState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                    MonitoringState.INACTIVE -> MaterialTheme.colorScheme.surfaceVariant
                    MonitoringState.ERROR -> MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Lite Thermal Monitor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatusChip(
                            text = getConnectionStatusText(deviceConnectionState),
                            color = getConnectionStatusColor(deviceConnectionState)
                        )
                        StatusChip(
                            text = getMonitoringStatusText(monitoringState),
                            color = getMonitoringStatusColor(monitoringState)
                        )
                    }
                }

                Switch(
                    checked = monitoringState == MonitoringState.ACTIVE,
                    onCheckedChange = { onToggleMonitoring() },
                    enabled = deviceConnectionState == DeviceConnectionState.CONNECTED
                )
            }
        }
    }

    @Composable
    private fun StatusChip(
        text: String,
        color: Color
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    @Composable
    private fun LiteThermalView(
        temperatureData: TemperatureData?,
        deviceConnectionState: DeviceConnectionState
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (deviceConnectionState == DeviceConnectionState.CONNECTED) {
                    // Temperature view integration (simplified for lite version)
                    AndroidView(
                        factory = { context ->
                            TemperatureView(context).apply {
                                // Configure for lite monitoring
                                setRegionMode(TemperatureView.REGION_MODE_POINT)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Connection placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "No connection",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when (deviceConnectionState) {
                                    DeviceConnectionState.DISCONNECTED -> "Device Not Connected"
                                    DeviceConnectionState.CONNECTING -> "Connecting..."
                                    DeviceConnectionState.ERROR -> "Connection Error"
                                    else -> "No Signal"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (deviceConnectionState == DeviceConnectionState.CONNECTING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TemperatureDisplayOverlay(
        temperatureData: TemperatureData?,
        modifier: Modifier = Modifier
    ) {
        temperatureData?.let { data ->
            Column(
                modifier = modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LiteTemperatureCard(
                    label = "Current",
                    temperature = "${data.currentTemp}°C",
                    isMain = true
                )
                
                LiteTemperatureCard(
                    label = "Max",
                    temperature = "${data.maxTemp}°C",
                    color = Color.Red
                )
                
                LiteTemperatureCard(
                    label = "Min",
                    temperature = "${data.minTemp}°C",
                    color = Color.Blue
                )
            }
        }
    }

    @Composable
    private fun LiteTemperatureCard(
        label: String,
        temperature: String,
        isMain: Boolean = false,
        color: Color = Color.White
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isMain) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
                Text(
                    text = temperature,
                    style = if (isMain) 
                        MaterialTheme.typography.titleSmall 
                    else 
                        MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }

    @Composable
    private fun DeviceStatusIndicator(
        connectionState: DeviceConnectionState,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = getConnectionStatusColor(connectionState).copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            getConnectionStatusColor(connectionState),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Text(
                    text = getConnectionStatusText(connectionState),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }

    @Composable
    private fun LiteControlsPanel(
        monitoringState: MonitoringState,
        monitoringData: MonitoringData?,
        onStartMonitoring: () -> Unit,
        onStopMonitoring: () -> Unit,
        onClearData: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Lite Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    // Simple control buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (monitoringState != MonitoringState.ACTIVE) {
                            Button(
                                onClick = onStartMonitoring,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Monitoring")
                            }
                        } else {
                            Button(
                                onClick = onStopMonitoring,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Stop Monitoring")
                            }
                        }
                    }
                }

                item {
                    Divider()
                }

                item {
                    // Monitoring data display
                    Text(
                        text = "Session Data",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                monitoringData?.let { data ->
                    items(data.getDataItems()) { dataItem ->
                        LiteDataRow(
                            label = dataItem.first,
                            value = dataItem.second
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    OutlinedButton(
                        onClick = onClearData,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Data")
                    }
                }
            }
        }
    }

    @Composable
    private fun LiteDataRow(
        label: String,
        value: String
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }

    // Helper functions
    private fun getConnectionStatusText(state: DeviceConnectionState): String = when (state) {
        DeviceConnectionState.CONNECTED -> "Connected"
        DeviceConnectionState.CONNECTING -> "Connecting"
        DeviceConnectionState.DISCONNECTED -> "Disconnected"
        DeviceConnectionState.ERROR -> "Error"
    }

    private fun getConnectionStatusColor(state: DeviceConnectionState): Color = when (state) {
        DeviceConnectionState.CONNECTED -> Color.Green
        DeviceConnectionState.CONNECTING -> Color(0xFFFFA500)
        DeviceConnectionState.DISCONNECTED -> Color.Gray
        DeviceConnectionState.ERROR -> Color.Red
    }

    private fun getMonitoringStatusText(state: MonitoringState): String = when (state) {
        MonitoringState.ACTIVE -> "Active"
        MonitoringState.INACTIVE -> "Inactive"
        MonitoringState.ERROR -> "Error"
    }

    private fun getMonitoringStatusColor(state: MonitoringState): Color = when (state) {
        MonitoringState.ACTIVE -> Color.Green
        MonitoringState.INACTIVE -> Color.Gray
        MonitoringState.ERROR -> Color.Red
    }

    // Data classes and enums
    data class TemperatureData(
        val currentTemp: Float,
        val maxTemp: Float,
        val minTemp: Float
    )

    data class MonitoringData(
        val duration: String,
        val sampleCount: Int,
        val averageTemp: Float,
        val startTime: Long
    ) {
        fun getDataItems(): List<Pair<String, String>> = listOf(
            "Duration" to duration,
            "Samples" to sampleCount.toString(),
            "Average" to "${String.format("%.1f", averageTemp)}°C",
            "Started" to SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(startTime))
        )
    }

    enum class DeviceConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    enum class MonitoringState {
        INACTIVE, ACTIVE, ERROR
    }
}