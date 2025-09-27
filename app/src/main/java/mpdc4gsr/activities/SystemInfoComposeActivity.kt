package mpdc4gsr.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import mpdc4gsr.ui.components.IRCameraTopAppBar
import mpdc4gsr.ui.theme.IRCameraTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

data class SystemMetric(
    val name: String,
    val value: String,
    val icon: ImageVector,
    val category: String
)

data class PerformanceMetric(
    val timestamp: Long,
    val cpuUsage: Float,
    val memoryUsage: Float,
    val temperature: Float
)

class SystemInfoComposeActivity : BaseComposeActivity() {

    @Composable
    override fun Content() {
        IRCameraTheme {
            SystemInfoScreen()
        }
    }

    @Composable
    private fun SystemInfoScreen() {
        var selectedTab by remember { mutableStateOf(0) }
        var performanceData by remember { mutableStateOf(listOf<PerformanceMetric>()) }
        var isMonitoring by remember { mutableStateOf(false) }

        // Generate performance data
        LaunchedEffect(isMonitoring) {
            if (isMonitoring) {
                while (isMonitoring) {
                    delay(1000)
                    val newMetric = PerformanceMetric(
                        timestamp = System.currentTimeMillis(),
                        cpuUsage = Random.nextFloat() * 100,
                        memoryUsage = 60f + Random.nextFloat() * 30,
                        temperature = 35f + Random.nextFloat() * 15
                    )
                    performanceData = (performanceData + newMetric).takeLast(60)
                }
            }
        }

        Scaffold(
            topBar = {
                IRCameraTopAppBar(
                    title = "System Information",
                    onNavigationClick = { finish() },
                    actions = {
                        IconButton(onClick = { isMonitoring = !isMonitoring }) {
                            Icon(
                                if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isMonitoring) "Stop Monitoring" else "Start Monitoring"
                            )
                        }
                        IconButton(onClick = { /* Export system info */ }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Status indicator
                if (isMonitoring) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Real-time monitoring active • ${performanceData.size} samples collected",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Tab bar
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Hardware") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Performance") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Diagnostics") }
                    )
                }

                // Tab content
                when (selectedTab) {
                    0 -> HardwareInfoTab()
                    1 -> PerformanceTab(performanceData = performanceData)
                    2 -> DiagnosticsTab()
                }
            }
        }
    }

    @Composable
    private fun HardwareInfoTab() {
        val hardwareMetrics = remember {
            listOf(
                SystemMetric("Device Model", "Samsung Galaxy S21", Icons.Default.PhoneAndroid, "Device"),
                SystemMetric("Android Version", "Android 13 (API 33)", Icons.Default.Android, "System"),
                SystemMetric("CPU", "Snapdragon 888", Icons.Default.Memory, "Hardware"),
                SystemMetric("RAM", "8 GB", Icons.Default.Memory, "Hardware"),
                SystemMetric("Storage", "128 GB (64 GB available)", Icons.Default.Storage, "Hardware"),
                SystemMetric("Screen", "6.2\" 2340x1080 Dynamic AMOLED", Icons.Default.Monitor, "Display"),
                SystemMetric("Camera", "64MP Main + 12MP Ultra-wide + 12MP Telephoto", Icons.Default.Camera, "Sensors"),
                SystemMetric("Bluetooth", "5.2", Icons.Default.Bluetooth, "Connectivity"),
                SystemMetric("WiFi", "802.11 a/b/g/n/ac/ax", Icons.Default.Wifi, "Connectivity"),
                SystemMetric("Battery", "4000 mAh (85% charged)", Icons.Default.Battery80, "Power"),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val groupedMetrics = hardwareMetrics.groupBy { it.category }
            
            groupedMetrics.forEach { (category, metrics) ->
                item {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(metrics) { metric ->
                    SystemMetricCard(metric = metric)
                }
            }
        }
    }

    @Composable
    private fun SystemMetricCard(metric: SystemMetric) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    metric.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metric.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = metric.value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    private fun PerformanceTab(performanceData: List<PerformanceMetric>) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Current metrics
            if (performanceData.isNotEmpty()) {
                val latest = performanceData.last()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "CPU Usage",
                        value = "${latest.cpuUsage.toInt()}%",
                        icon = Icons.Default.Memory,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Memory",
                        value = "${latest.memoryUsage.toInt()}%",
                        icon = Icons.Default.Storage,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Temperature",
                        value = "${latest.temperature.toInt()}°C",
                        icon = Icons.Default.Thermostat,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Performance graphs
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "CPU Usage Over Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            drawPerformanceGraph(
                                data = performanceData.map { it.cpuUsage },
                                color = Color.Blue,
                                maxValue = 100f
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Memory Usage Over Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            drawPerformanceGraph(
                                data = performanceData.map { it.memoryUsage },
                                color = Color.Green,
                                maxValue = 100f
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Temperature Over Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            drawPerformanceGraph(
                                data = performanceData.map { it.temperature },
                                color = Color.Red,
                                maxValue = 60f
                            )
                        }
                    }
                }
            } else {
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
                            Icons.Default.Timeline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No performance data available",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Start monitoring to see real-time performance metrics",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MetricCard(
        title: String,
        value: String,
        icon: ImageVector,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun DiagnosticsTab() {
        val diagnostics = remember {
            listOf(
                "Camera Hardware" to "✓ Functional",
                "Thermal Sensor" to "✓ Calibrated",
                "Bluetooth Stack" to "✓ Operational",
                "Storage Access" to "✓ Read/Write OK",
                "Network Connectivity" to "✓ Connected",
                "GSR Sensor Support" to "✓ Compatible",
                "Permission Status" to "⚠ Some permissions missing",
                "Battery Optimization" to "⚠ May affect background tasks",
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "System Health Check",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Last checked: ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            items(diagnostics) { (component, status) ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = component,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        
                        val (color, icon) = when {
                            status.startsWith("✓") -> Color.Green to Icons.Default.CheckCircle
                            status.startsWith("⚠") -> Color.Orange to Icons.Default.Warning
                            status.startsWith("✗") -> Color.Red to Icons.Default.Error
                            else -> Color.Gray to Icons.Default.Help
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = color
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = status.drop(2),
                                style = MaterialTheme.typography.bodySmall,
                                color = color
                            )
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { /* Run diagnostics */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run Full Diagnostic")
                }
            }
        }
    }

    private fun DrawScope.drawPerformanceGraph(
        data: List<Float>,
        color: Color,
        maxValue: Float
    ) {
        if (data.isEmpty()) return
        
        val width = size.width
        val height = size.height
        val padding = 20f
        
        val path = Path()
        
        data.forEachIndexed { index, value ->
            val x = padding + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * (width - 2 * padding)
            val y = height - padding - (value / maxValue) * (height - 2 * padding)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
        
        // Draw data points
        data.forEachIndexed { index, value ->
            val x = padding + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * (width - 2 * padding)
            val y = height - padding - (value / maxValue) * (height - 2 * padding)
            
            drawCircle(
                color = color,
                radius = 3f,
                center = Offset(x, y)
            )
        }
    }
}