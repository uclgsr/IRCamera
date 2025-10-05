package com.mpdc4gsr.module.thermalunified.activity

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class ThermalMonitoringComposeActivity : BaseComposeActivity<BaseViewModel>() {

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, ThermalMonitoringComposeActivity::class.java))
        }
    }

    override fun createViewModel(): BaseViewModel {
        return viewModels<BaseViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: BaseViewModel) {
        var selectedTab by remember { mutableStateOf(0) }
        var isMonitoring by remember { mutableStateOf(false) }
        var showAlertDialog by remember { mutableStateOf(false) }

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Monitoring",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            IconButton(onClick = { isMonitoring = !isMonitoring }) {
                                Icon(
                                    if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isMonitoring) "Stop Monitoring" else "Start Monitoring"
                                )
                            }
                            IconButton(onClick = { showAlertDialog = true }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                            }
                            IconButton(onClick = { 
                                // TODO: Implement monitoring options menu
                                android.widget.Toast.makeText(context, "More monitoring options", android.widget.Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                },
                bottomBar = {
                    MonitoringNavigationBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            ) { paddingValues ->
                MonitoringContent(
                    selectedTab = selectedTab,
                    isMonitoring = isMonitoring,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        if (showAlertDialog) {
            AlertConfigurationDialog(
                onDismiss = { showAlertDialog = false },
                onSaveAlerts = { alerts ->
                    // Save alert configuration
                    showAlertDialog = false
                }
            )
        }
    }
}

@Composable
private fun MonitoringContent(
    selectedTab: Int,
    isMonitoring: Boolean,
    modifier: Modifier = Modifier
) {
    when (selectedTab) {
        0 -> RealTimeMonitoringTab(
            isMonitoring = isMonitoring,
            modifier = modifier
        )

        1 -> ThermalAnalyticsTab(modifier = modifier)
        2 -> AlertsHistoryTab(modifier = modifier)
        3 -> MonitoringSettingsTab(modifier = modifier)
    }
}

@Composable
private fun RealTimeMonitoringTab(
    isMonitoring: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Monitoring status
        MonitoringStatusCard(
            isMonitoring = isMonitoring,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Temperature zones
        Text(
            text = "Temperature Zones",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val temperatureZones = getMockTemperatureZones()
        temperatureZones.forEach { zone ->
            TemperatureZoneCard(
                zone = zone,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Recent alerts
        Text(
            text = "Recent Alerts",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
        )

        val recentAlerts = getMockAlerts()
        if (recentAlerts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "No recent alerts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            recentAlerts.take(3).forEach { alert ->
                AlertCard(
                    alert = alert,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MonitoringStatusCard(
    isMonitoring: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isMonitoring)
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isMonitoring) "MONITORING ACTIVE" else "MONITORING STOPPED",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isMonitoring) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isMonitoring) "4 zones being monitored" else "Click start to begin monitoring",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isMonitoring) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
            }
        }
    }
}

@Composable
private fun TemperatureZoneCard(
    zone: TemperatureZone,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (zone.status) {
                "normal" -> MaterialTheme.colorScheme.surface
                "warning" -> Color(0xFFFFF3E0)
                "critical" -> Color(0xFFFFEBEE)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Zone indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(getZoneStatusColor(zone.status))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zone.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Current: ${zone.currentTemp}°C | Threshold: ${zone.threshold}°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${zone.currentTemp}°C",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = getTemperatureColor(zone.currentTemp, zone.threshold)
                )
                Text(
                    text = zone.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = getZoneStatusColor(zone.status),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: ThermalAlert,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (alert.severity) {
                "high" -> Color(0xFFFFEBEE)
                "medium" -> Color(0xFFFFF3E0)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (alert.severity) {
                    "high" -> Icons.Default.Error
                    "medium" -> Icons.Default.Warning
                    else -> Icons.Default.Info
                },
                contentDescription = alert.severity,
                tint = when (alert.severity) {
                    "high" -> Color(0xFFE53E3E)
                    "medium" -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${alert.zone} • ${alert.timestamp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThermalAnalyticsTab(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Analytics,
            contentDescription = "Analytics",
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFFF6B35)
        )
        Text(
            text = "Thermal Analytics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Advanced thermal data analysis and trends",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AlertsHistoryTab(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Alert History",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val allAlerts = getMockAlerts()
            items(allAlerts) { alert ->
                AlertCard(alert = alert)
            }
        }
    }
}

@Composable
private fun MonitoringSettingsTab(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Settings,
            contentDescription = "Settings",
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFFF6B35)
        )
        Text(
            text = "Monitoring Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configure monitoring parameters and alerts",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonitoringNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        val tabs = listOf(
            "Monitor" to Icons.Default.Monitor,
            "Analytics" to Icons.Default.Analytics,
            "Alerts" to Icons.Default.Notifications,
            "Settings" to Icons.Default.Settings
        )

        tabs.forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun AlertConfigurationDialog(
    onDismiss: () -> Unit,
    onSaveAlerts: (Map<String, Any>) -> Unit
) {
    var highTempThreshold by remember { mutableStateOf(80f) }
    var lowTempThreshold by remember { mutableStateOf(0f) }
    var enableNotifications by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alert Configuration") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "High Temperature Alert: ${highTempThreshold.toInt()}°C",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Slider(
                    value = highTempThreshold,
                    onValueChange = { highTempThreshold = it },
                    valueRange = 30f..150f
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Low Temperature Alert: ${lowTempThreshold.toInt()}°C",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Slider(
                    value = lowTempThreshold,
                    onValueChange = { lowTempThreshold = it },
                    valueRange = -20f..30f
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = enableNotifications,
                        onCheckedChange = { enableNotifications = it }
                    )
                    Text(
                        text = "Enable push notifications",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSaveAlerts(
                        mapOf(
                            "highThreshold" to highTempThreshold,
                            "lowThreshold" to lowTempThreshold,
                            "notifications" to enableNotifications
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getZoneStatusColor(status: String) = when (status) {
    "normal" -> Color(0xFF4CAF50)
    "warning" -> Color(0xFFFF9800)
    "critical" -> Color(0xFFE53E3E)
    else -> Color(0xFF9E9E9E)
}

private fun getTemperatureColor(current: Float, threshold: Float) = when {
    current > threshold * 1.1f -> Color(0xFFE53E3E)
    current > threshold * 0.9f -> Color(0xFFFF9800)
    else -> Color(0xFF4CAF50)
}

data class TemperatureZone(
    val name: String,
    val currentTemp: Float,
    val threshold: Float,
    val status: String
)

data class ThermalAlert(
    val message: String,
    val zone: String,
    val timestamp: String,
    val severity: String
)

private fun getMockTemperatureZones() = listOf(
    TemperatureZone("Zone A - Engine Bay", 76.5f, 80.0f, "warning"),
    TemperatureZone("Zone B - Electronics", 42.3f, 60.0f, "normal"),
    TemperatureZone("Zone C - Exhaust", 95.2f, 90.0f, "critical"),
    TemperatureZone("Zone D - Ambient", 24.8f, 40.0f, "normal")
)

private fun getMockAlerts() = listOf(
    ThermalAlert("Temperature exceeded threshold", "Zone C - Exhaust", "2 min ago", "high"),
    ThermalAlert("Warning temperature detected", "Zone A - Engine Bay", "5 min ago", "medium"),
    ThermalAlert("Normal operating temperature", "Zone B - Electronics", "10 min ago", "low")
)